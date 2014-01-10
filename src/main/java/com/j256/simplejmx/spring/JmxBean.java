package com.j256.simplejmx.spring;

import org.springframework.beans.factory.annotation.Required;

import com.j256.simplejmx.common.JmxAttributeFieldInfo;
import com.j256.simplejmx.common.JmxAttributeMethodInfo;
import com.j256.simplejmx.common.JmxOperationInfo;
import com.j256.simplejmx.common.JmxResourceInfo;

/**
 * This is used to identify an Spring wired object that we want to
 * 
 * @author graywatson
 */
public class JmxBean {

	private JmxResourceInfo jmxResourceInfo;
	private JmxAttributeFieldInfo[] attributeFieldInfos;
	private JmxAttributeMethodInfo[] attributeMethodInfos;
	private JmxOperationInfo[] operationInfos;
	private Object target;

	public JmxResourceInfo getJmxResourceInfo() {
		return jmxResourceInfo;
	}

	@Required
	public void setJmxResourceInfo(JmxResourceInfo jmxResourceInfo) {
		this.jmxResourceInfo = jmxResourceInfo;
	}

	public JmxAttributeFieldInfo[] getAttributeFieldInfos() {
		return attributeFieldInfos;
	}

	/**
	 * Optional.
	 */
	public void setAttributeFieldInfos(JmxAttributeFieldInfo[] attributeFieldInfos) {
		this.attributeFieldInfos = attributeFieldInfos;
	}

	public JmxAttributeMethodInfo[] getAttributeMethodInfos() {
		return attributeMethodInfos;
	}

	/**
	 * Optional.
	 */
	public void setAttributeMethodInfos(JmxAttributeMethodInfo[] attributeMethodInfos) {
		this.attributeMethodInfos = attributeMethodInfos;
	}

	public JmxOperationInfo[] getOperationInfos() {
		return operationInfos;
	}

	/**
	 * Optional.
	 */
	public void setOperationInfos(JmxOperationInfo[] operationInfos) {
		this.operationInfos = operationInfos;
	}

	public Object getTarget() {
		return target;
	}

	@Required
	public void setTarget(Object target) {
		this.target = target;
	}
}
