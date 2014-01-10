package com.j256.simplejmx.common;

/**
 * This identifies a get or set method that you want to expose via JMX.
 * 
 * @author graywatson
 */
public class JmxAttributeMethodInfo {

	public String methodName;
	public String description;

	public JmxAttributeMethodInfo() {
		// for spring
	}

	public JmxAttributeMethodInfo(String methodName, String description) {
		this.methodName = methodName;
		this.description = description;
	}

	public JmxAttributeMethodInfo(String methodName, JmxAttributeMethod jmxAttribute) {
		this.methodName = methodName;
		this.description = jmxAttribute.description();
	}

	public String getMethodName() {
		return methodName;
	}

	/**
	 * Required.
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Not required.
	 */
	public void setDescription(String description) {
		this.description = description;
	}
}
