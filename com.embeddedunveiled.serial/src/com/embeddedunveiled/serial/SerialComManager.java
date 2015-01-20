/**
 * Author : Rishi Gupta
 * Email  : gupt21@gmail.com
 * 
 * This file is part of 'serial communication manager' library.
 *
 * The 'serial communication manager' is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * The 'serial communication manager' is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with serial communication manager. If not, see <http://www.gnu.org/licenses/>.
 */

package com.embeddedunveiled.serial;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is the entry point to this library.
 */
public final class SerialComManager {

	public static boolean DEBUG = true;
	public static final String JAVA_LIB_VERSION = "1.0.0";

	private static int osType = -1;
	public static final int OS_LINUX    = 1;
	public static final int OS_WINDOWS  = 2;
	public static final int OS_SOLARIS  = 3;
	public static final int OS_MAC_OS_X = 4;
	
	public static final int DEFAULT_READBYTECOUNT = 1024;

	/** Pre-defined constants for baud rate values. */
	public enum BAUDRATE {
		B0(0), B50(50), B75(75), B110(110), B134(134), B150(150), B200(200), B300(300), B600(600), B1200(1200),
		B1800(1800), B2400(2400), B4800(4800), B9600(9600), B14400(14400), B19200(19200), B28800(28800), B38400(38400),
		B56000(56000), B57600(57600), B115200(115200), B128000(128000), B153600(153600), B230400(230400), B256000(256000), 
		B460800(460800), B500000(500000), B576000(576000), B921600(921600), B1000000(1000000), B1152000(1152000),
		B1500000(1500000),B2000000(2000000), B2500000(2500000), B3000000(3000000), B3500000(3500000), B4000000(4000000),
		BCUSTOM(251);
		
		private int value;
		private BAUDRATE(int value) {
			this.value = value;	
		}
		public int getValue() {
			return this.value;
		}
	}
	
	/** Pre-defined constants for number of data bits in a serial frame. */
	public enum DATABITS {
		DB_5(5), DB_6(6), DB_7(7), DB_8(8);
		
		private int value;
		private DATABITS(int value) {
			this.value = value;	
		}
		public int getValue() {
			return this.value;
		}
	}

	/** Pre-defined constants for number of stop bits in a serial frame. */
	// SB_1_5(4) is 1.5 stop bits.
	public enum STOPBITS {
		SB_1(1), SB_1_5(4), SB_2(2);
		
		private int value;
		private STOPBITS(int value) {
			this.value = value;	
		}
		public int getValue() {
			return this.value;
		}
	}

	/** Pre-defined constants for enabling type of parity in a serial frame. */
	public enum PARITY {
		P_NONE(1), P_ODD(2), P_EVEN(3), P_MARK(4), P_SPACE(5);
		
		private int value;
		private PARITY(int value) {
			this.value = value;	
		}
		public int getValue() {
			return this.value;
		}
	}
	
	/** Pre-defined constants for controlling data flow between DTE and DCE. */
	public enum FLOWCONTROL {
		NONE(1), HARDWARE(2), SOFTWARE(3);
		
		private int value;
		private FLOWCONTROL(int value) {
			this.value = value;	
		}
		public int getValue() {
			return this.value;
		}
	}
	
	/** Pre-defined constants for defining endianness of data to be sent over serial port. */
	public enum ENDIAN {
		E_LITTLE(1), E_BIG(2), E_DEFAULT(3);
		
		private int value;
		private ENDIAN(int value) {
			this.value = value;	
		}
		public int getValue() {
			return this.value;
		}
	}
	
	/** Pre-defined constants for defining number of bytes given data can be represented in. */
	public enum NUMOFBYTES {
		NUM_2(2), NUM_4(4);
		
		private int value;
		private NUMOFBYTES(int value) {
			this.value = value;	
		}
		public int getValue() {
			return this.value;
		}
	}
	
	/** Mask bits for UART control lines. */
	public static final int CTS =  0x01;  // 0000001
	public static final int DSR =  0x02;  // 0000010
	public static final int DCD =  0x04;  // 0000100
	public static final int RI  =  0x08;  // 0001000
	public static final int LOOP = 0x10;  // 0010000
	public static final int RTS =  0x20;  // 0100000
	public static final int DTR  = 0x40;  // 1000000

	/** These properties are used to load OS specific native library. */
	public static final String osName = System.getProperty("os.name");
	public static final String osArch = System.getProperty("os.arch");
	public static final String userHome = System.getProperty("user.home");
	public static final String javaTmpDir = System.getProperty("java.io.tmpdir");
	public static final String fileSeparator = System.getProperty("file.separator");
	
	/** Maintain integrity and consistency among all operations, therefore synchronise them for
	 *  making structural changes. This array can be sorted array if scaled to large scale. */
	private ArrayList<SerialComPortHandleInfo> handleInfo = new ArrayList<SerialComPortHandleInfo>();
	private List<SerialComPortHandleInfo> mPortHandleInfo = Collections.synchronizedList(handleInfo);

	private SerialComJNINativeInterface mNativeInterface = null;
	private SerialComPortsList mSerialComPortList = null;
	private SerialComErrorMapper mErrMapper = null;
	private SerialComCompletionDispatcher mEventCompletionDispatcher = null;

	/**
	 * Constructor, initialise various classes and load native libraries. 
	 */
	public SerialComManager() {
		String osNameMatch = osName.toLowerCase();
		if(osNameMatch.contains("linux")) {
			osType = OS_LINUX;
		}else if(osNameMatch.contains("windows")) {
			osType = OS_WINDOWS;
		}else if(osNameMatch.contains("solaris") || osNameMatch.contains("sunos")) {
			osType = OS_SOLARIS;
		}else if(osNameMatch.contains("mac os") || osNameMatch.contains("macos") || osNameMatch.contains("darwin")) {
			osType = OS_MAC_OS_X;
		}
		
		mErrMapper = new SerialComErrorMapper();
		mNativeInterface = new SerialComJNINativeInterface();
		mSerialComPortList = new SerialComPortsList(mNativeInterface);
		mEventCompletionDispatcher = new SerialComCompletionDispatcher(mNativeInterface, mErrMapper, mPortHandleInfo);
	}

	/**
	 * Gives library versions of java and native modules.
	 * 
	 * @return Java and C library versions implementing this library.
	 */
	public String getLibraryVersions() {
		String version = null;
		String nativeLibversion = mNativeInterface.getNativeLibraryVersion();
		if(nativeLibversion != null) {
			version = "Java lib version: " + JAVA_LIB_VERSION + "\n" + "Native lib version: " + nativeLibversion;
		}else {
			version = "Java lib version: " + JAVA_LIB_VERSION + "\n" + "Native lib version: " + "Could not be determined !";
		}
		return version;
	}

	/**
	 * For internal use.
	 * @return platform OS type, the library is running on (for internal use only). 
	 */
	public static int getOSType() {
		return osType;
	}

	/**
	 * Returns all available UART style ports available on this system, otherwise an empty array of strings, if no serial style port is
	 * found in the system. Note that the BIOS may ignore UART ports on a PCI card and therefore BIOS settings has to be corrected.
	 * 
	 * <p>Developers must consider using this method to know which ports are valid communications ports before opening them for writing
	 * more robust code.</p>
	 * 
	 * @return Available UART style ports name for windows, full path with name for Unix like OS, returns empty array if no ports found.
	 */
	public String[] listAvailableComPorts() {
		String[] availablePorts = null;
		if(mSerialComPortList != null) {
			availablePorts = mSerialComPortList.listAvailableComPorts();
			if(availablePorts == null) {
				return new String[]{};
			}
			return availablePorts;
		}
		return new String[]{};
	}

	/** 
	 * Developers are advised to use methods like openComPort(), closeComPort() and configureComPort() etc thread safe to maintian reliable
	 * and  operation. 
	 * 
	 * <p>This method runs to completion in synchronised manner, therefore a port before returning from this method have an exclusive owner 
	 * or not. If it has exclusive owner and an attempt is made to open it again, native code will return error. On the other hand if it 
	 * does not have an exclusive owner and application tries to open it in exclusive ownership mode, we issue a warning to the caller 
	 * that the port is already opened.</p>
	 * 
	 * <p>Note that even if the port is opened in exclusive ownership mode, the root user will still be able to access and operate on that
	 * serial port.</p>
	 * 
	 * @param portName name of the port to be opened for communication
	 * @param enableRead allows application to read bytes from this port
	 * @param enableWrite allows application to write bytes to this port
	 * @param exclusiveOwnerShip application wants to become exclusive owner of this port or not
	 * @throws SerialComException if null argument is passed, if both enableWrite and enableRead are false
	 * @return handle of the port successfully opened
	 */
	public long openComPort(String portName, boolean enableRead, boolean enableWrite, boolean exclusiveOwnerShip) throws SerialComException {
		if(portName == null) {
			throw new NullPointerException("openComPort(), " + SerialComErrorMapper.ERR_NULL_POINTER_FOR_PORT_OPENING);
		}
		
		if((enableRead == false) && (enableWrite == false)) {
			throw new SerialComException(portName, "openComPort()",  "Enable read, write or both.");
		}
		
		if(exclusiveOwnerShip == true) {
			for(SerialComPortHandleInfo mInfo: mPortHandleInfo){
				if(mInfo.containsPort(portName)) {
					System.out.println("The requested port " + portName + " is already opened.");
					return -1; // let application be not only aware of this but re-think its design
				}
			}
		}

		long handle = mNativeInterface.openComPort(portName, enableRead, enableWrite, exclusiveOwnerShip);
		if(handle < 0) {
			throw new SerialComException(portName, "openComPort()",  mErrMapper.getMappedError(handle));
		}
		
		boolean added = mPortHandleInfo.add(new SerialComPortHandleInfo(portName, handle, null, null, null));
		if(added != true) {
			System.out.println("Could not append information associated with port while opening port.");
		}
		
		return handle;
	}

	/**
	 * Close the serial port. Application should unregister listeners if it has registered any.
	 * 
	 * @param handle of the port to be closed
	 * @throws SerialComException if invalid handle is passed or when it fails in closing the port
	 * @return Return true on success in closing the port false otherwise
	 */
	public boolean closeComPort(long handle) throws SerialComException {
		boolean handlefound = false;
		SerialComPortHandleInfo mHandleInfo = null;
		
		for(SerialComPortHandleInfo mInfo: mPortHandleInfo){
			if(mInfo.containsHandle(handle)) {
				handlefound = true;
				mHandleInfo = mInfo;
				break;
			}
		}
		
		if(handlefound == false) {
			throw new SerialComException("closeComPort()", SerialComErrorMapper.ERR_WRONG_HANDLE);
		}
		
		if(mHandleInfo.getDataListener() != null) {
			System.out.println("CLOSING HANDLE WITHOUT UNREGISTERING DATA LISTENER !");
		}
		if(mHandleInfo.getEventListener() != null) {
			System.out.println("CLOSING HANDLE WITHOUT UNREGISTERING EVENT LISTENER !");
		}
		
		int ret = mNativeInterface.closeComPort(handle);
		// native close() returns 0 on success
		if(ret != 0) {
			throw new SerialComException("closeComPort()",  mErrMapper.getMappedError(ret));
		}
		
		return true;
	}

	/**
	 * This method writes bytes from the specified byte type buffer. To make read and write take as less time as possible,
	 * we do not check whether port has been opened and configured or not. If the method returns false, the application
	 * should try to re-send bytes. The data has been transmitted out of serial port when this method returns.
	 * 
	 * @param handle handle of the opened port on which to write bytes
	 * @param buffer byte type buffer containing bytes to be written to port
	 * @param delay interval to be maintained between writing two consecutive bytes
	 * @return true on success false otherwise
	 */
	public boolean writeBytes(long handle, byte[] buffer, int delay) {
		int ret = mNativeInterface.writeBytes(handle, buffer, delay);
		if(ret < 0) {
			if(DEBUG) System.out.println("writeBytes() encountered error: " + mErrMapper.getMappedError(ret));
			return false;
		}
		return true;
	}

	/**
	 * This method writes a single byte to the specified port. The data has been transmitted out of serial port when 
	 * this method returns.
	 * 
	 * @param handle handle of the opened port on which to write byte
	 * @param value byte to be written to port
	 * @return true on success false otherwise
	 */
	public boolean writeSingleByte(long handle, byte value) {
		return writeBytes(handle, new byte[] { value }, 0);
	}

	/**
	 * This method writes a string to the specified port. The library internally converts string to byte buffer. 
	 * The data has been transmitted out of serial port when this method returns.
	 * 
	 * @param handle handle of the opened port on which to write byte
	 * @param data the string to be send to port
	 * @param delay interval between two successive bytes while sending string
	 * @return true on success false otherwise
	 */
	public boolean writeString(long handle, String data, int delay) {
		return writeBytes(handle, data.getBytes(), delay);
	}

	/**
	 * This method writes a string to the specified port. The library internally converts string to byte buffer. 
	 * The data has been transmitted out of serial port when this method returns.
	 * 
	 * @param handle handle of the opened port on which to write byte
	 * @param data the string to be send to port
	 * @param charset the character set into which given string will be encoded
	 * @return true on success false otherwise
	 */
	public boolean writeString(long handle, String data, Charset charset, int delay) throws UnsupportedEncodingException {
		return writeBytes(handle, data.getBytes(charset), delay);
	}

	/** 
	 * Different CPU and OS will have different endianness. It is therefore we handle the endianness conversion 
	 * as per the user request. If the given integer is in range −32,768 to 32,767, only two bytes will be needed.
	 * In such case user might like to send only 2 bytes to serial port. On the other hand user might be implementing
	 * some custom protocol so that the data must be 4 bytes (irrespective of its range) in order to be interpreted 
	 * correctly by the receiver terminal. This method assumes that integer value can be represented by 32 or less
	 * number of bits. On x86_64 architecture, loss of precision will occur if the integer value is of more than 32 bit.
	 * 
	 * <p>The data has been transmitted out of serial port when this method returns.</p>
	 * 
	 * In java numbers are represented in 2's complement, so number 650 whose binary representation is 0000001010001010
	 * is printed byte by byte, then will be printed as 1 and -118, because 10001010 in 2's complement is negative number.
	 * 
	 * @param handle handle of the opened port on which to write byte
	 * @param data an integer number to be sent to port
	 * @param delay interval between two successive bytes 
	 * @param endianness big or little endian sequence to be followed while sending bytes representing this integer
	 * @param numOfBytes number of bytes this integer can be represented in
	 * @return true on success false otherwise
	 */
	public boolean writeSingleInt(long handle, int data, int delay, ENDIAN endianness, NUMOFBYTES numOfBytes) {
		byte[] buffer = null;
		
		if(numOfBytes.getValue() == 2) {             // conversion to two bytes data
			buffer = new byte[2];
			if(endianness.getValue() == 1) {         // Little endian
				buffer[1] = (byte) (data >>> 8);
				buffer[0] = (byte)  data;
			}else {                                 // big endian/default (java is big endian by default)
				buffer[1] = (byte)  data;
				buffer[0] = (byte) (data >>> 8);
			}
			return writeBytes(handle, buffer, delay);
		}else {                                     // conversion to four bytes data
			buffer = new byte[4];
			if(endianness.getValue() == 1) {        // Little endian
				buffer[3] = (byte) (data >>> 24);
				buffer[2] = (byte) (data >>> 16);
				buffer[1] = (byte) (data >>> 8);
				buffer[0] = (byte)  data;
			}else {                                 // big endian/default (java is big endian by default)
				buffer[3] = (byte)  data;
				buffer[2] = (byte) (data >>> 8);
				buffer[1] = (byte) (data >>> 16);
				buffer[0] = (byte) (data >>> 24);
			}
			return writeBytes(handle, buffer, delay);
		}
	}

	/** 
	 * This method send an array of integers on the specified port. The data has been transmitted out of serial 
	 * port when this method returns.
	 * 
	 * @param handle handle of the opened port on which to write byte
	 * @param buffer an array of integers to be sent to port
	 * @param delay interval between two successive bytes 
	 * @param endianness big or little endian sequence to be followed while sending bytes representing this integer
	 * @param numOfBytes number of bytes this integer can be represented in
	 * @return true on success false otherwise
	 */
	public boolean writeIntArray(long handle, int[] buffer, int delay, ENDIAN endianness, NUMOFBYTES numOfBytes) {
		byte[] localBuf = null;
		
		if(numOfBytes.getValue() == 2) {
			localBuf = new byte[2 * buffer.length];
			if(endianness.getValue() == 1) {                 // little endian
				int a = 0;
				for(int b=0; b<buffer.length; b++) {
					localBuf[a] = (byte)  buffer[b];
					a++;
					localBuf[a] = (byte) (buffer[b] >>> 8);
					a++;
				}
			}else {                                         // big/default endian
				int c = 0;
				for(int d=0; d<buffer.length; d++) {
					localBuf[c] = (byte) (buffer[d] >>> 8);
					c++;
					localBuf[c] = (byte)  buffer[d];
					c++;
				}
			}
			return writeBytes(handle, localBuf, delay);
		}else {
			localBuf = new byte[4 * buffer.length];
			if(endianness.getValue() == 1) {                  // little endian
				int e = 0;
				for(int f=0; f<buffer.length; f++) {
					localBuf[e] = (byte)  buffer[f];
					e++;
					localBuf[e] = (byte) (buffer[f] >>> 8);
					e++;
					localBuf[e] = (byte) (buffer[f] >>> 16);
					e++;
					localBuf[e] = (byte) (buffer[f] >>> 24);
					e++;
				}
			}else {                                          // big/default endian
				int g = 0;
				for(int h=0; h<buffer.length; h++) {
					localBuf[g] = (byte)  buffer[h];
					g++;
					localBuf[g] = (byte) (buffer[h] >>> 8);
					g++;
					localBuf[g] = (byte) (buffer[h] >>> 16);
					g++;
					localBuf[g] = (byte) (buffer[h] >>> 24);
					g++;
				}
			}
			return writeBytes(handle, localBuf, delay);
		}
	}

	/** 
	 * Read specified number of bytes from serial port. If any error is encountered then native library put
	 * that negative error number at 0th index of byte buffer. We do not validate arguments supplied to serve as
	 * fast as possible to the caller. Application can call this method even when they have registered a listener.
	 * 
	 * <p>Note that, we do not prevent caller from reading port even if he has registered a event listener for
	 * specified port. There may be cases where caller wants to read asynchronously outside the listener. It is callers
	 * responsibility to manage complexity associated with this use case.</p>
	 * 
	 * @param handle of port from which to read bytes
	 * @param byteCount number of bytes to read from this port
	 * @throws SerialComException if it is unable to read bytes from port
	 * @return array of bytes read from port
	 */
	public byte[] readBytes(long handle, int byteCount) throws SerialComException {
		byte[] buffer = mNativeInterface.readBytes(handle, byteCount);
		if(buffer == null) { 
			throw new SerialComException("readBytes()", mErrMapper.getMappedError(-240));
		}
		return buffer;
	}

	/** 
	 * If user does not specify any count, library read DEFAULT_READBYTECOUNT bytes as default value.
	 * 
	 * @param handle of port from which to read bytes
	 * @throws SerialComException if it is unable to read bytes from port
	 * @return array of bytes read from port
	 */
	public byte[] readBytes(long handle) throws SerialComException {
		return readBytes(handle, DEFAULT_READBYTECOUNT);
	}

	/**
	 * This method is for user to read input data as string and the method handles the conversion from bytes to string.
	 * Caller has more finer control over the byte operation
	 * 
	 * @param handle of port from which to read bytes
	 * @param byteCount number of bytes to read from this port
	 * @throws SerialComException if it is unable to read bytes from port
	 * @return string formed from bytes read from port
	 */
	public String readString(long handle, int byteCount) throws SerialComException {
		byte[] buffer = readBytes(handle, byteCount);
		return new String(buffer);
	}
	
	/**
	 * This method is for user to read input data as string and the method handles the conversion from bytes to string.
	 * 
	 * @param handle of port from which to read bytes
	 * @throws SerialComException if it is unable to read bytes from port
	 * @return string formed from bytes read from port
	 */
	public String readString(long handle) throws SerialComException {
		return readString(handle, DEFAULT_READBYTECOUNT);
	}

	/**
	 * This method configures the rate at which communication will occur and the format of data frame. Note that, most of the DTE/DCE (hardware)
	 * does not support different baud rates for transmission and reception and therefore we take only single value applicable to both transmission and
	 * reception. Further, all the hardware and OS does not support all the baud rates (maximum change in signal per second). It is the applications 
	 * responsibility to consider these factors when writing portable software.
	 * 
	 * <p>If parity is enabled, the parity bit will be removed from frame before passing it library.</p>
	 * 
	 * Note: (1) some restrictions apply in case of Windows. Please refer http://msdn.microsoft.com/en-us/library/windows/desktop/aa363214(v=vs.85).aspx
	 * for details.
	 * 
	 * <p>(2) Some drivers especially windows driver for usb to serial converters support non-standard baud rates. They either supply a text file that can be used for 
	 * configuration or user may edit windows registry directly to enable this support. The user supplied standard baud rate is translated to custom baud rate as 
	 * specified in vendor specific configuration file.</p>
	 * 
	 * @param handle of opened port to which this configuration applies to
	 * @param dataBits number of data bits in one frame (refer DATABITS enum for this)
	 * @param stopBits number of stop bits in one frame (refer STOPBITS enum for this)
	 * @param parity of the frame (refer PARITY enum for this)
	 * @param baudRate of the frame (refer BAUDRATE enum for this)
	 * @param custBaud custom baudrate if the desired rate is not included in BAUDRATE enum
	 * @throws SerialComException if invalid handle is passed or an error occurs in configuring the port
	 * @return true on success false otherwise
	 */
	public boolean configureComPortData(long handle, DATABITS dataBits, STOPBITS stopBits, PARITY parity, BAUDRATE baudRate, int custBaud) throws SerialComException {

		int baudRateTranslated = 0;
		int custBaudTranslated = 0;
		int baudRateGiven = baudRate.getValue();
		
		boolean handlefound = false;
		for(SerialComPortHandleInfo mInfo: mPortHandleInfo){
			if(mInfo.containsHandle(handle)) {
				handlefound = true;
				break;
			}
		}
		if(handlefound == false) {
			throw new SerialComException("configureComPortData()", SerialComErrorMapper.ERR_WRONG_HANDLE);
		}
		
		if(baudRateGiven != 251) {
			baudRateTranslated = baudRateGiven;
			custBaudTranslated = 0;
		}else {
			// custom baud rate
			baudRateTranslated = baudRateGiven;
			custBaudTranslated = custBaud;
		}
		
		int ret = mNativeInterface.configureComPortData(handle, dataBits.getValue(), stopBits.getValue(), parity.getValue(), baudRateTranslated, custBaudTranslated);
		if(ret < 0) {
			throw new SerialComException("configureComPortData()", mErrMapper.getMappedError(ret));
		}
		
		return true;
	}
	
	/**
	 * This method configures the way data communication will be controlled between DTE and DCE. This specifies flow control and actions that will
	 * be taken when an error is encountered in communication.
	 * 
	 * @param handle of opened port to which this configuration applies to
	 * @param flowctrl flow control, how data flow will be controlled (refer FLOWCONTROL enum for this)
	 * @param xon character representing on condition if software flow control is used
	 * @param xoff character representing off condition if software flow control is used
	 * @param ParFraError true if parity and frame errors are to be checked false otherwise
	 * @param overFlowErr true if overflow error is to be detected false otherwise
	 * @throws SerialComException if invalid handle is passed or an error occurs in configuring the port
	 * @return true on success false otherwise
	 */
	public boolean configureComPortControl(long handle, FLOWCONTROL flowctrl, char xon, char xoff, boolean ParFraError, boolean overFlowErr) throws SerialComException {

		boolean handlefound = false;
		for(SerialComPortHandleInfo mInfo: mPortHandleInfo){
			if(mInfo.containsHandle(handle)) {
				handlefound = true;
				break;
			}
		}
		if(handlefound == false) {
			throw new SerialComException("configureComPortControl()", SerialComErrorMapper.ERR_WRONG_HANDLE);
		}
		
		int ret = mNativeInterface.configureComPortControl(handle, flowctrl.getValue(), xon, xoff, ParFraError, overFlowErr);
		if(ret < 0) {
			throw new SerialComException("configureComPortControl()", mErrMapper.getMappedError(ret));
		}
		
		return true;
	}
	
	/**
	 * This method gives currently applicable settings associated with particular serial port.
	 * The values are bit mask so that application can manipulate them to get required information.
	 * 
	 * <p>For Linux the order is : c_iflag, c_oflag, c_cflag, c_lflag, c_line, c_cc[0], c_cc[1], c_cc[2], c_cc[3]
	 * c_cc[4], c_cc[5], c_cc[6], c_cc[7], c_cc[8], c_cc[9], c_cc[10], c_cc[11], c_cc[12], c_cc[13], c_cc[14],
	 * c_cc[15], c_cc[16], c_ispeed and c_ospeed.</p>
	 * 
	 * For Windows the order is :DCBlength, BaudRate, fBinary, fParity, fOutxCtsFlow, fOutxDsrFlow, fDtrControl,
	 * fDsrSensitivity, fTXContinueOnXoff, fOutX, fInX, fErrorChar, fNull, fRtsControl, fAbortOnError, fDummy2,
	 * wReserved, XonLim, XoffLim, ByteSize, Parity, StopBits, XonChar, XoffChar, ErrorChar, StopBits, EvtChar,
	 * wReserved1.
	 * 
	 * @param handle of the opened port
	 * @throws SerialComException if invalid handle is passed or an error occurs while reading current settings
	 * @return array of string giving configuration
	 */
	public String[] getCurrentConfiguration(long handle) throws SerialComException {

		boolean handlefound = false;
		for(SerialComPortHandleInfo mInfo: mPortHandleInfo){
			if(mInfo.containsHandle(handle)) {
				handlefound = true;
				break;
			}
		}
		if(handlefound == false) {
			throw new SerialComException("getCurrentConfiguration()", SerialComErrorMapper.ERR_WRONG_HANDLE);
		}
		
		if(getOSType() != OS_WINDOWS) {
			// for unix-like os
			int[] config = mNativeInterface.getCurrentConfigurationU(handle);
			String[] configuration = new String[config.length];
			if(config[0] < 0) {
				throw new SerialComException("getCurrentConfiguration()", mErrMapper.getMappedError(config[0]));
			}
			// if an error occurs, config[0] will contain error code, otherwise actual data
			for(int x=0; x<config.length; x++) {
				configuration[x] = "" + config[x];
			}
			return configuration;
		}else {
			// for windows os
			String[] configuration = mNativeInterface.getCurrentConfigurationW(handle);
			return configuration;
		}
	}

	/**
	 * This method assert/de-assert RTS line of serial port. Set "true" for asserting signal, false otherwise.
	 * 
	 * <p>The RS-232 standard defines the voltage levels that correspond to logical one and logical zero levels for the data 
	 * transmission and the control signal lines. Valid signals are either in the range of +3 to +15 volts or the range 
	 * −3 to −15 volts with respect to the ground/common pin; consequently, the range between −3 to +3 volts is not a 
	 * valid RS-232 level.</p>
	 * 
	 * For data lines (TxD, RxD and their secondary channel equivalents) logic one is defined as a negative voltage, the 
	 * signal condition is called "mark". Logic zero is positive and the signal condition is termed "space". 
	 * 
	 * <p>Control signals have the opposite polarity: the asserted or active state is positive voltage and the de-asserted 
	 * or inactive state is negative voltage. Examples of control lines include request to send (RTS), clear to send (CTS), 
	 * data terminal ready (DTR), and data set ready (DSR).</p>
	 * 
	 * @param handle of the opened port
	 * @param enabled if true RTS will be asserted and vice-versa
	 * @throws SerialComException if system is unable to complete requested operation
	 * @return true on success false otherwise
	 */
	public boolean setRTS(long handle, boolean enabled) throws SerialComException {
		int ret = mNativeInterface.setRTS(handle, enabled);
		if(ret < 0) {
			throw new SerialComException("setRTS()", mErrMapper.getMappedError(ret));
		}
		return true;
	}

	/**
	 * This method assert/de-assert DTR line of serial port. Set "true" for asserting signal, false otherwise.
	 * 
	 * @param handle of the opened port
	 * @param enabled if true DTR will be asserted and vice-versa
	 * @throws SerialComException if system is unable to complete requested operation
	 * @return true on success false otherwise
	 */
	public boolean setDTR(long handle, boolean enabled) throws SerialComException {
		int ret = mNativeInterface.setDTR(handle, enabled);
		if(ret < 0) {
			throw new SerialComException("setDTR()", mErrMapper.getMappedError(ret));
		}
		return true;
	}

	/**
	 * This method associate a data looper with the given listener. This looper will keep delivering new data whenever
	 * it is made available from native data collection and dispatching subsystem.
	 * Note that listener will start receiving new data, even before this method returns.
	 * 
	 * @param handle of the port opened
	 * @param dataListener instance of class which implements ISerialComDataListener interface
	 * @throws SerialComException if invalid handle passed, handle is null or data listener already exist for this handle
	 * @return true on success false otherwise
	 */
	public boolean registerDataListener(long handle, ISerialComDataListener dataListener) throws SerialComException {
		
		boolean handlefound = false;
		SerialComPortHandleInfo mHandleInfo = null;
		
		if (dataListener == null) {
			throw new NullPointerException("registerDataListener(), " + SerialComErrorMapper.ERR_NULL_POINTER_FOR_LISTENER);
		}
		
		for(SerialComPortHandleInfo mInfo: mPortHandleInfo){
			if(mInfo.containsHandle(handle)) {
				handlefound = true;
				if(mInfo.getDataListener() != null) {
					throw new SerialComException("registerDataListener()", SerialComErrorMapper.ERR_LISTENER_ALREADY_EXIST);
				}else {
					mHandleInfo = mInfo;
				}
				break;
			}
		}
		
		if(handlefound == false) {
			throw new SerialComException("registerDataListener()", SerialComErrorMapper.ERR_WRONG_HANDLE);
		}
		
		return mEventCompletionDispatcher.setUpDataLooper(handle, mHandleInfo, dataListener);
	}
	
	/**
	 * This method destroys complete java and native looper subsystem associated with this particular data listener. This has no
	 * effect on event looper subsystem.
	 * 
	 * @param dataListener instance of class which implemented ISerialComDataListener interface
	 * @throws SerialComException if null value is passed in dataListener field
	 * @return true on success false otherwise
	 */
	public boolean unregisterDataListener(ISerialComDataListener dataListener) throws SerialComException {
		if(dataListener == null) {
			throw new NullPointerException("unregisterDataListener(), " + SerialComErrorMapper.ERR_NULL_POINTER_FOR_LISTENER);
		}
		
		if(mEventCompletionDispatcher.destroyDataLooper(dataListener)) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * By default, the data listener will be called for every single byte available. This may not be optimal in case
	 * of data, but may be critical in case data actually is part of some custom protocol. So, applications can
	 * dynamically change the behaviour of 'calling data listener' based on the amount of data availability.
	 * 
	 * <p>Note: (1) If the port has been opened by more than one user, all the users will be affected by this method.
	 * (2) This is not supported on Windows OS</p>
	 * 
	 * @param handle of the opened port
	 * @param numOfBytes minimum number of bytes that would have been read from port to pass to listener
	 * @throws SerialComException if invalid value for numOfBytes is passed, wrong handle is passed, operation can not be done successfully
	 * @return true on success false otherwise
	 */
	public boolean setMinDataLength(long handle, int numOfBytes) throws SerialComException {
		
		if(getOSType() == OS_WINDOWS) {
			return false;
		}
		
		boolean handlefound = false;
		if (numOfBytes < 0) {
			throw new NullPointerException("setMinDataLength(), " + SerialComErrorMapper.ERR_INVALID_DATA_LENGTH);
		}
		
		for(SerialComPortHandleInfo mInfo: mPortHandleInfo){
			if(mInfo.containsHandle(handle)) {
				handlefound = true;
				break;
			}
		}
		
		if(handlefound == false) {
			throw new SerialComException("setMinDataLength()", SerialComErrorMapper.ERR_WRONG_HANDLE);
		}
		
		int ret = mNativeInterface.setMinDataLength(handle, numOfBytes);
		if(ret < 0) {
			throw new SerialComException("setMinDataLength()",  mErrMapper.getMappedError(ret));
		}
		return true;
	}

	/**
	 * This method associate a event looper with the given listener. This looper will keep delivering new event whenever
	 * it is made available from native event collection and dispatching subsystem.
	 * 
	 * <p>By default all four events are dispatched to listener. However, application can mask events through setEventsMask()
	 * method. In current implementation, native code sends all the events irrespective of mask and we actually filter
	 * them in java layers, to decide whether this should be sent to application or not (as per the mask set by
	 * setEventsMask() method).</p>
	 * 
	 * @param handle of the port opened
	 * @param eventListener instance of class which implements ISerialComEventListener interface
	 * @throws SerialComException if invalid handle passed, handle is null or event listener already exist for this handle
	 * @return true on success false otherwise
	 */
	public boolean registerLineEventListener(long handle, ISerialComEventListener eventListener) throws SerialComException {
		boolean handlefound = false;
		SerialComPortHandleInfo mHandleInfo = null;
		
		if(eventListener == null) {
			throw new NullPointerException("registerLineEventListener(), " + SerialComErrorMapper.ERR_NULL_POINTER_FOR_LISTENER);
		}
		
		for(SerialComPortHandleInfo mInfo: mPortHandleInfo){
			if(mInfo.containsHandle(handle)) {
				handlefound = true;
				if(mInfo.getEventListener() != null) {
					throw new SerialComException("registerLineEventListener()", SerialComErrorMapper.ERR_LISTENER_ALREADY_EXIST);
				}else {
					mHandleInfo = mInfo;
				}
				break;
			}
		}
		
		if(handlefound == false) {
			throw new SerialComException("registerLineEventListener()", SerialComErrorMapper.ERR_WRONG_HANDLE);
		}
		
		return mEventCompletionDispatcher.setUpEventLooper(handle, mHandleInfo, eventListener);
	}
	
	/**
	 * This method destroys complete java and native looper subsystem associated with this particular event listener. This has no
	 * effect on data looper subsystem.
	 * 
	 * @param eventListener instance of class which implemented ISerialComEventListener interface
	 * @throws SerialComException if null value is passed in eventListener field
	 * @return true on success false otherwise
	 */
	public boolean unregisterLineEventListener(ISerialComEventListener eventListener) throws SerialComException {
		if (eventListener == null) {
			throw new NullPointerException("unregisterLineEventListener(), " + SerialComErrorMapper.ERR_NULL_POINTER_FOR_LISTENER);
		}
		if(mEventCompletionDispatcher.destroyEventLooper(eventListener)) {
			return true;
		}
		
		return false;
	}

	
	/**
	 * The user don't need data for some time or he may be managing data more efficiently.
	 * 
	 * @param eventListener instance of class which implemented ISerialComEventListener interface
	 * @throws SerialComException if null is passed for eventListener field
	 * @return true on success false otherwise
	 */
	public boolean pauseListeningEvents(ISerialComEventListener eventListener) throws SerialComException {
		if(eventListener == null) {
			throw new NullPointerException("pauseListeningEvents(), " + SerialComErrorMapper.ERR_NULL_POINTER_FOR_LISTENER);

		}
		if(mEventCompletionDispatcher.pauseListeningEvents(eventListener)) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * The user don't need data for some time or he may be managing data more efficiently.
	 * Note that the native thread will continue to receive events and data, it will pass this data to
	 * java layer. User must be careful that new data will exist in queue if received after pausing, but
	 * it will not get delivered to application.
	 * 
	 * @param eventListener is an instance of class which implements ISerialComEventListener
	 * @throws SerialComException
	 * @return true on success false otherwise
	 */
	public boolean resumeListeningEvents(ISerialComEventListener eventListener) throws SerialComException {
		if(eventListener == null) {
			throw new NullPointerException("pauseListeningEvents(), " + SerialComErrorMapper.ERR_NULL_POINTER_FOR_LISTENER);

		}
		if(mEventCompletionDispatcher.resumeListeningEvents(eventListener)) {
			return true;
		}
		
		return false;
	}

	/**
	 * In future we may shift modifying mask in the native code itself, so as to prevent JNI transitions.
	 * This filters what events should be sent to application. Note that, although we sent only those event
	 * for which user has set mask, however native code send all the events to java layer as of now.
	 * 
	 * @param eventListener instance of class which implemented ISerialComEventListener interface
	 * @throws SerialComException if null is passed for listener field or invalid listener is passed
	 * @return true on success false otherwise
	 */
	public boolean setEventsMask(ISerialComEventListener eventListener, int newMask) throws SerialComException {
		
		SerialComLooper looper = null;
		ISerialComEventListener mEventListener = null;
		
		if(eventListener == null) {
			throw new NullPointerException("setEventsMask(), " + SerialComErrorMapper.ERR_NULL_POINTER_FOR_LISTENER);
		}

		for(SerialComPortHandleInfo mInfo: mPortHandleInfo){
			if(mInfo.containsEventListener(eventListener)) {
				looper = mInfo.getLooper();
				mEventListener = mInfo.getEventListener();
				break;
			}
		}
		
		if(looper != null && mEventListener != null) {
			looper.setEventsMask(newMask);
			return true;
		}else {
			throw new SerialComException("setEventsMask()", SerialComErrorMapper.ERR_WRONG_LISTENER_PASSED);
		}
	}

	/**
	 * This method return currently applicable mask for events on serial port.
	 * 
	 * @param eventListener instance of class which implemented ISerialComEventListener interface
	 * @throws SerialComException if null or wrong listener is passed
	 * @return an integer containing bit fields representing mask
	 */
	public int getEventsMask(ISerialComEventListener eventListener) throws SerialComException {
		
		SerialComLooper looper = null;
		ISerialComEventListener mEventListener = null;
		
		if(eventListener == null) {
			throw new NullPointerException("getEventsMask(), " + SerialComErrorMapper.ERR_NULL_POINTER_FOR_LISTENER);
		}
		

		for(SerialComPortHandleInfo mInfo: mPortHandleInfo){
			if(mInfo.containsEventListener(eventListener)) {
				looper = mInfo.getLooper();
				mEventListener = mInfo.getEventListener();
				break;
			}
		}
		
		if(looper != null && mEventListener != null) {
			return looper.getEventsMask();
		}else {
			throw new SerialComException("setEventsMask()", SerialComErrorMapper.ERR_WRONG_LISTENER_PASSED);
		}
	}

	/**
	 * Discards data sent to port but not transmitted, or data received but not read. Some device/OS/driver might
	 * not have support for this, but most of them may have.
	 * If there is some data to be pending for transmission, it will be discarded and therefore no longer sent.
	 * If the application wants to make sure that all data has been transmitted before discarding anything, it must
	 * first flush data and then call this method.
	 * 
	 * @param handle of the opened port
	 * @param clearRxPort if true receive buffer will be cleared otherwise will be left untouched 
	 * @param clearTxPort if true transmit buffer will be cleared otherwise will be left untouched
	 * @throws SerialComException if invalid handle is passed or operation can not be completed successfully
	 * @return true on success
	 */
	public synchronized boolean clearPortIOBuffers(long handle, boolean clearRxPort, boolean clearTxPort) throws SerialComException {
		boolean handlefound = false;
		for(SerialComPortHandleInfo mInfo: mPortHandleInfo){
			if(mInfo.containsHandle(handle)) {
				handlefound = true;
				break;
			}
		}
		if(handlefound == false) {
			throw new SerialComException("clearPortIOBuffers()", SerialComErrorMapper.ERR_WRONG_HANDLE);
		}
		
		if(clearRxPort == true || clearTxPort == true) {
			int ret = mNativeInterface.clearPortIOBuffers(handle, clearRxPort, clearTxPort);
			if(ret < 0) {
				throw new SerialComException("clearPortIOBuffers()", mErrMapper.getMappedError(ret));
			}
		}
		
		return true;
	}
	
	/**
	 * Assert a break condition on the specified port for the duration expressed in milliseconds.
	 * If the line is held in the logic low condition (space in UART jargon) for longer than a character 
	 * time, this is a break condition that can be detected by the UART.
	 * 
	 * A "break condition" occurs when the receiver input is at the "space" level for longer than some duration
	 * of time, typically, for more than a character time. This is not necessarily an error, but appears to the
	 * receiver as a character of all zero bits with a framing error. The term "break" derives from current loop
	 * signalling, which was the traditional signalling used for tele-typewriters. The "spacing" condition of a 
	 * current loop line is indicated by no current flowing, and a very long period of no current flowing is often
	 * caused by a break or other fault in the line.
	 * 
	 * @param handle of the opened port
	 * @param duration the time in milliseconds for which break will be active
	 * @throws SerialComException if invalid handle is passed or operation can not be successfully completed
	 * @return true on success
	 */
	public synchronized boolean sendBreak(long handle, int duration) throws SerialComException {
		boolean handlefound = false;
		for(SerialComPortHandleInfo mInfo: mPortHandleInfo){
			if(mInfo.containsHandle(handle)) {
				handlefound = true;
				break;
			}
		}
		if(handlefound == false) {
			throw new SerialComException("sendBreak()", SerialComErrorMapper.ERR_WRONG_HANDLE);
		}
		
		int ret = mNativeInterface.sendBreak(handle, duration);
		if(ret < 0) {
			throw new SerialComException("sendBreak()", mErrMapper.getMappedError(ret));
		}
		
		return true;
	}
	
	/**
	 * This method gives the number of serial line interrupts that have occurred. The interrupt count is in following
	 * order in array beginning from index 0 and ending with 11th index :
	 * CTS, DSR, RING, CARRIER DETECT, RECEIVER BUFFER, TRANSMIT BUFFER, FRAME ERROR, OVERRUN ERROR, PARITY ERROR,
	 * BREAK AND BUFFER OVERRUN.
	 * 
	 * <p>Note: It is supported on Linux OS only. For other operating systems, this will return 0 for all the indexes.</p>
	 * 
	 * @param handle of the port opened on which interrupts might have occurred
	 * @throws SerialComException if invalid handle is passed or operation can not be completed
	 * @return array of integers containing values corresponding to each interrupt source
	 */
	public int[] getInterruptCount(long handle) throws SerialComException {
		int x = 0;
		boolean handlefound = false;
		int[] ret = null;
		int[] interruptsCount = new int[11];
		
		for(SerialComPortHandleInfo mInfo: mPortHandleInfo){
			if(mInfo.containsHandle(handle)) {
				handlefound = true;
				break;
			}
		}
		if(handlefound == false) {
			throw new SerialComException("getInterruptCount()", SerialComErrorMapper.ERR_WRONG_HANDLE);
		}
		
		ret = mNativeInterface.getInterruptCount(handle);
		if(ret[0] < 0) {
			throw new SerialComException("getInterruptCount()", mErrMapper.getMappedError(ret[0]));
		}
		
		for(x=0; x<11; x++) {
			interruptsCount[x] = ret[x];
		}
		
		return interruptsCount;
	}
	
	/**
	 * <p>Gives status of serial port's control lines as supported by underlying operating system.</p>
	 * 
	 * The sequence of status in returned array is :
	 * Linux    : CTS, DSR, DCD, RI, LOOP, RTS, DTR respectively.
	 * MAC OS X : CTS, DSR, DCD, RI, 0,    RTS, DTR respectively.
	 * Windows  : CTS, DSR, DCD, RI, 0,    0,   0   respectively.
	 * 
	 * @param handle of the port opened
	 * @throws SerialComException if invalid handle is passed or operation can not be completed successfully
	 * @return status of control lines
	 */
	public int[] getLinesStatus(long handle) throws SerialComException {
		int x = 0;
		boolean handlefound = false;
		int[] ret = null;
		int[] status = {0,0,0,0,0,0,0};
		
		for(SerialComPortHandleInfo mInfo: mPortHandleInfo){
			if(mInfo.containsHandle(handle)) {
				handlefound = true;
				break;
			}
		}
		if(handlefound == false) {
			throw new SerialComException("getLinesStatus()", SerialComErrorMapper.ERR_WRONG_HANDLE);
		}
		
		ret = mNativeInterface.getLinesStatus(handle);
		if(ret[0] < 0) {
			throw new SerialComException("getLinesStatus()", mErrMapper.getMappedError(ret[0]));
		}
		
		for(x=0; x<7; x++) {
			status[x] = ret[x+1];
		}
		
		return status;
	}
	
	/**
	 * Get number of bytes in input and output port buffers used by operating system for instance tty buffers
	 * in Unix like systems. Sequence of data in array is : Input count, Output count.
	 * 
	 * <p>It should be noted that some chipset specially USB to UART converters might have FIFO buffers in chipset
	 * itself. For this reason number of bytes reported by this method and actual bytes received might differ.
	 * This is driver and OS specific scenario.</p>
	 * 
	 * @param handle of the opened port
	 * @throws SerialComException if invalid handle is passed or operation can not be completed successfully
	 * @return array containing number of bytes in input/output buffer
	 */
	public int[] getByteCountInPortIOBuffer(long handle) throws SerialComException {
		boolean handlefound = false;
		int[] ret = null;
		int[] numBytesInfo = {0,0};
		
		for(SerialComPortHandleInfo mInfo: mPortHandleInfo){
			if(mInfo.containsHandle(handle)) {
				handlefound = true;
				break;
			}
		}
		if(handlefound == false) {
			throw new SerialComException("getByteCountInPortIOBuffer()", SerialComErrorMapper.ERR_WRONG_HANDLE);
		}
		
		// ret[0]=error info, ret[1]=byte count in input buffer, ret[2]=byte count in output buffer
		ret = mNativeInterface.getByteCount(handle);
		if(ret[0] < 0) {
			throw new SerialComException("getByteCountInPortIOBuffer()", mErrMapper.getMappedError(ret[0]));
		}
		
		numBytesInfo[0] = ret[1];  // Input buffer count
		numBytesInfo[1] = ret[2];  // Output buffer count
		
		return numBytesInfo;
	}
	
	/**
	 * This will create a native thread that will invoke given listener whenever given port is removed.
	 * This is currently not supported for Solaris.
	 * 
	 * @param handle
	 * @return true on success false otherwise
	 * @throws SerialComException
	 */
	public boolean registerPortMonitorListener(long handle, IPortMonitor portMonitor) throws SerialComException {
		boolean handlefound = false;
		String portName = null;
		int ret = 0;
		
		for(SerialComPortHandleInfo mInfo: mPortHandleInfo){
			if(mInfo.containsHandle(handle)) {
				portName = mInfo.getOpenedPortName();
				handlefound = true;
				break;
			}
		}
		if(handlefound == false) {
			throw new SerialComException("registerPortMonitorListener()", SerialComErrorMapper.ERR_WRONG_HANDLE);
		}
		
		ret = mNativeInterface.registerPortMonitorListener(handle, portName, portMonitor);
		if(ret < 0) {
			throw new SerialComException("registerPortMonitorListener()", mErrMapper.getMappedError(ret));
		}
		
		return false;
	}

	/**
	 * Enable printing debugging messages and stack trace for development and debugging purpose.
	 * 
	 * @param enable if true debugging messages will be printed otherwise not
	 */
	public void enableDebugging(boolean enable) {
		mNativeInterface.debug(enable);
		SerialComManager.DEBUG = enable;
	}
}
