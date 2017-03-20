package com.j256.simplejmx.common;

import java.io.Closeable;
import java.io.IOException;

/**
 * Some simple IO utils.
 * 
 * @author graywatson
 */
public class IoUtils {

	/**
	 * Close a closeable and hide any exceptions.
	 */
	public static void closeQuietly(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException ioe) {
				// ignore exception
			}
		}
	}
}
