/*
 * (C) ActiveViam 2018
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.export;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.qfs.jmx.JmxAttribute;
import com.qfs.jmx.JmxOperation;
import com.qfs.store.IDatastore;
import com.qfs.store.query.ICursor;
import com.qfs.store.record.IRecordFormat;
import com.qfs.store.record.IRecordReader;
import com.quartetfs.fwk.monitoring.jmx.impl.JMXEnabler;

/**
 * 
 * Datastore export service, to export the records of a store
 * into a local CSV file.
 * <p>
 * This service can be exposed through JMX
 * using an ActiveViam {@link JMXEnabler}
 * 
 * @author ActiveViam
 *
 */
public class DatastoreExportService {

	/** Logger */
	public static final Logger LOG = Logger.getLogger(DatastoreExportService.class.getName());
	
	/** Default CSV field delimiter ';' */
	public static final String DEFAULT_DELIMITER = ";";
	
	/** CSV field delimiter */
	protected String delimiter;
	
	protected final IDatastore datastore;
	
	public DatastoreExportService(IDatastore datastore) {
		this.datastore = datastore;
		this.delimiter = DEFAULT_DELIMITER;
	}
	
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter != null ? delimiter : DEFAULT_DELIMITER;
	}
	

	/**
	 * 
	 * Export the records of a given store into a local temporary file
	 * 
	 * @param storeName
	 * @param full indicates if data in referenced stores should also be exported
	 * @return
	 */
	protected String export(String storeName, boolean full) {

		try {
			Path path = Files.createTempFile(storeName.replaceAll(" ", "-") + "-export-", ".csv");
			try(Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				final long[] recordCount = new long[1];
				
				// Export query
				final ICursor queryCursor;
				if(full) {
					queryCursor = datastore.getHead().getQueryManager()
							.forStore(storeName)
							.withoutCondition()
							.selectingAllReachableFields()
							.run();
				} else {
					queryCursor = datastore.getHead().getQueryManager()
							.forStore(storeName)
							.withoutCondition()
							.selectingAllStoreFields()
							.run();
				}

				final IRecordFormat format = queryCursor.getRecordFormat();
				String header = String.join(delimiter, format.getFieldNames());
				writer.write(header);
				
				while(queryCursor.next()) {
					IRecordReader record = queryCursor.getRecord();
					
					recordCount[0]++;
					writer.append("\n");
					for(int i = 0; i < format.getFieldCount(); i++) {
						if(i > 0) { writer.append(delimiter); }
						Object value = record.read(i);
						if(value != null) {
							writer.write(value.toString());
						}
					}
				}

				return "Successfully exported " + recordCount[0] + " records into " + path;
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "The export operation for store " + storeName + " has failed.", e);
			return "An error occured during the export operation:\n" + e.getMessage();
		}

	}

	
	
	
	// JMX declarations
	
	@JmxAttribute(desc = "Delimiter used in the CSV export")
	public String getDelimiter() {
		return delimiter;
	}
	
	@JmxAttribute(desc = "Stores available for export")
	public List<String> getStoreNames() {
		return datastore.getTransactionManager().getMetadata().getStoreNames();
	}
	
	
	/**
	 * 
	 * Export all the records of a single store
	 * 
	 * @param storeName
	 * @return message describing the status of the export operation
	 */
	@JmxOperation(
			desc = "Export the records of a single store",
			params = { "Name of the store to export" })
	public String export(String storeName) {
		return export(storeName, false);
	}
	
	/**
	 * 
	 * Export all the records of the given store, including
	 * the data referenced from other stores. The data is written into a
	 * temporary file.
	 * 
	 * @param storeName
	 * @return message describing the status of the export operation
	 */
	@JmxOperation(
			desc = "Export the records of a store, including the fields in referenced stores.",
			params = { "Name of the base store to export" })
	public String fullExport(String storeName) {
		return export(storeName, true);
	}
	
}
