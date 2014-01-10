package com.j256.simplejmx.common;

import static org.junit.Assert.*;

import org.junit.Test;

public class JmxAttributeFieldInfoTest {

	@Test
	public void testConstructor() {
		String name = "field";
		boolean isReadable = false;
		boolean isWritable = true;
		String desc = "fjewpofjewf";
		JmxAttributeFieldInfo info = new JmxAttributeFieldInfo(name, isReadable, isWritable, desc);
		assertEquals(name, info.getName());
		assertEquals(isReadable, info.isReadible());
		assertEquals(isWritable, info.isWritable());
		assertEquals(desc, info.getDescription());
	}

	@Test
	public void testGetSet() {
		JmxAttributeFieldInfo info = new JmxAttributeFieldInfo();
		String name = "field";
		info.setName(name);
		assertEquals(name, info.getName());
		boolean isReadable = false;
		info.setReadible(isReadable);
		assertEquals(isReadable, info.isReadible());
		boolean isWritable = true;
		info.setWritable(isWritable);
		assertEquals(isWritable, info.isWritable());
		String desc = "fjewpofjewf";
		info.setDescription(desc);
		assertEquals(desc, info.getDescription());
	}
}
