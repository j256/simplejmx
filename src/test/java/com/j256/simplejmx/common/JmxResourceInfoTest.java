package com.j256.simplejmx.common;

import static org.junit.Assert.*;

import org.junit.Test;

public class JmxResourceInfoTest {

	@Test
	public void testConstructor() {
		String domain = "foo.com";
		String objName = "someName";
		String folder1 = "folder1";
		String desc = "description of someName";
		JmxResourceInfo info =
				new JmxResourceInfo(domain, objName, new JmxFolderName[] { new JmxFolderName(folder1) }, desc);
		assertEquals(domain, info.getJmxDomainName());
		assertEquals(objName, info.getJmxNameOfObject());
		assertEquals(desc, info.getJmxDescription());
		assertEquals(1, info.getJmxFolderNames().length);
		assertEquals(folder1, info.getJmxFolderNames()[0].getValue());

		String folder2 = "folder2";
		info = new JmxResourceInfo(domain, objName, new String[] { folder2 }, desc);
		assertEquals(domain, info.getJmxDomainName());
		assertEquals(objName, info.getJmxNameOfObject());
		assertEquals(desc, info.getJmxDescription());
		assertEquals(1, info.getJmxFolderNames().length);
		assertEquals(1, info.getJmxFolderNames().length);
		assertEquals(folder2, info.getJmxFolderNames()[0].getValue());
	}

	@Test
	public void testSpring() {
		JmxResourceInfo info = new JmxResourceInfo();
		String domain = "foo.com";
		info.setJmxDomainName(domain);
		assertEquals(domain, info.getJmxDomainName());
		String objName = "someName";
		info.setJmxNameOfObject(objName);
		assertEquals(objName, info.getJmxNameOfObject());
		String desc = "description of someName";
		info.setJmxDescription(desc);
		assertEquals(desc, info.getJmxDescription());

		String folder1 = "folder1";
		info.setJmxFolderNames(new JmxFolderName[] { new JmxFolderName(folder1) });
		assertEquals(1, info.getJmxFolderNames().length);
		assertEquals(folder1, info.getJmxFolderNames()[0].getValue());

		String folder2 = "folder2";
		info.setJmxFolderNameStrings(new String[] { folder2 });
		assertEquals(1, info.getJmxFolderNames().length);
		assertEquals(folder2, info.getJmxFolderNames()[0].getValue());
	}
}
