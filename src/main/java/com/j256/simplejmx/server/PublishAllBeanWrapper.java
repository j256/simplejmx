package com.j256.simplejmx.server;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.j256.simplejmx.common.JmxAttributeFieldInfo;
import com.j256.simplejmx.common.JmxAttributeMethodInfo;
import com.j256.simplejmx.common.JmxOperationInfo;
import com.j256.simplejmx.common.JmxOperationInfo.OperationAction;
import com.j256.simplejmx.common.JmxResourceInfo;

/**
 * Wraps another bean exposing all public methods as operations and get/set/is methods as attributes.
 * 
 * @author graywatson
 */
public class PublishAllBeanWrapper {

	private Object target;
	private JmxResourceInfo jmxResourceInfo;

	private static final Set<String> ignoredMethods = new HashSet<String>();

	static {
		ignoredMethods.add("getClass");
		ignoredMethods.add("wait");
		ignoredMethods.add("equals");
		ignoredMethods.add("toString");
		ignoredMethods.add("hashCode");
		ignoredMethods.add("notify");
		ignoredMethods.add("notifyAll");
	}

	public PublishAllBeanWrapper() {
		// for spring
	}

	/**
	 * @param target
	 *            Object that we are exposing.
	 * @param jmxResourceInfo
	 *            Resource information about the bean.
	 */
	public PublishAllBeanWrapper(Object target, JmxResourceInfo jmxResourceInfo) {
		this.target = target;
		this.jmxResourceInfo = jmxResourceInfo;
		if (jmxResourceInfo.getJmxBeanName() == null) {
			jmxResourceInfo.setJmxBeanName(target.getClass().getSimpleName());
		}
	}

	public Object getTarget() {
		return target;
	}

	/**
	 * @deprecated Should use {@link #setTarget(Object)}.
	 */
	@Deprecated
	public void setDelegate(Object delegate) {
		this.target = delegate;
	}

	/**
	 * Required bean that we are wrapping.
	 */
	public void setTarget(Object target) {
		this.target = target;
	}

	public JmxResourceInfo getJmxResourceInfo() {
		return jmxResourceInfo;
	}

	/**
	 * Required resource information to provide domain and name information for the bean.
	 */
	public void setJmxResourceInfo(JmxResourceInfo jmxResourceInfo) {
		this.jmxResourceInfo = jmxResourceInfo;
	}

	public JmxAttributeFieldInfo[] getAttributeFieldInfos() {
		// run through all _public_ fields, add get/set, final is no-write

		List<JmxAttributeFieldInfo> fieldInfos = new ArrayList<JmxAttributeFieldInfo>();
		Set<String> knownFields = new HashSet<String>();
		for (Class<?> clazz = target.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
			for (Field field : clazz.getFields()) {
				if (!knownFields.add(field.getName())) {
					continue;
				}
				fieldInfos.add(new JmxAttributeFieldInfo(field.getName(), true,
						!Modifier.isFinal(field.getModifiers()), null));
			}
		}
		return fieldInfos.toArray(new JmxAttributeFieldInfo[fieldInfos.size()]);
	}

	public JmxAttributeMethodInfo[] getAttributeMethodInfos() {
		List<JmxAttributeMethodInfo> methodInfos = new ArrayList<JmxAttributeMethodInfo>();
		Set<String> knownMethods = new HashSet<String>();
		for (Class<?> clazz = target.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
			for (Method method : clazz.getMethods()) {
				if (!knownMethods.add(method.getName())) {
					continue;
				}
				String name = method.getName();
				if (!ignoredMethods.contains(name) && isGetGetAttributeMethod(name)) {
					methodInfos.add(new JmxAttributeMethodInfo(name, (String) null));
				}
			}
		}
		return methodInfos.toArray(new JmxAttributeMethodInfo[methodInfos.size()]);
	}

	public JmxOperationInfo[] getOperationInfos() {
		List<JmxOperationInfo> operationInfos = new ArrayList<JmxOperationInfo>();
		Set<String> knownMethods = new HashSet<String>();
		for (Class<?> clazz = target.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
			for (Method method : clazz.getMethods()) {
				if (!knownMethods.add(method.getName())) {
					continue;
				}
				String name = method.getName();
				if (!ignoredMethods.contains(name) && !isGetGetAttributeMethod(name)) {
					operationInfos.add(new JmxOperationInfo(name, null, null, OperationAction.UNKNOWN, null));
				}
			}
		}
		return operationInfos.toArray(new JmxOperationInfo[operationInfos.size()]);
	}

	private boolean isGetGetAttributeMethod(String name) {
		if (name.startsWith("is") && name.length() > 2) {
			return true;
		} else if (name.startsWith("get") && name.length() > 3) {
			return true;
		} else if (name.startsWith("set") && name.length() > 3) {
			return true;
		} else {
			return false;
		}
	}
}
