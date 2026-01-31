package com.j256.simplejmx.web;

import java.io.IOException;

/**
 * Definition of functionality needed by the webserver (jetty or otherwise) to display JMX information as a web page.
 * 
 * @author graywatson
 */
public interface JmxWebPublisher {

	/**
	 * Return the request path-info.
	 */
	public String getRequestPathInfo();

	/**
	 * Return the request query parameter by name.
	 */
	public String getRequestQueryParameter(String paramName);

	/**
	 * Set the response content type.
	 */
	public void setResponseContentType(String contentType) throws IOException;

	/**
	 * Set the response status code.
	 */
	public void setResponseStatusCode(int httpStatusCode) throws IOException;

	/**
	 * Send a redirect to a new url.
	 */
	public void sendResponseRedirect(String location) throws IOException;
}
