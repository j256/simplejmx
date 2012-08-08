package com.j256.simplejmx.common;

import javax.management.MBeanOperationInfo;

/**
 * This identifies a methods that is a JMX operation. It for methods that are _not_ named "get..." or "set...". The
 * method can either return void or return an object. It is recommended that the method return a simple object that will
 * be for sure in jconsole's classpath and also should not throw an unknown exception class either.
 * 
 * @author graywatson
 */
public class JmxOperationInfo {

	public String methodName;
	public String[] parameterNames;
	public String[] parameterDescriptions;
	public OperationAction action;
	public String description;

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
		@SuppressWarnings("deprecation")
		int actionVal = jmxOperation.action();
		if (actionVal == MBeanOperationInfo.UNKNOWN) {
			this.action = jmxOperation.operationAction();
		} else {
			this.action = OperationAction.fromMbeanOperationInfo(actionVal);
		}
		this.description = jmxOperation.description();
	}

	public String getMethodName() {
		return methodName;
	}

	public String[] getParameterNames() {
		return parameterNames;
	}

	public String[] getParameterDescriptions() {
		return parameterDescriptions;
	}

	public OperationAction getAction() {
		return action;
	}

	public String getDescription() {
		return description;
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

		private final int jmxAction;

		private OperationAction(int jmxAction) {
			this.jmxAction = jmxAction;
		}

		/**
		 * Return the associated MBeanOperationInfo constant.
		 */
		public int getJmxAction() {
			return jmxAction;
		}

		/**
		 * Return the enumerated action from the MBeanOperationInfo constant.
		 */
		public static OperationAction fromMbeanOperationInfo(int jmxAction) {
			for (OperationAction action : values()) {
				if (action.jmxAction == jmxAction) {
					return action;
				}
			}
			return UNKNOWN;
		}
	}
}
