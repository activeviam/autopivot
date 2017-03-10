/*
 * (C) Quartet FS 2013-2014
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.autopivot.spring;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.av.autopivot.AutoDescription;
import com.av.csv.discover.CSVDiscoveryResult;
import com.qfs.desc.IDatastoreSchemaDescription;
import com.qfs.desc.IReferenceDescription;
import com.qfs.desc.IStoreDescription;
import com.qfs.desc.impl.DatastoreSchemaDescription;
import com.qfs.multiversion.impl.KeepLastEpochPolicy;
import com.qfs.server.cfg.IDatastoreConfig;
import com.qfs.server.cfg.impl.ActivePivotConfig;
import com.qfs.store.IDatastore;
import com.qfs.store.build.impl.DatastoreBuilder;
import com.quartetfs.fwk.QuartetRuntimeException;

/**
 *
 * Spring configuration of the Datastore.
 *
 * @author Quartet FS
 *
 */
@Configuration
public class DatastoreConfig implements IDatastoreConfig {

	/** Spring environment, automatically wired */
	@Autowired
	protected Environment env;

	/** {@link ActivePivotConfig} spring configuration */
	@Autowired
	protected ActivePivotConfig apConfig;

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
		CSVDiscoveryResult discovery = sourceConfig.discoverFile();
		
		final Collection<IStoreDescription> stores = new LinkedList<>();
		stores.add(AutoDescription.createStoreDescription(discovery));
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
