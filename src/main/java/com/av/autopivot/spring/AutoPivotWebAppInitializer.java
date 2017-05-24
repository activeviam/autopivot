/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.av.autopivot.spring;

import static org.springframework.security.config.BeanIds.SPRING_SECURITY_FILTER_CHAIN;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

/**
 *
 * Initializer of the Web Application.
 * <p>
 * When bootstrapped by a servlet-3.0 application container, the Spring
 * Framework will automatically create an instance of this class and call its
 * startup callback method.
 * <p>
 * The content of this class replaces the old web.xml file in previous versions
 * of the servlet specification.
 *
 * @author ActiveViam
 *
 */
public class AutoPivotWebAppInitializer implements WebApplicationInitializer {

	/**
	 * Configure the given {@link ServletContext} with any servlets, filters, listeners
	 * context-params and attributes necessary for initializing this web application. See examples
	 * {@linkplain WebApplicationInitializer above}.
	 *
	 * @param servletContext the {@code ServletContext} to initialize
	 * @throws ServletException if any call against the given {@code ServletContext} throws a {@code ServletException}
	 */
	public void onStartup(ServletContext servletContext) throws ServletException {
		// Spring Context Bootstrapping
		AnnotationConfigWebApplicationContext rootAppContext = new AnnotationConfigWebApplicationContext();
		rootAppContext.register(AutoPivotConfig.class);
		servletContext.addListener(new ContextLoaderListener(rootAppContext));

		// Set the session cookie name. Must be done when there are several servers (AP,
		// Content server, ActiveMonitor) with the same URL but running on different ports.
		// Cookies ignore the port (See RFC 6265).
		CookieUtil.configure(servletContext.getSessionCookieConfig(), SecurityConfig.COOKIE_NAME);

		// The main servlet/the central dispatcher
		final DispatcherServlet servlet = new DispatcherServlet(rootAppContext);
		servlet.setDispatchOptionsRequest(true);
		Dynamic dispatcher = servletContext.addServlet("springDispatcherServlet", servlet);
		dispatcher.addMapping("/*");
		dispatcher.setLoadOnStartup(1);

		// Spring Security Filter
		final FilterRegistration.Dynamic springSecurity = servletContext.addFilter(SPRING_SECURITY_FILTER_CHAIN, new DelegatingFilterProxy());
		springSecurity.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");

	}

}
