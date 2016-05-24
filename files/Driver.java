import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import hasler.fpaaapp.ControllerActivity;

public class Driver extends GenericDriver {
    private final String TAG = "Driver";

    /* Original */
    D2xxManager d2xxManager;
    FT_Device ftDev = null;
    int devCount = -1;
    int currentIndex = -1;
    int openIndex = 1;

    /* Local variables */
    int baudRate = 115200;
    byte stopBit = 1;
    byte dataBit = 8;
    byte parity = 0;
    byte flowControl = 0;

    /* Parameters and more local variables */
    boolean bReadThreadGoing = false;
    boolean uartConfigured = false;

    private ControllerActivity parentContext;

    public Driver(ControllerActivity parentContext) {
        this.parentContext = parentContext;
        d2xxManager = parentContext.getDeviceManager();
    }

    public void setConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl) {
        if (!ftDev.isOpen()) {
            Toast.makeText(parentContext, "FT device is not open", Toast.LENGTH_SHORT).show();
            return;
        }

        // Configure to our port
        ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);
        ftDev.setBaudRate(baud);

        // Configure data bits
        switch (dataBits) {
            case 7:
                dataBits = D2xxManager.FT_DATA_BITS_7;
                break;
            case 8:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
            default:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
        }

        // Configure stop bits
        switch (stopBits) {
            case 1:
                stopBit = D2xxManager.FT_STOP_BITS_1;
                break;
            case 2:
                stopBits = D2xxManager.FT_STOP_BITS_2;
                break;
            default:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
        }

        // Configure parity
        switch (parity) {
            case 0:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
            case 1:
                parity = D2xxManager.FT_PARITY_ODD;
                break;
            case 2:
                parity = D2xxManager.FT_PARITY_EVEN;
                break;
            case 3:
                parity = D2xxManager.FT_PARITY_MARK;
                break;
            default:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
        }

        // Set data characteristics
        ftDev.setDataCharacteristics(dataBits, stopBits, parity);

        short flowControlSetting;
        switch (flowControl) {
            case 0:
                flowControlSetting = D2xxManager.FT_FLOW_NONE;
                break;
            case 1:
                flowControlSetting = D2xxManager.FT_FLOW_RTS_CTS;
                break;
            case 2:
                flowControlSetting = D2xxManager.FT_FLOW_DTR_DSR;
                break;
            case 3:
                flowControlSetting = D2xxManager.FT_FLOW_XON_XOFF;
                break;
            default:
                flowControlSetting = D2xxManager.FT_FLOW_NONE;
                break;
        }

        // Shouldn't be hard coded, but I don't know the correct way
        ftDev.setFlowControl(flowControlSetting, (byte) 0x0b, (byte) 0x0d);

        uartConfigured = true;
    }

    protected void createDeviceList() {
        int tempDevCount = d2xxManager.createDeviceInfoList(parentContext);
        if (tempDevCount > 0) {
            if (devCount != tempDevCount) {
                devCount = tempDevCount;
            }
        } else {
            devCount = -1;
            currentIndex = -1;
        }
    }

    public void disconnect() {
        devCount = -1;
        currentIndex = -1;
        bReadThreadGoing = false;

        // Sleep for 50 milliseconds
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Close the FT device, if it is open
        if (ftDev != null) {
            synchronized (ftDev) {
                if (ftDev.isOpen()) {
                    ftDev.close();
                }
            }
        }

        if (thread != null) {
            thread.cancel(false);
        }
    }

    private int readTime = 5000;
    private boolean readComplete;
    private byte[] data;
    private int readAmount, readLength;

    public void setReadTime(int time) {
        readTime = time;
    }

    protected byte[] read(int length) {
        return read(length, readTime);
    }

    protected ReadThread thread;
    protected byte[] read(int length, int n_millis) {
        readAmount = 0;
        readLength = length;
        data = new byte[readLength];
        readComplete = false;

        if (thread == null) {
            thread = new ReadThread();
            thread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        // wait for the thread to complete
        long startTime = System.currentTimeMillis();
        while (!readComplete) {
            if (System.currentTimeMillis() - startTime > n_millis) { // cancel after 5 seconds
                thread.cancel(false);
                break;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return data;
    }

    protected boolean write(byte... outData) {
        if (!ftDev.isOpen()) {
            Log.d(TAG, "FT Device is not open");
            return false;
        }

        int result = ftDev.write(outData, outData.length, false);

        return result == outData.length;
    }

    private class ReadThread extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            final int BUFFER_LENGTH = 70000;
            byte[] buf = new byte[BUFFER_LENGTH];

            int avail;
            while (!isCancelled()) {
                if (readComplete) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                while (readAmount < readLength) {
                    if (isCancelled()) {
                        break;
                    }

                    avail = ftDev.getQueueStatus();

                    if (avail > 0) {
                        if (avail > BUFFER_LENGTH) {
                            avail = BUFFER_LENGTH;
                        }

                        ftDev.read(buf, avail);
                        System.arraycopy(buf, 0, data, readAmount, Math.min(avail, readLength - readAmount));
                        readAmount += avail;

                        if (readAmount == readLength) {
                            ftDev.purge((byte) 1);
                        }
                    }

                    if (readAmount < readLength) {
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                readComplete = true;
            }

            return null;
        }
    }

    public boolean connect() {
        if (devCount <= 0) {
            createDeviceList();
        }
        if (devCount <= 0) return false;

        if (currentIndex != openIndex) {
            if (ftDev == null) {
                ftDev = d2xxManager.openByIndex(parentContext, openIndex);
            } else {
                synchronized (ftDev) {
                    ftDev = d2xxManager.openByIndex(parentContext, openIndex);
                }
            }
            uartConfigured = false;
        }

        if (ftDev == null) {
            return false;
        }

        if (ftDev.isOpen()) {
            currentIndex = openIndex;
        } else {
            return false;
        }

        if (!uartConfigured) {
            setConfig(baudRate, dataBit, stopBit, parity, flowControl);
        }
        if (!uartConfigured) return false;

        return true;
    }
}
