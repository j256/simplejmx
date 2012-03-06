package com.j256.simplejmx.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.management.JMException;
import javax.management.MBeanAttributeInfo;

import org.junit.Test;

import com.j256.simplejmx.client.JmxClient;

public class ReflectionMbeanTest {

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
			server.register(obj);

			try {
				client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "unknown");
				fail("Should have thrown");
			} catch (IllegalArgumentException e) {
				// ignored
			}

			try {
				client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "unknown", FOO_VALUE);
				fail("Should have thrown");
			} catch (IllegalArgumentException e) {
				// ignored
			}

			try {
				client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "unknown");
				fail("Should have thrown");
			} catch (IllegalArgumentException e) {
				// ignored
			}

			try {
				client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "getFoo");
				fail("Should have thrown");
			} catch (IllegalArgumentException e) {
				// ignored
			}

		} finally {
			server.unregister(obj);
			server.stop();
		}
	}

	@Test
	public void testGetAttributes() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		TestObject obj = new TestObject();
		JmxClient client;
		try {
			server.start();
			client = new JmxClient(DEFAULT_PORT);
			server.register(obj);

			MBeanAttributeInfo[] attributes = client.getAttributesInfo(DOMAIN_NAME, OBJECT_NAME);
			assertEquals(1, attributes.length);
			assertEquals("foo", attributes[0].getName());
			assertEquals(int.class.toString(), attributes[0].getType());

		} finally {
			server.unregister(obj);
			server.stop();
		}
	}

	@Test(expected = JMException.class)
	public void testBadGet() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		try {
			server.start();
			server.register(new BadGetName());
			fail("Should not get here");
		} finally {
			server.stop();
		}
	}

	@Test(expected = JMException.class)
	public void testBadGetReturnsVoid() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		try {
			server.start();
			server.register(new BadGetReturnsVoid());
			fail("Should not get here");
		} finally {
			server.stop();
		}
	}

	@Test(expected = JMException.class)
	public void testBadGetHasArgs() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		try {
			server.start();
			server.register(new BadGetHasArgs());
			fail("Should not get here");
		} finally {
			server.stop();
		}
	}

	@Test(expected = JMException.class)
	public void testBadSetNoArg() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		try {
			server.start();
			server.register(new BadSetNoArg());
			fail("Should not get here");
		} finally {
			server.stop();
		}
	}

	@Test(expected = JMException.class)
	public void testBadSetReturnsNotVoid() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		try {
			server.start();
			server.register(new BadSetReturnsNotVoid());
			fail("Should not get here");
		} finally {
			server.stop();
		}
	}

	@Test(expected = JMException.class)
	public void testBadOperationLooksLikeAttribute() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		try {
			server.start();
			server.register(new BadOperationLooksLikeAttribute());
			fail("Should not get here");
		} finally {
			server.stop();
		}
	}

	@Test
	public void testMultiOperationSameName() throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		try {
			server.start();
			server.register(new MultiOperationSameName());
			JmxClient client = new JmxClient(DEFAULT_PORT);
			int x = 1002;
			assertEquals(x, client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "assignX", x));
			int y = 2934;
			assertEquals(y, client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "assignX", x, y));
		} finally {
			server.stop();
		}
	}

	/* ======================================================================= */

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

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class BadGetName {
		@JmxAttribute(description = "A value")
		public int notGet() {
			return 0;
		}
	}

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class BadGetReturnsVoid {
		@JmxAttribute(description = "A value")
		public void getFoo() {
		}
	}

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class BadGetHasArgs {
		@JmxAttribute(description = "A value")
		public int getFoo(int x) {
			return 0;
		}
	}

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class BadSetNoArg {
		@JmxAttribute(description = "A value")
		public void setFoo() {
		}
	}

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class BadSetReturnsNotVoid {
		@JmxAttribute(description = "A value")
		public int setFoo(int x) {
			return 0;
		}
	}

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class BadOperationLooksLikeAttribute {
		@JmxOperation(description = "A value")
		public void setFoo(int x) {
		}
	}

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class MultiOperationSameName {
		int x;
		@JmxOperation(description = "Do stuff")
		public int assignX(int x) {
			return x;
		}
		@JmxOperation(description = "Do stuff")
		public int assignX(int x, int y) {
			return y;
		}
	}
}
