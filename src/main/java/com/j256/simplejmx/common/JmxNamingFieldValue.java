package com.j256.simplejmx.common;

import com.j256.simplejmx.server.JmxServer;

/**
 * Wrapper around a field and value pair that is used in {@link JmxResource#fieldValues()} and
 * {@link JmxServer#makeObjectName(String, String, String[])}.
 * 
 * @author graywatson
 */
public class JmxNamingFieldValue {

	private String field;
	private String value;

	public JmxNamingFieldValue(String field, String value) {
		this.field = field;
		this.value = value;
	}

	public String getField() {
		return field;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return field + "=" + value;
	}
}
