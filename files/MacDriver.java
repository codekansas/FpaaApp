import java.util.*;

public class MacDriver extends GenericDriver {
	private static SerialPort serialPort;
	private InputStream input;
	private OutputStream output;
	public String serialPortName;
	
	private boolean READY = false;
	
	/* Default connection parameters */
	private int TIME_OUT = 2000;
	private int BAUD_RATE = 115200;
	private int DATA_BITS = SerialPort.DATABITS_8;
	private int STOP_BITS = SerialPort.STOPBITS_1;
	private int PARITY = SerialPort.PARITY_NONE;
	private int FLOW_CONTROL = SerialPort.FLOWCONTROL_NONE;
	
	/* Access methods */
	public void set(String what, int val) {
		switch (what.toLowerCase()) {
		case "time_out":	TIME_OUT = val;
		case "baud_rate":	BAUD_RATE = val;
		case "data_bits":	DATA_BITS = val;
		case "stop_bits":	STOP_BITS = val;
		case "parity":		PARITY = val;
		}
		
		if (READY) {
			try {
				serialPort.setSerialPortParams(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY);
				serialPort.setFlowControlMode(FLOW_CONTROL);
			} catch (UnsupportedCommOperationException e) {
				e.printStackTrace();
			}
		}
	}
	public void setTimeOut(int time_out) { set("TIME_OUT", time_out); }
	public void setBaudRate(int baud_rate) { set("BAUD_RATE", baud_rate); }
	public void setDataBits(int data_bits) { set("DATA_BITS", data_bits); }
	public void setStopBits(int stop_bits) { set("STOP_BITS", stop_bits); }
	public void setParity(int parity) { set("PARITY", parity); }
	
	/**
	 * Connect to a particular device port
	 * @param portPath 
	 * @throws DeviceConnectionError
	 */
	public void connect(String portPath) throws DeviceConnectionError {
		// This is the default serial port on my Mac. Use "ls /dev/{tty,cu}.*" in bash shell
		// to list available ports for Mac, I believe it is similar for Linux / Windows
		System.setProperty("gnu.io.rxtx.SerialPorts", portPath);
				
		CommPortIdentifier portId;
		try {
			portId = CommPortIdentifier.getPortIdentifier(portPath);
		} catch (NoSuchPortException e) {
			throw new DeviceConnectionError("Could not connect to port at " + portPath);
		}
		
		// Set connection parameters, throw errors for improved debugging
		
		if (portId == null) throw new DeviceConnectionError("Could not find COM port");
		if (portId.isCurrentlyOwned()) throw new DeviceConnectionError("Device is currently in use");
		
		try { serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT); }
		catch (Exception e) { e.printStackTrace(); throw new DeviceConnectionError("Error opening the device"); }
		
		try { serialPort.setSerialPortParams(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY); }
		catch (Exception e) { e.printStackTrace(); throw new DeviceConnectionError("Error settings serial port parameters (after successfully opening the device)"); }
		
		try { serialPort.setFlowControlMode(FLOW_CONTROL); }
		catch (Exception e) { e.printStackTrace(); throw new DeviceConnectionError("Error setting flow control"); }
		
		try { input = serialPort.getInputStream(); output = serialPort.getOutputStream(); }
		catch (Exception e) { e.printStackTrace(); throw new DeviceConnectionError("Successfully opened the device, but an error occured in initializing input and output streams"); }
		
		sendSynchronizationFrame();
		
		READY = true;
		
		flush();
	}
	
	/**
	 * Close the device. Apparently this causes a fatal error whenever it runs.
	 */
	public synchronized void close() {
		// Delete log files (specific to my computer)
		File dir = new File("/Users/benjaminbolte/Documents/HaslerLab/FTDI-Chip");
		
		for (File f: dir.listFiles()) {
		    if (f.getName().endsWith(".log")) {
		        f.delete();
		    }
		}
		
		if (serialPort != null) {
			try {
				flush();
				output.close();
				input.close();
				serialPort.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean write(int... data) {
		return write(Utils.toByte(data));
	}

	protected boolean write(byte... data) {
		try {
			output.write(data);
			
			/* For debugging */
			String s = "TX: ";
			int i = 0;
			for (byte b : data) {
				s += String.format("%02x ", b);
				if (i++ > 10) {
					s += "(" + (data.length - i) + " more)...";
					break;
				}
			}
			System.out.println(s.substring(0, s.length() - 1));
			/* End debugging */
			
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Failed to write data to device");
			return false;
		}
		return true;
	}
	
	
	protected byte[] read(int length) {
		byte[] data = new byte[length];
		try {
			int idx = 0;
			while (length > 0) {
				int n_read = input.read(data, idx, length);
				length -= n_read;
				idx += n_read;
			}
			Utils.reverse(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/* For debugging */
		String s = "RX: ";
		int i = 0;
		for (byte b : data) {
			s += String.format("%02x ", b);
			if (i++ > 10) {
				s += "(" + data.length + " total) ";
				break;
			}
		}
		System.out.println(s.substring(0, s.length() - 1));
		/* End debugging */
		
		return data;
	}
	
	/**
	 * Flushes everything from the RX buffer?
	 */
	public void flush() {
		byte[] readBuffer = new byte[20];
		int n_bytes;
		try {
			while (input.available() > 0) {
				n_bytes = input.read(readBuffer);
				System.out.println("Read " + n_bytes + " bytes: " + Utils.join(readBuffer));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
