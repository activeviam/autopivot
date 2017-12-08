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
package com.av.autopivot.server;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.annotations.ClassInheritanceHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.web.WebApplicationInitializer;

import com.av.autopivot.spring.AutoPivotWebAppInitializer;

/**
 * 
 * Launch the AutoPivot application from an
 * embedded Jetty Server.
 * 
 * @author ActiveViam
 *
 */
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
			final Set<String> set = Collections.singleton(AutoPivotWebAppInitializer.class.getName());
			final Map<String, Set<String>> map = new ClassInheritanceMap();
			map.put(WebApplicationInitializer.class.getName(), set);
			context.setAttribute(CLASS_INHERITANCE_MAP, map);
			_classInheritanceHandler = new ClassInheritanceHandler(map);
		}

	}

}