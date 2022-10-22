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

import com.qfs.server.cfg.impl.ASpringResourceServerConfig;
import com.qfs.util.impl.QfsArrays;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * Spring configuration for ActiveUI web application
 *
 * @author ActiveViam
 *
 */
@Configuration
public class ActiveUIResourceServerConfig extends ASpringResourceServerConfig {

	/** The namespace of the ActiveUI web application */
	public static final String NAMESPACE = "ui";

	/** Constructor */
	public ActiveUIResourceServerConfig() {
		super("/" + NAMESPACE);
	}

	@Override
	protected void registerRedirections(final ResourceRegistry registry) {
		super.registerRedirections(registry);
		// Redirect from the root to ActiveUI
		registry.redirectTo(NAMESPACE + "/index.html", "/");
	}


	/**
	 * Registers resources to serve.
	 *
	 * @param registry registry to use
	 */
	@Override
	protected void registerResources(final ResourceRegistry registry) {
		super.registerResources(registry);

		// ActiveUI web app also serves request to the root, 
		// so that the redirection from root to ActiveUI works
		registry.serve("/")
				.addResourceLocations("/", "classpath:META-INF/resources/")
				.setCacheControl(getDefaultCacheControl());
	}

	/**
	 * Gets the extensions of files to serve.
	 * @return all files extensions
	 */
	@Override
	public Set<String> getServedExtensions() {
		return QfsArrays.mutableSet(
				// Default html files
				"html", "js", "css", "map", "json",
				// Image extensions
				"png", "jpg", "gif", "ico",
				// Font extensions
				"eot", "svg", "ttf", "woff", "woff2"
		);
	}

	@Override
	public Set<String> getServedDirectories() {
		return QfsArrays.mutableSet("/");
	}
	
	@Override
	public Set<String> getResourceLocations() {
		// ActiveUI is integrated in the sandbox project thanks to Maven integration.
		// You can read more about this feature here
		// https://support.activeviam.com/documentation/activeui/4.2.0/dev/setup/maven-integration.html

		return QfsArrays.mutableSet(
				"/activeui/", // index.html, favicon.ico, etc.
				"classpath:META-INF/resources/webjars/activeui/"); // ActiveUI assets
	}

}
