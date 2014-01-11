package com.j256.simplejmx.common;

/**
 * This is used programmically to register another class for JMX exposure.
 * 
 * @author graywatson
 */
public class JmxResourceInfo implements JmxSelfNaming {

	private String jmxDomainName;
	private String jmxBeanName;
	private JmxFolderName[] jmxFolderNames;
	private String jmxDescription;

	public JmxResourceInfo() {
		// for spring
	}

	public JmxResourceInfo(String jmxDomainName, String jmxBeanName, JmxFolderName[] jmxFolderNames,
			String jmxDescription) {
		this.jmxDomainName = jmxDomainName;
		this.jmxBeanName = jmxBeanName;
		this.jmxFolderNames = jmxFolderNames;
		this.jmxDescription = jmxDescription;
	}

	public JmxResourceInfo(String jmxDomainName, String jmxBeanName, String[] jmxFolderNameStrings,
			String jmxDescription) {
		this.jmxDomainName = jmxDomainName;
		this.jmxBeanName = jmxBeanName;
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
	 * Required domain name which is the top-level folder in jconsole.
	 * 
	 * @see JmxResource#domainName()
	 */
	public void setJmxDomainName(String jmxDomainName) {
		this.jmxDomainName = jmxDomainName;
	}

	public String getJmxBeanName() {
		return jmxBeanName;
	}

	/**
	 * NotRequired name of the object. The- default is the Spring bean name or object name.
	 * 
	 * @see JmxResource#beanName()
	 */
	public void setJmxBeanName(String jmxBeanName) {
		this.jmxBeanName = jmxBeanName;
	}

	public JmxFolderName[] getJmxFolderNames() {
		return jmxFolderNames;
	}

	/**
	 * NotRequired array of folders where the bean will live. Default is no folders. Either this or
	 * {@link #setJmxFolderNameStrings(String[])} should be used.
	 * 
	 * @see JmxResource#folderNames()
	 */
	public void setJmxFolderNames(JmxFolderName[] jmxFolderNames) {
		this.jmxFolderNames = jmxFolderNames;
	}

	/**
	 * NotRequired array of folders where the bean will live. Default is no folders. Either this or
	 * {@link #setJmxFolderNames(JmxFolderName[])} should be used. Can be used to specify an array of folder-names
	 * instead of having to construct a JmxFolderName array.
	 * 
	 * @see JmxResource#folderNames()
	 */
	public void setJmxFolderNameStrings(String[] jmxFolderNameStrings) {
		this.jmxFolderNames = new JmxFolderName[jmxFolderNameStrings.length];
		for (int i = 0; i < jmxFolderNameStrings.length; i++) {
			this.jmxFolderNames[i] = new JmxFolderName(jmxFolderNameStrings[i]);
		}
	}

	public String getJmxDescription() {
		return jmxDescription;
	}

	/**
	 * Description of the class for jconsole. Not required. Default is something like: "Information about class-name".
	 * 
	 * @see JmxResource#description()
	 */
	public void setJmxDescription(String jmxDescription) {
		this.jmxDescription = jmxDescription;
	}
}
