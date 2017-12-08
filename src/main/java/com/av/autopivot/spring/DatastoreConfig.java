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

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.av.autopivot.AutoPivotGenerator;
import com.av.csv.CSVFormat;
import com.qfs.desc.IDatastoreSchemaDescription;
import com.qfs.desc.IReferenceDescription;
import com.qfs.desc.IStoreDescription;
import com.qfs.desc.impl.DatastoreSchemaDescription;
import com.qfs.multiversion.impl.KeepLastEpochPolicy;
import com.qfs.server.cfg.IDatastoreConfig;
import com.qfs.store.IDatastore;
import com.qfs.store.build.impl.DatastoreBuilder;
import com.quartetfs.fwk.QuartetRuntimeException;

/**
 *
 * Spring configuration of the Datastore.
 *
 * @author ActiveViam
 *
 */
@Configuration
public class DatastoreConfig implements IDatastoreConfig {

	/** Spring environment, automatically wired */
	@Autowired
	protected Environment env;

	/** Source configuration */
	@Autowired
	protected SourceConfig sourceConfig;


	// ////////////////////////////////////////////////
	// Schema & Datastore
	// ////////////////////////////////////////////////

	/** @return the references between stores */
	public Collection<IReferenceDescription> references() {
		final Collection<IReferenceDescription> references = new LinkedList<>();

		return references;
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
	
	/**
	 *
	 * Provide the schema description of the datastore.
	 * <p>
	 * It is based on the descriptions of the stores in
	 * the datastore, the descriptions of the references
	 * between those stores, and the optimizations and
	 * constraints set on the schema.
	 *
	 * @return schema description
	 */
	@Bean
	public IDatastoreSchemaDescription schemaDescription() throws IOException {
		CSVFormat discovery = sourceConfig.discoverFile();
		AutoPivotGenerator generator = generator();
		
		final Collection<IStoreDescription> stores = new LinkedList<>();
		stores.add(generator.createStoreDescription(discovery, env));
		return new DatastoreSchemaDescription(stores, references());
	}

	/**
	 * Instantiate the datastore. There is only one Datastore in the application
	 * and it is automatically wired in other Spring configuration files.
	 *
	 * @return datastore bean
	 */
	@Bean
	public IDatastore datastore() {
		try {
			return new DatastoreBuilder()
					.setSchemaDescription(schemaDescription())
					// Only keep latest version of the data
					.setEpochManagementPolicy(new KeepLastEpochPolicy())
					// Build the datastore (no transaction log)
					.build();
		} catch(Exception e) {
			throw new QuartetRuntimeException("Error, impossible to build datastore", e);
		}
	}

}
