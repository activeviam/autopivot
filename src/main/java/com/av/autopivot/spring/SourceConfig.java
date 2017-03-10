/*
 * (C) ActiveViam 2017
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.autopivot.spring;


import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import com.av.autopivot.AutoDescription;
import com.av.csv.calculator.DateDayCalculator;
import com.av.csv.calculator.DateMonthCalculator;
import com.av.csv.calculator.DateYearCalculator;
import com.av.csv.discover.CSVDiscovery;
import com.av.csv.discover.CSVDiscoveryResult;
import com.qfs.msg.IColumnCalculator;
import com.qfs.msg.csv.ICSVSource;
import com.qfs.msg.csv.ICSVSourceConfiguration;
import com.qfs.msg.csv.IFileInfo;
import com.qfs.msg.csv.ILineReader;
import com.qfs.msg.csv.filesystem.impl.SingleFileCSVTopic;
import com.qfs.msg.csv.impl.CSVParserConfiguration;
import com.qfs.msg.csv.impl.CSVSource;
import com.qfs.source.impl.CSVMessageChannelFactory;
import com.qfs.source.impl.Fetch;
import com.quartetfs.fwk.QuartetRuntimeException;

/**
 *
 * Spring configuration of the Sandbox ActivePivot server.<br>
 * The parameters of the Sandbox ActivePivot server can be quickly changed by modifying the
 * pojo.properties file.
 *
 * @author ActiveViam
 *
 */
@PropertySource(value = { "classpath:data.properties" })
@Configuration
public class SourceConfig {

	/** Logger **/
	protected static Logger LOGGER = Logger.getLogger(SourceConfig.class.getName());

	/** Property to identify the name of teh file to load */
	public static final String FILENAME_PROPERTY = "fileName";
	
	/** Spring environment, automatically wired */
	@Autowired
	protected Environment env;

	/** Application datastore, automatically wired */
	@Autowired
	protected DatastoreConfig datastoreConfig;

	/** Create and configure the CSV engine */
	@Bean
	public ICSVSource<Path> CSVSource() throws IOException {

		CSVSource<Path> source = new CSVSource<Path>();
		
		Properties properties = new Properties();
		properties.put(ICSVSourceConfiguration.BUFFER_SIZE_PROPERTY, "1024");
		properties.put(ICSVSourceConfiguration.PARSER_THREAD_PROPERTY, "4");
		source.configure(properties);
		
		return source;
	}
	
	/** Discover the input data file (CSV separator, column types) */
	@Bean
	public CSVDiscoveryResult discoverFile() {
		String fileName = env.getRequiredProperty("fileName");
		try {
			CSVDiscoveryResult discovery = new CSVDiscovery().discoverFile(fileName);
			return discovery;
		} catch(Exception e) {
			throw new QuartetRuntimeException("Could not discover csv file: " + fileName , e);
		}
	}
	
	/**
	 * Load the CSV file
	 */
	@Bean
	public Void loadData(ICSVSource<Path> source) throws Exception {
		
		CSVDiscoveryResult discovery = discoverFile();
		
		// Create parser configuration
		CSVParserConfiguration configuration = new CSVParserConfiguration(
				Charset.defaultCharset(), 
				discovery.getSeparator().charAt(0),
				discovery.getColumnCount(),
				true,
				1,
				CSVParserConfiguration.toMap(discovery.getColumnNames()));
		
		String fileName = env.getRequiredProperty("fileName");
		SingleFileCSVTopic topic = new SingleFileCSVTopic(AutoDescription.BASE_STORE, configuration, fileName, 1000);
		source.addTopic(topic);
		
		CSVMessageChannelFactory<Path> channelFactory = new CSVMessageChannelFactory<>(source, datastoreConfig.datastore());
		
		// Derive calculated columns
		List<IColumnCalculator<ILineReader>> calculatedColumns = new ArrayList<IColumnCalculator<ILineReader>>();
		for(int c = 0; c < discovery.getColumnCount(); c++) {
			String columnName = discovery.getColumnName(c);
			String columnType = discovery.getColumnType(c);
			
			// When a date field is detected, we automatically
			// calculate the YEAR, MONTH and DAY fields.
			if(columnType.startsWith("DATE")) {
				calculatedColumns.add(new DateYearCalculator(columnName, columnName + ".YEAR"));
				calculatedColumns.add(new DateMonthCalculator(columnName, columnName + ".MONTH"));
				calculatedColumns.add(new DateDayCalculator(columnName, columnName + ".DAY"));
			}
			
		};
		channelFactory.setCalculatedColumns(AutoDescription.BASE_STORE, calculatedColumns);
		
		
		Fetch<IFileInfo<Path>, ILineReader> fetch = new Fetch<IFileInfo<Path>, ILineReader>(channelFactory);
		fetch.fetch(source);
		
		return null; // Void
	}
	
}
