# FpaaApp

### Requirements

This driver supports Android version 15 and up. It also uses the FTDI Android driver, which can be downloaded [here](http://www.ftdichip.com/Android.htm).

### Tutorial

Here, I will provide a simple tutorial on building an application using the above utilities.

### Manual

##### `Driver.java`

Contains methods for connecting and communicating with the FPAA.

`void setConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl)`
 - Set baud rate, data bit, stop bit, parity bit, and flow control for the device

`boolean connect()`
 - Connect to the device, returns `true` if no connection issues occured.

`void disconnect()`
 - Disconnect from the device

`void setReadTime(int time)`
 - Set the amount of time allocated for a read
 - If the device is busy, the thread may lock for `time` milliseconds
 - If there is a lot to write (e.g. a memory dump) the thread may end before everything has been written
 - Default is 5000 milliseconds

`boolean running()`
 - Check if the device is running (`true`) or not running (`false`)
 - May take up to `Driver.readTime` milliseconds to check; set `readTime` using `void setReadTime(int time)`

`void sleep(int n_millis)`
 - Sleep the driver thread for a set length of time, without throwing an exception
 
`boolean programData(byte[] data)`
 - Programs data to the device and puts the device in run mode

`boolean programDataAndWait(byte[] data)`
 - Runs `programData(data)` and waits for the device to finish running the program

`void printAllRegisters()`
 - Helpful debugging function which prints the contents of all device registers

`byte[] readMem(int start_address, int length)`
 - Read `length` words from memory, starting at address `start_address`, and return them as a `byte[]` array, where each byte corresponds to half a word
 - Can use `Utils.toInts` and `Utils.toDoubles` to convert the returned array, where each int or double in the resulting array corresponds to a single word

`boolean writeMem(int start_address, byte / int / char ... data)`
 - Write data to the device, starting at address `start_address`
 - Returns `true` if the data was written, `false` otherwise

##### `Utils.java`

`String join(byte / char ... data)`
 - Serialize data as a string of hex data (similar to `toHex`)
 - Example: `join(0x12, 0x23) -> "0x12 0x23"`

`int toInt(byte... data)`
 - Converts a byte array into a single integer
 - Example: `toInt(0x11, 0x11) -> 0x1111`

`int[] toInts(byte... b)`
 - Convert the output of a device memory read to a list of integers, where each integer represents a single word of the device memory
 - Example: `toInts(0x11, 0x12, 0x13, 0x14) -> { 0x1112, 0x1314 }`

`double[] toDoubles(int... t)`
 - Converts an array of integers to an array of doubles

`double[] toDoubles(byte... b)`
 - Combines `toInts` and `toDoubles`, e.g. returns `toDoubles(toInts(b))`

`byte[] swapBytes(byte... data)`
 - Swaps each pair of bytes in-place
 - Example: `b = { 0x01, 0x02, 0x03, 0x04 }; swapBytes(b) -> b = { 0x02, 0x01, 0x04, 0x03 }`

`byte[] reverse(byte... data)`
 - Reverse a byte array in-place
 - Example: `b = { 0x01, 0x02, 0x03, 0x04 }; swapBytes(b) -> b = { 0x04, 0x03, 0x02, 0x01 }`

`byte[] toByte(char... data)`
 - Converts an array of chars to bytes, where the first byte is the least significant byte of the first char
 - Example: { 0x1234, 0x5678 } -> { 0x34, 0x12, 0x78, 0x56 }

`byte[] toByte(int... data)`
 - Converts an array of ints to bytes, where the first byte is the least significant byte of the first int
 - Example: { 0x1234, 0x5678 } -> { 0x34, 0x12, 0x78, 0x56 }

`Map<String,byte[]> getZipContents(String path)`
 - Unzips data in a zip file into byte arrays mapped by their file names

`byte[] parseHexAscii(byte[] b)`
 - Basically Integer.parseInt(n, 16)
 - Bytes are ASCII-encoded hex values (e.g. "0x5000")
 - This was necessary for doing a particular operation.

`byte[] compileElf(byte[] data) throws IOException`
 - Convert ELF binary data to the bytes to program to the MSP430

`void interpreter(InputStream in, PrintStream out)`
 - Tool for converting commands to MSP430 bytes
 - Usually done from the command line, e.g. `interpreter(System.in, System.out)`

`void uninterpreter(InputStream in, PrintStream out)`
 - Tool for converting MSP430 bytes to commands
 - Usually done from the command line, e.g. `uninterpreter(System.in, System.out)`

`byte compile(String command, String action)`
 - Turn a command and an action into a bit representation
 - Mostly used internally

`String uncompile(byte output)`
 - Given a binary command, figure out what it is in as assembly language
 - Mostly used internally
