package hasler.fpaaapp.views;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import hasler.fpaaapp.R;
import hasler.fpaaapp.utils.DriverFragment;
import hasler.fpaaapp.utils.Utils;

public class DacAdcView extends DriverFragment {
    private final String TAG = "DacAdcView";

    protected Handler mHandler = new Handler();
    protected ProgressBar progressBar;
    protected GraphView graph;

    public static DacAdcView newInstance() {
        return new DacAdcView();
    }
    public DacAdcView() { /* Required empty public constructor */ }

    public static int[] toInts(byte... b) {
        int[] t = new int[b.length / 2];
        for (int i = 0; i < t.length; i++) {
            t[i] = ((int) b[2*i] << 8) | ((int) b[2*i+1]);
        }
        return t;
    }

    public static double[] toDoubles(int... t) {
        double[] d = new double[t.length];
        for (int i = 0; i < t.length; i++) d[i] = (double) t[i];
        return d;
    }

    public static double[] toDoubles(byte... b) {
        return toDoubles(toInts(b));
    }

    public static double[] linspace(double low, double high, int num) {
        double[] d = new double[num];
        for (int i = 0; i < num; i++) {
            d[i] = (high - low) * i / num + low;
        }
        return d;
    }

    public static void plot(GraphView graph, double[] xvals, double[] yvals) {
        graph.removeAllSeries();

        DataPoint[] dataPoints = new DataPoint[xvals.length];
        for (int i = 0; i < xvals.length; i++) {
            dataPoints[i] = new DataPoint(xvals[i], yvals[i]);
        }

        LineGraphSeries<DataPoint> graphSeries = new LineGraphSeries<>(dataPoints);
        graph.addSeries(graphSeries);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) return null;
        super.onCreate(savedInstanceState);

        // Inflate the view XML file
        final View view = inflater.inflate(R.layout.fragment_dac_adc, container, false);

        // Progress bar (used by both processes)
        progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        graph = (GraphView) view.findViewById(R.id.graph);
        final Button programDesignButton = (Button) view.findViewById(R.id.program_design_button);
        final Button getDataButton = (Button) view.findViewById(R.id.get_data_button);

        /***********
         * PROGRAM *
         ***********/

        programDesignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ThreadRunnable() {
                    @Override
                    protected void onPreExecute() {
                        getDataButton.setEnabled(false);
                        programDesignButton.setEnabled(false);
                        progressBar.setProgress(0);
                    }

                    @TargetApi(Build.VERSION_CODES.KITKAT)
                    @Override
                    public Boolean doInBackground(Void... params) {
                        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        File file = new File(path, "dac_adc.zip");

                        // Download the file if it doesn't exist
                        if (!file.exists()) {
                            String url = "https://github.com/codekansas/designs/blob/master/dac_adc.zip?raw=true";
                            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                            request.setTitle("DAC ADC");
                            request.setDescription("Downloading the DAC ADC programming file");

                            request.allowScanningByMediaScanner();
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "dac_adc.zip");

                            DownloadManager manager = (DownloadManager) parentContext.getSystemService(Context.DOWNLOAD_SERVICE);
                            manager.enqueue(request);
                        }
                        updateProgressBar(10);

                        // Check to see if the file downloaded
                        int counter = 0, MAX_COUNTER = 100;
                        while (counter <= MAX_COUNTER && !file.exists()) {
                            counter++;
                            driver.sleep(10);
                        }
                        if (counter > MAX_COUNTER) {
                            makeToastMessage("Downloading programming files failed (or is taking too long)");
                            return null;
                        }
                        updateProgressBar(20);

                        byte[] data;
                        Map<String, byte[]> zipped_files = Utils.getZipContents(file.getAbsolutePath());

                        if (!driver.connect()) return false;

                        // Write the input vector
                        data = Utils.swapBytes(Utils.parseHexAscii(zipped_files.get("input_vector")));
                        if (!driver.writeMem(0x4300, data)) return false;
                        updateProgressBar(30);

                        // Write the output vector
                        data = Utils.swapBytes(Utils.parseHexAscii(zipped_files.get("output_info")));
                        if (!driver.writeMem(0x4200, data)) return false;
                        updateProgressBar(40);

                        // Compile the voltage measurement program
                        try {
                            data = Utils.compileElf(zipped_files.get("voltage_meas.elf"));
                        } catch (IOException e) {
                            makeToastMessage("Error while compiling voltage measurement program");
                            return false;
                        }
                        if (!driver.programData(data)) return false;
                        updateProgressBar(50);

                        driver.sleep(10000);

                        data = driver.readMem(0x6000, 24);
                        double[] xvals = linspace(0, 24, 24), yvals = toDoubles(data);
                        updateGraph(xvals, yvals);
                        updateProgressBar(60);

                        // Load and run the tunneling program
                        try {
                            data = Utils.compileElf(zipped_files.get("tunnel_revtun_SWC_CAB.elf"));
                        } catch (IOException e) {
                            makeToastMessage("Error while compiling tunneling program");
                            return false;
                        }
                        if (!driver.programData(data)) return false;
                        updateProgressBar(70);
                        driver.sleep(1000);

                        // Write the switch list: May have to swap the bytes for correct endianness
                        data = Utils.swapBytes(Utils.parseHexAscii(zipped_files.get("switch_info")));
                        if (!driver.writeMem(0x7000, data)) return false;
                        updateProgressBar(80);

                        // Run the switch program
                        try {
                            data = Utils.compileElf(zipped_files.get("switch_program.elf"));
                        } catch (IOException e) {
                            makeToastMessage("Error while compiling switch program");
                            return false;
                        }
                        if (!driver.programData(data)) return false;
                        updateProgressBar(90);
                        driver.sleep(1000 * 100);

                        data = driver.readMem(0x5000, 58);
                        if (data.length == 0) return false;
                        xvals = linspace(0, 58, 58);
                        yvals = toDoubles(data);
                        updateGraph(xvals, yvals);

                        return true;
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        super.onPostExecute(result);

                        if (!result) {
                            makeToastMessage("Error while trying to program the design");
                        }

                        progressBar.setProgress(100);
                        getDataButton.setEnabled(true);
                        programDesignButton.setEnabled(true);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        /************
         * GET DATA *
         ************/

        getDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDataButton.setEnabled(false);
                programDesignButton.setEnabled(false);
                progressBar.setProgress(0);

                // plot some dummy data
                plot(graph, new double[]{1.0, 2.0, 3.0}, new double[]{3.0, 2.0, 1.0});

                progressBar.setProgress(100);
                getDataButton.setEnabled(true);
                programDesignButton.setEnabled(true);
            }
        });

        return view;
    }

    protected abstract class ThreadRunnable extends AsyncTask<Void, Void, Boolean> {
        public void updateProgressBar(final int i) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressBar.setProgress(i);
                }
            });
        }

        public void updateGraph(final double[] xdata, final double[] ydata) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    plot(graph, xdata, ydata);
                }
            });
        }

        public void makeToastMessage(final String text) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(parentContext, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}
