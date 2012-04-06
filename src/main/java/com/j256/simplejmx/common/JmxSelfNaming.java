package com.j256.simplejmx.common;

import javax.management.ObjectName;

import com.j256.simplejmx.server.JmxServer;

/**
 * This allows objects to name themselves based on some internal values. This is often used by objects that have
 * multiple instances and that are dynamically added and removed. Objects that are passed to
 * {@link JmxServer#register(Object)} must either implement this interface or have a {@link JmxResource} annotation.
 * 
 * @author graywatson
 */
public interface JmxSelfNaming {

	/**
	 * Return the domain name of the object that is used to built the associated {@link ObjectName}. Return null to use
	 * the one from the {@link JmxResource#domainName()} annotation instead.
	 */
	public String getJmxDomainName();

	/**
	 * Return the name of the object that will be the "name=..." part of the associated {@link ObjectName}. Return null
	 * to use the one from the {@link JmxResource#objectName()} annotation instead.
	 */
	public String getJmxNameOfObject();

	/**
	 * Return the appropriate array of folder names used to built the associated {@link ObjectName}. Return null for no
	 * folders in which case the bean will be at the top of the hierarchy in jconsole without any sub-folders.
	 */
	public JmxFolderName[] getJmxFolderNames();
}
