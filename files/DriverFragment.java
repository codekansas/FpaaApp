import android.os.Bundle;
import android.support.v4.app.Fragment;

import hasler.fpaaapp.ControllerActivity;

public class DriverFragment extends Fragment {

    protected ControllerActivity parentContext;
    protected hasler.fpaaapp.utils.Driver driver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get parent context and instructions to execute
        parentContext = (ControllerActivity) getActivity();
        
        // The driver is an object of the parent activity, in this case
        driver = new hasler.fpaaapp.utils.Driver(parentContext);
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
