/*
 * (C) ActiveViam FS 2013-2018
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.autopivot.spring;

import static com.av.autopivot.spring.SecurityConfig.ROLE_ADMIN;
import static com.av.autopivot.spring.SecurityConfig.ROLE_USER;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Configuration;

import com.av.autopivot.AutoPivotGenerator;
import com.qfs.desc.IStoreSecurity;
import com.qfs.desc.IStoreSecurityBuilder;
import com.qfs.desc.impl.StoreSecurityBuilder;
import com.qfs.service.store.IDatastoreServiceConfiguration;
import com.quartetfs.fwk.format.IFormatter;
import com.quartetfs.fwk.format.IParser;

/**
 * @author ActiveViam
 */
@Configuration
public class DatastoreServiceConfig implements IDatastoreServiceConfiguration {

	/** @see #getStoresSecurity() */
	protected Map<String, IStoreSecurity> storesSecurity;
	
	/** @see #getCustomParsers() */
	protected Map<String, Map<String, IParser<?>>> customParsers;

	/** @see #getCustomFormatters() */
	protected Map<String, Map<String, IFormatter>> customFormatters;

	/** Default query timeout for queries */
	protected static final long DEFAULT_QUERY_TIMEOUT = 30_000L;

	/**
	 * Constructor of {@link DatastoreServiceConfig}.
	 */
	public DatastoreServiceConfig() {

		// SECURITY
		this.storesSecurity = new HashMap<>();
		IStoreSecurityBuilder builder = StoreSecurityBuilder.startBuildingStoreSecurity()
				.supportInsertion()
				.supportDeletion()
				.withStoreWriters(ROLE_ADMIN)
				.withStoreReaders(ROLE_USER);
		storesSecurity.put(AutoPivotGenerator.BASE_STORE, builder.build());
		
		// ADDITIONAL FORMATTERS
		this.customFormatters = new HashMap<>();

		// ADDITIONAL PARSERS
		this.customParsers = new HashMap<>();
	}

	@Override
	public Map<String, Map<String, IParser<?>>> getCustomParsers() {
		return this.customParsers;
	}

	@Override
	public Map<String, Map<String, IFormatter>> getCustomFormatters() {
		return this.customFormatters;
	}

	@Override
	public Map<String, IStoreSecurity> getStoresSecurity() {
		return storesSecurity;
	}

	@Override
	public long getDefaultQueryTimeout() { return DEFAULT_QUERY_TIMEOUT; }

}
