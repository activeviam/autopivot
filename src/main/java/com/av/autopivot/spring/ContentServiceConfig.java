/*
 * (C) ActiveViam
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.autopivot.spring;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.av.autopivot.AutoDescription;
import com.av.csv.discover.CSVDiscoveryResult;
import com.qfs.content.cfg.impl.ContentServerRestServicesConfig;
import com.qfs.content.service.IContentService;
import com.qfs.pivot.content.IActivePivotContentService;
import com.qfs.pivot.content.impl.ActivePivotContentServiceBuilder;
import com.qfs.server.cfg.IActivePivotContentServiceConfig;
import com.quartetfs.biz.pivot.definitions.IActivePivotManagerDescription;
import com.quartetfs.biz.pivot.definitions.IActivePivotSchemaDescription;
import com.quartetfs.biz.pivot.definitions.IActivePivotSchemaInstanceDescription;
import com.quartetfs.biz.pivot.definitions.ICatalogDescription;
import com.quartetfs.biz.pivot.definitions.impl.ActivePivotManagerDescription;
import com.quartetfs.biz.pivot.definitions.impl.ActivePivotSchemaInstanceDescription;
import com.quartetfs.biz.pivot.definitions.impl.CatalogDescription;

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

	/**
	 * @return ActivePivot content service used to store context
	 * values, calculated members, and ActiveUI settings and bookmarks.
	 */
	@Bean
	@Override
	public IActivePivotContentService activePivotContentService() {

		return new ActivePivotContentServiceBuilder()
				.withoutPersistence()
				.withoutCache()
				.needInitialization("ROLE_USER", "ROLE_USER")

				// Push the context values stored in ROLE-INF
				.withDescription(createActivePivotManagerDescription())
				.withContextValues("ROLE-INF")
				.build();
	}

	@Bean
	@Override
	public IContentService contentService() {
		// Return the real content service used by the activePivotContentService instead of the wrapped one
		return activePivotContentService().getContentService().getUnderlying();
	}

	public IActivePivotManagerDescription createActivePivotManagerDescription() {
		
		CSVDiscoveryResult discovery = sourceConfig.discoverFile();
		
		ICatalogDescription catalog = new CatalogDescription("AUTOPIVOT_CATALOG", Arrays.asList(AutoDescription.PIVOT));
		IActivePivotSchemaDescription schema = AutoDescription.createActivePivotSchemaDescription(discovery);
		IActivePivotSchemaInstanceDescription instance = new ActivePivotSchemaInstanceDescription("AUTOPIVOT_SCHEMA", schema);
		
		ActivePivotManagerDescription desc = new ActivePivotManagerDescription();
		desc.setCatalogs(Arrays.asList(catalog));
		desc.setSchemas(Arrays.asList(instance));
		return desc;
	}

}
