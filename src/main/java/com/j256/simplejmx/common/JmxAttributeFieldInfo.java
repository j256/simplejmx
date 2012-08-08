package com.j256.simplejmx.common;

/**
 * This identifies a field that you want to expose via JMX.
 * 
 * @author graywatson
 */
public class JmxAttributeFieldInfo {

	public final String name;
	public final boolean isReadible;
	public final boolean isWritable;
	public final String description;

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

	public boolean isReadible() {
		return isReadible;
	}

	public boolean isWritable() {
		return isWritable;
	}

	public String getDescription() {
		return description;
	}
}
