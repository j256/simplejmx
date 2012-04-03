package com.j256.simplejmx.server;

import org.junit.Ignore;

import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;

/**
 * Here's a little example program that was written to show off the basic features of SimpleJmx.
 * 
 * @author graywatson
 */
@Ignore
public class ExampleTest {

	public static void main(String[] args) throws Exception {
		LookupCache lookupCache = new LookupCache();
		// create a new server listening on port 8000
		JmxServer jmxServer = new JmxServer(8000);
		try {
			// start our server
			jmxServer.start();
			// register our lookupCache object defined below
			jmxServer.register(lookupCache);
			// jmxServer.register(someOtherObject);

			// do your other code here...

			// we just sleep to let he server do its stuff
			Thread.sleep(100000);
		} finally {
			// stop our server
			jmxServer.stop();
		}
	}

	@JmxResource(description = "Lookup cache", domainName = "j256", objectName = "lookupCache")
	public static class LookupCache {

		private int hitCount;
		private boolean enabled;

		@JmxAttributeMethod(description = "Number of hits in the cache")
		public int getHitCount() {
			return hitCount;
		}

		@JmxAttributeMethod(description = "Is the cache enabled?")
		public boolean getEnabled() {
			return enabled;
		}

		@JmxAttributeMethod(description = "Is the cache enabled?")
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		@JmxOperation(description = "Flush the cache")
		public String flushCache() {
			return "Cache is flushed";
		}
	}
}
