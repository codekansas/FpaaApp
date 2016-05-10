package hasler.fpaaapp.views;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import hasler.fpaaapp.ControllerActivity;
import hasler.fpaaapp.R;
import hasler.fpaaapp.utils.Utils;

public class ReadWriteView extends Fragment {
    private final String TAG = "RunCodeFragment";

    private ControllerActivity parentContext;
    private hasler.fpaaapp.utils.Driver driver;

    /* Input text */
    TextView output;

    public static ReadWriteView newInstance() {
        return new ReadWriteView();
    }

    public ReadWriteView() { /* Required empty public constructor */ }

    public int getShownIndex() { return getArguments().getInt("index", 5); }

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
        final View view = inflater.inflate(R.layout.fragment_run_code, container, false);

        // Get the connection values
        output = (TextView) view.findViewById(R.id.output);

        // Runs when the "run" button is clicked
        view.findViewById(R.id.run_both).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                driver.connect();

                driver.writeMem(0x5000, 0x1234, 0x4321, 0xACED, 0xFACE);
                byte[] b = driver.readMem(0x5000, 4);

                output.append("Read: " + Utils.join(Utils.reverse(b)) + "\n");
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
