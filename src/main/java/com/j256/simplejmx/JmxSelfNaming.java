package com.j256.simplejmx;

import javax.management.ObjectName;

/**
 * Objects that know how to name themselves. This is usually for objects that have multiple instances.
 * 
 * @author graywatson
 */
public interface JmxSelfNaming {

	/**
	 * Return the appropriate array of "key=value" strings for a {@link ObjectName}.
	 */
	public String[] getJmxFieldValues();

	/**
	 * Return the name of the object that will be part of "name=..." for a {@link ObjectName}.
	 */
	public String getJmxObjectName();
}
