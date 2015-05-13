package com.j256.simplejmx.example;

import com.j256.simplejmx.common.JmxResourceInfo;
import com.j256.simplejmx.server.JmxServer;
import com.j256.simplejmx.server.PublishAllBeanWrapper;

/**
 * Example program that was written to show off how you can publish any object using the {@link PublishAllBeanWrapper}
 * which publishes all public fields and methods. You may also want to look at the {@link RandomObjectExample} which
 * publishes objects to JMX programmatically.
 * 
 * <p>
 * <b>NOTE:</b> For more details, see the SimpleJMX website: http://256.com/sources/simplejmx/
 * </p>
 * 
 * @author graywatson
 */
public class PublishAllExample {

	private static final int JMX_PORT = 8000;

	public static void main(String[] args) throws Exception {
		new PublishAllExample().doMain(args);
	}

	private void doMain(String[] args) throws Exception {

		// create the object we will be exposing with JMX
		RuntimeCounter counter = new RuntimeCounter();

		// create a new JMX server listening on a specific port
		JmxServer jmxServer = new JmxServer(JMX_PORT);

		try {
			// start our server
			jmxServer.start();

			// register our object using the PublishAllBeanWrapper to expose the public fields and methods
			jmxServer.register(new PublishAllBeanWrapper(counter, new JmxResourceInfo("com.j256", null,
					"runtime counter")));

			// we just sleep forever to let the jmx server do its stuff
			System.out.println("Sleeping for a while to let the server do its stuff");
			System.out.println("JMX server on port " + JMX_PORT);
			Thread.sleep(1000000000);

		} finally {
			// unregister is not necessary if we are stopping the server
			jmxServer.unregister(counter);
			// stop our server
			jmxServer.stop();
		}
	}

	/**
	 * Here is our little bean that we are exposing via JMX by using the {@link PublishAllBeanWrapper}. It can be in
	 * another class. It's just an inner class here for convenience.
	 */
	public static class RuntimeCounter {

		private long startMillis = System.currentTimeMillis();

		public boolean showSeconds;

		public long getRunTime() {
			// show how long we are running
			long diffMillis = System.currentTimeMillis() - startMillis;
			if (showSeconds) {
				// as seconds
				return diffMillis / 1000;
			} else {
				// or as milliseconds
				return diffMillis;
			}
		}

		// no set method so it won't be writable

		public String resetStartTime() {
			startMillis = System.currentTimeMillis();
			return "Timer has been reset to current millis";
		}

		public String addToStartTime(long offset) {
			long old = startMillis;
			startMillis += offset;
			return "Timer value changed from " + old + " to " + startMillis;
		}
	}
}
