package com.j256.simplejmx.spring;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;

/**
 * Used by the SpringTextProgram and loaded in the spring.xml file.
 * 
 * @author graywatson
 */
@JmxResource(domainName = "j256.simplejmx", beanName = "Main", description = "Main Jmx class")
public class MainJmx {

	private long start = System.currentTimeMillis();
	private CountDownLatch shutdownLatch = new CountDownLatch(1);

	@JmxAttributeMethod(description = "Start time in millis.")
	public long getStartTimeMillis() {
		return start;
	}

	@JmxAttributeMethod(description = "Start time string")
	public String getStartTimeString() {
		return millisTimeToStart(start);
	}

	@JmxAttributeMethod(description = "Current time in millis.")
	public long getCurrentTimeMillis() {
		return System.currentTimeMillis();
	}

	@JmxAttributeMethod(description = "Current time string.")
	public String getCurrentTimeString() {
		return millisTimeToStart(System.currentTimeMillis());
	}

	@JmxAttributeMethod(description = "Run time in millis.")
	public long getRunTimeMillis() {
		return System.currentTimeMillis() - start;
	}

	@JmxOperation(description = "Stop the application")
	public void shutdown() {
		shutdownLatch.countDown();
	}

	/**
	 * Wait for shutdown.
	 */
	public void waitForShutdown() {
		try {
			shutdownLatch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private String millisTimeToStart(long millis) {
		return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z").format(new Date(millis));
	}
}
