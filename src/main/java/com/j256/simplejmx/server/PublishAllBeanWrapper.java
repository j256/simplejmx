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

	private Object delegate;
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
	 * @param delegate
	 *            Object that we are exposing.
	 * @param jmxResourceInfo
	 *            Resource information about the bean.
	 */
	public PublishAllBeanWrapper(Object delegate, JmxResourceInfo jmxResourceInfo) {
		this.delegate = delegate;
		this.jmxResourceInfo = jmxResourceInfo;
		if (jmxResourceInfo.getJmxBeanName() == null) {
			jmxResourceInfo.setJmxBeanName(delegate.getClass().getSimpleName());
		}
	}

	public Object getDelegate() {
		return delegate;
	}

	public void setDelegate(Object delegate) {
		this.delegate = delegate;
	}

	public JmxResourceInfo getJmxResourceInfo() {
		return jmxResourceInfo;
	}

	public void setJmxResourceInfo(JmxResourceInfo jmxResourceInfo) {
		this.jmxResourceInfo = jmxResourceInfo;
	}

	public JmxAttributeFieldInfo[] getAttributeFieldInfos() {
		// run through all _public_ fields, add get/set, final is no-write

		List<JmxAttributeFieldInfo> fieldInfos = new ArrayList<JmxAttributeFieldInfo>();
		Field[] fields = delegate.getClass().getFields();
		for (Field field : fields) {
			fieldInfos.add(new JmxAttributeFieldInfo(field.getName(), true, !Modifier.isFinal(field.getModifiers()),
					null));
		}
		return fieldInfos.toArray(new JmxAttributeFieldInfo[fieldInfos.size()]);
	}

	public JmxAttributeMethodInfo[] getAttributeMethodInfos() {
		List<JmxAttributeMethodInfo> methodInfos = new ArrayList<JmxAttributeMethodInfo>();
		Method[] methods = delegate.getClass().getMethods();
		for (Method method : methods) {
			String name = method.getName();
			if (!ignoredMethods.contains(name) && isGetGetAttributeMethod(name)) {
				methodInfos.add(new JmxAttributeMethodInfo(name, (String) null));
			}
		}
		return methodInfos.toArray(new JmxAttributeMethodInfo[methodInfos.size()]);
	}

	public JmxOperationInfo[] getOperationInfos() {
		List<JmxOperationInfo> operationInfos = new ArrayList<JmxOperationInfo>();
		Method[] methods = delegate.getClass().getMethods();
		for (Method method : methods) {
			String name = method.getName();
			if (!ignoredMethods.contains(name) && !isGetGetAttributeMethod(name)) {
				operationInfos.add(new JmxOperationInfo(name, null, null, OperationAction.UNKNOWN, null));
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
