package com.j256.simplejmx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Similar to Spring's ManagedAttribute to show which methods are setters and getters.
 * 
 * @author graywatson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface JmxAttribute {

	/**
	 * Description of the attribute for jconsole.
	 */
	public String description();
}
