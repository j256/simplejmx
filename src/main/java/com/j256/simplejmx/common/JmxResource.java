package com.j256.simplejmx.common;

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
	 * JMX domain name of the object. This turns into the top-level folder inside of JMX. *
	 * <p>
	 * If the object implements {@link JmxSelfNaming} then this would be replaced by
	 * {@link JmxSelfNaming#getJmxDomainName()}. If the object doesn't implement {@link JmxSelfNaming} and this is not
	 * specified then an exception is thrown.
	 * </p>
	 */
	public String domainName() default "";

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
	 * Other strings which go before the name= line which translate into sub-folders below the domain-name that was
	 * specified above. They can either be in "name=value" format in which case they should be in alphabetic order by
	 * name. They can also just be in "value" format in which case a ## prefix will be added by the code.x
	 * 
	 * <p>
	 * The following are basically synonymous:
	 * 
	 * <pre>
	 * fieldValues = { "Database", "Connections" })
	 * fieldValues = { "00=Database", "01=Connections" })
	 * </pre>
	 * 
	 * </p>
	 * 
	 * *
	 * <p>
	 * If the object implements {@link JmxSelfNaming} then this would be replaced by
	 * {@link JmxSelfNaming#getJmxFolderNames()}. If the object doesn't implement {@link JmxSelfNaming} and this is not
	 * specified then this bean will be at the top without any sub-folders.
	 * </p>
	 */
	public String[] folderNames() default {};

	/**
	 * Description of the class for jconsole. Default is something like: "Jmx information about class-name".
	 */
	public String description() default "";
}
