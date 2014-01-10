package com.j256.simplejmx.common;

import static org.junit.Assert.*;

import org.junit.Test;

public class JmxAttributeMethodInfoTest {

	@Test
	public void testConstructor() {
		String methodName = "field";
		String desc = "fjewpofjewf";
		JmxAttributeMethodInfo info = new JmxAttributeMethodInfo(methodName, desc);
		assertEquals(methodName, info.getMethodName());
		assertEquals(desc, info.getDescription());
	}

	@Test
	public void testGetSet() {
		JmxAttributeMethodInfo info = new JmxAttributeMethodInfo();
		String methodName = "field";
		info.setMethodName(methodName);
		assertEquals(methodName, info.getMethodName());
		String desc = "fjewpofjewf";
		info.setDescription(desc);
		assertEquals(desc, info.getDescription());
	}
}
