package com.j256.simplejmx.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.j256.simplejmx.client.JmxClient;
import com.j256.simplejmx.common.JmxResourceInfo;

public class PublishAllBeanWrapperTest {

	private static final int DEFAULT_PORT = 5256;
	private static final String DOMAIN_NAME = "j256";
	private static final String OBJECT_NAME = PublishAllBeanWrapperTest.class.getSimpleName();
	private static final int FOO_VALUE = 1459243;
	private static final int BAR_VALUE = 1423459243;

	private JmxServer server;

	@Before
	public void before() throws Exception {
		server = new JmxServer(DEFAULT_PORT);
		server.start();
	}

	@After
	public void after() {
		if (server != null) {
			server.stop();
			server = null;
		}
	}

	@Test
	public void testRegister() throws Exception {
		TestObject obj = new TestObject();
		JmxResourceInfo resourceInfo = new JmxResourceInfo(DOMAIN_NAME, OBJECT_NAME, "description");
		JmxClient client;
		try {
			client = new JmxClient(DEFAULT_PORT);
			server.register(new PublishAllBeanWrapper(obj, resourceInfo));

			assertEquals(FOO_VALUE, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));
			assertEquals(BAR_VALUE, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "bar"));

			int val = FOO_VALUE + 1;
			client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "foo", val);
			assertEquals(val, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));
			client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "resetFoo");
			assertEquals(0, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));
			val = FOO_VALUE + 2;
			client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "resetFoo", val);
			assertEquals(val, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));

			try {
				client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "unknown");
				fail("Should have thrown");
			} catch (Exception e) {
				// ignored
			}
			try {
				client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "unknown", FOO_VALUE);
				fail("Should have thrown");
			} catch (Exception e) {
				// ignored
			}
			try {
				client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "unknown");
				fail("Should have thrown");
			} catch (Exception e) {
				// ignored
			}
			try {
				client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "getFoo");
				fail("Should have thrown");
			} catch (Exception e) {
				// ignored
			}

		} finally {
			server.unregister(obj);
		}
	}

	/* ======================================================================= */

	protected static class TestObject {
		private int foo = FOO_VALUE;
		public int bar = BAR_VALUE;
		public int getFoo() {
			return foo;
		}
		public void setFoo(int foo) {
			this.foo = foo;
		}
		public void resetFoo() {
			this.foo = 0;
		}
		public void resetFoo(int newValue) {
			this.foo = newValue;
		}
	}
}
