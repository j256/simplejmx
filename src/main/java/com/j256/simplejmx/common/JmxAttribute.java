package com.j256.simplejmx.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Similar to Spring's ManagedAttribute to show which methods are setters and getters. This can be used on methods that
 * are in the form setXxx() or getXxx(). The "Xxx" should match precisely to line up the get and set JMX features. For
 * example, if you are getting and setting the "foo" field then it should be "getFoo()" and "setFoo()".
 * 
 * <p>
 * In addition, the "getXxx()" method must return void and must take a single argument. The "setXxx()" method must not
 * return void and must have no arguments.
 * </p>
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
