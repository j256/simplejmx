package com.j256.simplejmx.client;

import java.io.File;
import java.util.Arrays;

/**
 * Main class which starts our JMX client. We have this separation with {@link CommandLineJmxClient} so others can use
 * it in their Main classes.
 * 
 * @author graywatson
 */
public class Main {

	public static void main(String[] args) throws Exception {
		// turn into an instance various quickly
		Main main = new Main();
		main.doMain(args, false);
	}

	/**
	 * This is public for testing purposes.
	 * 
	 * @param throwOnError
	 *            Throw an exception when we quit otherwise exit.
	 */
	public void doMain(String[] args, boolean throwOnError) throws Exception {
		if (args.length == 0) {
			usage(throwOnError, "no arguments specified");
			return;
		} else if (args.length > 2) {
			usage(throwOnError, "improper number of arguments:" + Arrays.toString(args));
			return;
		}

		CommandLineJmxClient jmxClient;
		if (args[0].indexOf('/') >= 0) {
			jmxClient = new CommandLineJmxClient(args[0]);
		} else {
			String[] parts = args[0].split(":");
			if (parts.length != 2) {
				usage(throwOnError, "argument should be in 'hostname:port' format, not: " + args[0]);
				return;
			}
			int port = 0;
			try {
				port = Integer.parseInt(parts[1]);
			} catch (NumberFormatException e) {
				usage(throwOnError, "port number not in the right format: " + parts[1]);
				return;
			}
			jmxClient = new CommandLineJmxClient(parts[0], port);
		}

		if (args.length == 1) {
			jmxClient.runCommandLine();
		} else if (args.length == 2) {
			jmxClient.runBatchFile(new File(args[1]));
		}
	}

	private void usage(boolean throwOnError, String label) {
		if (label != null) {
			System.err.print("Error: ");
			System.err.println(label);
			System.err.println();
		}
		System.err.println("Usage: java -jar simplejmx.jar host/port/url [script]");
		System.err.println("host/port/url can be one of:");
		System.err.println("       hostname:port");
		System.err.println("or");
		System.err.println("       jmx-url      (ex. service:jmx:rmi:///jndi/rmi://localhost:8000/jmxrmi)");
		System.err.println("The optional [script] will read the commands from the file else stdin.");
		if (throwOnError) {
			throw new IllegalArgumentException("Usage problems: " + label);
		}
	}
}
