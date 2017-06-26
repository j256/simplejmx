package com.j256.simplejmx.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.JMException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.j256.simplejmx.client.JmxClient;
import com.j256.simplejmx.common.IoUtils;
import com.j256.simplejmx.common.JmxAttributeField;
import com.j256.simplejmx.common.JmxAttributeFieldInfo;
import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxAttributeMethodInfo;
import com.j256.simplejmx.common.JmxFolderName;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxOperationInfo;
import com.j256.simplejmx.common.JmxOperationInfo.OperationAction;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.common.JmxSelfNaming;
import com.j256.simplejmx.common.ObjectNameUtil;

public class JmxServerTest {

	private static final int DEFAULT_PORT = 5256;
	private static final String DOMAIN_NAME = "j256";
	private static final String OBJECT_NAME = "testObject";
	private static final int FOO_VALUE = 1459243;
	private static final String FOLDER_FIELD_NAME = "00";
	private static final String FOLDER_VALUE_NAME = "FolderName";
	private static final String FOLDER_NAME = FOLDER_FIELD_NAME + "=" + FOLDER_VALUE_NAME;

	private static JmxServer server;
	private static InetAddress serverAddress;
	private static AtomicInteger differentPost = new AtomicInteger(DEFAULT_PORT);

	@BeforeClass
	public static void beforeClass() throws Exception {
		serverAddress = InetAddress.getByName("127.0.0.1");
		server = new JmxServer(serverAddress, DEFAULT_PORT);
		server.start();
	}

	@AfterClass
	public static void afterClass() {
		IoUtils.closeQuietly(server);
	}

	@Test
	public void testJmxServerStartStopStart() throws Exception {
		JmxServer server = new JmxServer(serverAddress, differentPost.incrementAndGet());
		try {
			server.stop();
			server.start();
		} finally {
			IoUtils.closeQuietly(server);
		}
	}

	@Test(expected = JMException.class)
	public void testJmxServerDoubleInstance() throws Exception {
		int port = differentPost.incrementAndGet();
		JmxServer first = new JmxServer(serverAddress, port);
		JmxServer second = new JmxServer(serverAddress, port);
		try {
			first.start();
			second.start();
		} finally {
			IoUtils.closeQuietly(first);
			IoUtils.closeQuietly(second);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testJmxServerWildPort() throws Exception {
		JmxServer server = new JmxServer(serverAddress, -10);
		try {
			server.start();
		} finally {
			IoUtils.closeQuietly(server);
		}
	}

	@Test(expected = JMException.class)
	public void testDoubleClose() throws Exception {
		JmxServer server = new JmxServer(serverAddress, differentPost.incrementAndGet());
		try {
			server.start();
		} finally {
			Field field = server.getClass().getDeclaredField("rmiRegistry");
			field.setAccessible(true);
			Registry rmiRegistry = (Registry) field.get(server);
			// close the rmi registry through trickery!
			UnicastRemoteObject.unexportObject(rmiRegistry, true);
			try {
				server.stopThrow();
			} finally {
				IoUtils.closeQuietly(server);
			}
		}
	}

	@Test
	public void testJmxServerInt() throws Exception {
		JmxServer server = new JmxServer();
		try {
			server.setPort(DEFAULT_PORT + 11);
			server.setInetAddress(serverAddress);
			server.start();
		} finally {
			IoUtils.closeQuietly(server);
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testJmxServerStartNoPort() throws Exception {
		JmxServer server = new JmxServer();
		try {
			server.start();
			fail("Should not have gotten here");
		} finally {
			IoUtils.closeQuietly(server);
		}
	}

	@Test
	public void testRegister() throws Exception {
		TestObject obj = new TestObject();
		JmxClient client = null;
		try {
			client = new JmxClient(serverAddress, DEFAULT_PORT);

			try {
				client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo");
				fail("should not get here");
			} catch (Exception e) {
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

			assertEquals(1, client.getAttributesInfo(DOMAIN_NAME, OBJECT_NAME).length);
			assertEquals(2, client.getOperationsInfo(DOMAIN_NAME, OBJECT_NAME).length);

			server.unregister(obj);

			try {
				client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo");
				fail("should not get here");
			} catch (Exception e) {
				// ignored
			}
		} finally {
			IoUtils.closeQuietly(client);
			server.unregister(obj);
		}
	}

	@Test(expected = JMException.class)
	public void testDoubleRegister() throws Exception {
		TestObject obj = new TestObject();
		try {
			server.register(obj);
			server.register(obj);
		} finally {
			server.unregister(obj);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRegisterNoJmxResource() throws Exception {
		try {
			// this class has no JmxResource annotation
			server.register(this);
		} finally {
			server.unregister(this);
		}
	}

	@Test(expected = JMException.class)
	public void testShortAttributeMethodName() throws Exception {
		ShortAttributeMethodName obj = new ShortAttributeMethodName();
		try {
			server.register(obj);
		} finally {
			server.unregister(obj);
		}
	}

	@Test(expected = JMException.class)
	public void testJustGetAttributeMethodName() throws Exception {
		JustGet obj = new JustGet();
		try {
			server.register(obj);
		} finally {
			server.unregister(obj);
		}
	}

	@Test
	public void testRegisterFolders() throws Exception {
		TestObjectFolders obj = new TestObjectFolders();
		JmxClient client = null;
		try {
			server.register(obj);
			client = new JmxClient(serverAddress, DEFAULT_PORT);
			assertEquals(FOO_VALUE, client.getAttribute(
					ObjectNameUtil.makeObjectName(DOMAIN_NAME, OBJECT_NAME, new String[] { FOLDER_NAME }), "foo"));
			assertEquals(FOO_VALUE, client.getAttribute(ObjectNameUtil.makeObjectName(DOMAIN_NAME, OBJECT_NAME,
					new String[] { FOLDER_FIELD_NAME + "=" + FOLDER_VALUE_NAME }), "foo"));
		} finally {
			IoUtils.closeQuietly(client);
			server.unregister(obj);
		}
	}

	@Test
	public void testSelfNaming() throws Exception {
		SelfNaming obj = new SelfNaming();
		JmxClient client = null;
		try {
			server.register(obj);
			client = new JmxClient(serverAddress, DEFAULT_PORT);
			assertEquals(FOO_VALUE, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));
		} finally {
			IoUtils.closeQuietly(client);
			server.unregister(obj);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidDomain() throws Exception {
		InvalidDomain obj = new InvalidDomain();
		try {
			server.register(obj);
		} finally {
			server.unregister(obj);
		}
	}

	@Test
	public void testNoDescriptions() throws Exception {
		NoDescriptions obj = new NoDescriptions();
		JmxClient client = null;
		try {
			server.register(obj);
			client = new JmxClient(serverAddress, DEFAULT_PORT);
			assertEquals(FOO_VALUE, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));
		} finally {
			IoUtils.closeQuietly(client);
			server.unregister(obj);
		}
	}

	@Test
	public void testHasDescription() throws Exception {
		HasDescriptions obj = new HasDescriptions();
		JmxClient client = null;
		try {
			server.register(obj);
			client = new JmxClient(serverAddress, DEFAULT_PORT);
			assertEquals(FOO_VALUE, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));
		} finally {
			IoUtils.closeQuietly(client);
			server.unregister(obj);
		}
	}

	@Test
	public void testOperationParameters() throws Exception {
		OperationParameters obj = new OperationParameters();
		JmxClient client = null;
		try {
			server.register(obj);
			client = new JmxClient(serverAddress, DEFAULT_PORT);
			String someArg = "pfoewjfpeowjfewf ewopjfwefew";
			assertEquals(someArg, client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "doSomething", someArg));
		} finally {
			IoUtils.closeQuietly(client);
			server.unregister(obj);
		}
	}

	@Test(expected = ReflectionException.class)
	public void testThrowingGet() throws Exception {
		MisterThrow obj = new MisterThrow();
		JmxClient client = null;
		try {
			server.register(obj);
			client = new JmxClient(serverAddress, DEFAULT_PORT);
			client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo");
		} finally {
			IoUtils.closeQuietly(client);
			server.unregister(obj);
		}
	}

	@Test(expected = ReflectionException.class)
	public void testThrowingSet() throws Exception {
		MisterThrow obj = new MisterThrow();
		JmxClient client = null;
		try {
			server.register(obj);
			client = new JmxClient(serverAddress, DEFAULT_PORT);
			client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "foo", 0);
		} finally {
			IoUtils.closeQuietly(client);
			server.unregister(obj);
		}
	}

	@Test(expected = ReflectionException.class)
	public void testThrowingOperation() throws Exception {
		MisterThrow obj = new MisterThrow();
		JmxClient client = null;
		try {
			server.register(obj);
			client = new JmxClient(serverAddress, DEFAULT_PORT);
			client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "someCall");
		} finally {
			IoUtils.closeQuietly(client);
			server.unregister(obj);
		}
	}

	@Test
	public void testRegisterObjUserInfoFieldAttribute() throws Exception {
		RandomObject obj = new RandomObject();
		ObjectName objectName = ObjectNameUtil.makeObjectName(DOMAIN_NAME, OBJECT_NAME);
		JmxClient client = null;
		try {
			server.register(obj, objectName,
					new JmxAttributeFieldInfo[] {
							new JmxAttributeFieldInfo("foo", true, false /* not writable */, "description") },
					null, null);
			client = new JmxClient(serverAddress, DEFAULT_PORT);
			int val = 4232431;
			obj.setFoo(val);
			assertEquals(val, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));
			try {
				client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "foo", 1);
				fail("Expected this to throw");
			} catch (JMException e) {
				// ignored
			}
		} finally {
			IoUtils.closeQuietly(client);
			server.unregister(objectName);
		}
	}

	@Test
	public void testRegisterObjUserInfoFieldMethod() throws Exception {
		RandomObject obj = new RandomObject();
		ObjectName objectName = ObjectNameUtil.makeObjectName(DOMAIN_NAME, OBJECT_NAME);
		JmxClient client = null;
		try {
			server.register(obj, objectName, null,
					new JmxAttributeMethodInfo[] { new JmxAttributeMethodInfo("getFoo", "description"),
							new JmxAttributeMethodInfo("setFoo", "description") },
					null);
			client = new JmxClient(serverAddress, DEFAULT_PORT);
			int val = 4232431;
			client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "foo", val);
			assertEquals(val, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));
		} finally {
			IoUtils.closeQuietly(client);
			server.unregister(objectName);
		}
	}

	@Test
	public void testRegisterObjUserInfoOperationOnly() throws Exception {
		RandomObject obj = new RandomObject();
		ObjectName objectName = ObjectNameUtil.makeObjectName(DOMAIN_NAME, OBJECT_NAME);
		JmxClient client = null;
		try {
			server.register(obj, objectName, null, null, new JmxOperationInfo[] {
					new JmxOperationInfo("resetFoo", null, null, OperationAction.UNKNOWN, "description") });
			client = new JmxClient(serverAddress, DEFAULT_PORT);
			obj.setFoo(1);
			client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "resetFoo");
			assertEquals(0, obj.getFoo());
			try {
				client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo");
				fail("Expected this to throw");
			} catch (JMException e) {
				// ignored
			}
		} finally {
			IoUtils.closeQuietly(client);
			server.unregister(objectName);
		}
	}

	@Ignore("this fails every so often")
	@Test
	public void testLoopBackAddress() throws Exception {
		// this fails every so often so let's see if it a port clearing thing
		Thread.sleep(1000);
		testAddress(null, differentPost.incrementAndGet());
	}

	@Test
	public void testLocalAddress() throws Exception {
		testAddress(serverAddress, differentPost.incrementAndGet());
	}

	@Test
	public void testSubClass() throws Exception {
		SubClassTestObject obj = new SubClassTestObject();
		JmxClient client = null;
		try {
			client = new JmxClient(serverAddress, DEFAULT_PORT);

			try {
				client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo");
				fail("should not get here");
			} catch (Exception e) {
				// ignored
			}

			server.register(obj);
			assertEquals(FOO_VALUE, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));

			assertEquals(2, client.getAttributesInfo(DOMAIN_NAME, OBJECT_NAME).length);
			assertEquals(3, client.getOperationsInfo(DOMAIN_NAME, OBJECT_NAME).length);

			int newValue = FOO_VALUE + 2;
			client.setAttribute(DOMAIN_NAME, OBJECT_NAME, "foo", newValue);
			assertEquals(newValue, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));

			client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "resetFoo");
			assertEquals(0, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));

			newValue = FOO_VALUE + 12;
			client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "resetFoo", newValue);
			assertEquals(newValue, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));

			newValue = FOO_VALUE + 32;
			client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "resetFoo", Integer.toString(newValue));
			assertEquals(newValue * 2, client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo"));

			server.unregister(obj);

			try {
				client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo");
				fail("should not get here");
			} catch (Exception e) {
				// ignored
			}
		} finally {
			IoUtils.closeQuietly(client);
			server.unregister(obj);
		}
	}

	/* =========================================================================================== */

	private void testAddress(InetAddress address, int port) throws Exception {
		JmxServer server;
		if (address == null) {
			server = new JmxServer(port);
		} else {
			server = new JmxServer(address, port);
		}
		RandomObject obj = new RandomObject();
		ObjectName objectName = ObjectNameUtil.makeObjectName(DOMAIN_NAME, OBJECT_NAME);
		JmxClient client = null;
		try {
			server.start();
			server.register(obj, objectName, null, null, new JmxOperationInfo[] {
					new JmxOperationInfo("resetFoo", null, null, OperationAction.UNKNOWN, "description") });
			if (address == null) {
				client = new JmxClient(port);
			} else {
				client = new JmxClient(address.getHostAddress(), port);
			}
			obj.setFoo(1);
			client.invokeOperation(DOMAIN_NAME, OBJECT_NAME, "resetFoo");
			assertEquals(0, obj.getFoo());
			try {
				client.getAttribute(DOMAIN_NAME, OBJECT_NAME, "foo");
				fail("Expected this to throw");
			} catch (JMException e) {
				// ignored
			}
		} finally {
			IoUtils.closeQuietly(client);
			server.unregister(objectName);
			IoUtils.closeQuietly(server);
		}
	}

	/* =========================================================================================== */

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

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME, folderNames = { FOLDER_NAME })
	protected static class TestObjectFolders {

		@JmxAttributeMethod
		public int getFoo() {
			return FOO_VALUE;
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class ShortAttributeMethodName {
		@JmxAttributeMethod
		public int x() {
			return 0;
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class JustGet {
		@JmxAttributeMethod
		public int get() {
			return 0;
		}
	}

	@JmxResource(domainName = "not the domain name", beanName = "not the object name")
	protected static class SelfNaming implements JmxSelfNaming {
		@JmxAttributeMethod
		public int getFoo() {
			return FOO_VALUE;
		}

		@Override
		public String getJmxDomainName() {
			return DOMAIN_NAME;
		}

		@Override
		public String getJmxBeanName() {
			return OBJECT_NAME;
		}

		@Override
		public JmxFolderName[] getJmxFolderNames() {
			return null;
		}
	}

	@JmxResource(domainName = "", beanName = OBJECT_NAME)
	protected static class InvalidDomain {
		@JmxAttributeMethod
		public int getFoo() {
			return FOO_VALUE;
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class NoDescriptions {
		@JmxAttributeMethod
		public int getFoo() {
			return FOO_VALUE;
		}

		@JmxOperation
		public void doSomething() {
		}
	}

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class HasDescriptions {
		@JmxAttributeMethod(description = "Foo value")
		public int getFoo() {
			return FOO_VALUE;
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class OperationParameters {
		@JmxOperation(description = "A value", parameterNames = { "first" },
				parameterDescriptions = { "First argument" })
		public String doSomething(String first) {
			return first;
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class MisterThrow {
		@JmxAttributeMethod
		public int getFoo() {
			throw new IllegalStateException("because I can");
		}

		@JmxAttributeMethod
		public void setFoo(int value) {
			throw new IllegalStateException("because I can");
		}

		@JmxOperation
		public void someCall() {
			throw new IllegalStateException("because I can");
		}
	}

	protected static class RandomObject {

		private int foo = FOO_VALUE;

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

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	protected static class SubClassTestObject extends TestObject {

		@JmxAttributeField
		private int bar;

		// this stops the jmx access
		@Override
		public void setFoo(int foo) {
			super.setFoo(foo);
		}

		// set it via a String
		@JmxOperation
		public void resetFoo(String newValue) {
			resetFoo(Integer.parseInt(newValue) * 2);
		}
	}
}
