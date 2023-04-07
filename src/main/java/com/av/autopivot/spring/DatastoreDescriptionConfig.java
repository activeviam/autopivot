/*
 * (C) ActiveViam 2017-2022
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of ActiveViam. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */

package com.av.autopivot.spring;

import com.av.autopivot.AutoPivotGenerator;
import com.av.csv.CSVFormat;
import com.qfs.desc.IDatastoreSchemaDescription;
import com.qfs.desc.IStoreDescription;
import com.qfs.desc.impl.DatastoreSchemaDescription;
import com.qfs.server.cfg.IDatastoreSchemaDescriptionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Spring configuration file that exposes the datastore {@link IDatastoreSchemaDescription description}.
 *
 * @author ActiveViam
 */
@Configuration
public class DatastoreDescriptionConfig implements IDatastoreSchemaDescriptionConfig {

	/** Spring environment */
	@Autowired
	protected Environment env;

	/** CSV Format */
	@Autowired
	protected CSVFormat discovered;

	/** AutoPivot Generator */
	@Autowired
	protected AutoPivotGenerator generator;

	@Bean
	@Override
	public IDatastoreSchemaDescription datastoreSchemaDescription() {
		final Collection<IStoreDescription> stores = new LinkedList<>();
		stores.add(generator.createStoreDescription(discovered, env));
		return new DatastoreSchemaDescription(stores, Collections.emptyList());
	}

}
