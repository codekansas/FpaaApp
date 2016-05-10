package hasler.fpaaapp;

import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.Toast;

import com.ftdi.j2xx.D2xxManager;

import hasler.fpaaapp.views.HomeView;
import hasler.fpaaapp.views.ReadWriteView;

public class ControllerActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    /* Device manager stuff */
    public D2xxManager d2xxManager;
    public D2xxManager getDeviceManager() {
        return d2xxManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);

        // Instantiate navigation drawer
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // Instantiate the D2xxManager for the whole project
        try {
            d2xxManager = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException e) {
            e.printStackTrace();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.setPriority(500);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();

        Fragment newFragment;

        switch (position) {
            case 0:
                newFragment = HomeView.newInstance();
                break;
            case 1:
                newFragment = ReadWriteView.newInstance();
                break;
            default:
                newFragment = HomeView.newInstance();
        }

        ft.replace(R.id.container, newFragment).commit();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.controller, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static Fragment currentFragment = null;

    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();

        try {
            // Relevant callbacks to run when a USB device is connected or disconnected
            if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                if (currentFragment instanceof HomeView) {
                    ((HomeView) currentFragment).update();
                } else if (currentFragment instanceof ReadWriteView) {
                    ((ReadWriteView) currentFragment).createDeviceList();
                }
            } else if (action.equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {
                if (currentFragment instanceof ReadWriteView) {
                    ((ReadWriteView) currentFragment).disconnect();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_controller, container, false);
            return rootView;
        }
    }

}
