package com.j256.simplejmx.client;

import java.io.File;
import java.util.Arrays;

/**
 * Sample main class which starts our JMX client. We have this separated from {@link CommandLineJmxClient} so others can
 * use it in their Main classes.
 * 
 * @author graywatson
 */
public class Main {

	/**
	 * Standard main method that can be called from the command line.
	 */
	public static void main(String[] args) throws Exception {
		// turn into an instance variable quickly
		new Main().doMain(args, false);
	}

	/**
	 * This is public for testing purposes.
	 * 
	 * @param throwOnError
	 *            If true then throw an exception when we quit otherwise exit.
	 */
	public void doMain(String[] args, boolean throwOnError) throws Exception {
		if (args.length == 0) {
			usage(throwOnError, "no arguments specified");
			return;
		} else if (args.length > 2) {
			usage(throwOnError, "improper number of arguments:" + Arrays.toString(args));
			return;
		}

		// check for --usage or --help
		if (args.length == 1 && ("--usage".equals(args[0]) || "--help".equals(args[0]))) {
			usage(throwOnError, null);
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
			String hostName = parts[0];
			int port = 0;
			try {
				port = Integer.parseInt(parts[1]);
			} catch (NumberFormatException e) {
				usage(throwOnError, "port number not in the right format: " + parts[1]);
				return;
			}
			jmxClient = new CommandLineJmxClient(hostName, port);
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
		System.err.println("Usage: java -jar simplejmx.jar host/port/url [batch-script]");
		System.err.println("host/port/url can be one of:");
		System.err.println("       hostname:port");
		System.err.println("or");
		System.err.println("       jmx-url      (ex. service:jmx:rmi:///jndi/rmi://localhost:8000/jmxrmi)");
		System.err.println("The optional [batch-script] will read the commands from the file otherwise stdin.");
		if (throwOnError) {
			throw new IllegalArgumentException("Usage problems: " + label);
		}
	}
}
