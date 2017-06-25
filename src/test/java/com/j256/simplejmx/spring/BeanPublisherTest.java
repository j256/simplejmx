package com.j256.simplejmx.spring;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.j256.simplejmx.common.JmxAttributeField;
import com.j256.simplejmx.common.JmxFolderName;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.common.JmxSelfNaming;
import com.j256.simplejmx.server.JmxServer;

public class BeanPublisherTest {

	private static final String[] SPRING_CONFIG_FILES = new String[] { "classpath:/beanPublisherTest.xml" };

	@Test
	public void testStuff() {
		ClassPathXmlApplicationContext context = null;

		try {
			// load our context
			context = new ClassPathXmlApplicationContext(SPRING_CONFIG_FILES);
			context.registerShutdownHook();

			// get our MainJmx bean from the context
			context.getBean("jmxServer", JmxServer.class);
		} finally {
			if (context != null) {
				context.close();
			}
		}
	}

	@JmxResource(folderNames = { "foo", "bar" })
	public static class OurJmxObject implements JmxSelfNaming {

		@JmxAttributeField
		public long foo = 10;

		@Override
		public String getJmxDomainName() {
			return "domain";
		}

		@Override
		public String getJmxBeanName() {
			return "bean";
		}

		@Override
		public JmxFolderName[] getJmxFolderNames() {
			return null;
		}

		public void voidMethod() {
			// for testing factory bean handling
		}
	}
}
