package com.j256.simplejmx.common;

import javax.management.ObjectName;

/**
 * Utility class that creates {@link ObjectName} objects from various input information.
 * 
 * @author graywatson
 */
public class ObjectNameUtil {

	private ObjectNameUtil() {
		// only for static methods
	}

	/**
	 * Used to construct an object name the same in the client and the server.
	 * 
	 * @param jmxResource
	 *            Annotation from the class for which we are creating our ObjectName.
	 * @param selfNamingObj
	 *            Object that implements the self-naming interface.
	 */
	public static ObjectName makeObjectName(JmxResource jmxResource, JmxSelfNaming selfNamingObj) {
		String domainName = selfNamingObj.getJmxDomainName();
		if (domainName == null) {
			domainName = jmxResource.domainName();
			if (domainName.length() == 0) {
				throw new IllegalArgumentException(
						"Could not create ObjectName because domain name not specified in getJmxDomainName() nor @JmxResource");
			}
		}
		String objectName = selfNamingObj.getJmxObjectName();
		if (objectName == null) {
			objectName = jmxResource.objectName();
			if (objectName.length() == 0) {
				objectName = selfNamingObj.getClass().getSimpleName();
			}
		}
		return makeObjectName(domainName, objectName, selfNamingObj.getJmxFolderNames(), null);
	}

	/**
	 * Used to construct an object name the same in the client and the server.
	 * 
	 * @param jmxResource
	 *            Annotation from the class for which we are creating our ObjectName.
	 * @param obj
	 *            Object for which we are creating our ObjectName
	 */
	public static ObjectName makeObjectName(JmxResource jmxResource, Object obj) {
		String domainName = jmxResource.domainName();
		if (domainName.length() == 0) {
			throw new IllegalArgumentException(
					"Could not create ObjectName because domain name not specified in @JmxResource");
		}
		String objectName = jmxResource.objectName();
		if (objectName.length() == 0) {
			objectName = obj.getClass().getSimpleName();
		}
		return makeObjectName(domainName, objectName, null, jmxResource.folderNames());
	}

	/**
	 * Used to construct an object name the same in the client and the server. Mostly for testing purposes.
	 * 
	 * @param domainName
	 *            This is the top level folder name for the beans.
	 * @param objectName
	 *            This is the bean name in the lowest folder level.
	 * @param folderNameStrings
	 *            These can be used to setup folders inside of the top folder. Each of the entries in the array can
	 *            either be in "value" or "name=value" format.
	 */
	public static ObjectName makeObjectName(String domainName, String objectName, String[] folderNameStrings) {
		return makeObjectName(domainName, objectName, null, folderNameStrings);
	}

	/**
	 * Used to construct an object name the same in the client and the server.
	 * 
	 * @param domainName
	 *            This corresponds to the {@link JmxResource#domainName()} and is the top level folder name for the
	 *            beans.
	 * @param objectName
	 *            This corresponds to the {@link JmxResource#objectName()} and is the bean name in the lowest folder
	 *            level.
	 */
	public static ObjectName makeObjectName(String domainName, String objectName) {
		return makeObjectName(domainName, objectName, null, null);
	}

	private static ObjectName makeObjectName(String domainName, String objectName, JmxFolderName[] folderNames,
			String[] folderNameStrings) {
		// j256:00=clients,name=Foo
		StringBuilder sb = new StringBuilder();
		sb.append(domainName);
		sb.append(':');
		boolean first = true;
		int prefixC = 0;
		if (folderNames != null) {
			for (JmxFolderName folderName : folderNames) {
				if (first) {
					first = false;
				} else {
					sb.append(',');
				}
				String fieldName = folderName.getField();
				if (fieldName == null) {
					appendPrefix(sb, prefixC++);
				} else {
					sb.append(fieldName);
				}
				sb.append('=');
				sb.append(folderName.getValue());
			}
		}
		if (folderNameStrings != null) {
			for (String folderNameString : folderNameStrings) {
				if (first) {
					first = false;
				} else {
					sb.append(',');
				}
				// if we have no = then prepend a number
				if (folderNameString.indexOf('=') == -1) {
					appendPrefix(sb, prefixC++);
					sb.append('=');
				}
				sb.append(folderNameString);
			}
		}
		if (!first) {
			sb.append(',');
		}
		sb.append("name=");
		sb.append(objectName);
		String objectNameString = sb.toString();
		try {
			return new ObjectName(objectNameString);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid ObjectName generated: " + objectNameString, e);
		}
	}

	private static void appendPrefix(StringBuilder sb, int prefixC) {
		if (prefixC < 10) {
			sb.append('0');
		}
		sb.append(prefixC);
	}
}
