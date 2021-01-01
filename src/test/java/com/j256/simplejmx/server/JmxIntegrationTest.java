package com.j256.simplejmx.server;

import org.junit.Ignore;

import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxFolderName;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.common.JmxSelfNaming;

@Ignore("Just for integration testing")
public class JmxIntegrationTest {

	private static final int DEFAULT_PORT = 5256;
	private static final String DOMAIN_NAME = "j256";
	private static final String OBJECT_NAME = "testObject";
	private static final int FOO_VALUE = 1459243;

	public static void main(String args[]) throws Exception {
		new JmxIntegrationTest().doMain(args);
	}

	public void doMain(String args[]) throws Exception {
		JmxServer server = new JmxServer(DEFAULT_PORT);
		try {
			server.start();
			server.register(new TestObject(server));
			server.register(new TestSubObject());
			System.out.println("JMX server running on port: " + DEFAULT_PORT);
			Thread.sleep(1000000000);
		} finally {
			server.stop();
		}
	}

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	public static class TestObject {

		private final JmxServer jmxServer;
		private int foo = FOO_VALUE;

		public TestObject(JmxServer jmxServer) {
			this.jmxServer = jmxServer;
		}

		@JmxAttributeMethod(description = "Integer value")
		public int getFoo() {
			return foo;
		}

		@JmxAttributeMethod(description = "Integer value")
		public void setFoo(int foo) {
			this.foo = foo;
		}

		@JmxOperation(description = "Set Foo to be 0")
		public void resetFoo() {
			this.foo = 0;
		}

		@JmxOperation(description = "Set Foo to be a particular value as an operation",
				parameterNames = { "newValue" }, parameterDescriptions = { "new value to set to foo" })
		public void resetFoo(int newValue) {
			this.foo = newValue;
		}

		@JmxOperation(description = "Add the two params", parameterNames = { "first", "second" },
				parameterDescriptions = { "First one", "Second one" })
		public String twoArguments(int first, int second) {
			return first + " + " + second + " = " + (first + second);
		}

		// another operation without any parameter information
		@JmxOperation(description = "Return the parameter")
		public String anotherOperation(String someParam) {
			return "Parameter was: " + someParam;
		}

		@JmxOperation(description = "Add another object to JMX")
		public void addAnotherObject() throws Exception {
			jmxServer.register(new AnotherObject(jmxServer));
		}
	}

	@JmxResource(domainName = DOMAIN_NAME, beanName = OBJECT_NAME)
	public static class TestSubObject implements JmxSelfNaming {

		int value = 0;

		@Override
		public String getJmxDomainName() {
			return null;
		}

		@Override
		public String getJmxBeanName() {
			return null;
		}

		@Override
		public JmxFolderName[] getJmxFolderNames() {
			return new JmxFolderName[] { new JmxFolderName("Sub") };
		}

		@JmxAttributeMethod
		public int getValue() {
			return value;
		}

		@JmxAttributeMethod
		public void setValue(int value) {
			this.value = value;
		}

		@JmxOperation
		public String returnString() {
			return "here's your string";
		}
	}

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME)
	protected static class AnotherObject implements JmxSelfNaming {

		private final JmxServer jmxServer;

		public AnotherObject(JmxServer jmxServer) {
			this.jmxServer = jmxServer;
		}

		@Override
		public String getJmxDomainName() {
			return null;
		}

		@Override
		public JmxFolderName[] getJmxFolderNames() {
			return new JmxFolderName[] { new JmxFolderName("00", "AnotherObjects") };
		}

		@Override
		public String getJmxBeanName() {
			return Integer.toString(System.identityHashCode(this));
		}

		@JmxOperation(description = "Remove this object from JMX")
		public String remove() {
			jmxServer.unregister(this);
			return "Removed";
		}
	}
}
