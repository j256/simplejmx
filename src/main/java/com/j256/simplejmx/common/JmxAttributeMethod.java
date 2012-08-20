package com.j256.simplejmx.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This identifies which getter and setter methods you want exposed via JMX . This is added to methods that are in the
 * form <tt>setXxx()</tt>, <tt>getXxx()</tt>, or <tt>isXxx()</tt> for the <tt>xxx</tt> field. The <tt>Xxx</tt> should
 * match precisely to line up the get and set JMX features. For example, if you are getting and setting the
 * <tt>fooBar</tt> field then it should be <tt>getFooBar()</tt> and <tt>setFooBar()</tt> .
 * <tt>isFooBar()<tt> is also allowed if <tt>foobar</tt> is a boolean or Boolean field. Instead of annotating the
 * methods, you can use the {@link JmxAttributeField} to annotate the fields you want to expose.
 * 
 * <p>
 * Notice that although the field-name is <tt>fooBar</tt> with a lowercase 'f', the method name camel-cases it and turns
 * it into <tt>getFooBar()</tt> with a capital 'F'. This class is similar to Spring's &#64;ManagedAttribute. In
 * addition, the "getXxx()" method must not return void and must have no arguments. The "setXxx()" method must return
 * void and must take a single argument.
 * <tt>isXxx()<tt> is  allowed if it returns boolean or Boolean and the method has no arguments.
 * </p>
 * 
 * <p>
 * 
 * <pre>
 * &#64;JmxAttributeMethod(description = "Number of times our cache was hit")
 * public int getCacheHitCount() {
 * </pre>
 * 
 * </p>
 * 
 * @author graywatson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface JmxAttributeMethod {

	/**
	 * Description of the attribute for jconsole. Default is something like: "someField attribute".
	 */
	public String description() default "";
}
