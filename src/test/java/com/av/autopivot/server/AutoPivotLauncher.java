/*
 * (C) ActiveViam 2017
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.autopivot.server;

import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.annotations.ClassInheritanceHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.web.WebApplicationInitializer;

import com.av.autopivot.spring.AutoPivotWebAppInitializer;

public class AutoPivotLauncher {

	/** Root of the web application files, defined relatively to the project root */
	protected static final String WEBAPP = "src/main/webapp";
	
	/** Server default port (9090) */
	public static final int DEFAULT_PORT = 9090;
	
	public static void main(String[] args) throws Exception {
		
		Server server = createServer(DEFAULT_PORT);
        server.start();
        server.join();

	}

	public static Server createServer(int port) {
		
		WebAppContext context = new WebAppContext();
		context.setContextPath("/");
		context.setResourceBase(WEBAPP);
		context.setConfigurations(new Configuration[] { new JettyAnnotationConfiguration() });
		
		// Create server and configure it
		final Server server = new Server(port);
		server.setHandler(context);

		return server;
	}

	

	/**
	 *
	 * When the Jetty servlet-3.0 annotation parser is used, it only
	 * scans the jar files in the classpath. This small override will
	 * allow Jetty to also see the Sandbox web application initializer
	 * in the classpath of the IDE (Eclipse for instance).
	 *
	 * @author ActiveViam
	 *
	 */
	public static class JettyAnnotationConfiguration extends AnnotationConfiguration {

		@Override
		public void preConfigure(WebAppContext context) throws Exception {
			ConcurrentHashSet<String> set = new ConcurrentHashSet<>();
			ConcurrentHashMap<String, ConcurrentHashSet<String>> map = new ClassInheritanceMap();
			set.add(AutoPivotWebAppInitializer.class.getName());
			map.put(WebApplicationInitializer.class.getName(), set);
			context.setAttribute(CLASS_INHERITANCE_MAP, map);
			_classInheritanceHandler = new ClassInheritanceHandler(map);
		}

	}

}