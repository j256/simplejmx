package com.j256.simplejmx.common;

/**
 * This identifies a field that you want to expose via JMX.
 * 
 * @author graywatson
 */
public class JmxAttributeFieldInfo {

	public String name;
	public boolean isReadible = true;
	public boolean isWritable;
	public String description;

	public JmxAttributeFieldInfo() {
		// for spring
	}

	public JmxAttributeFieldInfo(String name, boolean isReadible, boolean isWritable, String description) {
		this.name = name;
		this.isReadible = isReadible;
		this.isWritable = isWritable;
		this.description = description;
	}

	public JmxAttributeFieldInfo(String name, JmxAttributeField jmxAttribute) {
		this.name = name;
		this.isReadible = jmxAttribute.isReadible();
		this.isWritable = jmxAttribute.isWritable();
		this.description = jmxAttribute.description();
	}

	public String getName() {
		return name;
	}

	/**
	 * Required.
	 */
	public void setName(String name) {
		this.name = name;
	}

	public boolean isReadible() {
		return isReadible;
	}

	/**
	 * Not required. Default is true.
	 */
	public void setReadible(boolean isReadible) {
		this.isReadible = isReadible;
	}

	public boolean isWritable() {
		return isWritable;
	}

	/**
	 * Not required. Default is false.
	 */
	public void setWritable(boolean isWritable) {
		this.isWritable = isWritable;
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
