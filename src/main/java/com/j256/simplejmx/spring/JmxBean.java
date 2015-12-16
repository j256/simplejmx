package com.j256.simplejmx.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Required;

import com.j256.simplejmx.common.JmxAttributeFieldInfo;
import com.j256.simplejmx.common.JmxAttributeMethodInfo;
import com.j256.simplejmx.common.JmxOperationInfo;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.common.JmxResourceInfo;
import com.j256.simplejmx.common.JmxSelfNaming;

/**
 * With this bean, which is auto-detect by the {@link BeanPublisher}, you can configure in Spring a JMX bean for an
 * object that does not use the {@link JmxResource} annotation or {@link JmxSelfNaming} interface.
 * 
 * <p>
 * For example:
 * </p>
 * 
 * <pre>
 * &lt;!-- some random bean defined in your spring files --&gt;
 * &lt;bean id="someBean" class="your.domain.SomeBean"&gt;
 *    ...
 * &lt;/bean&gt;
 * 
 * &lt;!-- publish information about that bean via JMX --&gt;
 * &lt;bean id="someBeanJmx" class="com.j256.simplejmx.spring.JmxBean"&gt;
 *    &lt;!-- helps build the ObjectName --&gt;
 *    &lt;property name="jmxResourceInfo"&gt;
 *       &lt;bean class="com.j256.simplejmx.common.JmxResourceInfo"&gt;
 *          &lt;property name="jmxDomainName" value="your.domain" /&gt;
 *          &lt;property name="jmxBeanName" value="SomeBean" /&gt;
 *       &lt;/bean&gt;
 *    &lt;/property&gt;
 *    &lt;!-- defines the fields that are exposed for JMX --&gt;
 *    &lt;property name="attributeFieldInfos"&gt;
 *       &lt;array&gt;
 *          &lt;bean class="com.j256.simplejmx.common.JmxAttributeFieldInfo"&gt;
 *             &lt;property name="name" value="someCounter" /&gt;
 *          &lt;/bean&gt;
 *       &lt;/array&gt;
 *    &lt;/property&gt;
 *    &lt;!-- defines the get/is/set methods exposed --&gt;
 *    &lt;property name="attributeMethodInfos"&gt;
 *       &lt;array&gt;
 *          &lt;bean class="com.j256.simplejmx.common.JmxAttributeMethodInfo"&gt;
 *             &lt;property name="methodName" value="getSomeValue" /&gt;
 *          &lt;/bean&gt;
 *       &lt;/array&gt;
 *    &lt;/property&gt;
 *    &lt;!-- defines the operations (i.e. non get/is/set) methods exposed --&gt;
 *    &lt;property name="operationInfos"&gt;
 *       &lt;array&gt;
 *          &lt;bean class="com.j256.simplejmx.common.JmxOperationInfo"&gt;
 *             &lt;property name="methodName" value="someMethod" /&gt;
 *          &lt;/bean&gt;
 *       &lt;/array&gt;
 *    &lt;/property&gt;
 *    &lt;property name="target" ref="someBean" /&gt;
 * &lt;/bean&gt;
 * </pre>
 * 
 * You can also use {@link #setAttributeFieldNames(String)}, {@link #setAttributeMethodNames(String)}, and
 * {@link #setOperationNames(String)} by injecting a comma-separate list of names.
 * 
 * <pre>
 * &lt;bean id="jmxServerJmx" class="com.j256.simplejmx.spring.JmxBean"&gt;
 *    &lt;property name="jmxResourceInfo"&gt; ... &lt;/property&gt;
 *    &lt;!-- defines the fields names that are exposed for JMX --&gt;
 *    &lt;property name="attributeFieldNames" value="someCounter,someOtherField" /&gt;
 *    &lt;property name="attributeMethodNames" value="getSomeValue" /&gt;
 *    &lt;property name="operationNames" value="methodName" /&gt;
 *    &lt;property name="target" ref="someBean" /&gt;
 * &lt;/bean&gt;
 * </pre>
 * 
 * @author graywatson
 */
public class JmxBean {

	private JmxResourceInfo jmxResourceInfo;
	private List<JmxAttributeFieldInfo> attributeFieldInfos;
	private List<JmxAttributeMethodInfo> attributeMethodInfos;
	private List<JmxOperationInfo> operationInfos;
	private Object target;

	public JmxResourceInfo getJmxResourceInfo() {
		return jmxResourceInfo;
	}

	/**
	 * Required resource information which helps to make the ObjectName for the bean that we are exposing via JMX.
	 */
	@Required
	public void setJmxResourceInfo(JmxResourceInfo jmxResourceInfo) {
		this.jmxResourceInfo = jmxResourceInfo;
	}

	public JmxAttributeFieldInfo[] getAttributeFieldInfos() {
		if (attributeFieldInfos == null) {
			return null;
		} else {
			return attributeFieldInfos.toArray(new JmxAttributeFieldInfo[attributeFieldInfos.size()]);
		}
	}

	/**
	 * Optional setting which defines the fields to be exposed as attribute via JMX. If you just need to list the field
	 * names then you should use {@link #setAttributeFieldNames(String)}.
	 */
	public void setAttributeFieldInfos(JmxAttributeFieldInfo[] attributeFieldInfos) {
		if (this.attributeFieldInfos == null) {
			this.attributeFieldInfos = Arrays.asList(attributeFieldInfos);
		} else {
			for (JmxAttributeFieldInfo attributeFieldInfo : attributeFieldInfos) {
				this.attributeFieldInfos.add(attributeFieldInfo);
			}
		}
	}

	/**
	 * Optional setting which defines a comma separated list of field names. If you want descriptions then you should
	 * use {@link #setAttributeFieldInfos(JmxAttributeFieldInfo[])}.
	 */
	public void setAttributeFieldNames(String fieldNames) {
		String[] names = fieldNames.split(",");
		if (this.attributeFieldInfos == null) {
			this.attributeFieldInfos = new ArrayList<JmxAttributeFieldInfo>();
		}
		for (int i = 0; i < names.length; i++) {
			this.attributeFieldInfos.add(new JmxAttributeFieldInfo(names[i]));
		}
	}

	public JmxAttributeMethodInfo[] getAttributeMethodInfos() {
		if (attributeMethodInfos == null) {
			return null;
		} else {
			return attributeMethodInfos.toArray(new JmxAttributeMethodInfo[attributeMethodInfos.size()]);
		}
	}

	/**
	 * Optional setting which defines the methods (get/is/set...) to be exposed as attributes via JMX. If you just need
	 * to list the method names then you should use {@link #setAttributeMethodNames(String)}.
	 */
	public void setAttributeMethodInfos(JmxAttributeMethodInfo[] attributeMethodInfos) {
		if (this.attributeMethodInfos == null) {
			this.attributeMethodInfos = Arrays.asList(attributeMethodInfos);
		} else {
			for (JmxAttributeMethodInfo attributeMethodInfo : attributeMethodInfos) {
				this.attributeMethodInfos.add(attributeMethodInfo);
			}
		}
	}

	/**
	 * Optional setting which defines a comma separated list of field names. If you want descriptions then you should
	 * use {@link #setAttributeMethodInfos(JmxAttributeMethodInfo[])}.
	 */
	public void setAttributeMethodNames(String methodNames) {
		String[] names = methodNames.split(",");
		if (this.attributeMethodInfos == null) {
			this.attributeMethodInfos = new ArrayList<JmxAttributeMethodInfo>();
		}
		for (int i = 0; i < names.length; i++) {
			this.attributeMethodInfos.add(new JmxAttributeMethodInfo(names[i]));
		}
	}

	public JmxOperationInfo[] getOperationInfos() {
		if (operationInfos == null) {
			return null;
		} else {
			return operationInfos.toArray(new JmxOperationInfo[operationInfos.size()]);
		}
	}

	/**
	 * Optional setting which defines the additional methods (not get/is/set...) to be exposed as operations via JMX.
	 */
	public void setOperationInfos(JmxOperationInfo[] operationInfos) {
		if (this.operationInfos == null) {
			this.operationInfos = Arrays.asList(operationInfos);
		} else {
			for (JmxOperationInfo opertionInfo : operationInfos) {
				this.operationInfos.add(opertionInfo);
			}
		}
	}

	/**
	 * Optional setting which defines a comma separated list of field names. If you want descriptions then you should
	 * use {@link #setOperationInfos(JmxOperationInfo[])}.
	 */
	public void setOperationNames(String operationNames) {
		String[] names = operationNames.split(",");
		if (this.operationInfos == null) {
			this.operationInfos = new ArrayList<JmxOperationInfo>();
		}
		for (int i = 0; i < names.length; i++) {
			this.operationInfos.add(new JmxOperationInfo(names[i]));
		}
	}

	public Object getTarget() {
		return target;
	}

	/**
	 * Required target object which specifies the Spring bean that we are exposing via JMX.
	 */
	@Required
	public void setTarget(Object target) {
		this.target = target;
	}
}
