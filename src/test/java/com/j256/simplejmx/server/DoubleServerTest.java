package com.j256.simplejmx.server;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;

import org.junit.Ignore;
import org.junit.Test;

import com.j256.simplejmx.client.JmxClient;
import com.j256.simplejmx.common.IoUtils;
import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;

@Ignore("Could not get this to work.  Maybe with some more fiddling?")
public class DoubleServerTest {

	private static final int DEFAULT_PORT = 5256;
	private static final String DOMAIN_NAME = "j256";
	private static final String OBJECT_NAME = "testObject";
	private static final int FOO_VALUE = 1459243;

	@Test
	public void testJmxServer() throws Exception {
		JmxServer server1 = null;
		JmxServer server2 = null;
		JmxClient client1 = null;
		JmxClient client2 = null;
		TestObject testObject = new TestObject();
		try {
			InetAddress serverAddr1 = InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 });
			int serverPort1 = DEFAULT_PORT;
			server1 = new JmxServer(serverAddr1, serverPort1);
			InetAddress serverAddr2 = InetAddress.getLocalHost();
			int serverPort2 = DEFAULT_PORT + 1;
			server2 = new JmxServer(serverAddr2, serverPort2);

			server1.start();
			server2.start();

			/*
			 * NOTE: since the platform MbeanServer is used, this registers one both servers.
			 */
			server1.register(testObject);

			// Thread.sleep(1000000000);

			client1 = new JmxClient(serverAddr1, serverPort1);
			client2 = new JmxClient(serverAddr2, serverPort2);

			testClient(client1);
			testClient(client2);
		} finally {
			if (server1 != null) {
				server1.unregister(testObject);
				IoUtils.closeQuietly(server1);
			}
			if (server2 != null) {
				server2.unregister(testObject);
				IoUtils.closeQuietly(server2);
			}
			System.gc();
		}
	}

	private void testClient(JmxClient client) throws Exception {

		int newValue = FOO_VALUE + 2;
		client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "foo", newValue);
		assertEquals(newValue, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));

		client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "resetFoo");
		assertEquals(0, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));

		newValue = FOO_VALUE + 12;
		client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "resetFoo", (Integer) newValue);
		assertEquals(newValue, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));

		newValue = FOO_VALUE + 32;
		client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "resetFoo", Integer.toString(newValue));
		assertEquals(newValue, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class TestObject {

		private int foo = FOO_VALUE;

		@JmxAttributeMethod
		public int getFoo() {
			return foo;
		}

		@JmxAttributeMethod
		public void setFoo(int foo) {
			this.foo = foo;
		}

		@JmxOperation
		public void resetFoo() {
			this.foo = 0;
		}

		@JmxOperation
		public void resetFoo(int newValue) {
			this.foo = newValue;
		}
	}
}
