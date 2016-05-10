package hasler.fpaaapp.utils;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import hasler.fpaaapp.ControllerActivity;
import hasler.fpaaapp.R;
import hasler.fpaaapp.utils.Utils;

public class Driver {
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
        d2xxManager = parentContext.d2xxManager;
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

    public void createDeviceList() {
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
    }

    private boolean readComplete;
    private byte[] data;
    public byte[] read(int length) {
        readComplete = false;

        ReadThread thread = new ReadThread();
        thread.execute(length);

        // wait for the thread to complete
        long startTime = System.currentTimeMillis();
        while (!readComplete) {
            if (System.currentTimeMillis() - startTime > 100) { // cancel after 100 milliseconds
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

    public boolean write(byte... outData) {
        if (!ftDev.isOpen()) {
            return false;
        }

        int result = ftDev.write(outData);

        return result == outData.length;
    }

    private class ReadThread extends AsyncTask<Integer, Integer, Void> {
        @Override
        protected Void doInBackground(Integer... params) {
            final int BUFFER_LENGTH = 512;
            int length = params[0];
            byte[] buf = new byte[BUFFER_LENGTH];
            data = new byte[length];

            int c = 0, avail;

            while (c < length) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (isCancelled()) {
                    break;
                }

                avail = ftDev.getQueueStatus();
                if (avail > 0) {
                    if (avail > BUFFER_LENGTH) {
                        avail = BUFFER_LENGTH;
                    }

                    ftDev.read(buf, avail);

                    for (int i = 0; i < avail && c < length; i++) {
                        data[c++] = buf[i];
                    }

                    ftDev.purge((byte) 1);
                }
            }

            readComplete = true;
            return null;
        }
    }

    /***************
     * DRIVER PART *
     ***************/


    /**
     * Convert integer data to byte data to write
     * @param data The data to write
     * @return Whether or not the data was written
     */
    protected boolean write(int... data) {
        byte[] b = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            b[i] = (byte) data[i];
        }
        return write(b);
    }

    /**
     * Put the thread to sleep for a certain amount of time
     * @param n_millis: Number of milliseconds to sleep for
     */
    public void sleep(int n_millis) {
        try {
            Thread.sleep(n_millis);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Connect to the device
     * @return Whether or not the device is connected
     */
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

        sendSynchronizationFrame();

        // Perform a connection check
        if (!verifyCpuId()) {
            return false;
        }

        getDevice();

        // Perform another connection check: This is actually important
        if (!verifyCpuId()) {
            return false;
        }

        initBreakUnits();

        return true;
    }

    /**
     * Check whether the device is currently running
     * @return The device's status (running or not)
     */
    public boolean running() {
        return (Utils.toInt(readRegister("CPU_STAT")) & 0x1) == 0;
    }

    /**
     * Program some data and wait for it to finish execution
     * @param data The data to program
     * @return Whether or not the data was programmed
     */
    public boolean programDataAndWait(byte[] data) {
        boolean b = programData(data);

        if (!b) return false;

        boolean cpu_halted = false;

        while (!cpu_halted) {
            sleep(1000);

            int cpu_stat_val = Utils.toInt(readRegister("CPU_STAT"));
            if (cpu_stat_val != 0) {
                cpu_halted = true;
            }
        }

        return true;
    }

    /**
     * Program data to the device
     * @param data The data to program
     * @return Whether or not the data was programmed
     */
    public boolean programData(byte[] data) {

        // Perform a connection check
        if (!verifyCpuId()) {
            return false;
        }

        getDevice();

        // Perform a connection check again: This is actually pretty important
        if (!verifyCpuId()) {
            return false;
        }

        // Initialize break units: Maybe not so important, but it looks nice
        initBreakUnits();

        // Number of bytes
        int byte_size = data.length;

        // POR and halt the CPU
        executePorHalt();

        // Write the program to memory
        int startAddress = 0x10000 - byte_size;

        writeBurst(startAddress, data);
        sleep(500);

        // Verify that the data was written correctly
        if (!verifyMemory(startAddress, data)) {
            return false;
        }

        // Run the CPU
        int cpuCtlOrg = Utils.toInt(readRegister("CPU_CTL"));
        writeRegister("CPU_CTL", cpuCtlOrg | 0x02);

        return true;
    }

    /**
     * Print the value of each register to the default print stream
     * Useful for debugging
     */
    public void printAllRegisters() {
        for (String reg : Utils.ADDRESSES) {
            Log.e(TAG, reg + ": " + Utils.join(readRegister(reg)));
        }
    }

    /**
     * Read a serial debug interface register
     * @param register_name
     * @return
     */
    public byte[] readRegister(String register_name) {
        byte cmd = Utils.compile(register_name, "RD");
        this.write(cmd);

        if ((cmd & 0x40) == 0) { // 16 bit
            return this.read(2);
        } else { // 8 bit
            return this.read(1);
        }
    }

    /**
     * Helper method for writing integer data
     * @param register_name The register to write to
     * @param data The data to write
     * @return Whether the data was written
     */
    private boolean writeRegister(String register_name, int... data) {
        return writeRegister(register_name, Utils.toByte(data));
    }

    /**
     * Write some data to a serial debug interface register
     * @param register_name The register to write to
     * @param data The data to write
     * @return Whether the data was written
     */
    private boolean writeRegister(String register_name, byte... data) {
        byte cmd = Utils.compile(register_name, "WR");
        this.write(cmd);

        if ((cmd & 0x40) == 0) { // 16 bit
            if (data.length > 1) {
                return this.write(data[0]) && this.write(data[1]);
            } else {
                return this.write(data[0]) && this.write((byte) 0x00);
            }
        } else { // 8 bit
            return this.write(data[0]);
        }
    }

    /**
     * Synchronize the device after connecting to it
     * @return Whether it was synchronized
     */
    protected boolean sendSynchronizationFrame() {
        // Send synchronization frame
        write((byte) 0x80);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
        }

        // Send dummy frame in case the debug interface is already synchronized
        write((byte) 0xC0);
        return write((byte) 0x00);
    }

    /**
     * Write a bunch of chars to the device. This is a wrapper for the byte function.
     * @param start_address The starting address to write to
     * @param data The character data to write
     * @return Whether it was written correctly
     */
    public boolean writeBurst(int start_address, char... data) {
        return writeBurst(start_address, Utils.toByte(data));
    }

    /**
     * Write a bunch of data to the device
     * @param start_address
     * @param data
     * @return
     */
    private boolean writeBurst(int start_address, byte... data) {
        // Make sure our data consists of 16 bit units only
        if (data.length % 2 != 0) {
            byte[] n_data = new byte[data.length - 1];
            for (int i = 0; i < data.length - 1; i++) {
                n_data[i] = data[i];
            }
            data = n_data;
        }
        writeRegister("MEM_CNT", data.length / 2 - 1);
        writeRegister("MEM_ADDR", start_address);
        writeRegister("MEM_CTL", 0x03);
        return this.write(data);
    }

    /**
     * Write some integer data to memory. This is a wrapper for the byte function.
     * @param start_address The starting address to write to
     * @param data The data to write
     * @return Whether it was written correctly
     */
    public boolean writeMem(int start_address, int... data) {
        return writeMem(start_address, Utils.toByte(data));
    }

    /**
     * Write some character data to memory. This is a wrapper for the byte function.
     * @param start_address The starting address to write to
     * @param data The data to write
     * @return Whether it was written correctly
     */
    public boolean writeMem(int start_address, char... data) {
        return writeMem(start_address, Utils.toByte(data));
    }

    /**
     * Write some data to memory. This writes the data safely, by performing the required checks.
     * @param start_address The starting address to write to
     * @param data The data to write
     * @return Whether the data was successfully written
     */
    public boolean writeMem(int start_address, byte... data) {
        // Perform a connection check
        if (!connect()) return false;

        // Does this get the program counter?

        haltCpu();

        return writeBurst(start_address, data);
    }

    /**
     * For reading lots of data from an address
     * @param start_address The address to read from
     * @param length The amount of data to read
     * @return The read data
     */
    private byte[] readBurst(int start_address, int length) {
        writeRegister("MEM_CNT", length - 1);		// This sets burst mode
        writeRegister("MEM_ADDR", start_address);	// Write start address to MEM_ADDR
        writeRegister("MEM_CTL", 0x01);				// Initiate read command
        return this.read(length * 2);				// Read the data
    }

    /**
     * Read a certain amount of data from memory. This reads data safely, performing the necessary checks.
     * @param start_address The address to read from
     * @param length The amount of data to read
     * @return The read data
     */
    public byte[] readMem(int start_address, int length) {
        // Perform a connection check
        if (!connect()) return new byte[length * 2];;

        // Perform a connection check
        if (!verifyCpuId()) { return new byte[length * 2]; }
        getDevice();
        if (!verifyCpuId()) { return new byte[length * 2]; }

        // Break units
        initBreakUnits();

        haltCpu();

        return readBurst(start_address, length);
    }

	/* ----------------------------------
	 * Below this line are helper methods
	 * ---------------------------------- */

    /**
     * Get device: Enable auto-freeze and software breakpoints
     * @return Whether the correct register was written
     */
    private boolean getDevice() {
        return writeRegister("CPU_CTL", 0x18);
    }

    /**
     * Halt the CPU
     * @return Whether or not the device was halted
     */
    private boolean haltCpu() {
        int cpu_ctl_org = Utils.toInt(readRegister("CPU_CTL"));

        // Stop CPU
        writeRegister("CPU_CTL", 0x01 | cpu_ctl_org);

        // Check status: Make sure the CPU halted
        int cpu_stat_val = Utils.toInt(readRegister("CPU_STAT"));
        return (0x01 & cpu_stat_val) != 1;
    }

    /**
     * Verifies that data was written to memory at the correct location
     * @param start_address Start address for written data
     * @param data The data that was written
     * @return Whether data was successfully written
     */
    private boolean verifyMemory(int start_address, byte[] data) {
        byte[] read_data = Utils.reverse(readBurst(start_address, data.length / 2));
        if (read_data.length != data.length) return false;
        for (int i = 0; i < read_data.length; i++) {
            if (read_data[i] != data[i]) return false;
        }
        return true;
    }

    /**
     * Execute a power-on reset and halts the CPU
     * @return whether or not it was halted
     */
    private boolean executePorHalt() {
        int cpu_ctl_org = Utils.toInt(readRegister("CPU_CTL"));

        // Perform PUC
        writeRegister("CPU_CTL", 0x60 | cpu_ctl_org);
        sleep(100);
        writeRegister("CPU_CTL", cpu_ctl_org);

        // Check status: Make sure a PUC occurred and that the CPU is halted
        int cpu_stat_val = Utils.toInt(readRegister("CPU_STAT"));
        if ((0x05 & cpu_stat_val) != 5) return false;

        // Clear PUC pending flag
        writeRegister("CPU_STAT", 0x04);

        return true;
    }

    /**
     * This method gets the values of three registers and returns them
     * @return The values of the CPU_ID_LO, CPU_ID_HI and CPU_NR registers
     */
    private int[] getCpuId() {
        int cpuIdLo = Utils.toInt(readRegister("CPU_ID_LO"));
        int cpuIdHi = Utils.toInt(readRegister("CPU_ID_HI"));
        int cpuNr = Utils.toInt(readRegister("CPU_NR"));

        return new int[]{ (cpuIdHi << 8) + cpuIdLo, cpuNr };
    }

    /**
     * This method checks to make sure the CPU is connected
     * @return Whether the CPU is connected
     */
    private boolean verifyCpuId() {
        return getCpuId()[0] != 0;
    }

    /**
     * Initialize break units
     * @return Whether the break units were initialized
     */
    private int initBreakUnits() {
        int num_brk_units = 0;
        for (int i = 0; i < 4; i++) {
            String reg_name = "BRK" + i + "_ADDR0";
            writeRegister(reg_name, 0x1234);
            int newVal = Utils.toInt(readRegister(reg_name));
            if (newVal == 0x1234) {
                num_brk_units++;
                writeRegister("BRK" + i + "_CTL", 0x00);
                writeRegister("BRK" + i + "_STAT", 0xff);
                writeRegister("BRK" + i + "_ADDR0", 0x0000);
                writeRegister("BRK" + i + "_ADDR1", 0x0000);
            }
        }
        return num_brk_units;
    }

    /**
     * Clear the status registers
     * @return Whether the status registers were cleared
     */
    protected boolean clearStatus() {
        return writeRegister("CPU_STAT", 0xff) &&
                writeRegister("BRK0_STAT", 0xff) &&
                writeRegister("BRK1_STAT", 0xff) &&
                writeRegister("BRK2_STAT", 0xff) &&
                writeRegister("BRK3_STAT", 0xff);
    }

    /**
     * Device connection error: Standard exception to indicate a device connection error
     */
    public static class DeviceConnectionError extends Exception {
        private static final long serialVersionUID = 1L;

        public DeviceConnectionError() { }
        public DeviceConnectionError(String message) { super(message); }
        public DeviceConnectionError(Throwable cause) { super(cause); }
        public DeviceConnectionError(String message, Throwable cause) { super(message, cause); }
    }
}
