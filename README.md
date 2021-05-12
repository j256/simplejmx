Simple Java JMX
===============

This package provides some Java classes to help with the publishing of objects using JMX.

* For more information, visit the [SimpleJMX home page](http://256stuff.com/sources/simplejmx/).
* Online documentation can be found on the home page.  Here are the [SimpleJMX Javadocs](http://256stuff.com/sources/simplejmx/javadoc/simplejmx/).
* Code available from the [git repository](https://github.com/j256/simplejmx).  [![CircleCI](https://circleci.com/gh/j256/simplejmx.svg?style=svg)](https://circleci.com/gh/j256/simplejmx) [![CodeCov](https://img.shields.io/codecov/c/github/j256/simplejmx.svg)](https://codecov.io/github/j256/simplejmx/)
* Maven packages are published via [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.j256.simplejmx/simplejmx/badge.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/com.j256.simplejmx/simplejmx/) [![javadoc](https://javadoc.io/badge2/com.j256.simplejmx/simplejmx/javadoc.svg)](https://javadoc.io/doc/com.j256.simplejmx/simplejmx)

Enjoy.  Gray Watson

## Little Sample Program

Here's a [little sample program](http://256stuff.com/sources/simplejmx/docs/example-simple) to help you get started.

## Publishing JMX Beans over HTTP for Web Browser

SimpleJMX also contains a simple web-server handler that uses Jetty so that you can access JMX information from a web
browser or other web client using the ```JmxWebServer``` class.  To use this class you need to provide a Jetty
version in your dependency list or classpath.  You just need to add the following code to your application startup.

	// start a web server for exposing jmx beans listing on port 8080
	JmxWebServer jmxWebServer = new JmxWebServer(8080);
	jmxWebServer.start();

For more details, see the [web server sample program](http://256stuff.com/sources/simplejmx/docs/example-web).

## Sample Jmx Code

First we create a server either as a wrapper around the default mbean server running in the JVM or one that listens
on it's own port.

	// create a new JMX server listening on a specific port
	JmxServer jmxServer = new JmxServer(JMX_PORT);
	// NOTE: you could also use the platform mbean server:
	// JmxServer jmxServer = new JmxServer(ManagementFactory.getPlatformMBeanServer());
	
	// start the server
	jmxServer.start();
 	
	// create the object we will be exposing with JMX
	RuntimeCounter counter = new RuntimeCounter();
	// register our object
	jmxServer.register(counter);
	...
	// shutdown our server
	jmxServer.stop();
	...

Here's the class we are publishing via the server.  The class is annotated with `@JmxResource` to define the bean
name.  The fields and get/set methods are annotated to show attributes (`@JmxAttributeField`, `@JmxAttributeMethod`).
Other methods can be annotated with `@JmxOperation` to expose them as operations.

	@JmxResource(domainName = "j256")
	public class RuntimeCounter {
		private long startMillis = System.currentTimeMillis();
		
		// we can annotate fields directly to be published, isReadible defaults to true
		@JmxAttributeField(description = "Show runtime in seconds", isWritable = true)
		private boolean showSeconds;
		
		// we can annotate getter methods
		@JmxAttributeMethod(description = "Run time in seconds or milliseconds")
		public long getRunTime() {
			long diffMillis = System.currentTimeMillis() - startMillis;
			return diffMillis / (showSeconds ? 1000 : 1);
		}
		
		// this is an operation that shows up in the operations tab in jconsole.
		@JmxOperation(description = "Reset our start time to the current millis")
		public String resetStartTime() {
			startMillis = System.currentTimeMillis();
			return "Timer has been reset to current millis";
		}
 	}

# Maven Configuration

* Maven packages are published via [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.j256.simplejmx/simplejmx/badge.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/com.j256.simplejmx/simplejmx/)

``` xml
<dependency>
	<groupId>com.j256.simplejmx</groupId>
	<artifactId>simplejmx</artifactId>
	<version>1.19</version>
</dependency>
```

# ChangeLog Release Notes

See the [ChangeLog.txt file](src/main/javadoc/doc-files/changelog.txt).
