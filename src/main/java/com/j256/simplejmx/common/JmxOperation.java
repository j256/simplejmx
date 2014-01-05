package com.j256.simplejmx.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.j256.simplejmx.common.JmxOperationInfo.OperationAction;

/**
 * This identifies which methods are operations. It is added to methods that are _not_ named "get...", "set...", or
 * "is...". The method can either return void or an object. It is recommended that the method return a simple object
 * that will be for sure in jconsole's classpath. It also should not throw an unknown exception class. This class is
 * similar to Spring's &#64;ManagedOperation.
 * 
 * <p>
 * 
 * <pre>
 * &#64;JmxOperation(description = "Reset our max/min values",
 *               parameterNames = { "minValue", "maxValue" },
 *               parameterDescriptions = { "low water mark", "high water mark" }
 * public void resetMaxMin(int minValue, int maxValue) {
 *    ...
 * </pre>
 * 
 * </p>
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
	 * Optional array of strings which gives the name of each of the method parameters. The array should be the same
	 * length as the {@link #parameterDescriptions()} array. Default is something like "p0". For example:
	 * 
	 * <p>
	 * 
	 * <pre>
	 * &#64;JmxOperation(parameterNames = { "minValue", "maxValue" },
	 *               parameterDescriptions = { "low water mark", "high water mark" }
	 * public void resetMaxMin(int minValue, int maxValue) {
	 * ...
	 * </pre>
	 * 
	 * </p>
	 */
	public String[] parameterNames() default {};

	/**
	 * Optional array of strings which describes each of the method parameters. The array should be the same length as
	 * the {@link #parameterNames()} array. If not specified then it will create one with the parameter number and type
	 * -- something like "parameter #xf0 of type: int".
	 * 
	 * <p>
	 * 
	 * <pre>
	 * &#64;JmxOperation(parameterNames = { "minValue", "maxValue" },
	 *               parameterDescriptions = { "low water mark", "high water mark" }
	 * public void resetMaxMin(int minValue, int maxValue) {
	 * ...
	 * </pre>
	 * 
	 * </p>
	 */
	public String[] parameterDescriptions() default {};

	/**
	 * This optional is used by the JMX system to describe what sort of work is being done in this operation.
	 */
	public OperationAction operationAction() default OperationAction.UNKNOWN;
}
