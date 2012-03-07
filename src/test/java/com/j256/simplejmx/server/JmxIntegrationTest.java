package com.j256.simplejmx.server;

import com.j256.simplejmx.common.JmxAttribute;
import com.j256.simplejmx.common.JmxNamingFieldValue;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.common.JmxSelfNaming;

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
			synchronized (this) {
				this.wait();
			}
		} finally {
			server.stop();
		}
	}

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME, objectName = OBJECT_NAME)
	protected static class TestObject {

		private final JmxServer jmxServer;
		private int foo = FOO_VALUE;

		public TestObject(JmxServer jmxServer) {
			this.jmxServer = jmxServer;
		}

		@JmxAttribute(description = "Integer value")
		public int getFoo() {
			return foo;
		}

		@JmxAttribute(description = "Integer value")
		public void setFoo(int foo) {
			this.foo = foo;
		}

		@JmxOperation(description = "Set Foo to be 0")
		public void resetFoo() {
			this.foo = 0;
		}

		@JmxOperation(description = "Set Foo to be a particular value as an operation", parameterNames = { "newValue" }, parameterDescriptions = { "new value to set to foo" })
		public void resetFoo(int newValue) {
			this.foo = newValue;
		}

		@JmxOperation(description = "Add the two params", parameterNames = { "first", "second" }, parameterDescriptions = {
				"First one", "Second one" })
		public String twoArguements(int first, int second) {
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

	@JmxResource(description = "Test object", domainName = DOMAIN_NAME)
	protected static class AnotherObject implements JmxSelfNaming {

		private final JmxServer jmxServer;

		public AnotherObject(JmxServer jmxServer) {
			this.jmxServer = jmxServer;
		}

		public JmxNamingFieldValue[] getJmxFieldValues() {
			return new JmxNamingFieldValue[] { new JmxNamingFieldValue("00", "AnotherObjects") };
		}

		public String getJmxObjectName() {
			return Integer.toString(System.identityHashCode(this));
		}

		@JmxOperation(description = "Remove this object from JMX")
		public String remove() {
			jmxServer.unregister(this);
			return "Removed";
		}
	}
}
