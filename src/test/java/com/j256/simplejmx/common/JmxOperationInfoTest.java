package com.j256.simplejmx.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.j256.simplejmx.common.JmxOperationInfo.OperationAction;

public class JmxOperationInfoTest {

	@Test
	public void testConstructor() {
		String methodName = "field";
		String paramName = "fewopjfewf";
		String paramDesc = "ffjfjepoewopjfewf";
		OperationAction action = OperationAction.ACTION_INFO;
		String desc = "fjewpofjewf";
		JmxOperationInfo info =
				new JmxOperationInfo(methodName, new String[] { paramName }, new String[] { paramDesc }, action, desc);
		assertEquals(methodName, info.getMethodName());
		assertEquals(1, info.getParameterNames().length);
		assertEquals(paramName, info.getParameterNames()[0]);
		assertEquals(1, info.getParameterDescriptions().length);
		assertEquals(paramDesc, info.getParameterDescriptions()[0]);
		assertEquals(action, info.getAction());
		assertEquals(desc, info.getDescription());
	}

	@Test
	public void testGetSet() {
		JmxOperationInfo info = new JmxOperationInfo();
		String methodName = "field";
		info.setMethodName(methodName);
		assertEquals(methodName, info.getMethodName());
		String paramName = "fewopjfewf";
		info.setParameterNames(new String[] { paramName });
		assertEquals(1, info.getParameterNames().length);
		assertEquals(paramName, info.getParameterNames()[0]);
		String paramDesc = "ffjfjepoewopjfewf";
		info.setParameterDescriptions(new String[] { paramDesc });
		assertEquals(1, info.getParameterDescriptions().length);
		assertEquals(paramDesc, info.getParameterDescriptions()[0]);
		OperationAction action = OperationAction.ACTION_INFO;
		info.setAction(action);
		assertEquals(action, info.getAction());
		String desc = "fjewpofjewf";
		info.setDescription(desc);
		assertEquals(desc, info.getDescription());
	}
}
