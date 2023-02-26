package com.j256.simplejmx.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Here's a little example program that was written to show how to use SimpleJMX with the Spring Framework. Support for
 * Spring is optional and will only work if you have a dependency on and import the Spring jar(s) in your project.
 * 
 * <p>
 * <b>NOTE:</b> This is posted on the http://256stuff.com/sources/simplejmx/ website.
 * </p>
 * 
 * @author graywatson
 */
public class SpringTestProgram {

	private static final String[] SPRING_CONFIG_FILES = new String[] { "classpath:/spring.xml" };

	public static void main(String[] args) throws Exception {
		new SpringTestProgram().doMain(args);
	}

	private void doMain(String[] args) throws Exception {

		ClassPathXmlApplicationContext mainContext = null;

		try {
			// load our context
			mainContext = new ClassPathXmlApplicationContext(SPRING_CONFIG_FILES);
			mainContext.registerShutdownHook();

			// get our MainJmx bean from the context
			MainJmx mainJmx = (MainJmx) mainContext.getBean("mainJmx", MainJmx.class);

			// wait for someone to run the shutdown operation
			System.out.println("waiting for the shutdown operation to be run via Jconsole");
			mainJmx.waitForShutdown();
			System.out.println("exiting");
		} finally {
			if (mainContext != null) {
				mainContext.close();
			}
		}
	}
}
