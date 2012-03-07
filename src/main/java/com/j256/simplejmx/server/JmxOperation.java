package com.j256.simplejmx.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
	 * Description of the attribute for jconsole.
	 */
	public String description();
}
