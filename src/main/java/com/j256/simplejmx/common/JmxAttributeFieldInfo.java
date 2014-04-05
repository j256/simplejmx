package com.j256.simplejmx.common;

/**
 * This is used programmatically to identify a field that you want to expose via JMX. This is used when you are wiring
 * using code or Spring another object that does not use the {@link JmxResource} annotation or {@link JmxSelfNaming}.
 * 
 * @author graywatson
 */
public class JmxAttributeFieldInfo {

	public String fieldName;
	public boolean isReadible = true;
	public boolean isWritable;
	public String description;

	public JmxAttributeFieldInfo() {
		// for spring
	}

	public JmxAttributeFieldInfo(String fieldName, boolean isReadible, boolean isWritable, String description) {
		this.fieldName = fieldName;
		this.isReadible = isReadible;
		this.isWritable = isWritable;
		this.description = description;
	}

	public JmxAttributeFieldInfo(String fieldName, JmxAttributeField jmxAttribute) {
		this.fieldName = fieldName;
		this.isReadible = jmxAttribute.isReadible();
		this.isWritable = jmxAttribute.isWritable();
		this.description = jmxAttribute.description();
	}

	public String getFieldName() {
		return fieldName;
	}

	/**
	 * @deprecated Should use {@link #setFieldName(String)}.
	 */
	@Deprecated
	public void setName(String name) {
		this.fieldName = name;
	}

	/**
	 * Required.
	 */
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
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

	@Override
	public String toString() {
		return fieldName + '(' + (isReadible ? "r" : "") + (isWritable ? "w" : "") + ')';
	}
}
