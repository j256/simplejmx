package com.j256.simplejmx.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.management.MBeanOperationInfo;

/**
 * Similar to Spring's ManagedOperation to show which methods are operations. This gets set on a method that is not
 * named "get..." or "set...". The method can either return void or return an object. It is recommended that the method
 * return a simple object that will be for sure in jconsole's classpath and will not throw an unknown exception class
 * either.
 * 
 * @author graywatson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface JmxOperation {

	/**
	 * Description of the attribute for jconsole. Default is something like "someMethod operation".
	 */
	public String description() default "";

	/**
	 * Array of strings which gives the name each of the parameters to the operation method. This array should be the
	 * same length as the parameterDescriptions array. Default is something like "p0".
	 */
	public String[] parameterNames() default {};

	/**
	 * Array of strings which describes each of the parameters to the operation method. This array should be the same
	 * length as the parameterNames array. If not specified then it will create one with the parameter number and type.
	 */
	public String[] parameterDescriptions() default {};

	/**
	 * This is used by the JMX system to describe what sort of work is being done in this operation. Current choices
	 * are: INFO (read-like that returns information), ACTION (write-like that modified the bean in some way,
	 * ACTION_INFO (both read-like and write-like), and UNKNOWN (the default which is no action specified).
	 */
	public int action() default MBeanOperationInfo.UNKNOWN;
}
