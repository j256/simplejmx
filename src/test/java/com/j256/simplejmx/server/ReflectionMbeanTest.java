package com.j256.simplejmx.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.management.AttributeNotFoundException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.j256.simplejmx.client.JmxClient;
import com.j256.simplejmx.common.JmxAttributeField;
import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;

public class ReflectionMbeanTest {

	private static final int DEFAULT_PORT = 5256;
	private static final String DOMAIN_NAME = "j256";
	private static final String OBJECT_NAME = "testObject";
	private static final int FOO_VALUE = 1459243;

	private static final int READ_ONLY_DEFAULT = 789543534;
	private static final int READ_WRITE_DEFAULT = 234534;
	private static final int WRITE_ONLY_DEFAULT = 623423;
	private static final int NEITHER_DEFAULT = 7985547;

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
		JmxClient client;
		try {
			client = new JmxClient(DEFAULT_PORT);
			server.register(obj);

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

	@Test
	public void testGetAttributes() throws Exception {
		TestObject obj = new TestObject();
		JmxClient client;
		try {
			client = new JmxClient(DEFAULT_PORT);
			server.register(obj);

			MBeanAttributeInfo[] attributes = client.getAttributesInfo(DOMAIN_NAME, OBJECT_NAME);
			assertEquals(1, attributes.length);
			assertEquals("foo", attributes[0].getName());
			assertEquals(int.class.toString(), attributes[0].getType());

		} finally {
			server.unregister(obj);
		}
	}

	@Test(expected = JMException.class)
	public void testBadGet() throws Exception {
		BadGetName obj = new BadGetName();
		try {
			server.register(obj);
			fail("Should not get here");
		} finally {
			server.unregister(obj);
		}
	}

	@Test(expected = JMException.class)
	public void testBadGetReturnsVoid() throws Exception {
		BadGetReturnsVoid obj = new BadGetReturnsVoid();
		try {
			server.register(obj);
			fail("Should not get here");
		} finally {
			server.unregister(obj);
		}
	}

	@Test(expected = JMException.class)
	public void testBadGetHasArgs() throws Exception {
		BadGetHasArgs obj = new BadGetHasArgs();
		try {
			server.register(obj);
		} finally {
			server.unregister(obj);
		}
	}

	@Test(expected = JMException.class)
	public void testBadSetNoArg() throws Exception {
		BadSetNoArg obj = new BadSetNoArg();
		try {
			server.register(obj);
		} finally {
			server.unregister(obj);
		}
	}

	@Test(expected = JMException.class)
	public void testBadSetReturnsNotVoid() throws Exception {
		BadSetReturnsNotVoid obj = new BadSetReturnsNotVoid();
		try {
			server.register(obj);
		} finally {
			server.unregister(obj);
		}
	}

	@Test(expected = JMException.class)
	public void testBadOperationLooksLikeAttribute() throws Exception {
		BadOperationLooksLikeAttribute obj = new BadOperationLooksLikeAttribute();
		try {
			server.register(obj);
		} finally {
			server.unregister(obj);
		}
	}

	@Test
	public void testMultiOperationSameName() throws Exception {
		MultiOperationSameName obj = new MultiOperationSameName();
		try {
			server.register(obj);
			JmxClient client = new JmxClient(DEFAULT_PORT);
			int x = 1002;
			assertEquals(x, client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "assignX", x));
			int y = 2934;
			assertEquals(y, client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "assignX", x, y));
		} finally {
			server.unregister(obj);
		}
	}

	@Test
	public void testAttributeFieldGets() throws Exception {
		AttributeField attributeField = new AttributeField();
		try {
			server.register(attributeField);
			JmxClient client = new JmxClient(DEFAULT_PORT);
			assertEquals(READ_ONLY_DEFAULT, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "readOnly"));
			assertEquals(READ_WRITE_DEFAULT, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "readWrite"));
			try {
				client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "writeOnly");
				fail("Should have thrown");
			} catch (AttributeNotFoundException e) {
				// expected
			}
			try {
				client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "neither");
				fail("Should have thrown");
			} catch (AttributeNotFoundException e) {
				// expected
			}
			try {
				client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "noAnnotation");
				fail("Should have thrown");
			} catch (AttributeNotFoundException e) {
				// expected
			}
		} finally {
			server.unregister(attributeField);
		}
	}

	@Test
	public void testAttributeFieldSets() throws Exception {
		AttributeField attributeField = new AttributeField();
		try {
			server.register(attributeField);
			JmxClient client = new JmxClient(DEFAULT_PORT);
			try {
				client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "readOnly", 1);
				fail("Should have thrown");
			} catch (AttributeNotFoundException e) {
				// expected
			}
			assertEquals(READ_WRITE_DEFAULT, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "readWrite"));
			int val = 530534543;
			client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "readWrite", val);
			assertEquals(val, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "readWrite"));
			assertEquals(val, attributeField.readWrite);
			val = 342323423;
			client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "writeOnly", val);
			assertEquals(val, attributeField.writeOnly);
			try {
				client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "neither", 1);
				fail("Should have thrown");
			} catch (AttributeNotFoundException e) {
				// expected
			}
			try {
				client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "noAnnotation", 1);
				fail("Should have thrown");
			} catch (AttributeNotFoundException e) {
				// expected
			}
		} finally {
			server.unregister(attributeField);
		}
	}

	/* ======================================================================= */

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class TestObject {

		private int foo = FOO_VALUE;

		@JmxAttributeMethod(description = "A value")
		public int getFoo() {
			return foo;
		}

		@JmxAttributeMethod(description = "A value")
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
		@JmxAttributeMethod(description = "A value")
		public int notGet() {
			return 0;
		}
	}

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class BadGetReturnsVoid {
		@JmxAttributeMethod(description = "A value")
		public void getFoo() {
		}
	}

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class BadGetHasArgs {
		@JmxAttributeMethod(description = "A value")
		public int getFoo(int x) {
			return 0;
		}
	}

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class BadSetNoArg {
		@JmxAttributeMethod(description = "A value")
		public void setFoo() {
		}
	}

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class BadSetReturnsNotVoid {
		@JmxAttributeMethod(description = "A value")
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

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class AttributeField {
		@SuppressWarnings("unused")
		@JmxAttributeField(description = "some thing")
		private int readOnly = READ_ONLY_DEFAULT;
		@JmxAttributeField(isWritable = true)
		private int readWrite = READ_WRITE_DEFAULT;
		@JmxAttributeField(isReadible = false, isWritable = true)
		private int writeOnly = WRITE_ONLY_DEFAULT;
		@SuppressWarnings("unused")
		@JmxAttributeField(isReadible = false, isWritable = false)
		private int neither = NEITHER_DEFAULT;
		@SuppressWarnings("unused")
		private int noAnnotation = 4;
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
