package hasler.fpaaapp.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import hasler.fpaaapp.R;
import hasler.fpaaapp.utils.DriverFragment;
import hasler.fpaaapp.utils.Utils;

public class ReadWriteView extends DriverFragment {
    private final String TAG = "ReadWriteView";

    public static ReadWriteView newInstance() {
        return new ReadWriteView();
    }
    public ReadWriteView() { /* Required empty public constructor */ }

    public static byte[] toBytes(String s) {
        char[] c = s.toCharArray();
        byte[] b = new byte[c.length * 2];

        for (int i = 0; i < c.length; i++) {
            b[2*i] = (byte) c[i];
            b[2*i+1] = (byte) (c[i] >> 8);
        }

        return b;
    }

    public static String fromBytes(byte[] b) {
        Utils.reverse(b);
        char[] c = new char[b.length / 2];

        for (int i = 0; i < c.length; i++) {
            c[i] = (char) ((char) b[2*i] | (char) b[2*i+1] << 8);
        }

        return new String(c);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) return null;
        super.onCreate(savedInstanceState);

        // Inflate the view XML file
        final View view = inflater.inflate(R.layout.fragment_read_write, container, false);

        /********
         * READ *
         ********/

        final EditText addressToRead = (EditText) view.findViewById(R.id.address_to_read);
        final EditText amountToRead = (EditText) view.findViewById(R.id.amount_to_read);
        final TextView readData = (TextView) view.findViewById(R.id.read_data);
        view.findViewById(R.id.read_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String addressString = addressToRead.getText().toString();
                int address;
                try {
                    address = Integer.parseInt(addressString, 16);
                } catch (NumberFormatException e) {
                    Toast.makeText(parentContext, "Invalid address: " + addressString, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (address < 0x0000 || address > 0x10000) {
                    Toast.makeText(parentContext, "Address " + addressString + " is out of range", Toast.LENGTH_SHORT).show();
                    return;
                }

                String amountString = amountToRead.getText().toString();
                int amount;
                try {
                    amount = Integer.parseInt(amountString, 10);
                } catch (NumberFormatException e) {
                    Toast.makeText(parentContext, "Invalid length: " + amountString, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (amount < 0 || amount > 100) {
                    Toast.makeText(parentContext, "Cannot read " + amountString + " words", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!driver.connect()) {
                    Toast.makeText(parentContext, "Connection error", Toast.LENGTH_SHORT).show();
                    return;
                }
                byte[] b = driver.readMem(address, amount);

                readData.setText(fromBytes(b));
            }
        });


        /*********
         * WRITE *
         *********/

        final EditText addressToWrite = (EditText) view.findViewById(R.id.address_to_write);
        final EditText dataToWrite = (EditText) view.findViewById(R.id.data_to_write);
        view.findViewById(R.id.write_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String addressString = addressToWrite.getText().toString();
                int address;
                try {
                    address = Integer.parseInt(addressString, 16);
                } catch (NumberFormatException e) {
                    Toast.makeText(parentContext, "Invalid address: " + addressString, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (address < 0x0000 || address > 0x10000) {
                    Toast.makeText(parentContext, "Address " + addressString + " is out of range", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!driver.connect()) {
                    Toast.makeText(parentContext, "Connection error", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!driver.writeMem(address, toBytes(dataToWrite.getText().toString()))) {
                    Toast.makeText(parentContext, "Connection okay, but encountered an error while writing data", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(parentContext, "Wrote data", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
