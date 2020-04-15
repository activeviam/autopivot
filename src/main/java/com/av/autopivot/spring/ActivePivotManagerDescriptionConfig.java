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

import com.qfs.desc.IDatastoreSchemaDescription;
import com.qfs.desc.IStoreDescription;
import com.qfs.desc.impl.DatastoreSchemaDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.av.autopivot.AutoPivotGenerator;
import com.av.csv.CSVFormat;
import com.qfs.server.cfg.IActivePivotManagerDescriptionConfig;
import com.quartetfs.biz.pivot.definitions.IActivePivotManagerDescription;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * 
 * Configure the ActivePivot Manager for the AutoPivot application.
 * <p>
 * The description of the cube are generated automatically
 * based on the format of the CSV file.
 * 
 * @author ActiveViam
 *
 */
@Configuration
public class ActivePivotManagerDescriptionConfig implements IActivePivotManagerDescriptionConfig {

	/** Spring environment */
	@Autowired
	protected Environment env;

	/** Datasource configuration */
	@Autowired
	protected SourceConfig sourceConfig;

	@Bean
	@Override
	public IActivePivotManagerDescription userManagerDescription() {
		CSVFormat discovery = sourceConfig.discoverFile();

		AutoPivotGenerator generator = generator();
		IActivePivotManagerDescription manager =
				generator.createActivePivotManagerDescription(discovery, env);

		return manager;
	}

	/**
	 *
	 * Generator of store and cube descriptions.
	 *
	 * @return ActivePivot generator
	 */
	@Bean
	public AutoPivotGenerator generator() {
		return new AutoPivotGenerator();
	}

	@Bean
	@Override
	public IDatastoreSchemaDescription userSchemaDescription() {
		CSVFormat discovery = sourceConfig.discoverFile();
		AutoPivotGenerator generator = generator();

		final Collection<IStoreDescription> stores = new LinkedList<>();
		stores.add(generator.createStoreDescription(discovery, env));
		return new DatastoreSchemaDescription(stores, Collections.emptyList());
	}

}
