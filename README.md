Simple Java JMX
===============

This package provides some Java classes to help with the publishing of objects using JMX.

For more information, visit the [SimpleJMX home page](http://256stuff.com/sources/simplejmx/).

Online documentation can be found on the home page.  Here are the [SimpleJMX Javadocs](http://256stuff.com/sources/simplejmx/javadoc/simplejmx/).

You can see the source on the [git repository](https://github.com/j256/simplejmx).

Maven packages are published via the [maven central repo](http://repo1.maven.org/maven2/com/j256/simplejmx/simplejmx/).

Here's a [little working example program](http://256stuff.com/sources/simplejmx/docs/example-simple).

Enjoy,
Gray Watson

## Little Sample Program

Here's a [little sample program](http://256stuff.com/sources/simplejmx/docs/example-simple) to help you get started.

## Sample Code

 // create a new JMX server listening on a specific port
 JmxServer jmxServer = new JmxServer(JMX_PORT);
 // NOTE: you could also do: new JmxServer(ManagementFactory.getPlatformMBeanServer());
 // start our server
 jmxServer.start();
 
 // create the object we will be exposing with JMX
 RuntimeCounter counter = new RuntimeCounter();
 // register our object
 jmxServer.register(counter);
 ...
 // shutdown our server
 jmxServer.stop();
 ...
 
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
		return diffMillis / (showSeconds ? 1 : 1000);
	}

	// this is an operation that shows up in the operations tab in jconsole.
	@JmxOperation(description = "Reset our start time to the current millis")
	public String resetStartTime() {
		startMillis = System.currentTimeMillis();
		return "Timer has been reset to current millis";
	}
 }
