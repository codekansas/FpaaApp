package hasler.fpaaapp.views;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import hasler.fpaaapp.ControllerActivity;
import hasler.fpaaapp.R;

public class DacAdcView extends Fragment {
    private final String TAG = "DacAdcView";

    private ControllerActivity parentContext;
    private hasler.fpaaapp.utils.Driver driver;

    public static DacAdcView newInstance() {
        return new DacAdcView();
    }
    public DacAdcView() { /* Required empty public constructor */ }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get parent context and instructions to execute
        parentContext = (ControllerActivity) getActivity();
        driver = new hasler.fpaaapp.utils.Driver(parentContext);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) return null;
        super.onCreate(savedInstanceState);

        // Inflate the view XML file
        final View view = inflater.inflate(R.layout.fragment_dac_adc, container, false);

        // Progress bar (used by both processes)
        final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        final Button programDesignButton = (Button) view.findViewById(R.id.program_design_button);
        final Button getDataButton = (Button) view.findViewById(R.id.get_data_button);

        /***********
         * PROGRAM *
         ***********/

        programDesignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDataButton.setEnabled(false);
                programDesignButton.setEnabled(false);
                progressBar.setProgress(0);

                progressBar.setProgress(100);
                getDataButton.setEnabled(true);
                programDesignButton.setEnabled(true);
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

                progressBar.setProgress(100);
                getDataButton.setEnabled(true);
                programDesignButton.setEnabled(true);
            }
        });

        return view;
    }

    public void onConnect() {
        driver.connect();
    }

    public void onDisconnect() {
        driver.disconnect();
    }

    @Override
    public void onStart() {
        super.onStart();
        onConnect();
    }

    @Override
    public void onStop() {
        onDisconnect();
        super.onStop();
    }
}
