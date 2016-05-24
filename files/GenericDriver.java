public abstract class GenericDriver {
	/* -----------------------------------------------------------
	 * GenericDriver class: Generic programs for interfacing with
	 * openMSP430. To use it, implement the read and write methods
	 * depending on the serial port.
	 * ----------------------------------------------------------- */

    private final String TAG = "GenericDriver";

    protected abstract boolean write(byte... b);
    protected abstract byte[] read(int length);

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
        } catch (Exception ignored) { }
    }

    /**
     * Connect to the device
     * @return Whether or not the device is connected
     */
    protected boolean connectToDevice() {
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
     * Program data to the device
     * @param data The data to program
     * @return Whether or not the data was programmed
     */
    public boolean programData(byte[] data) {
        sendSynchronizationFrame();
        if (!connectToDevice()) return false;

        // Number of bytes
        int byte_size = data.length;

        // POR and halt the CPU
        executePorHalt();

        // Write the program to memory
        int startAddress = 0x10000 - byte_size;

        writeBurst(startAddress, data);
        sleep(500);

        // Verify that the data was written correctly
        if (!verifyMemory(startAddress, data)) return false;

        // Run the CPU
        int cpuCtlOrg = Utils.toInt(readRegister("CPU_CTL"));
        writeRegister("CPU_CTL", cpuCtlOrg | 0x02);

        return true;
    }

    /**
     * Run CPU without loading data to memory (assumes a program is already written)
     * @return
     */
    public boolean runWithoutData() {
        sendSynchronizationFrame();
        if (!connectToDevice()) return false;

        // POR and halt the CPU
        executePorHalt();

        // Run the CPU
        int cpuCtlOrg = Utils.toInt(readRegister("CPU_CTL"));
        writeRegister("CPU_CTL", cpuCtlOrg | 0x02);

        return true;
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
        sleep(10);

        // Send dummy frame in case the debug interface is already synchronized
        return write((byte) 0xC0) && write((byte) 0x00);
    }

    /**
     * Write a bunch of chars to the device. This is a wrapper for the byte function.
     * @param start_address The starting address to write to
     * @param data The character data to write
     * @return Whether it was written correctly
     */
    private boolean writeBurst(int start_address, char... data) {
        return writeBurst(start_address, Utils.toByte(data));
    }

    /**
     * Write a bunch of data to the device
     * @param start_address
     * @param data
     * @return
     */
    private boolean writeBurst(int start_address, byte... data) {
        final int MAX_LENGTH = 256;
        if (data.length > MAX_LENGTH) {
            for (int i = 0; i < data.length; i+= MAX_LENGTH) {
                int l = Math.min(data.length-i, MAX_LENGTH);
                byte[] b = new byte[l];
                System.arraycopy(data, i, b, 0, l);

                if (!writeBurst(start_address + i, b)) return false;
            }
            return true;
        }

        // Make sure our data consists of 16 bit units only
        if (data.length % 2 != 0) {
            byte[] n_data = new byte[data.length - 1];
            System.arraycopy(data, 0, n_data, 0, data.length - 1);
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
        sendSynchronizationFrame();

        // Perform a connection check
        if (!connectToDevice()) return false;
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
        boolean b = writeRegister("MEM_CNT", length - 1);		// This sets burst mode
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
        sendSynchronizationFrame();

        // Perform a connection check
        if (!connectToDevice()) return new byte[0];
        haltCpu();

        return Utils.reverse(readBurst(start_address, length));
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
        byte[] read_data = Utils.reverse(readMem(start_address, data.length / 2));

        if (read_data.length == 0) return false;
        if (read_data.length != data.length) {
            System.out.println("Read length: " + read_data.length + ", Expected length: " + data.length);
        }
        for (int i = 0; i < data.length / 2; i++) {
            if (read_data[i] != data[i]) {
                System.out.println("Error while verifying data at location " + i + ": Expected " + data[i] + ", got " + read_data[i]);
                System.out.println("Expected: " + Utils.join(data));
                System.out.println("Got: " + Utils.join(read_data));
                return false;
            }
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
        if ((0x05 & cpu_stat_val) != 0x05) return false;

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
