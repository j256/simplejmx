package com.j256.simplejmx.server;

import org.junit.Ignore;

import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;

@Ignore("We will just run this as an application")
public class PlatformMbeanServerTest {

	public static void main(String[] args) throws Exception {
		JmxServer server = new JmxServer();
		server.setUsePlatformMBeanServer(true);
		server.start();
		try {
			SomeBean someBean = new SomeBean();
			server.register(someBean);

			System.out.println("waiting for the shutdown JMX command");
			while (!someBean.shutdown) {
				Thread.sleep(100);
			}
			System.out.println("shutdown");
		} finally {
			server.stop();
		}
	}

	@JmxResource(domainName = "j256.com")
	protected static class SomeBean {
		public volatile boolean shutdown;
		@JmxOperation
		public void shutdown() {
			shutdown = true;
		}
	}
}
