package com.j256.simplejmx.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.ReflectionException;

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
		JmxClient client = new JmxClient(DEFAULT_PORT);
		try {
			server.register(attributeField);
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
			client.close();
		}
	}

	@Test
	public void testIsMethod() throws Exception {
		IsMethod isMethod = new IsMethod();
		JmxClient client = new JmxClient(DEFAULT_PORT);
		try {
			server.register(isMethod);
			assertFalse((Boolean) client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "flag"));
			client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "flag", true);
			assertTrue((Boolean) client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "flag"));
		} finally {
			server.unregister(isMethod);
			client.close();
		}
	}

	@Test(expected = AttributeNotFoundException.class)
	public void testUnknownSetter() throws Exception {
		TestObject testObj = new TestObject();
		JmxClient client = new JmxClient(DEFAULT_PORT);
		try {
			server.register(testObj);
			client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "unknown-attribute", 1);
		} finally {
			server.unregister(testObj);
			client.close();
		}
	}

	@Test(expected = ReflectionException.class)
	public void testGetThrows() throws Exception {
		AttributeThrows getThrow = new AttributeThrows();
		JmxClient client = new JmxClient(DEFAULT_PORT);
		try {
			server.register(getThrow);
			client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "throws");
		} finally {
			server.unregister(getThrow);
			client.close();
		}
	}

	@Test(expected = ReflectionException.class)
	public void testSetThrows() throws Exception {
		AttributeThrows setThrow = new AttributeThrows();
		JmxClient client = new JmxClient(DEFAULT_PORT);
		try {
			server.register(setThrow);
			client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "throws", 1);
		} finally {
			server.unregister(setThrow);
			client.close();
		}
	}

	@Test
	public void testGetSetMultiple() throws Exception {
		MultipleAttributes obj = new MultipleAttributes();
		JmxClient client = new JmxClient(DEFAULT_PORT);
		try {
			server.register(obj);
			int x = 2134;
			obj.x = x;
			int y = 242634;
			obj.y = y;

			List<Attribute> attributes = client.getAttributes(DOMAIN_NAME, OBJECT_NAME, new String[] { "x", "y" });
			assertEquals(2, attributes.size());
			assertEquals(x, attributes.get(0).getValue());
			assertEquals(y, attributes.get(1).getValue());

			int x2 = x + 1;
			int y2 = y + 1;
			attributes.clear();
			attributes.add(new Attribute("x", x2));
			attributes.add(new Attribute("y", y2));
			client.setAttributes(DOMAIN_NAME, OBJECT_NAME, attributes);

			attributes = client.getAttributes(DOMAIN_NAME, OBJECT_NAME, new String[] { "x", "y" });
			assertEquals(2, attributes.size());
			assertEquals("x", attributes.get(0).getName());
			assertEquals(x2, attributes.get(0).getValue());
			assertEquals("y", attributes.get(1).getName());
			assertEquals(y2, attributes.get(1).getValue());
		} finally {
			server.unregister(obj);
			client.close();
		}
	}

	@Test(expected = JMException.class)
	public void testIsNotBoolean() throws Exception {
		IsNotBoolean isNotBoolean = new IsNotBoolean();
		server.register(isNotBoolean);
	}

	/* ======================================================================= */

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
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

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class BadGetName {
		@JmxAttributeMethod(description = "A value")
		public int notGet() {
			return 0;
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class BadGetReturnsVoid {
		@JmxAttributeMethod(description = "A value")
		public void getFoo() {
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class BadGetHasArgs {
		@JmxAttributeMethod(description = "A value")
		public int getFoo(int x) {
			return 0;
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class BadSetNoArg {
		@JmxAttributeMethod(description = "A value")
		public void setFoo() {
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class BadSetReturnsNotVoid {
		@JmxAttributeMethod(description = "A value")
		public int setFoo(int x) {
			return 0;
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class BadOperationLooksLikeAttribute {
		@JmxOperation(description = "A value")
		public void setFoo(int x) {
		}
	}

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
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

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class IsMethod {
		boolean flag;
		@JmxAttributeMethod
		public boolean isFlag() {
			return flag;
		}
		@JmxAttributeMethod
		public void setFlag(boolean flag) {
			this.flag = flag;
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
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

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class AttributeThrows {

		@JmxAttributeMethod
		public int getThrows() {
			throw new IllegalStateException("throw away!");
		}

		@JmxAttributeMethod
		public void setThrows(int val) {
			throw new IllegalStateException("throw away!");
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class MultipleAttributes {
		@JmxAttributeField(isWritable = true)
		int x;
		@JmxAttributeField(isWritable = true)
		int y;
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class IsNotBoolean {
		@JmxAttributeMethod
		public String isFoo() {
			return "";
		}
	}
}
