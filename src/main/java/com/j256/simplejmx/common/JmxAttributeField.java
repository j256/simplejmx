package com.j256.simplejmx.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This identifies which fields you want to expose via JMX via reflection. If the field is not public then it will try
 * to open the accessibility on the field. Instead of annotating the fields, you can annotate your {@code getXxx()},
 * {@code setXxx()}, and {@code isXxx()} methods with {@link JmxAttributeMethod}.
 * 
 * <p>
 * 
 * <pre>
 * &#064;JmxAttributeField(description = &quot;Number of times our cache was hit&quot;)
 * private int cacheHitCount;
 * </pre>
 * 
 * </p>
 * 
 * @author graywatson
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface JmxAttributeField {

	/**
	 * Description of the attribute for jconsole. Default is something like: "someField attribute".
	 */
	public String description() default "";

	/**
	 * Set to false if the field should not be read through JMX. Default is true.
	 */
	public boolean isReadible() default true;

	/**
	 * Set to true if the field can be written by JMX. Default is false.
	 */
	public boolean isWritable() default false;
}
