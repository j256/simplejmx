package com.j256.simplejmx.spring;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.common.JmxSelfNaming;
import com.j256.simplejmx.server.JmxServer;

/**
 * Runs through and discovers any beans that need to be registered with the JmxServer. This will only compile if
 * com.springframework jar(s) are available to the application. Otherwise it will throw ClassNotFound exceptions if
 * used.
 * 
 * @author graywatson
 */
public class BeanPublisher implements InitializingBean, ApplicationContextAware {

	private ApplicationContext applicationContext;
	private JmxServer jmxServer;

	public void afterPropertiesSet() throws Exception {
		// do the annotations
		Map<String, Object> beans = applicationContext.getBeansOfType(null);
		for (Object bean : beans.values()) {
			// we handle @JmxResource annotations of JmxSelfNaming
			if (bean.getClass().isAnnotationPresent(JmxResource.class) || bean instanceof JmxSelfNaming) {
				jmxServer.register(bean);
			}
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
