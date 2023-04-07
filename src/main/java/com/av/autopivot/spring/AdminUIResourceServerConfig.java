/*
 * (C) ActiveViam 2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.av.autopivot.spring;

import com.qfs.server.cfg.impl.ASpringResourceServerConfig;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * Spring configuration for AdminUI web application. It contains a UI to browse
 * the Content Service and another one to browse the Database.
 * <p>
 * You will need to import the AdminUI's maven artifact in your project and create an
 * {@code static/admin-ui/env.js} file.
 *
 * @implNote To change the namespace, one can override this class and call
 *           {@link #AdminUIResourceServerConfig(String)}
 * @implNote Compatible with AdminUI 5.0.3+
 * @author ActiveViam
 */
@Configuration
public class AdminUIResourceServerConfig extends ASpringResourceServerConfig {

	/** The namespace of the AdminUI web application. */
	public static final String DEFAULT_NAMESPACE = "admin/ui";

	/**
	 * Constructor.
	 * <p>
	 * The resources will be exposed on {@link #DEFAULT_NAMESPACE}.
	 */
	public AdminUIResourceServerConfig() {
		this(DEFAULT_NAMESPACE);
	}

	/**
	 * Constructor.
	 *
	 * @param namespace the default namespace of the AdminUI web application
	 */
	protected AdminUIResourceServerConfig(String namespace) {
		super("/" + namespace);
	}

	/**
	 * Gets the relative path where the UI is exposed.
	 */
	public String getNamespace() {
		return namespace;
	}

	@Override
	protected void registerRedirections(final ResourceRegistry registry) {
		super.registerRedirections(registry);
		// Redirect the calls to env*.js to the AP ones rather than AdminUI
		registry.serve(namespace + "/env*.js").addResourceLocations("classpath:/static/admin-ui/");
	}

	@Override
	public Set<String> getServedExtensions() {
		return Set.of(
				// Default HTML files
				"html",
				"js",
				"css",
				"map",
				"json",
				// Image extensions
				"png",
				"jpg",
				"gif",
				"ico",
				// Font extensions
				"eot",
				"svg",
				"ttf",
				"woff",
				"woff2");
	}

	@Override
	public Set<String> getResourceLocations() {
		// Directory where the assets are in admin-ui.jar
		return Set.of("classpath:" + "META-INF/resources/webjars/admin-ui/");
	}

}
