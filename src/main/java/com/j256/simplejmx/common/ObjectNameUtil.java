package com.j256.simplejmx.common;

import javax.management.ObjectName;

public class ObjectNameUtil {

	/**
	 * Used to construct an object name the same in the client and the server.
	 * 
	 * @param domain
	 *            This corresponds to the {@link JmxResource#domainName()} and is the top level folder name for the
	 *            beans.
	 * @param name
	 *            This corresponds to the {@link JmxResource#objectName()} and is the bean name in the lowest folder
	 *            level.
	 * @param fieldValues
	 *            These can be used to setup folders inside of the top folder. Each of the entries in the array are in
	 *            "name=value" format. It is recommended that they are in alphabetic order. I often use ("00",
	 *            "FolderName"). As far as I can tell, the 00 name is ignored by jconsole.
	 */
	public static ObjectName makeObjectName(String domain, String name, JmxNamingFieldValue[] fieldValues) {
		return makeObjectName(domain, name, fieldValues, null);
	}

	/**
	 * Used to construct an object name the same in the client and the server.
	 * 
	 * @param domain
	 *            This corresponds to the {@link JmxResource#domainName()} and is the top level folder name for the
	 *            beans.
	 * @param name
	 *            This corresponds to the {@link JmxResource#objectName()} and is the bean name in the lowest folder
	 *            level.
	 * @param fieldValuesStrings
	 *            These can be used to setup folders inside of the top folder. Each of the entries in the array should
	 *            be in "name=value" format. It is recommended that they are in alphabetic order. I often use
	 *            ("00=FolderName"). As far as I can tell, the 00 name is ignored by jconsole.
	 */
	public static ObjectName makeObjectName(String domain, String name, String[] fieldValueStrings) {
		return makeObjectName(domain, name, null, fieldValueStrings);
	}

	/**
	 * Used to construct an object name the same in the client and the server.
	 * 
	 * @param domain
	 *            This corresponds to the {@link JmxResource#domainName()} and is the top level folder name for the
	 *            beans.
	 * @param name
	 *            This corresponds to the {@link JmxResource#objectName()} and is the bean name in the lowest folder
	 *            level.
	 */
	public static ObjectName makeObjectName(String domain, String name) {
		return makeObjectName(domain, name, null, null);
	}

	private static ObjectName makeObjectName(String domain, String name, JmxNamingFieldValue[] fieldValues,
			String[] fieldValueStrings) {
		// j256.backupd:00=clients,name=
		StringBuilder sb = new StringBuilder();
		sb.append(domain);
		sb.append(':');
		boolean first = true;
		if (fieldValues != null) {
			for (JmxNamingFieldValue fieldValue : fieldValues) {
				if (first) {
					first = false;
				} else {
					sb.append(',');
				}
				sb.append(fieldValue.getField());
				sb.append('=');
				sb.append(fieldValue.getValue());
			}
		}
		if (fieldValueStrings != null) {
			for (String fieldValue : fieldValueStrings) {
				if (first) {
					first = false;
				} else {
					sb.append(',');
				}
				sb.append(fieldValue);
			}
		}
		if (!first) {
			sb.append(',');
		}
		sb.append("name=");
		sb.append(name);
		try {
			return new ObjectName(sb.toString());
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid ObjectName generated: " + sb.toString(), e);
		}
	}
}
