/*
 * (C) ActiveViam
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.autopivot.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.av.autopivot.AutoPivotGenerator;
import com.av.csv.CSVFormat;
import com.qfs.content.cfg.impl.ContentServerRestServicesConfig;
import com.qfs.content.service.IContentService;
import com.qfs.pivot.content.IActivePivotContentService;
import com.qfs.pivot.content.impl.ActivePivotContentServiceBuilder;
import com.qfs.server.cfg.IActivePivotContentServiceConfig;
import com.quartetfs.biz.pivot.definitions.IActivePivotManagerDescription;

/**
 * Spring configuration of the <b>Content Service</b> backed by a local <b>Content Server</b>.
 * <p>
 * This configuration imports {@link ContentServerRestServicesConfig} to expose the content service.
 *
 * @author ActiveViam
 */
@Configuration
public class ContentServiceConfig implements IActivePivotContentServiceConfig {

	/** Datasource configuration */
	@Autowired
	protected SourceConfig sourceConfig;
	

	/** Datastore configuration */
	@Autowired
	protected DatastoreConfig datastoreConfig;

	/**
	 * @return ActivePivot content service used to store context
	 * values, calculated members, and ActiveUI settings and bookmarks.
	 */
	@Bean
	@Override
	public IActivePivotContentService activePivotContentService() {

		CSVFormat discovery = sourceConfig.discoverFile();
		AutoPivotGenerator generator = datastoreConfig.generator();
		
		IActivePivotManagerDescription manager = generator.createActivePivotManagerDescription(discovery);
		
		return new ActivePivotContentServiceBuilder()
				.withoutPersistence()
				.withoutCache()
				.needInitialization("ROLE_USER", "ROLE_USER")
				.withDescription(manager)
				// Push the context values stored in ROLE-INF directory
				.withContextValues("ROLE-INF")
				.build();
	}

	@Bean
	@Override
	public IContentService contentService() {
		// Return the real content service used by the activePivotContentService instead of the wrapped one
		return activePivotContentService().getContentService().getUnderlying();
	}

}
