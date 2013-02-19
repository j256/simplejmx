package com.j256.simplejmx.server;

import com.j256.simplejmx.common.JmxAttributeFieldInfo;
import com.j256.simplejmx.common.JmxAttributeMethodInfo;
import com.j256.simplejmx.common.JmxOperationInfo;
import com.j256.simplejmx.common.JmxOperationInfo.OperationAction;
import com.j256.simplejmx.common.ObjectNameUtil;

/**
 * Here's a little example program that was written to show how you can expose any object -- even those without
 * annotations. This allows you to expose objects that you don't have code access to without writing your own wrapper
 * class.
 * 
 * <p>
 * <b>NOTE:</b> This is posted on the http://256.com/sources/simplejmx/ website.
 * </p>
 * 
 * @author graywatson
 */
public class RandomObjectTestProgram {

	private static final int JMX_PORT = 8000;

	public static void main(String[] args) throws Exception {
		new RandomObjectTestProgram().doMain(args);
	}

	private void doMain(String[] args) throws Exception {

		// create the object we will be exposing with JMX
		RuntimeCounter lookupCache = new RuntimeCounter();

		// create a new JMX server listening on a port
		JmxServer jmxServer = new JmxServer(JMX_PORT);
		try {
			// start our server
			jmxServer.start();

			// attribute fields exposed through reflection
			JmxAttributeFieldInfo[] attributeFieldInfos =
					new JmxAttributeFieldInfo[] {
							new JmxAttributeFieldInfo("startMillis", true, false /* not writable */,
									"When our timer started"),
							new JmxAttributeFieldInfo("showSeconds", true, true, "Show runtime in seconds") };
			// attribute get/set/is methods
			JmxAttributeMethodInfo[] attributeMethodInfos =
					new JmxAttributeMethodInfo[] { new JmxAttributeMethodInfo("getRunTime",
							"Run time in seconds or milliseconds") };
			// method operations
			JmxOperationInfo[] operationInfos =
					new JmxOperationInfo[] {
							new JmxOperationInfo("restartTimer", null, null, OperationAction.UNKNOWN,
									"Restart the timer to the current time"),
							new JmxOperationInfo("restartTimerToValue", new String[] { "startMillis" },
									new String[] { "Milliseconds to set our start-time to" }, OperationAction.UNKNOWN,
									"Set the timer as starting from a particular milliseconds value") };
			/*
			 * Register our lookupCache object defined below but with specific field-attributes, method-attributes, and
			 * method-operations defined.
			 */
			jmxServer.register(lookupCache, ObjectNameUtil.makeObjectName("j256.simplejmx", "RuntimeCounter"),
					attributeFieldInfos, attributeMethodInfos, operationInfos);
			// we can register other objects here
			// jmxServer.register(someOtherObject);

			// do your other code here...
			// we just sleep forever to let the server do its stuff
			System.out.println("Sleeping for a while to let the server do its stuff");
			Thread.sleep(1000000000);

		} finally {
			// unregister is not necessary if we are stopping the server
			jmxServer.unregister(lookupCache);
			// stop our server
			jmxServer.stop();
		}
	}

	/**
	 * Here is our little bean that we are exposing via JMX but without annotations. It can be in another class. It's
	 * just an inner class here for convenience.
	 */
	public static class RuntimeCounter {

		// start our timer
		private long startMillis = System.currentTimeMillis();

		// this field is exposed using JmxAttributeFieldInfo code above
		private boolean showSeconds;

		// this get method is exposed using JmxAttributeMethodInfo code above
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

		// this method is exposed using JmxOperationInfo code above
		public String restartTimer() {
			startMillis = System.currentTimeMillis();
			return "Timer has been restarted to " + startMillis;
		}

		// this method is exposed using JmxOperationInfo code above
		public String restartTimerToValue(long startMillis) {
			this.startMillis = startMillis;
			return "Timer has been restarted to " + startMillis;
		}
	}
}
