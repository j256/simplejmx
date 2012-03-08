package com.j256.simplejmx.client;

/**
 * Main class which starts our JMX client. We have this separation with {@link CommandLineJmxClient} so others can use it in
 * their Main classes.
 * 
 * @author graywatson
 */
public class Main {

	public static void main(String[] args) throws Exception {
		// turn into an instance various quickly
		Main main = new Main();
		main.doMain(args);
	}

	private void doMain(String[] args) throws Exception {
		if (args.length == 0) {
			usage();
		}

		CommandLineJmxClient jmxClient;
		if (args[0].indexOf('/') == -1) {
			String[] parts = args[0].split(":");
			if (parts.length != 2) {
				usage();
			}
			int port = 0;
			try {
				port = Integer.parseInt(parts[1]);
			} catch (NumberFormatException e) {
				usage();
			}
			jmxClient = new CommandLineJmxClient(parts[0], port);
		} else {
			jmxClient = new CommandLineJmxClient(args[0]);
		}

		if (args.length == 1) {
			jmxClient.runCommandLine();
		} else if (args.length == 2) {
			jmxClient.runBatchFile(args[1]);
		} else {
			usage();
		}
	}

	private void usage() {
		System.err.println("Usage: java -jar simplejmx.jar host/port/url [script]");
		System.err.println("host/port/url can be one of:");
		System.err.println("       hostname:port");
		System.err.println("or");
		System.err.println("       jmx-url      (ex. service:jmx:rmi:///jndi/rmi://localhost:8000/jmxrmi)");
		System.err.println("The optional [script] will read the commands from the file else stdin.");
		System.exit(0);
	}
}
