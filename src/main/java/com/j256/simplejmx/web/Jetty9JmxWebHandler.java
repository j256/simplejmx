package com.j256.simplejmx.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * Jetty 9 web handler that implements the {@link JmxWebPublisher} functions to allow the {@link JmxWebService} to
 * display web simple JMX web pages.
 * 
 * @author graywatson
 */
public class Jetty9JmxWebHandler extends AbstractHandler implements JmxWebPublisher {

	private JmxWebHandler webHandler;
	private HttpServletRequest request;
	private HttpServletResponse response;

	/** prefix to all of the web requests if we are working in a web app with other requests */
	private String pathPrefix;

	public Jetty9JmxWebHandler() {
		// for spring
	}

	public Jetty9JmxWebHandler(JmxWebHandler webHandler, String pathPrefix) {
		this.webHandler = webHandler;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		if (webHandler == null) {
			response.sendError(HttpStatus.NOT_FOUND_404);
			return;
		}

		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()));) {
			this.request = request;
			this.response = response;
			webHandler.handle(this, writer, pathPrefix);
		}
		baseRequest.setHandled(true);
	}

	@Override
	public String getRequestPathInfo() {
		return request.getPathInfo();
	}

	@Override
	public String getRequestQueryParameter(String paramName) {
		return request.getParameter(paramName);
	}

	@Override
	public void setResponseContentType(String contentType) {
		response.setContentType(contentType);
	}

	@Override
	public void setResponseStatusCode(int httpStatusCode) {
		response.setStatus(httpStatusCode);
	}

	@Override
	public void sendResponseRedirect(String location) throws IOException {
		response.sendRedirect(location);
	}

	public void setWebHandler(JmxWebHandler webHandler) {
		this.webHandler = webHandler;
	}
}
