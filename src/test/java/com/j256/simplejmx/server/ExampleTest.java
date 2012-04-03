package com.j256.simplejmx.server;

import org.junit.Ignore;

import com.j256.simplejmx.common.JmxAttributeField;
import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;

/**
 * Here's a little example program that was written to show off the basic features of SimpleJmx.
 * 
 * @author graywatson
 */
@Ignore("Just here as an example")
public class ExampleTest {

	public static void main(String[] args) throws Exception {
		new ExampleTest().doMain(args);
	}

	private void doMain(String[] args) throws Exception {
		RuntimeCounter lookupCache = new RuntimeCounter();
		// create a new server listening on port 8000
		JmxServer jmxServer = new JmxServer(8000);
		try {
			// start our server
			jmxServer.start();
			// register our lookupCache object defined below
			jmxServer.register(lookupCache);
			// jmxServer.register(someOtherObject);

			// do your other code here...

			// we just sleep forever to let the server do its stuff
			Thread.sleep(1000000000);
		} finally {
			// stop our server
			jmxServer.stop();
		}
	}

	@JmxResource(description = "Runtime counter", domainName = "j256", objectName = "runtimeCounter")
	public static class RuntimeCounter {

		// start our timer
		private long startMillis = System.currentTimeMillis();

		// or we can do fields directly, isReadible defaults to true
		@JmxAttributeField(description = "Show runtime in seconds", isWritable = true)
		private boolean showSeconds;

		@JmxAttributeMethod(description = "Run time in seconds or milliseconds")
		public long getRunTime() {
			long diffMillis = System.currentTimeMillis() - startMillis;
			if (showSeconds) {
				return diffMillis / 1000;
			} else {
				return diffMillis;
			}
		}

		/*
		 * NOTE: there is no setHitCount() so it won't be writable.
		 */

		// this is an operation that shows up on the operations jconsole tab
		@JmxOperation(description = "Restart our timer")
		public String restartTimer() {
			startMillis = System.currentTimeMillis();
			return "Timer has been restarted";
		}
	}
}
