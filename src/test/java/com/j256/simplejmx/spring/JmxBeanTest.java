package com.j256.simplejmx.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import com.j256.simplejmx.common.JmxAttributeFieldInfo;
import com.j256.simplejmx.common.JmxAttributeMethodInfo;
import com.j256.simplejmx.common.JmxOperationInfo;
import com.j256.simplejmx.common.JmxResourceInfo;

public class JmxBeanTest {

	@Test
	public void testCoverage() {
		JmxBean jmxBean = new JmxBean();
		JmxResourceInfo info = new JmxResourceInfo();
		assertNull(jmxBean.getJmxResourceInfo());
		jmxBean.setJmxResourceInfo(info);
		assertSame(info, jmxBean.getJmxResourceInfo());

		assertNull(jmxBean.getTarget());
		jmxBean.setTarget(this);
		assertSame(this, jmxBean.getTarget());
	}

	@Test
	public void testSetAttributeFieldInfos() {
		JmxBean jmxBean = new JmxBean();
		assertNull(jmxBean.getAttributeFieldInfos());
		jmxBean.setAttributeFieldInfos(new JmxAttributeFieldInfo[] { new JmxAttributeFieldInfo() });
		jmxBean.setAttributeFieldInfos(new JmxAttributeFieldInfo[] { new JmxAttributeFieldInfo() });
		assertNotNull(jmxBean.getAttributeFieldInfos());
	}

	@Test
	public void testSetAttributeFieldNames() {
		JmxBean jmxBean = new JmxBean();

		String first = "field1";
		String second = "field2";
		jmxBean.setAttributeFieldNames(first + "," + second);
		assertNotNull(jmxBean.getAttributeFieldInfos());
		assertEquals(2, jmxBean.getAttributeFieldInfos().length);
		assertEquals(first, jmxBean.getAttributeFieldInfos()[0].getFieldName());
		assertEquals(second, jmxBean.getAttributeFieldInfos()[1].getFieldName());
		jmxBean.setAttributeFieldNames(first + "," + second);
	}

	@Test
	public void testSetAttributeMethodInfos() {
		JmxBean jmxBean = new JmxBean();

		assertNull(jmxBean.getAttributeMethodInfos());
		jmxBean.setAttributeMethodInfos(new JmxAttributeMethodInfo[] { new JmxAttributeMethodInfo() });
		jmxBean.setAttributeMethodInfos(new JmxAttributeMethodInfo[] { new JmxAttributeMethodInfo() });
		assertNotNull(jmxBean.getAttributeMethodInfos());
	}

	@Test
	public void testSetMethodNames() {
		JmxBean jmxBean = new JmxBean();
		jmxBean = new JmxBean();

		String first = "meth1";
		String second = "meth2";
		jmxBean.setAttributeMethodNames(first + "," + second);
		assertNotNull(jmxBean.getAttributeMethodInfos());
		assertEquals(2, jmxBean.getAttributeMethodInfos().length);
		assertEquals(first, jmxBean.getAttributeMethodInfos()[0].getMethodName());
		assertEquals(second, jmxBean.getAttributeMethodInfos()[1].getMethodName());
		jmxBean.setAttributeMethodNames(first + "," + second);
	}

	@Test
	public void testSetOperationInfos() {
		JmxBean jmxBean = new JmxBean();

		assertNull(jmxBean.getOperationInfos());
		jmxBean.setOperationInfos(new JmxOperationInfo[] { new JmxOperationInfo() });
		jmxBean.setOperationInfos(new JmxOperationInfo[] { new JmxOperationInfo() });
		assertNotNull(jmxBean.getOperationInfos());
	}

	@Test
	public void testSetOperationNames() {
		JmxBean jmxBean = new JmxBean();
		jmxBean = new JmxBean();

		String first = "op1";
		String second = "op2";
		jmxBean.setOperationNames(first + "," + second);
		assertNotNull(jmxBean.getOperationInfos());
		assertEquals(2, jmxBean.getOperationInfos().length);
		assertEquals(first, jmxBean.getOperationInfos()[0].getMethodName());
		assertEquals(second, jmxBean.getOperationInfos()[1].getMethodName());
		jmxBean.setOperationNames(first + "," + second);
	}
}
