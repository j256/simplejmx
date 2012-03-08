package com.j256.simplejmx.common;

import javax.management.ObjectName;

/**
 * This allows objcts to name themselves based on some internal values. This is usually for objects that have multiple
 * instances and that are dynamically added and removed.
 * 
 * @author graywatson
 */
public interface JmxSelfNaming {

	/**
	 * Return the domain name of the object that is used to built the associated {@link ObjectName}. Return null to use
	 * the one from the {@link JmxResource} annotation instead.
	 */
	public String getJmxDomainName();

	/**
	 * Return the name of the object that will be the "name=..." part of the associated {@link ObjectName}. Return null
	 * to use the one from the {@link JmxResource} annotation instead.
	 */
	public String getJmxObjectName();

	/**
	 * Return the appropriate array of folder names used to built the associated {@link ObjectName}.
	 */
	public JmxFolderName[] getJmxFolderNames();
}
