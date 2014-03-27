package com.j256.simplejmx.spring;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.common.JmxSelfNaming;
import com.j256.simplejmx.server.JmxServer;
import com.j256.simplejmx.server.PublishAllBeanWrapper;

/**
 * Utility class designed to be used with Spring which runs through and discovers any beans that need to be registered
 * with the JmxServer. This looks for beans annotated with {@link JmxResource}, that extend {@link JmxSelfNaming}, that
 * are of type {@link JmxBean}, or are of type {@link PublishAllBeanWrapper}.
 * 
 * <p>
 * <b>NOTE:</b> This will only compile if com.springframework jar(s) are available to the application. Otherwise it will
 * throw ClassNotFound exceptions if used.
 * </p>
 * 
 * @author graywatson
 */
public class BeanPublisher implements InitializingBean, ApplicationContextAware, DisposableBean {

	private ApplicationContext applicationContext;
	private JmxServer jmxServer;
	private Set<ObjectName> registeredBeans = new HashSet<ObjectName>();

	public void afterPropertiesSet() throws Exception {
		// do the annotations
		Map<String, Object> beans = applicationContext.getBeansOfType(null);
		for (Object bean : beans.values()) {
			// we handle @JmxResource annotations or JmxSelfNaming
			ObjectName objectName = null;
			if (bean.getClass().isAnnotationPresent(JmxResource.class) || bean instanceof JmxSelfNaming) {
				objectName = jmxServer.register(bean);
			} else if (bean instanceof JmxBean) {
				JmxBean jmxBean = (JmxBean) bean;
				objectName =
						jmxServer.register(jmxBean.getTarget(), jmxBean.getJmxResourceInfo(),
								jmxBean.getAttributeFieldInfos(), jmxBean.getAttributeMethodInfos(),
								jmxBean.getOperationInfos());
			} else if (bean instanceof PublishAllBeanWrapper) {
				PublishAllBeanWrapper wrapper = (PublishAllBeanWrapper) bean;
				objectName = jmxServer.register(wrapper);
			}
			if (objectName != null) {
				registeredBeans.add(objectName);
			}
		}
	}

	public void destroy() {
		for (ObjectName objectName : registeredBeans) {
			jmxServer.unregister(objectName);
		}
	}

	@Required
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Required
	public void setJmxServer(JmxServer jmxServer) {
		this.jmxServer = jmxServer;
	}
}
