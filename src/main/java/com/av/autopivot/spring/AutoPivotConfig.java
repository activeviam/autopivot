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

import static com.quartetfs.fwk.types.impl.ExtendedPluginInjector.inject;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import com.qfs.distribution.security.IDistributedSecurityManager;
import com.qfs.messenger.IDistributedMessenger;
import com.qfs.monitoring.HealthCheckAgent;
import com.qfs.pivot.content.impl.DynamicActivePivotContentServiceMBean;
import com.qfs.server.cfg.IActivePivotConfig;
import com.qfs.server.cfg.IActivePivotContentServiceConfig;
import com.qfs.server.cfg.IDatastoreConfig;
import com.qfs.server.cfg.IJwtConfig;
import com.qfs.server.cfg.impl.ActivePivotConfig;
import com.qfs.server.cfg.impl.ActivePivotRemotingServicesConfig;
import com.qfs.server.cfg.impl.ActivePivotServicesConfig;
import com.qfs.server.cfg.impl.ActivePivotWebServicesConfig;
import com.qfs.server.cfg.impl.ActivePivotXmlaServletConfig;
import com.qfs.server.cfg.impl.JwtConfig;
import com.qfs.server.cfg.impl.QfsRestServicesConfig;
import com.qfs.server.cfg.impl.StreamingMonitorConfig;
import com.quartetfs.biz.pivot.monitoring.impl.JMXEnabler;
import com.quartetfs.fwk.Registry;
import com.quartetfs.fwk.contributions.impl.ClasspathContributionProvider;

/**
 * Generic Spring configuration of the Sandbox ActivePivot server application.
 *
 * <p>
 * This is the entry point for the Spring "Java Config" of the entire application. This is
 * referenced in corresponding WebAppInitializer to bootstrap the application (as per Spring
 * framework principles).
 *
 * <p>
 * We use {@link PropertySource} annotation(s) to define some .properties file(s), whose content
 * will be loaded into the Spring {@link Environment}, allowing some externally-driven configuration
 * of the application.
 *
 * <p>
 * We use {@link Import} annotation(s) to reference additional Spring {@link Configuration} classes,
 * so that we can manage the application configuration in a modular way (split by domain/feature,
 * re-use of core config, override of core config, customized config, etc...).
 *
 * <p>
 * Spring best practices recommends not to have arguments in bean methods if possible. One should
 * rather autowire the appropriate spring configurations (and not beans directly unless necessary),
 * and use the beans from there.
 *
 * @author Quartet FS
 */
@PropertySource(value = { "classpath:autopivot.properties", "classpath:jwt.properties" })
@Configuration
@Import(
value = {
		ActivePivotConfig.class,
		DatastoreConfig.class,
		ContentServiceConfig.class,
		SourceConfig.class,
		SecurityConfig.class,
		JwtConfig.class,
		CorsFilterConfig.class,

		ActivePivotServicesConfig.class,
		ActivePivotWebServicesConfig.class,
		ActivePivotRemotingServicesConfig.class,
		ActivePivotXmlaServletConfig.class,

		// i18n support
		I18nConfig.class,
		
		// Rest services
		QfsRestServicesConfig.class,

		// Monitoring for the Streaming services
		StreamingMonitorConfig.class,
		
		// ActiveUI resource server configuration
		ActiveUIResourceServerConfig.class
})
public class AutoPivotConfig {

	/** Logger **/
	protected static Logger LOGGER = Logger.getLogger(AutoPivotConfig.class.getName());

	/** Before anything else we statically initialise the ActiveViam Registry. */
	static {
		Registry.setContributionProvider(new ClasspathContributionProvider("com.av", "com.qfs", "com.quartetfs"));
	}

	/** Spring environment, automatically wired */
	@Autowired
	protected Environment env;

	/** Datastore spring configuration */
	@Autowired
	protected IDatastoreConfig datastoreConfig;

	/** ActivePivot spring configuration */
	@Autowired
	protected IActivePivotConfig apConfig;

	/** JWT spring configuration */
	@Autowired
	protected IJwtConfig jwtConfig;

	/** ActivePivot content service spring configuration */
	@Autowired
	protected IActivePivotContentServiceConfig apCSConfig;

	/** SecurityConfig spring configuration */
	@Autowired
	protected SecurityConfig securityConfig;

	/** ActivePivot Service Config */
	@Autowired
	protected ActivePivotServicesConfig apServiceConfig;

	/**
	 *
	 * Initialize and start the ActivePivot Manager, after performing all the injections into the
	 * ActivePivot plug-ins.
	 *
	 * @see #apManagerInitPrerequisitePluginInjections()
	 * @return void
	 * @throws Exception any exception that occurred during the manager's start up
	 */
	@Bean
	public Void startManager() throws Exception {
		/* ********************************************************************** */
		/* Inject dependencies before the ActivePivot components are initialized. */
		/* ********************************************************************** */
		apManagerInitPrerequisitePluginInjections();

		/* *********************************************** */
		/* Initialize the ActivePivot Manager and start it */
		/* *********************************************** */

		apConfig.activePivotManager().init(null);
		apConfig.activePivotManager().start();

		return null;
	}

	/**
	 * Enable JMX Monitoring for the Datastore
	 *
	 * @return the {@link JMXEnabler} attached to the datastore
	 */
	@Bean
	public JMXEnabler JMXDatastoreEnabler() {
		return new JMXEnabler(datastoreConfig.datastore());
	}

	/**
	 * Enable JMX Monitoring for ActivePivot Components
	 *
	 * @return the {@link JMXEnabler} attached to the activePivotManager
	 */
	@Bean
	@DependsOn(value = "startManager")
	public JMXEnabler JMXActivePivotEnabler() {
		return new JMXEnabler(apConfig.activePivotManager());
	}

	/**
	 * Enable JMX Monitoring for the Content Service
	 *
	 * @return the {@link JMXEnabler} attached to the content service.
	 */
	@Bean
	public JMXEnabler JMXActivePivotContentServiceEnabler() {
		// to allow operations from the JMX bean
		return new JMXEnabler(
				new DynamicActivePivotContentServiceMBean(
						apCSConfig.activePivotContentService(),
						apConfig.activePivotManager()));
	}

	/**
	 * Health Check Agent bean
	 *
	 * @return the health check agent
	 */
	@Bean(initMethod = "start", destroyMethod = "interrupt")
	public HealthCheckAgent healthCheckAgent() {
		return new HealthCheckAgent(60); // One trace per minute
	}

	/**
	 * Extended plugin injections that are required before doing the startup of the ActivePivot
	 * manager.
	 *
	 * @see #startManager()
	 * @throws Exception any exception that occurred during the injection
	 */
	protected void apManagerInitPrerequisitePluginInjections() throws Exception {
		/* ********************************************************* */
		/* Core injections for distributed architecture (when used). */
		/* ********************************************************* */
		// Inject the distributed messenger with security services
		for (Object key : Registry.getExtendedPlugin(IDistributedMessenger.class).keys()) {
			inject(IDistributedMessenger.class, String.valueOf(key), apConfig.contextValueManager());
		}

		// Inject the distributed security manager with security services
		for (Object key : Registry.getExtendedPlugin(IDistributedSecurityManager.class).keys()) {
			inject(IDistributedSecurityManager.class, String.valueOf(key), securityConfig.qfsUserDetailsService());
		}
	}

}
