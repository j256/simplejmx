package com.j256.simplejmx.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.j256.simplejmx.common.IoUtils;

/**
 * Command-line client that can be used to support interactive or batch-file JMX operations. It can be used with the
 * {@link Main} class as a jmx client out of the SimpleJMX jar directly.
 * 
 * <p>
 * See the {@link CommandLineJmxClient#helpOutput()} and {@link CommandLineJmxClient#exampleOutput()} methods for
 * extensive help/usage information.
 * </p>
 * 
 * @author graywatson
 */
public class CommandLineJmxClient {

	private static final String HELP_COMMAND = "help";
	private static final String DEFAULT_PROMPT = "Jmx: ";

	private JmxClient jmxClient;

	/**
	 * Create a command line interface on the passed in client.
	 */
	public CommandLineJmxClient(JmxClient jmxClient) {
		this.jmxClient = jmxClient;
	}

	/**
	 * Create a command line interface connected to the local host at a certain port number.
	 */
	public CommandLineJmxClient(int port) throws JMException {
		jmxClient = new JmxClient(port);
	}

	/**
	 * Create a command line interface connected to a host and port combination.
	 */
	public CommandLineJmxClient(String host, int port) throws JMException {
		jmxClient = new JmxClient(host, port);
	}

	/**
	 * Connect the client to an address and port combination.
	 */
	public CommandLineJmxClient(InetAddress address, int port) throws JMException {
		this(address.getHostAddress(), port);
	}

	/**
	 * <p>
	 * Create a command line interface connected to a JMX server using the full JMX URL format. The URL should look
	 * something like:
	 * </p>
	 * 
	 * <pre>
	 * service:jmx:rmi:///jndi/rmi://hostName:portNumber/jmxrmi
	 * </pre>
	 */
	public CommandLineJmxClient(String jmxUrl) throws JMException {
		jmxClient = new JmxClient(jmxUrl);
	}

	/**
	 * Run commands from the String array.
	 */
	public void runCommands(final String[] commands) throws IOException {
		doLines(0, new LineReader() {
			private int commandC = 0;

			@Override
			public String getNextLine(String prompt) {
				if (commandC >= commands.length) {
					return null;
				} else {
					return commands[commandC++];
				}
			}
		}, true);
	}

	/**
	 * Read in commands from the batch-file and execute them.
	 */
	public void runBatchFile(File batchFile) throws IOException {
		final BufferedReader reader = new BufferedReader(new FileReader(batchFile));
		try {
			doLines(0, new LineReader() {
				@Override
				public String getNextLine(String prompt) throws IOException {
					return reader.readLine();
				}
			}, true);
		} finally {
			reader.close();
		}
	}

	/**
	 * Run the Jmx command line client reading commands from {@link System#in}.
	 */
	public void runCommandLine() throws IOException {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		try {
			System.out.println("Running jmx client interface.  Type '" + HELP_COMMAND + "' for help.");
			doLines(0, new LineReader() {
				@Override
				public String getNextLine(String prompt) throws IOException {
					System.out.print(prompt);
					System.out.flush();
					return reader.readLine();
				}
			}, false);
		} finally {
			reader.close();
		}
	}

	/**
	 * Close the associated Jmx client.
	 */
	public void close() {
		IoUtils.closeQuietly(jmxClient);
		jmxClient = null;
	}

	/**
	 * Do the lines from the reader. This might go recursive if we run a script.
	 */
	private void doLines(int levelC, LineReader lineReader, boolean batch) throws IOException {
		if (levelC > 20) {
			System.out.print("Ignoring possible recursion after including 20 times");
			return;
		}
		while (true) {
			String line = lineReader.getNextLine(DEFAULT_PROMPT);
			if (line == null) {
				break;
			}
			// skip blank lines and comments
			if (line.length() == 0 || line.startsWith("#") || line.startsWith("//")) {
				continue;
			}
			if (batch) {
				// if we are in batch mode, spit out the line we just read
				System.out.println("> " + line);
			}
			String[] lineParts = line.split(" ");
			String command = lineParts[0];
			if (command.startsWith(HELP_COMMAND)) {
				helpOutput();
			} else if (command.startsWith("objects")) {
				listBeans(lineParts);
			} else if (command.startsWith("run")) {
				if (lineParts.length == 2) {
					runScript(lineParts[1], levelC);
				} else {
					System.out.println("Error.  Usage: run script");
				}
			} else if (command.startsWith("attrs")) {
				listAttributes(lineParts);
			} else if (command.startsWith("get")) {
				if (lineParts.length == 2) {
					getAttributes(lineParts);
				} else {
					getAttribute(lineParts);
				}
			} else if (command.startsWith("set")) {
				setAttribute(lineParts);
			} else if (command.startsWith("opers") || command.startsWith("ops")) {
				listOperations(lineParts);
			} else if (command.startsWith("dolines")) {
				invokeOperationLines(lineReader, lineParts, batch);
			} else if (command.startsWith("do")) {
				invokeOperation(lineParts);
			} else if (command.startsWith("sleep")) {
				if (lineParts.length == 2) {
					try {
						Thread.sleep(Long.parseLong(lineParts[1]));
					} catch (NumberFormatException e) {
						System.out.println("Error.  Usage: sleep millis, invalid millis number '" + lineParts[1] + "'");
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				} else {
					System.out.println("Error.  Usage: sleep millis");
				}
			} else if (command.startsWith("examples")) {
				exampleOutput();
			} else if (command.startsWith("quit")) {
				break;
			} else {
				System.out.println("Error.  Unknown command.  Type '" + HELP_COMMAND + "' for help: " + command);
			}
		}
	}

	private void helpOutput() {
		System.out.println("objects [regex] -  list object-names exposed by JMX");
		System.out.println("run script  -  execute a script from a file path");
		System.out.println(HELP_COMMAND + "  -  output this information");
		System.out.println("examples  -  examples on how to use this utility");
		System.out.println("sleep millis  -  sleep for a certain nunber of milliseconds (for scripts)");
		System.out.println("quit  -  quit this application");
		System.out.println("");
		System.out.println("attrs object-name  -  list the attributes associated with the object-name");
		System.out.println("get object-name  -  get all values associated with this attribute");
		System.out.println("get object-name attr  -  output the value associated with this attribute");
		System.out.println("set object-name attr val  -  set the value associated with this attribute");
		System.out.println("ops object-name  -  list the operations associated with the bean");
		System.out.println("do object-name oper arg1 arg2 ... -  invoke this method name with variable number of args");
		System.out.println("dolines object-name oper -  invoke method name, args on next line(s), end with blank");
	}

	private void exampleOutput() {
		System.out.println("");
		System.out.println("To use this utility you can do something like the following:");
		System.out.println("# list the objects published by the server");
		System.out.println(DEFAULT_PROMPT + "objects");
		System.out.println("  ...");
		System.out.println("# list the objects with a regex filter");
		System.out.println(DEFAULT_PROMPT + "objects java.lang");
		System.out.println("  ...");
		System.out.println("  java.lang:type=Memory");
		System.out.println("  ...");
		System.out.println("# show the attributes published by an object");
		System.out.println(DEFAULT_PROMPT + "attrs java.lang:type=Memory");
		System.out.println("  ...");
		System.out.println("# Verbose is attribute name, supports get/set");
		System.out.println("  Verbose(get, set boolean)");
		System.out.println("  ...");
		System.out.println("# get all attributes from an object");
		System.out.println(DEFAULT_PROMPT + "get java.lang:type=Memory");
		System.out.println("  ...");
		System.out.println("  get Verbose in 20ms = false");
		System.out.println("  ...");
		System.out.println("# get just the Verbose value");
		System.out.println(DEFAULT_PROMPT + "get java.lang:type=Memory Verbose");
		System.out.println("  get Verbose in 20ms = false");
		System.out.println("# set the Verbose value");
		System.out.println(DEFAULT_PROMPT + "set java.lang:type=Memory Verbose true");
		System.out.println("  Attribute java.lang:type=Memory set to false");
		System.out.println("# show the available operations for an object");
		System.out.println(DEFAULT_PROMPT + "ops java.lang:type=Memory");
		System.out.println(" void gc()");
		System.out.println("# issue the gc command, any arguments are space seperated after the gc");
		System.out.println(DEFAULT_PROMPT + "do java.lang:type=Memory gc");
		System.out.println(" do gc in 1077ms = null");
		System.out.println("# issue the gc command, but this time arguments on lines after the command");
		System.out.println("# this allows args with spaces");
		System.out.println(DEFAULT_PROMPT + "dolines java.lang:type=Memory gc");
		System.out.println("Enter args for gc, end with blank line");
		System.out.println("args: ");
		System.out.println(" dolines gc in 984ms = null");
		System.out.println(DEFAULT_PROMPT + "quit");
		System.out.println("");
	}

	private void listBeans(String[] args) {
		Set<ObjectName> objectNames;
		try {
			objectNames = jmxClient.getBeanNames();
		} catch (Exception e) {
			System.out.println("Error.  Problems getting bean names information: " + e.getMessage());
			return;
		}
		List<String> names = new ArrayList<String>();
		Pattern[] patterns = null;
		if (args.length > 1) {
			patterns = new Pattern[args.length - 1];
			for (int i = 1; i < args.length; i++) {
				patterns[i - 1] = Pattern.compile("(?i).*" + args[i] + ".*");
			}
		}
		OBJNAME: for (ObjectName objectName : objectNames) {
			String name = objectName.getCanonicalName();
			if (patterns != null) {
				for (Pattern pattern : patterns) {
					if (!pattern.matcher(name).matches()) {
						continue OBJNAME;
					}
				}
			}
			names.add(name);
		}
		Collections.sort(names);
		for (String name : names) {
			System.out.println("  " + name);
		}
	}

	/**
	 * Run a script. This might go recursive if we run from within a script.
	 */
	private void runScript(String alias, int levelC) throws IOException {
		String scriptFile = alias;
		InputStream stream;
		try {
			stream = getInputStream(scriptFile);
			if (stream == null) {
				System.out.println("Error.  Script file is not found: " + scriptFile);
				return;
			}
		} catch (IOException e) {
			System.out.println("Error.  Could not load script file " + scriptFile + ": " + e.getMessage());
			return;
		}
		final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		try {
			doLines(levelC + 1, new LineReader() {
				@Override
				public String getNextLine(String prompt) throws IOException {
					return reader.readLine();
				}
			}, true);
		} finally {
			reader.close();
		}
	}

	private void listAttributes(String[] parts) {
		ObjectName currentName = getObjectName("attrs", parts);
		if (currentName == null) {
			return;
		}

		MBeanAttributeInfo[] attrs;
		try {
			attrs = jmxClient.getAttributesInfo(currentName);
		} catch (Exception e) {
			System.out.println("Error.  Problems getting information about " + currentName + ": " + e.getMessage());
			return;
		}
		for (MBeanAttributeInfo info : attrs) {
			StringBuilder infoString = new StringBuilder();
			infoString.append("  ").append(info.getName());
			infoString.append('(');
			boolean comma = false;
			if (info.isReadable()) {
				infoString.append("get");
				comma = true;
			}
			if (info.isWritable()) {
				if (comma) {
					infoString.append(", ");
				}
				infoString.append("set ").append(info.getType());
				comma = true;
			}
			infoString.append(')');
			System.out.println(infoString.toString());
		}
	}

	private void getAttribute(String[] parts) {
		ObjectName currentName = getObjectName("get", parts);
		if (currentName == null) {
			return;
		}
		if (parts.length != 3) {
			System.out.println("Error.  Usage: get objectName [attr]");
			return;
		}

		try {
			long start = System.currentTimeMillis();
			Object value = jmxClient.getAttribute(currentName, parts[2]);
			displayValue("get", parts[2], value, System.currentTimeMillis() - start);
		} catch (Exception e) {
			System.out.println("Error.  Problems getting attribute: " + e.getMessage());
			return;
		}
	}

	private void getAttributes(String[] parts) {
		ObjectName currentName = getObjectName("get", parts);
		if (currentName == null) {
			return;
		}
		// have to have 2 arguments

		MBeanAttributeInfo[] attrs;
		try {
			attrs = jmxClient.getAttributesInfo(currentName);
		} catch (Exception e) {
			System.out.println("Error.  Problems getting bean information from " + currentName + ": " + e.getMessage());
			return;
		}
		long start = System.currentTimeMillis();
		Object[] output = new Object[attrs.length];
		for (int i = 0; i < attrs.length; i++) {
			try {
				output[i] = jmxClient.getAttribute(currentName, attrs[i].getName());
			} catch (Exception e) {
				System.out.println("Error.  Problems getting attribute data " + currentName + " bean "
						+ attrs[i].getName() + ": " + e.getMessage());
				output[i] = "(error)";
			}
		}
		long millis = System.currentTimeMillis() - start;
		for (int i = 0; i < attrs.length; i++) {
			displayValue("get", attrs[i].getName(), output[i], millis);
		}
	}

	private void setAttribute(String[] parts) {
		ObjectName currentName = getObjectName("set", parts);
		if (currentName == null) {
			return;
		}
		// it can be 4 or above so we can append the args
		if (parts.length < 4) {
			System.out.println("Error.  Usage: set objectName attr value");
			return;
		}
		String attrName = parts[2];
		StringBuilder sb = new StringBuilder();
		for (int partC = 3; partC < parts.length; partC++) {
			if (partC > 3) {
				sb.append(' ');
			}
			sb.append(parts[partC]);
		}
		String valueString = sb.toString();

		try {
			jmxClient.setAttribute(currentName, attrName, valueString);
		} catch (Exception e) {
			System.out.println("Error.  Problems setting information about attribute: " + e.getMessage());
			return;
		}
		Object attr;
		try {
			attr = jmxClient.getAttribute(currentName, attrName);
		} catch (Exception e) {
			System.out.println("Error.  Problems setting information about attribute: " + e.getMessage());
			return;
		}
		// check to make sure the value we get back is the same as what we set
		if (attr.toString().equals(valueString)) {
			System.out.println("  Attribute " + parts[1] + " set to " + valueString);
		} else {
			// may never happen but let's be careful out there
			System.out
					.println("Error.  Set attribute " + parts[1] + " to " + valueString + " but new value is " + attr);
		}
	}

	private void listOperations(String[] parts) {
		ObjectName currentName = getObjectName("opers", parts);
		if (currentName == null) {
			return;
		}
		if (parts.length != 2) {
			System.out.println("Error.  Usage: ops objectName");
			return;
		}

		MBeanOperationInfo[] opers;
		try {
			opers = jmxClient.getOperationsInfo(currentName);
		} catch (Exception e) {
			System.out.println("Error.  Problems getting information about name: " + e.getMessage());
			return;
		}
		for (MBeanOperationInfo info : opers) {
			System.out.print("  " + info.getReturnType() + " " + info.getName() + "(");
			boolean first = true;
			for (MBeanParameterInfo param : info.getSignature()) {
				if (first) {
					first = false;
				} else {
					System.out.print(", ");
				}
				System.out.print(param.getType());
			}
			System.out.println(")");
		}
	}

	private void invokeOperation(String[] parts) {
		ObjectName currentName = getObjectName("do", parts);
		if (currentName == null) {
			return;
		}
		if (parts.length < 3) {
			System.out.println("Error.  Usage: do objectName operation [arg1] [arg2] ...");
			return;
		}

		// create argument array of our optional args
		String[] args = new String[parts.length - 3];
		for (int argC = 3; argC < parts.length; argC++) {
			args[argC - 3] = parts[argC];
		}
		invokeJmx("do", currentName, parts[2], args);
	}

	private void invokeOperationLines(LineReader lineReader, String[] parts, boolean batch) throws IOException {
		ObjectName currentName = getObjectName("dolines", parts);
		if (currentName == null) {
			return;
		}
		if (parts.length < 3) {
			System.out.println("Error.  Usage: dolines objectName operation");
			return;
		}
		if (!batch) {
			System.out.println("Enter args for " + parts[2] + ", end with blank line");
		}
		List<String> argList = new ArrayList<String>();
		while (true) {
			String line;
			if (batch) {
				line = lineReader.getNextLine(null);
			} else {
				line = lineReader.getNextLine("args: ");
			}
			if (line == null || line.length() == 0) {
				break;
			}
			if (batch) {
				// if we are in batch mode, spit out the line we just read
				System.out.println(">>> " + line);
			}
			argList.add(line);
		}
		String[] args = argList.toArray(new String[argList.size()]);

		invokeJmx("dolines", currentName, parts[2], args);
	}

	private void invokeJmx(String command, ObjectName currentName, String oper, String[] args) {
		try {
			long start = System.currentTimeMillis();
			Object value = jmxClient.invokeOperation(currentName, oper, args);
			displayValue(command, oper, value, System.currentTimeMillis() - start);
		} catch (Exception e) {
			System.out.println("Error.  Problems invoking operation " + oper + ":");
			for (Throwable y = e; y != null; y = y.getCause()) {
				System.out.println("  " + y);
			}
		}
	}

	private ObjectName getObjectName(String command, String[] parts) {
		if (parts.length < 2) {
			System.out.println("Error.  Usage: " + command + " objectName [...]");
			return null;
		}
		try {
			return new ObjectName(parts[1]);
		} catch (MalformedObjectNameException e) {
			System.out.println("Error.  Invalid object name: " + parts[1]);
			return null;
		}
	}

	private void displayValue(String action, String what, Object value, long millis) {
		displayValue(value, "  ", action + " '" + what + "' in " + millis + "ms");
	}

	private void displayValue(Object value, String indent, String prefix) {
		System.out.print(indent);
		System.out.print(prefix);
		if (value == null) {
			System.out.println(" = null");
			return;
		}
		Class<? extends Object> clazz = value.getClass();
		if (!clazz.isArray()) {
			System.out.println(" = " + value.toString());
			return;
		}

		System.out.println(" is a " + clazz.getSimpleName() + " array:");
		if (clazz == byte[].class) {
			byte[] array = (byte[]) value;
			for (int arrayC = 0; arrayC < array.length; arrayC++) {
				// recurse to get any sub-arrays
				displayValue(array[arrayC], indent + "  ", "[" + arrayC + "]");
			}
		} else if (clazz == short[].class) {
			short[] array = (short[]) value;
			for (int arrayC = 0; arrayC < array.length; arrayC++) {
				// recurse to get any sub-arrays
				displayValue(array[arrayC], indent + "  ", "[" + arrayC + "]");
			}
		} else if (clazz == int[].class) {
			int[] array = (int[]) value;
			for (int arrayC = 0; arrayC < array.length; arrayC++) {
				// recurse to get any sub-arrays
				displayValue(array[arrayC], indent + "  ", "[" + arrayC + "]");
			}
		} else if (clazz == long[].class) {
			long[] array = (long[]) value;
			for (int arrayC = 0; arrayC < array.length; arrayC++) {
				// recurse to get any sub-arrays
				displayValue(array[arrayC], indent + "  ", "[" + arrayC + "]");
			}
		} else if (clazz == boolean[].class) {
			boolean[] array = (boolean[]) value;
			for (int arrayC = 0; arrayC < array.length; arrayC++) {
				// recurse to get any sub-arrays
				displayValue(array[arrayC], indent + "  ", "[" + arrayC + "]");
			}
		} else if (clazz == char[].class) {
			char[] array = (char[]) value;
			for (int arrayC = 0; arrayC < array.length; arrayC++) {
				// recurse to get any sub-arrays
				displayValue(array[arrayC], indent + "  ", "[" + arrayC + "]");
			}
		} else if (clazz == float[].class) {
			float[] array = (float[]) value;
			for (int arrayC = 0; arrayC < array.length; arrayC++) {
				// recurse to get any sub-arrays
				displayValue(array[arrayC], indent + "  ", "[" + arrayC + "]");
			}
		} else if (clazz == double[].class) {
			double[] array = (double[]) value;
			for (int arrayC = 0; arrayC < array.length; arrayC++) {
				// recurse to get any sub-arrays
				displayValue(array[arrayC], indent + "  ", "[" + arrayC + "]");
			}
		} else {
			Object[] array = (Object[]) value;
			for (int arrayC = 0; arrayC < array.length; arrayC++) {
				// recurse to get any sub-arrays
				displayValue(array[arrayC], indent + "  ", "[" + arrayC + "]");
			}
		}
		System.out.println(indent + "END of " + clazz.getSimpleName() + " array:");
	}

	private InputStream getInputStream(String filePath) throws IOException {
		InputStream inputStream;
		// first we try the classpath
		if (filePath.startsWith("classpath:")) {
			String[] paths = filePath.split(":", 2);
			assert paths.length > 1;
			inputStream = filePath.getClass().getResourceAsStream(paths[1]);
			if (inputStream == null) {
				return null;
			}
		} else {
			// otherwise we fall back to the filesystem
			File inputFile = new File(filePath);
			if (inputFile.exists()) {
				inputStream = new FileInputStream(inputFile);
			} else {
				return null;
			}
		}
		return inputStream;
	}

	private static interface LineReader {
		public String getNextLine(String prompt) throws IOException;
	}
}
