package com.j256.simplejmx.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.j256.simplejmx.server.JmxServer;

/**
 * This is used to identify an object which is going to be exported using JMX. Objects that are passed to
 * {@link JmxServer#register(Object)} must either have this annotation or must implement {@link JmxSelfNaming}. This
 * class is similar to Spring's &#64;ManagedResource.
 * 
 * @author graywatson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface JmxResource {

	/**
	 * Domain name of the object which turns into the top-level folder inside of jconsole.
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
	 * Other strings which go before the <tt>name=</tt> line which translate into sub-folders below the domain-name that
	 * was specified above. They can either be in <tt>name=value</tt> format in which case they should be in alphabetic
	 * order by name. They can also just be in <tt>value</tt> format in which case a ## prefix will be added by the
	 * code.
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
	 * <p>
	 * If the object implements {@link JmxSelfNaming} then this would be replaced by
	 * {@link JmxSelfNaming#getJmxFolderNames()}. If the object doesn't implement {@link JmxSelfNaming} and this is not
	 * specified then this bean will be at the top without any sub-folders.
	 * </p>
	 */
	public String[] folderNames() default {};

	/**
	 * Description of the class for jconsole. Default is something like: "Information about class-name".
	 */
	public String description() default "";
}
