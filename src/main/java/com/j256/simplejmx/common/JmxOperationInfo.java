package com.j256.simplejmx.common;

import javax.management.MBeanOperationInfo;

/**
 * This identifies a method that is _not_ named "get...", "is...", or "set..." to be a JMX operation. The method can
 * either return void or return an object. It is recommended that the method return a simple object that will be for
 * sure in jconsole's classpath and also should not throw an unknown exception class either. This is used when you are
 * wiring using code or Spring another object that does not use the {@link JmxResource} annotation or
 * {@link JmxSelfNaming}.
 * 
 * @author graywatson
 */
public class JmxOperationInfo {

	public String methodName;
	public String[] parameterNames;
	public String[] parameterDescriptions;
	public OperationAction action = OperationAction.UNKNOWN;
	public String description;

	public JmxOperationInfo() {
		// for spring
	}

	public JmxOperationInfo(String methodName, String[] parameterNames, String[] parameterDescriptions,
			OperationAction action, String description) {
		this.methodName = methodName;
		this.parameterNames = parameterNames;
		this.parameterDescriptions = parameterDescriptions;
		this.action = action;
		this.description = description;
	}

	public JmxOperationInfo(String methodName, JmxOperation jmxOperation) {
		this.methodName = methodName;
		this.parameterNames = jmxOperation.parameterNames();
		this.parameterDescriptions = jmxOperation.parameterDescriptions();
		this.action = jmxOperation.operationAction();
		this.description = jmxOperation.description();
	}

	public String getMethodName() {
		return methodName;
	}

	/**
	 * Required.
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String[] getParameterNames() {
		return parameterNames;
	}

	/**
	 * Not required. Default is none.
	 */
	public void setParameterNames(String[] parameterNames) {
		this.parameterNames = parameterNames;
	}

	public String[] getParameterDescriptions() {
		return parameterDescriptions;
	}

	/**
	 * Not required. Default is none.
	 */
	public void setParameterDescriptions(String[] parameterDescriptions) {
		this.parameterDescriptions = parameterDescriptions;
	}

	public OperationAction getAction() {
		return action;
	}

	/**
	 * Not required. Default is UNKNOWN.
	 */
	public void setAction(OperationAction action) {
		this.action = action;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Not required. Default is "Information about class".
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return methodName;
	}

	/**
	 * An enumerated version of the constants from {@link MBeanOperationInfo}.
	 */
	public enum OperationAction {
		/**
		 * Indicates that the operation is read-like, it basically returns information.
		 */
		INFO(MBeanOperationInfo.INFO),
		/**
		 * Indicates that the operation is a write-like, and would modify the MBean in some way, typically by writing
		 * some value or changing a configuration.
		 */
		ACTION(MBeanOperationInfo.ACTION),
		/**
		 * Indicates that the operation is both read-like and write-like.
		 */
		ACTION_INFO(MBeanOperationInfo.ACTION_INFO),
		/**
		 * Indicates that the operation has an "unknown" nature.
		 */
		UNKNOWN(MBeanOperationInfo.UNKNOWN),
		// end
		;

		private final int actionValue;

		private OperationAction(int jmxActionValue) {
			this.actionValue = jmxActionValue;
		}

		/**
		 * Return the associated MBeanOperationInfo int constant.
		 */
		public int getActionValue() {
			return actionValue;
		}
	}
}
