package com.j256.simplejmx.common;

/**
 * Base class which has default implementations of all of the {@link JmxSelfNaming} methods. This is designed to allow a
 * subclass to extend it and only override the methods to affect the object-name that they want.
 * 
 * @author graywatson
 */
public class BaseJmxSelfNaming implements JmxSelfNaming {

	@Override
	public String getJmxDomainName() {
		return null;
	}

	@Override
	public String getJmxBeanName() {
		return null;
	}

	@Override
	public JmxFolderName[] getJmxFolderNames() {
		return null;
	}
}
