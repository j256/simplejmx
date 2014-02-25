package com.j256.simplejmx.example;

import com.j256.simplejmx.common.JmxAttributeField;
import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.server.JmxServer;
import com.j256.simplejmx.web.JmxWebServer;

public class ExampleJmxWebServer {

	private static final int JMX_PORT = 8000;
	private static final int WEB_PORT = 8080;

	public static void main(String[] args) throws Exception {
		new ExampleJmxWebServer().doMain(args);
	}

	private void doMain(String[] args) throws Exception {

		// create a new JMX server listening on a specific port
		JmxServer jmxServer = new JmxServer(JMX_PORT);
		jmxServer.start();

		RuntimeCounter counter = new RuntimeCounter();
		jmxServer.register(counter);

		// create a web server publisher listening on a specific port
		JmxWebServer jmxWebServer = new JmxWebServer(WEB_PORT);
		jmxWebServer.start();

		try {
			// do your other code here...
			// we just sleep forever to let the jmx server do its stuff
			System.out.println("Sleeping for a while to let the server do its stuff");
			Thread.sleep(1000000000);

		} finally {
			// stop our server
			jmxWebServer.stop();
			jmxServer.stop();
		}
	}

	/**
	 * Here is our little bean that we are exposing via JMX. For more documentation about how it works, see
	 * {@link ExampleTestProgram}.
	 */
	@JmxResource(domainName = "j256.simplejmx")
	public static class RuntimeCounter {

		private long startMillis = System.currentTimeMillis();
		@JmxAttributeField(isWritable = true)
		private boolean showSeconds;

		@JmxAttributeMethod
		public long getRunTime() {
			long diffMillis = System.currentTimeMillis() - startMillis;
			if (showSeconds) {
				return diffMillis / 1000;
			} else {
				return diffMillis;
			}
		}

		@JmxOperation
		public String resetStartTime() {
			startMillis = System.currentTimeMillis();
			return "Timer has been reset to current millis";
		}

		@JmxOperation(parameterNames = { "long offset" }, parameterDescriptions = { "offset value to add" })
		public String addToStartTime(long offset) {
			long old = startMillis;
			startMillis += offset;
			return "Timer value changed from " + old + " to " + startMillis;
		}
	}
}
