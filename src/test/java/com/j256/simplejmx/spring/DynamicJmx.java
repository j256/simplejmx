package com.j256.simplejmx.spring;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.JMException;

import org.springframework.beans.factory.annotation.Required;

import com.j256.simplejmx.common.BaseJmxSelfNaming;
import com.j256.simplejmx.common.JmxAttributeField;
import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.server.JmxServer;

/**
 * Used by the SpringTextProgram and loaded in the spring.xml file. It shows how we can register and unregister dynamic
 * objects at runtime.
 * 
 * @author graywatson
 */
@JmxResource(domainName = "j256.simplejmx", beanName = "Dynamic", description = "Dynamic Jmx class")
public class DynamicJmx {

	private JmxServer jmxServer;

	private final AtomicInteger thingCount = new AtomicInteger(0);
	private final Map<Integer, Thing> things = new HashMap<Integer, Thing>();

	@JmxAttributeMethod(description = "Number of things created")
	public int getNumThings() {
		return things.size();
	}

	@JmxOperation(description = "Create a new thing")
	public String createThing() {
		int number = thingCount.getAndIncrement();
		Thing thing = new Thing(number);
		things.put(number, thing);
		try {
			/*
			 * This adds the thing to the JMX bean list.
			 */
			jmxServer.register(thing);
			return "Created #" + number;
		} catch (JMException e) {
			return "Trying to register thing threw exception: " + e.getMessage();
		}
	}

	@JmxOperation(description = "Remove an existing thing")
	public String removeThing(int number) {
		Thing thing = things.remove(number);
		if (thing == null) {
			return "#" + number + " not found";
		} else {
			/*
			 * This removes the thing from the JMX bean list.
			 */
			jmxServer.unregister(thing);
			return "Removed #" + number;
		}
	}

	@Required
	public void setJmxServer(JmxServer jmxServer) {
		this.jmxServer = jmxServer;
	}

	// the JmxResource annotation sets the domain-name and folders, self-naming could do that as well
	@JmxResource(domainName = "j256.simplejmx", folderNames = { "things" })
	private static class Thing extends BaseJmxSelfNaming {

		@JmxAttributeField(description = "Our thing number")
		private int number;
		@SuppressWarnings("unused")
		@JmxAttributeField(description = "Time in millis when we were created")
		private long startTimeMillis = System.currentTimeMillis();

		public Thing(int number) {
			this.number = number;
		}

		@Override
		public String getJmxBeanName() {
			// we just return our number as our bean name in JMX
			return Integer.toString(number);
		}
	}
}
