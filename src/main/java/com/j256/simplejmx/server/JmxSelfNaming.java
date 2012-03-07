package com.j256.simplejmx.server;

import javax.management.ObjectName;

/**
 * This allows objcts to name themselves based on some internal values. This is usually for objects that have multiple
 * instances and that are dynamically added and removed.
 * 
 * @author graywatson
 */
public interface JmxSelfNaming {

	/**
	 * Return the appropriate array of key and value strings for a {@link ObjectName}.
	 */
	public JmxNamingFieldValue[] getJmxFieldValues();

	/**
	 * Return the name of the object that will be part of "name=..." for a {@link ObjectName}.
	 */
	public String getJmxObjectName();
}
