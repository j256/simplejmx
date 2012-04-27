package com.j256.simplejmx.common;

import javax.management.ObjectName;

/**
 * Utility class that creates {@link ObjectName} objects from various arguments.
 * 
 * @author graywatson
 */
public class ObjectNameUtil {

	private ObjectNameUtil() {
		// only for static methods
	}

	/**
	 * Constructs an object-name from a jmx-resource and a self naming object.
	 * 
	 * @param jmxResource
	 *            Annotation from the class for which we are creating our ObjectName. It may be null.
	 * @param selfNamingObj
	 *            Object that implements the self-naming interface.
	 */
	public static ObjectName makeObjectName(JmxResource jmxResource, JmxSelfNaming selfNamingObj) {
		String domainName = selfNamingObj.getJmxDomainName();
		if (domainName == null) {
			if (jmxResource != null) {
				domainName = jmxResource.domainName();
			}
			if (domainName == null || domainName.length() == 0) {
				throw new IllegalArgumentException(
						"Could not create ObjectName because domain name not specified in getJmxDomainName() nor @JmxResource");
			}
		}
		String beanName = selfNamingObj.getJmxNameOfObject();
		if (beanName == null) {
			if (jmxResource != null) {
				beanName = getBeanName(jmxResource);
			}
			if (beanName == null || beanName.length() == 0) {
				beanName = selfNamingObj.getClass().getSimpleName();
			}
		}
		return makeObjectName(domainName, beanName, selfNamingObj.getJmxFolderNames(), null);
	}

	/**
	 * Constructs an object-name from a jmx-resource and a object which is not self-naming.
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
		String beanName = getBeanName(jmxResource);
		if (beanName == null) {
			beanName = obj.getClass().getSimpleName();
		}
		return makeObjectName(domainName, beanName, null, jmxResource.folderNames());
	}

	/**
	 * Constructs an object-name from a domain-name, object-name, and folder-name strings.
	 * 
	 * @param domainName
	 *            This is the top level folder name for the beans.
	 * @param beanName
	 *            This is the bean name in the lowest folder level.
	 * @param folderNameStrings
	 *            These can be used to setup folders inside of the top folder. Each of the entries in the array can
	 *            either be in "value" or "name=value" format.
	 */
	public static ObjectName makeObjectName(String domainName, String beanName, String[] folderNameStrings) {
		return makeObjectName(domainName, beanName, null, folderNameStrings);
	}

	/**
	 * Constructs an object-name from a domain-name and object-name.
	 * 
	 * @param domainName
	 *            This corresponds to the {@link JmxResource#domainName()} and is the top level folder name for the
	 *            beans.
	 * @param beanName
	 *            This corresponds to the {@link JmxResource#objectName()} and is the bean name in the lowest folder
	 *            level.
	 */
	public static ObjectName makeObjectName(String domainName, String beanName) {
		return makeObjectName(domainName, beanName, null, null);
	}

	private static ObjectName makeObjectName(String domainName, String beanName, JmxFolderName[] folderNames,
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
		sb.append(beanName);
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

	private static String getBeanName(JmxResource jmxResource) {
		String beanName = jmxResource.beanName();
		if (beanName.length() > 0) {
			return beanName;
		}
		@SuppressWarnings("deprecation")
		String deprecatedName = jmxResource.objectName();
		beanName = deprecatedName;
		if (beanName.length() > 0) {
			return beanName;
		} else {
			return null;
		}
	}
}
