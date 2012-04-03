package com.j256.simplejmx.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This identify which methods are setters and getters. This is added to methods that are in the form setXxx() or
 * getXxx(). The "Xxx" should match precisely to line up the get and set JMX features. For example, if you are getting
 * and setting the "foo" field then it should be "getFoo()" and "setFoo()". Similar to Spring's ManagedAttribute.
 * 
 * <p>
 * In addition, the "getXxx()" method must return void and must take a single argument. The "setXxx()" method must not
 * return void and must have no arguments.
 * </p>
 * 
 * <p>
 * 
 * <pre>
 * &#64;JmxAttribute(description = "Number of times our cache was hit")
 * public int getCacheHitCount() {
 * </pre>
 * 
 * </p>
 * 
 * @author graywatson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface JmxAttribute {

	/**
	 * Description of the attribute for jconsole. Default is something like: "someField attribute".
	 */
	public String description() default "";
}
