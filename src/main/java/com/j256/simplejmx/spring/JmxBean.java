package com.j256.simplejmx.spring;

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
 * &lt;bean id="jmxServerJmx" class="com.j256.simplejmx.spring.JmxBean"&gt;
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
 *    &lt;property name="target" ref="jmxServer" /&gt;
 * &lt;/bean&gt;
 * </pre>
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

	/**
	 * Required resource information which helps to make the ObjectName for the bean that we are exposing via JMX.
	 */
	@Required
	public void setJmxResourceInfo(JmxResourceInfo jmxResourceInfo) {
		this.jmxResourceInfo = jmxResourceInfo;
	}

	public JmxAttributeFieldInfo[] getAttributeFieldInfos() {
		return attributeFieldInfos;
	}

	/**
	 * Optional setting which defines the fields to be exposed as attribute via JMX.
	 */
	public void setAttributeFieldInfos(JmxAttributeFieldInfo[] attributeFieldInfos) {
		this.attributeFieldInfos = attributeFieldInfos;
	}

	public JmxAttributeMethodInfo[] getAttributeMethodInfos() {
		return attributeMethodInfos;
	}

	/**
	 * Optional setting which defines the methods (get/is/set...) to be exposed as attributes via JMX.
	 */
	public void setAttributeMethodInfos(JmxAttributeMethodInfo[] attributeMethodInfos) {
		this.attributeMethodInfos = attributeMethodInfos;
	}

	public JmxOperationInfo[] getOperationInfos() {
		return operationInfos;
	}

	/**
	 * Optional setting which defines the additional methods (not get/is/set...) to be exposed as operations via JMX.
	 */
	public void setOperationInfos(JmxOperationInfo[] operationInfos) {
		this.operationInfos = operationInfos;
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
