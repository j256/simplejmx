package com.j256.simplejmx.common;

/**
 * This is used programmically to register another class for JMX exposure.
 * 
 * @author graywatson
 */
public class JmxResourceInfo implements JmxSelfNaming {

	private String jmxDomainName;
	private String jmxNameOfObject;
	private JmxFolderName[] jmxFolderNames;
	private String jmxDescription;

	public JmxResourceInfo() {
		// for spring
	}

	public JmxResourceInfo(String jmxDomainName, String jmxNameOfObject, JmxFolderName[] jmxFolderNames,
			String jmxDescription) {
		this.jmxDomainName = jmxDomainName;
		this.jmxNameOfObject = jmxNameOfObject;
		this.jmxFolderNames = jmxFolderNames;
		this.jmxDescription = jmxDescription;
	}

	public JmxResourceInfo(String jmxDomainName, String jmxNameOfObject, String[] jmxFolderNameStrings,
			String jmxDescription) {
		this.jmxDomainName = jmxDomainName;
		this.jmxNameOfObject = jmxNameOfObject;
		this.jmxFolderNames = new JmxFolderName[jmxFolderNameStrings.length];
		for (int i = 0; i < jmxFolderNameStrings.length; i++) {
			this.jmxFolderNames[i] = new JmxFolderName(jmxFolderNameStrings[i]);
		}
		this.jmxDescription = jmxDescription;
	}

	public String getJmxDomainName() {
		return jmxDomainName;
	}

	/**
	 * Required.
	 */
	public void setJmxDomainName(String jmxDomainName) {
		this.jmxDomainName = jmxDomainName;
	}

	public String getJmxNameOfObject() {
		return jmxNameOfObject;
	}

	/**
	 * NotRequired -- default is the spring bean name
	 */
	public void setJmxNameOfObject(String jmxNameOfObject) {
		this.jmxNameOfObject = jmxNameOfObject;
	}

	public JmxFolderName[] getJmxFolderNames() {
		return jmxFolderNames;
	}

	/**
	 * NotRequired -- default is no folders. Either this or {@link #setJmxFolderNameStrings(String[])} should be used.
	 */
	public void setJmxFolderNames(JmxFolderName[] jmxFolderNames) {
		this.jmxFolderNames = jmxFolderNames;
	}

	/**
	 * NotRequired -- default is no folders. Either this or {@link #setJmxFolderNames(JmxFolderName[])} should be used.
	 * Can be used to specify an array of folder-names instead of having to construct a JmxFolderName array.
	 */
	public void setJmxFolderNameStrings(String[] jmxFolderNameStrings) {
		this.jmxFolderNames = new JmxFolderName[jmxFolderNameStrings.length];
		for (int i = 0; i < jmxFolderNameStrings.length; i++) {
			this.jmxFolderNames[i] = new JmxFolderName(jmxFolderNameStrings[i]);
		}
	}

	/**
	 * @see {@link JmxResource#description()}
	 */
	public String getJmxDescription() {
		return jmxDescription;
	}

	/**
	 * NotRequired -- default is none.
	 */
	public void setJmxDescription(String jmxDescription) {
		this.jmxDescription = jmxDescription;
	}
}
