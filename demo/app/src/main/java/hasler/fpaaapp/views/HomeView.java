package hasler.fpaaapp.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.support.v4.app.Fragment;

import com.ftdi.j2xx.D2xxManager;

import java.util.ArrayList;
import java.util.List;

import hasler.fpaaapp.ControllerActivity;
import hasler.fpaaapp.R;
import hasler.fpaaapp.lists.ItemListAdapter;
import hasler.fpaaapp.lists.ItemListItem;
import hasler.fpaaapp.utils.Utils;

public class HomeView extends Fragment {

    private D2xxManager d2xxManager;
    private ControllerActivity parentContext;

    private AbsListView mListView;
    private ListAdapter mAdapter;

    private ItemListItem n_devices = new ItemListItem("Number of devices", "");
    private ItemListItem d_type = new ItemListItem("Device type", "");
    private ItemListItem d_serial_number = new ItemListItem("Device serial number", "");
    private ItemListItem d_description = new ItemListItem("Device description", "");
    private ItemListItem d_location = new ItemListItem("Device location", "");
    private ItemListItem d_id = new ItemListItem("Device ID", "");

    public static HomeView newInstance() { // Context context, D2xxManager d2xxManager) {
        return new HomeView();
    }

    public HomeView() { /* Mandatory empty constructor for the fragment manager to instantiate the fragment */ }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        parentContext = (ControllerActivity) getActivity();
        d2xxManager = parentContext.getDeviceManager();

        List<ItemListItem> itemList = new ArrayList<>();

        itemList.add(n_devices);
        itemList.add(d_type);
        itemList.add(d_serial_number);
        itemList.add(d_description);
        itemList.add(d_location);
        itemList.add(d_id);

        mAdapter = new ItemListAdapter(getActivity(), itemList);

        update();
    }

    public void update() {
        int deviceCount = d2xxManager.createDeviceInfoList(getActivity());
        n_devices.setValue(deviceCount);

        if (deviceCount > 0) {
            D2xxManager.FtDeviceInfoListNode[] deviceList = new D2xxManager.FtDeviceInfoListNode[deviceCount];
            d2xxManager.getDeviceInfoList(deviceCount, deviceList);
            D2xxManager.FtDeviceInfoListNode device = deviceList[0];

            d_type.setValue(Utils.D2xxUtils.getType(device.type));
            d_serial_number.setValue(device.serialNumber);
            d_description.setValue(device.description);
            d_location.setValue(String.format("%04x", device.location));
            d_id.setValue(String.format("%08x", device.id));
        } else {
            d_type.setValue("Not connected");
            d_serial_number.setValue("Not connected");
            d_description.setValue("Not connected");
            d_location.setValue("Not connected");
            d_id.setValue("Not connected");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);

        return view;
    }

}
