package com.j256.simplejmx.server;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;

import com.j256.simplejmx.common.JmxAttributeField;
import com.j256.simplejmx.common.JmxAttributeFieldInfo;
import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxAttributeMethodInfo;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxOperationInfo;
import com.j256.simplejmx.common.JmxResource;

/**
 * This wraps an object that has been registered in the server using {@link JmxServer#register(Object)}. We wrap the
 * object so we can expose its attributes and operations using annotations and reflection. This handles the JMX server
 * calls to attributes and operations by calling through the delegation object.
 * 
 * @author graywatson
 */
public class ReflectionMbean implements DynamicMBean {

	private final Object target;
	private final String description;
	private final Map<String, AttributeMethodInfo> attributeMethodMap = new HashMap<String, AttributeMethodInfo>();
	private final Map<NameParams, Method> operationMethodMap = new HashMap<NameParams, Method>();
	private final Map<String, AttributeFieldInfo> attributeFieldMap = new HashMap<String, AttributeFieldInfo>();
	private final MBeanInfo mbeanInfo;

	/**
	 * Create a mbean associated with a target object that must have a {@link JmxResource} annotation.
	 */
	public ReflectionMbean(Object target, String description) {
		this(target, description, null, null, null, false);
	}

	/**
	 * Create a mbean associated with a wrapped object that exposes all public fields and methods.
	 */
	public ReflectionMbean(PublishAllBeanWrapper wrapper) {
		this(wrapper.getTarget(), null, wrapper.getAttributeFieldInfos(), wrapper.getAttributeMethodInfos(),
				wrapper.getOperationInfos(), true);
	}

	/**
	 * Create a mbean associated with a target object with user provided attribute and operation information.
	 */
	public ReflectionMbean(Object target, String description, JmxAttributeFieldInfo[] attributeFieldInfos,
			JmxAttributeMethodInfo[] attributeMethodInfos, JmxOperationInfo[] operationInfos, boolean ignoreErrors) {
		this.target = target;
		this.description = preprocessDescription(target, description);
		this.mbeanInfo = buildMbeanInfo(attributeFieldInfos, attributeMethodInfos, operationInfos, ignoreErrors);
	}

	public MBeanInfo getMBeanInfo() {
		return mbeanInfo;
	}

	public Object getAttribute(String attributeName) throws AttributeNotFoundException, ReflectionException {
		AttributeMethodInfo methodInfo = attributeMethodMap.get(attributeName);
		if (methodInfo == null) {
			AttributeFieldInfo fieldInfo = attributeFieldMap.get(attributeName);
			if (fieldInfo == null || !fieldInfo.isGetter) {
				throwUnknownAttributeException(attributeName);
			}
			try {
				// get the value by using reflection on the Field
				return fieldInfo.field.get(target);
			} catch (Exception e) {
				throw new ReflectionException(e, "Invoking getter attribute on field " + fieldInfo.field.getName()
						+ " on " + target.getClass() + " threw exception");
			}
		} else {
			if (methodInfo.getterMethod == null) {
				throwUnknownAttributeException(attributeName);
			}
			try {
				// get the value by calling the method
				return methodInfo.getterMethod.invoke(target);
			} catch (Exception e) {
				throw new ReflectionException(e, "Invoking getter attribute method "
						+ methodInfo.getterMethod.getName() + " on " + target.getClass() + " threw exception");
			}
		}
	}

	public AttributeList getAttributes(String[] attributeNames) {
		AttributeList returnList = new AttributeList();
		for (String name : attributeNames) {
			try {
				returnList.add(new Attribute(name, getAttribute(name)));
			} catch (Exception e) {
				returnList.add(new Attribute(name, "Getting attribute threw: " + e.getMessage()));
			}
		}
		return returnList;
	}

	public void setAttribute(Attribute attribute) throws AttributeNotFoundException, ReflectionException {
		AttributeMethodInfo methodInfo = attributeMethodMap.get(attribute.getName());
		if (methodInfo == null) {
			AttributeFieldInfo fieldInfo = attributeFieldMap.get(attribute.getName());
			if (fieldInfo == null || !fieldInfo.isSetter) {
				throwUnknownAttributeException(attribute.getName());
			}
			try {
				fieldInfo.field.set(target, attribute.getValue());
			} catch (Exception e) {
				throw new ReflectionException(e, "Invoking setter attribute on field " + fieldInfo.field.getName()
						+ " on " + target.getClass() + " threw exception");
			}
		} else {
			if (methodInfo.setterMethod == null) {
				throwUnknownAttributeException(attribute.getName());
			}
			try {
				methodInfo.setterMethod.invoke(target, attribute.getValue());
			} catch (Exception e) {
				throw new ReflectionException(e, "Invoking setter attribute method "
						+ methodInfo.setterMethod.getName() + " on " + target.getClass() + " threw exception");
			}
		}
	}

	public AttributeList setAttributes(AttributeList attributes) {
		AttributeList returnList = new AttributeList(attributes.size());
		for (Attribute attribute : attributes.asList()) {
			String name = attribute.getName();
			try {
				setAttribute(attribute);
				returnList.add(new Attribute(name, getAttribute(name)));
			} catch (Exception e) {
				returnList.add(new Attribute(name, e.getMessage()));
			}
		}
		return returnList;
	}

	public Object invoke(String actionName, Object[] params, String[] signatureTypes) throws MBeanException,
			ReflectionException {
		Method method = operationMethodMap.get(new NameParams(actionName, signatureTypes));
		if (method == null) {
			throw new MBeanException(new IllegalArgumentException("Unknown action '" + actionName
					+ "' with parameter types " + Arrays.toString(signatureTypes)));
		}
		try {
			return method.invoke(target, params);
		} catch (Exception e) {
			throw new ReflectionException(e, "Invoking operation method " + method.getName() + " on "
					+ target.getClass() + " threw exception");
		}
	}

	private static String preprocessDescription(Object target, String description) {
		if (description == null) {
			return "Information about " + target.getClass();
		} else {
			return description;
		}
	}

	/**
	 * Build our JMX information object by using reflection.
	 */
	private MBeanInfo buildMbeanInfo(JmxAttributeFieldInfo[] attributeFieldInfos,
			JmxAttributeMethodInfo[] attributeMethodInfos, JmxOperationInfo[] operationInfos, boolean ignoreErrors) {

		// NOTE: setup the maps that track previous class configuration
		Map<String, JmxAttributeFieldInfo> attributeFieldInfoMap = null;
		if (attributeFieldInfos != null) {
			attributeFieldInfoMap = new HashMap<String, JmxAttributeFieldInfo>();
			for (JmxAttributeFieldInfo info : attributeFieldInfos) {
				attributeFieldInfoMap.put(info.getFieldName(), info);
			}
		}
		Map<String, JmxAttributeMethodInfo> attributeMethodInfoMap = null;
		if (attributeMethodInfos != null) {
			attributeMethodInfoMap = new HashMap<String, JmxAttributeMethodInfo>();
			for (JmxAttributeMethodInfo info : attributeMethodInfos) {
				attributeMethodInfoMap.put(info.getMethodName(), info);
			}
		}
		Map<String, JmxOperationInfo> attributeOperationInfoMap = null;
		if (operationInfos != null) {
			attributeOperationInfoMap = new HashMap<String, JmxOperationInfo>();
			for (JmxOperationInfo info : operationInfos) {
				attributeOperationInfoMap.put(info.getMethodName(), info);
			}
		}

		Set<String> attributeNameSet = new HashSet<String>();
		List<MBeanAttributeInfo> attributes = new ArrayList<MBeanAttributeInfo>();
		// NOTE: methods override fields so subclasses can stop exposing of fields
		discoverAttributeMethods(attributeMethodInfoMap, attributeNameSet, attributes, ignoreErrors);
		discoverAttributeFields(attributeFieldInfoMap, attributeNameSet, attributes);
		List<MBeanOperationInfo> operations = discoverOperations(attributeOperationInfoMap);

		return new MBeanInfo(target.getClass().getName(), description,
				attributes.toArray(new MBeanAttributeInfo[attributes.size()]), null,
				operations.toArray(new MBeanOperationInfo[operations.size()]), null);
	}

	/**
	 * Find attribute methods from our object that will be exposed via JMX.
	 */
	private void discoverAttributeMethods(Map<String, JmxAttributeMethodInfo> attributeMethodInfoMap,
			Set<String> attributeNameSet, List<MBeanAttributeInfo> attributes, boolean ignoreErrors) {
		for (Class<?> clazz = target.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
			discoverAttributeMethods(attributeMethodInfoMap, attributeNameSet, attributes, ignoreErrors, clazz);
		}
		/*
		 * we have to go back and post process the attribute-method-map because the getter and setter methods change the
		 * method-info multiple times.
		 */
		for (AttributeMethodInfo methodInfo : attributeMethodMap.values()) {
			attributes.add(new MBeanAttributeInfo(methodInfo.varName, methodInfo.type.getName(),
					methodInfo.description, (methodInfo.getterMethod != null), (methodInfo.setterMethod != null),
					methodInfo.isIs()));
			attributeNameSet.add(methodInfo.varName);
		}
	}

	private void discoverAttributeMethods(Map<String, JmxAttributeMethodInfo> attributeMethodInfoMap,
			Set<String> attributeNameSet, List<MBeanAttributeInfo> attributes, boolean ignoreErrors, Class<?> clazz) {
		for (Method method : clazz.getMethods()) {
			JmxAttributeMethod jmxAttribute = method.getAnnotation(JmxAttributeMethod.class);
			JmxAttributeMethodInfo attributeMethodInfo = null;
			if (jmxAttribute == null) {
				// skip it if no annotation
				if (attributeMethodInfoMap != null) {
					// was this attribute method already configured?
					attributeMethodInfo = attributeMethodInfoMap.get(method.getName());
				}
				if (attributeMethodInfo == null) {
					continue;
				}
			} else {
				attributeMethodInfo = new JmxAttributeMethodInfo(method.getName(), jmxAttribute);
				jmxAttribute = null;
			}

			try {
				discoverAttributeMethod(attributeNameSet, method, attributeMethodInfo);
			} catch (IllegalArgumentException iae) {
				if (!ignoreErrors) {
					throw iae;
				}
			}
		}
	}

	private void discoverAttributeMethod(Set<String> attributeNameSet, Method method,
			JmxAttributeMethodInfo attributeMethodInfo) {
		String methodName = method.getName();
		boolean isIs;
		if (methodName.startsWith("is")) {
			if (method.getReturnType() != boolean.class && method.getReturnType() != Boolean.class) {
				throw new IllegalArgumentException("Method '" + method
						+ "' starts with 'is' but does not return a boolean or Boolean class");
			}
			isIs = true;
		} else {
			isIs = false;
		}
		String varName = buildMethodSuffix(method, methodName, isIs);
		if (attributeNameSet.contains(varName)) {
			return;
		}
		AttributeMethodInfo methodInfo = attributeMethodMap.get(varName);
		if (isIs || methodName.startsWith("get")) {
			if (method.getParameterTypes().length != 0) {
				throw new IllegalArgumentException("Method '" + method + "' starts with 'get' but has arguments");
			}
			if (method.getReturnType() == void.class) {
				throw new IllegalArgumentException("Method '" + method
						+ "' starts with 'get' but does not return anything");
			}
			if (methodInfo == null) {
				attributeMethodMap.put(varName, new AttributeMethodInfo(varName, attributeMethodInfo.getDescription(),
						method, null));
			} else {
				// setter must have already started our method-info, add the getter to it
				methodInfo.getterMethod = method;
			}
		} else if (methodName.startsWith("set")) {
			if (method.getParameterTypes().length != 1) {
				throw new IllegalArgumentException("Method '" + method
						+ "' starts with 'set' but does not have 1 argument");
			}
			if (method.getReturnType() != void.class) {
				throw new IllegalArgumentException("Method '" + method + "' starts with 'set' but does not return void");
			}
			if (methodInfo == null) {
				attributeMethodMap.put(varName, new AttributeMethodInfo(varName, attributeMethodInfo.getDescription(),
						null, method));
			} else {
				// getter must have already started our method-info, add the setter to it
				methodInfo.setterMethod = method;
			}
		} else {
			throw new IllegalArgumentException("Method '" + method
					+ "' is marked as an attribute but does not start with 'get' or 'set'");
		}
	}

	/**
	 * Find attribute methods from our object that will be exposed via JMX.
	 */
	private void discoverAttributeFields(Map<String, JmxAttributeFieldInfo> attributeFieldInfoMap,
			Set<String> attributeNameSet, List<MBeanAttributeInfo> attributes) {
		for (Class<?> clazz = target.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
			discoverAttributeFields(attributeFieldInfoMap, attributeNameSet, attributes, clazz);
		}
	}

	private void discoverAttributeFields(Map<String, JmxAttributeFieldInfo> attributeFieldInfoMap,
			Set<String> attributeNameSet, List<MBeanAttributeInfo> attributes, Class<?> clazz) {
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			String fieldName = field.getName();
			if (attributeNameSet.contains(fieldName)) {
				continue;
			}
			JmxAttributeField attributeField = field.getAnnotation(JmxAttributeField.class);
			JmxAttributeFieldInfo attributeFieldInfo = null;
			if (attributeField == null) {
				if (attributeFieldInfoMap != null) {
					// was this attribute field already configured?
					attributeFieldInfo = attributeFieldInfoMap.get(fieldName);
				}
				if (attributeFieldInfo == null) {
					continue;
				}
			} else {
				attributeFieldInfo = new JmxAttributeFieldInfo(fieldName, attributeField);
				attributeField = null;
			}

			if (!field.isAccessible()) {
				field.setAccessible(true);
			}
			attributeFieldMap.put(fieldName, new AttributeFieldInfo(field, attributeFieldInfo.isReadible(),
					attributeFieldInfo.isWritable()));

			String description = attributeFieldInfo.getDescription();
			if (isEmpty(description)) {
				description = fieldName + " attribute";
			}

			boolean isIs;
			if (fieldName.startsWith("is") && (field.getType() == boolean.class || field.getType() == Boolean.class)) {
				isIs = true;
			} else {
				isIs = false;
			}
			attributes.add(new MBeanAttributeInfo(fieldName, field.getType().getName(), description,
					attributeFieldInfo.isReadible(), attributeFieldInfo.isWritable(), isIs));
			attributeNameSet.add(fieldName);
		}
	}

	/**
	 * Find operation methods from our object that will be exposed via JMX.
	 */
	private List<MBeanOperationInfo> discoverOperations(Map<String, JmxOperationInfo> attributeOperationInfoMap) {
		Set<MethodSignature> methodSignatureSet = new HashSet<MethodSignature>();
		List<MBeanOperationInfo> operations = new ArrayList<MBeanOperationInfo>(operationMethodMap.size());
		for (Class<?> clazz = target.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
			discoverOperations(attributeOperationInfoMap, methodSignatureSet, operations, clazz);
		}
		return operations;
	}

	private void discoverOperations(Map<String, JmxOperationInfo> attributeOperationInfoMap,
			Set<MethodSignature> methodSignatureSet, List<MBeanOperationInfo> operations, Class<?> clazz) {
		for (Method method : clazz.getMethods()) {
			MethodSignature methodSignature = new MethodSignature(method);
			if (methodSignatureSet.contains(methodSignature)) {
				continue;
			}
			String methodName = method.getName();
			JmxOperation jmxOperation = method.getAnnotation(JmxOperation.class);
			JmxOperationInfo operationInfo = null;
			if (jmxOperation == null) {
				if (attributeOperationInfoMap != null) {
					// was this operation already configured?
					operationInfo = attributeOperationInfoMap.get(methodName);
				}
				if (operationInfo == null) {
					continue;
				}
			} else {
				operationInfo = new JmxOperationInfo(methodName, jmxOperation);
				jmxOperation = null;
			}
			if (methodName.startsWith("get") || methodName.startsWith("is") || methodName.startsWith("set")) {
				throw new IllegalArgumentException("Operation method " + method
						+ " cannot start with 'get', 'is', or 'set'.  Did you use the wrong annotation?");
			}
			Class<?>[] types = method.getParameterTypes();
			String[] stringTypes = new String[types.length];
			for (int i = 0; i < types.length; i++) {
				stringTypes[i] = types[i].getName();
			}
			NameParams nameParams = new NameParams(methodName, stringTypes);
			MBeanParameterInfo[] parameterInfos = buildOperationParameterInfo(method, operationInfo);
			operationMethodMap.put(nameParams, method);

			String description = operationInfo.getDescription();
			if (isEmpty(description)) {
				description = methodName + " operation";
			}

			operations.add(new MBeanOperationInfo(methodName, description, parameterInfos, method.getReturnType()
					.getName(), operationInfo.getAction().getActionValue()));
			methodSignatureSet.add(methodSignature);
		}
	}

	/**
	 * Build our parameter information for an operation.
	 */
	private MBeanParameterInfo[] buildOperationParameterInfo(Method method, JmxOperationInfo operationInfo) {
		Class<?>[] types = method.getParameterTypes();
		MBeanParameterInfo[] parameterInfos = new MBeanParameterInfo[types.length];
		String[] parameterNames = operationInfo.getParameterNames();
		String[] parameterDescriptions = operationInfo.getParameterDescriptions();
		for (int i = 0; i < types.length; i++) {
			String parameterName;
			if (parameterNames == null || i >= parameterNames.length) {
				parameterName = "p" + (i + 1);
			} else {
				parameterName = parameterNames[i];
			}
			String typeName = types[i].getName();
			String description;
			if (parameterDescriptions == null || i >= parameterDescriptions.length) {
				description = "parameter #" + (i + 1) + " of type: " + typeName;
			} else {
				description = parameterDescriptions[i];
			}
			parameterInfos[i] = new MBeanParameterInfo(parameterName, typeName, description);
		}
		return parameterInfos;
	}

	private String buildMethodSuffix(Method method, String methodName, boolean isIs) {
		if (isIs) {
			if (methodName.length() < 3) {
				throw new IllegalArgumentException("Method '" + methodName + "' has a name that is too short");
			}
			return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
		} else {
			if (methodName.length() < 4) {
				throw new IllegalArgumentException("Method '" + methodName + "' has a name that is too short");
			}
			return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
		}
	}

	/**
	 * We do this to standardize our exceptions around unknown attributes.
	 */
	private void throwUnknownAttributeException(String attributeName) throws AttributeNotFoundException {
		throw new AttributeNotFoundException("Unknown attribute " + attributeName);
	}

	private static boolean isEmpty(String string) {
		return string == null || string.trim().length() == 0;
	}

	/**
	 * Key class for our hashmap to find matching methods based on name and parameter list.
	 */
	private static class NameParams {
		String name;
		String[] paramTypes;
		public NameParams(String name, String[] paramTypes) {
			this.name = name;
			this.paramTypes = paramTypes;
		}

		@Override
		public int hashCode() {
			int hashCode = 31 * (31 + name.hashCode());
			if (paramTypes != null) {
				hashCode += Arrays.hashCode(paramTypes);
			}
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			NameParams other = (NameParams) obj;
			if (!this.name.equals(other.name)) {
				return false;
			}
			return Arrays.equals(this.paramTypes, other.paramTypes);
		}
	}

	/**
	 * Information about attribute methods.
	 */
	private static class AttributeMethodInfo {
		final String varName;
		final String description;
		Method getterMethod;
		Method setterMethod;
		final Class<?> type;

		public AttributeMethodInfo(String varName, String description, Method getterMethod, Method setterMethod) {
			this.varName = varName;
			if (isEmpty(description)) {
				this.description = varName + " attribute";
			} else {
				this.description = description;
			}
			this.getterMethod = getterMethod;
			this.setterMethod = setterMethod;
			if (getterMethod == null) {
				type = setterMethod.getParameterTypes()[0];
			} else {
				type = getterMethod.getReturnType();
			}
		}

		public boolean isIs() {
			if (getterMethod != null && getterMethod.getName().startsWith("is")
					&& (type == boolean.class || type == Boolean.class)) {
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Information about attribute fields
	 */
	private static class AttributeFieldInfo {

		final Field field;
		final boolean isGetter;
		final boolean isSetter;

		public AttributeFieldInfo(Field field, boolean isGetter, boolean isSetter) {
			this.field = field;
			this.isGetter = isGetter;
			this.isSetter = isSetter;
		}
	}

	/**
	 * Method signature that matches the name and parameter-types. We don't care about return type because Java doesn't
	 * match methods using it.
	 */
	private static class MethodSignature {

		final String name;
		final Class<?>[] parameterTypes;

		private MethodSignature(Method method) {
			this.name = method.getName();
			this.parameterTypes = method.getParameterTypes();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = prime + name.hashCode();
			result = prime * result + Arrays.hashCode(parameterTypes);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			MethodSignature other = (MethodSignature) obj;
			return name.equals(other.name) && Arrays.equals(parameterTypes, other.parameterTypes);
		}
	}
}
