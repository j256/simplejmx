package com.j256.simplejmx.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.management.JMException;

import org.junit.Test;

import com.j256.simplejmx.client.JmxClient;

public class JmxServerTest {

	private static final int DEFAULT_PORT = 5256;
	private static final String DOMAIN_NAME = "j256";
	private static final String OBJECT_NAME = "testObject";
	private static final int FOO_VALUE = 1459243;

	@Test
	public void testJmxServer() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		try {
			server.start();
		} finally {
			server.stop();
		}
	}

	@Test
	public void testJmxServerStartStopStart() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		try {
			server.start();
			server.stop();
			server.start();
		} finally {
			server.stop();
		}
	}

	@Test
	public void testJmxServerInt() throws Exception {
		JmxServer server = new JmxServer();
		try {
			server.setPort(DEFAULT_PORT);
			server.start();
		} finally {
			server.stop();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testJmxServerStartNoPort() throws Exception {
		JmxServer server = new JmxServer();
		try {
			server.start();
			fail("Should not have gotten here");
		} finally {
			server.stop();
		}
	}

	@Test
	public void testRegister() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		TestObject obj = new TestObject();
		JmxClient client;
		try {
			server.start();
			client = new JmxClient(DEFAULT_PORT);

			try {
				client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo");
				fail("should not get here");
			} catch (IllegalArgumentException e) {
				// ignored
			}

			server.register(obj);
			assertEquals(FOO_VALUE, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));

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

			server.unregister(obj);

			try {
				client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo");
				fail("should not get here");
			} catch (IllegalArgumentException e) {
				// ignored
			}
		} finally {
			server.unregister(obj);
			server.stop();
		}
	}

	@Test(expected = JMException.class)
	public void testDoubleRegister() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		TestObject obj = new TestObject();
		try {
			server.start();
			server.register(obj);
			server.register(obj);
		} finally {
			server.unregister(obj);
			server.stop();
		}
	}

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class TestObject {

		private int foo = FOO_VALUE;

		@JmxAttribute(description = "A value")
		public int getFoo() {
			return foo;
		}

		@JmxAttribute(description = "A value")
		public void setFoo(int foo) {
			this.foo = foo;
		}

		@JmxOperation(description = "A value")
		public void resetFoo() {
			this.foo = 0;
		}

		@JmxOperation(description = "A value")
		public void resetFoo(int newValue) {
			this.foo = newValue;
		}
	}
}
