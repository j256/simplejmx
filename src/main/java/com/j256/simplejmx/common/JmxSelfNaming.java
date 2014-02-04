package com.j256.simplejmx.common;

import javax.management.ObjectName;

import com.j256.simplejmx.server.JmxServer;

/**
 * This allows objects to name themselves based on fields or values internal to the _instance_ of the class. This is
 * often used by objects that have multiple instances and that are dynamically added and removed. Examples of objects
 * that might use self-naming are: database connections, log instances, network config objects. Anytime you have
 * multiple instances of the same object that you want to expose via JMX.
 * 
 * <p>
 * You register these objects using the standard {@link JmxServer#register(Object)} methods and then call
 * {@link JmxServer#unregister(Object)} if they are removed later. For an example of dynamic self-naming objects, see
 * the JmxIntegrationTest class.
 * </p>
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
	 * to use the one from the {@link JmxResource#beanName()} annotation instead.
	 */
	public String getJmxBeanName();

	/**
	 * Return the appropriate array of folder names used to built the associated {@link ObjectName}. Return null to use
	 * the folder names specified in the {@link JmxResource#folderNames()} annotation instead.
	 */
	public JmxFolderName[] getJmxFolderNames();
}
