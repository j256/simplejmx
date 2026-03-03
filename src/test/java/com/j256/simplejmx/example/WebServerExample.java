package com.j256.simplejmx.example;

import java.net.ServerSocket;

import com.j256.simplejmx.common.JmxAttributeField;
import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.server.JmxServer;
import com.j256.simplejmx.web.JmxJetty9WebServer;

/**
 * Example program which uses the {@link JmxJetty9WebServer} which publishes the beans over JMX _and_ over a simple web
 * interface.
 * 
 * <p>
 * <b>NOTE:</b> For more details, see the SimpleJMX website: http://256stuff.com/sources/simplejmx/
 * </p>
 * 
 * @author graywatson
 */
public class WebServerExample {

	public static void main(String[] args) throws Exception {
		new WebServerExample().doMain(args);
	}

	private void doMain(String[] args) throws Exception {

		int jmxPort;
		try (ServerSocket socket = new ServerSocket(0)) {
			socket.setReuseAddress(true);
			jmxPort = socket.getLocalPort();
		}
		int webPort;
		try (ServerSocket socket = new ServerSocket(0)) {
			socket.setReuseAddress(true);
			webPort = socket.getLocalPort();
		}

		// create a new JMX server listening on a specific port
		JmxServer jmxServer = new JmxServer(jmxPort);
		jmxServer.start();

		RuntimeCounter counter = new RuntimeCounter();
		jmxServer.register(counter);

		// create a web server publisher listening on a specific port
		JmxJetty9WebServer jmxWebServer = new JmxJetty9WebServer(webPort);
		jmxWebServer.start();

		try {
			// do your other code here...
			// we just sleep forever to let the jmx server do its stuff
			System.out.println("Sleeping for a while to let the server do its stuff");
			System.out.println("JMX server on port " + jmxPort);
			System.out.println("Web server on port " + webPort);
			Thread.sleep(1000000000);

		} finally {
			// stop our server
			jmxWebServer.close();
			jmxServer.close();
		}
	}

	/**
	 * Here is our little bean that we are exposing via JMX. For more documentation about how it works, see
	 * {@link BasicExample}.
	 */
	@JmxResource(domainName = "j256.simplejmx", description = "Counter that shows how long we have been running")
	public static class RuntimeCounter {

		private long startMillis = System.currentTimeMillis();
		@JmxAttributeField(isWritable = true, description = "Show the time in seconds if true else milliseconds")
		private boolean showSeconds;

		@JmxAttributeMethod(description = "The time we have been running in seconds or milliseconds")
		public long getRunTime() {
			long diffMillis = System.currentTimeMillis() - startMillis;
			if (showSeconds) {
				return diffMillis / 1000;
			} else {
				return diffMillis;
			}
		}

		@JmxOperation(description = "Reset the start time to the current time millis")
		public String resetStartTime() {
			startMillis = System.currentTimeMillis();
			return "Timer has been reset to current millis";
		}

		@JmxOperation(description = "Add a positive or negative offset to the start time milliseconds",
				parameterNames = { "offset in millis" },
				parameterDescriptions = { "offset milliseconds value to add to start time millis" })
		public String addToStartTime(long offset) {
			long old = startMillis;
			startMillis += offset;
			return "Timer value changed from " + old + " to " + startMillis;
		}
	}
}
