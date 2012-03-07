package com.j256.simplejmx.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Similar to Spring's ManagedResource to have the object specify its own ObjectName.
 * 
 * @author graywatson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface JmxResource {

	/**
	 * JMX domain name of the object. This turns into the top-level folder inside of JMX.
	 */
	public String domainName();

	/**
	 * Name of the object for the name= part of the ObjectName. This turns into the name of the JMX bean in the folder.
	 * <p>
	 * If the object implements {@link JmxSelfNaming} then this would be replaced by
	 * {@link JmxSelfNaming#getJmxObjectName()}. If the object doesn't implement {@link JmxSelfNaming} and this is not
	 * specified then the object class name is used.
	 * </p>
	 */
	public String objectName() default "";

	/**
	 * Other field=value strings which go before the name= line. Could be replaced by
	 * {@link JmxSelfNaming#getJmxFieldValues()}. These will turn into the sub-folders below the domain-name specified
	 * above. They should be in alphabetic order. I use something like:
	 * 
	 * <p>
	 * 
	 * <pre>
	 * fieldValues = { "00=Database", "01=Connections" })
	 * </pre>
	 * 
	 * </p>
	 */
	public String[] fieldValues() default {};

	/**
	 * Description on the class.
	 */
	public String description() default "";
}
