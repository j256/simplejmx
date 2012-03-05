package com.j256.simplejmx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Similar to Spring's ManagedOperation to show which methods are operations.
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
