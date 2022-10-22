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
package com.av.autopivot;

import static com.av.csv.discover.CSVDiscovery.isDate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import com.activeviam.database.api.query.AliasedField;
import com.qfs.chunk.impl.Chunks;
import com.qfs.desc.*;
import com.qfs.pivot.cube.provider.multi.IMultipleAggregateProvider;
import com.qfs.store.part.INumaSelectorDescription;
import com.quartetfs.biz.pivot.definitions.*;
import com.quartetfs.biz.pivot.postprocessing.IPostProcessorConstants;
import org.springframework.core.env.Environment;

import com.av.csv.CSVFormat;
import com.qfs.desc.IOptimizationDescription.Optimization;
import com.qfs.desc.impl.FieldDescription;
import com.qfs.desc.impl.OptimizationDescription;
import com.qfs.desc.impl.StoreDescription;
import com.qfs.platform.IPlatform;
import com.qfs.store.part.IPartitioningDescription;
import com.qfs.store.part.impl.ModuloFunctionDescription;
import com.qfs.store.part.impl.PartitioningDescriptionBuilder;
import com.qfs.util.impl.QfsArrays;
import com.quartetfs.biz.pivot.cube.dimension.IDimension.DimensionType;
import com.quartetfs.biz.pivot.cube.hierarchy.ILevelInfo.LevelType;
import com.quartetfs.biz.pivot.cube.hierarchy.measures.IMeasureHierarchy;
import com.quartetfs.biz.pivot.definitions.impl.ActivePivotDescription;
import com.quartetfs.biz.pivot.definitions.impl.ActivePivotInstanceDescription;
import com.quartetfs.biz.pivot.definitions.impl.ActivePivotManagerDescription;
import com.quartetfs.biz.pivot.definitions.impl.ActivePivotSchemaDescription;
import com.quartetfs.biz.pivot.definitions.impl.ActivePivotSchemaInstanceDescription;
import com.quartetfs.biz.pivot.definitions.impl.AggregateProviderDefinition;
import com.quartetfs.biz.pivot.definitions.impl.AggregatedMeasureDescription;
import com.quartetfs.biz.pivot.definitions.impl.AggregatesCacheDescription;
import com.quartetfs.biz.pivot.definitions.impl.AxisDimensionDescription;
import com.quartetfs.biz.pivot.definitions.impl.AxisDimensionsDescription;
import com.quartetfs.biz.pivot.definitions.impl.AxisHierarchyDescription;
import com.quartetfs.biz.pivot.definitions.impl.AxisLevelDescription;
import com.quartetfs.biz.pivot.definitions.impl.CatalogDescription;
import com.quartetfs.biz.pivot.definitions.impl.MeasuresDescription;
import com.quartetfs.biz.pivot.definitions.impl.NativeMeasureDescription;
import com.quartetfs.biz.pivot.definitions.impl.PostProcessorDescription;
import com.quartetfs.biz.pivot.definitions.impl.SelectionDescription;

/**
 * 
 * Describe the components of an ActivePivot application
 * automatically based on the input data format.
 * 
 * @author ActiveViam
 *
 */
public class AutoPivotGenerator {

	public static final String YEAR = ".YEAR";
	public static final String MONTH = ".MONTH";
	public static final String DAY = ".DAY";
	public static final String FLOAT = "float";
	public static final String DOUBLE = "double";
	public static final String LONG = "long";
	public static final String INT = "int";
	/** Logger **/
	protected static Logger LOGGER = Logger.getLogger(AutoPivotGenerator.class.getName());
	
	/** Default name of the base store */
	public static final String BASE_STORE = "DATA";

	/** Default name of the base pivot */
	public static final String PIVOT = "AUTOPIVOT";

	/** Default format for double measures */
	public static final String DOUBLE_FORMAT = "DOUBLE[#,###.00;-#,###.00]";
	
	/** Default format for integer measures */
	public static final String INTEGER_FORMAT = "INT[#,###;-#,###]";
	
	/** Default format for time levels */
	public static final String TIME_FORMAT = "DATE[HH:mm:ss]";	

	/**
	 * 
	 * Generate a store description based on the discovery of the input data.
	 * 
	 * @param format
	 * @return store description
	 */
	public IStoreDescription createStoreDescription(CSVFormat format, Environment env) {

		List<IFieldDescription> fields = new ArrayList<>();

		for(int c = 0; c < format.getColumnCount(); c++) {
			String columnName = format.getColumnName(c);
			String columnType = format.getColumnType(c);
			FieldDescription desc = new FieldDescription(columnName, columnType);

			// For date fields automatically add YEAR - MONTH - DAY fields
			if(isDate(columnType)) {
				FieldDescription year = new FieldDescription(columnName + YEAR, INT);
				FieldDescription month = new FieldDescription(columnName + MONTH, INT);
				FieldDescription day = new FieldDescription(columnName + DAY, INT);

				fields.add(year);
				fields.add(month);
				fields.add(day);
			}

			fields.add(desc);
		}

		// Partitioning
		IPartitioningDescription partitioning = createPartitioningDescription(format, env);

			@SuppressWarnings("unchecked")
		StoreDescription desc = new StoreDescription(BASE_STORE,
					Collections.EMPTY_LIST,
					fields,
					partitioning,
					Collections.emptyList(),
					false,
					Chunks.DEFAULT_CHUNK_SIZE,
					(IDuplicateKeyHandler)null,
					(IStoreDescriptionBuilder.IRemoveUnknownKeyListener)null,
					(Properties)null,
					(INumaSelectorDescription)null,
					false);

		return desc;
	}


	/**
	 * 
	 * Automatically configure the partitioning of the datastore.
	 * The first non floating point field is used as the partitioning
	 * field, and the number of partitions is half the number
	 * of cores.
	 * 
	 * @param format
	 * @return partitioning description
	 */
	public IPartitioningDescription createPartitioningDescription(CSVFormat format, Environment env) {
		
		int processorCount = IPlatform.CURRENT_PLATFORM.getProcessorCount();
		int partitionCount = processorCount/2;
		if(partitionCount > 1) {

			String partitioningField = env.getProperty("datastore.partitioningField");
			if(partitioningField != null) {
				
				for(int c = 0; c < format.getColumnCount(); c++) {
					String fieldName = format.getColumnName(c);
					if(fieldName.equalsIgnoreCase(partitioningField)) {
						return new PartitioningDescriptionBuilder()
						.addSubPartitioning(fieldName, new ModuloFunctionDescription(partitionCount))
						.build();
					}
				}
				
				LOGGER.warning(()->"Configured partitioning field '" + partitioningField + "' does not exist in input file format. Default partitioning will be used.");
				
			}
			
			// Default partitioning, partition on the first field
			// that is not numerical
				
			for(int c = 0; c < format.getColumnCount(); c++) {
				String fieldName = format.getColumnName(c);
				String fieldType = format.getColumnType(c);
					
				if(!FLOAT.equalsIgnoreCase(fieldType) && !DOUBLE.equalsIgnoreCase(fieldType) && !LONG.equalsIgnoreCase(fieldType)) {
					LOGGER.info(()->"Applying default partitioning policy: " + partitionCount + " partitions with partitioning field '" + fieldName + "'");
					return new PartitioningDescriptionBuilder()
					.addSubPartitioning(fieldName, new ModuloFunctionDescription(partitionCount))
					.build();
				}
			}
			
		}
		
		return null;
	}
	
	
	
	/**
	 * 
	 * Create the description of an ActivePivot cube automatically,
	 * based on the description of the input dataset.
	 * 
	 * @param format
	 * @return AcivePivot description
	 */
	public IActivePivotDescription createActivePivotDescription(CSVFormat format, Environment env) {
		
		ActivePivotDescription desc = new ActivePivotDescription();
		
		
		IAggregateProviderDefinition apd = new AggregateProviderDefinition(IMultipleAggregateProvider.DEFAULT_UNDERLYINGPROVIDER_PLUGINKEY);
		desc.setAggregateProvider(apd);
		
		// Hierarchies and dimensions
		AxisDimensionsDescription dimensions = new AxisDimensionsDescription();
		
		Set<String> numerics = QfsArrays.mutableSet(DOUBLE, FLOAT, INT, LONG);
		Set<String> integers = QfsArrays.mutableSet(INT, LONG);
		Set<String> decimals = QfsArrays.mutableSet(DOUBLE, FLOAT);
		Set<String> numericsOnly = QfsArrays.mutableSet(DOUBLE, FLOAT, LONG);

		for(int f = 0; f < format.getColumnCount(); f++) {
			String fieldName = format.getColumnName(f);
			String fieldType = format.getColumnType(f);
			

			if(!numericsOnly.contains(fieldType)) {
				AxisDimensionDescription dimension = new AxisDimensionDescription(fieldName);
				IAxisHierarchyDescription h = new AxisHierarchyDescription(fieldName);
				IAxisLevelDescription l = new AxisLevelDescription(fieldName,fieldName);
				h.getLevels().add(l);
				dimension.getHierarchies().add(h);
				dimensions.addValues(Arrays.asList(dimension));
				
				// For date fields generate the YEAR-MONTH-DAY hierarchy
				if(isDate(fieldType)) {
					dimension.setDimensionType(DimensionType.TIME);
					
					List<IHierarchyDescription> hierarchies = new ArrayList<>();
					IAxisHierarchyDescription hierarchy = new AxisHierarchyDescription(fieldName);
					hierarchy.setDefaultHierarchy(true);
					IAxisLevelDescription dateLevel = new AxisLevelDescription(fieldName,fieldName);
					dateLevel.setLevelType(LevelType.TIME);
					hierarchy.setLevels(Arrays.asList(dateLevel));
					hierarchies.add(hierarchy);
					
					IAxisHierarchyDescription ymd = new AxisHierarchyDescription(fieldName + "_YMD");
					List<IAxisLevelDescription> levels = new ArrayList<>();
					levels.add(new AxisLevelDescription("Year", fieldName + YEAR));
					levels.add(new AxisLevelDescription("Month", fieldName + MONTH));
					levels.add(new AxisLevelDescription("Day", fieldName + DAY));
					ymd.setLevels(levels);
					hierarchies.add(ymd);
					
					dimension.setHierarchies(hierarchies);
				}
			}
		}

		desc.setAxisDimensions(dimensions);
		
		
		// Measures
		MeasuresDescription measureDesc = new MeasuresDescription();
		List<IAggregatedMeasureDescription> measures = new ArrayList<>();
		List<IPostProcessorDescription> postProcessors = new ArrayList<>();
		
		for(int f = 0; f < format.getColumnCount(); f++) {
			String fieldName = format.getColumnName(f).trim();
			String fieldType = format.getColumnType(f);
			if(numerics.contains(fieldType) && !fieldName.endsWith("id") && !fieldName.endsWith("ID")) {
				
				// For each numerical input value, create aggregations for SUM, MIN, MAX
				AggregatedMeasureDescription sum = new AggregatedMeasureDescription(fieldName, "SUM");
				AggregatedMeasureDescription min = new AggregatedMeasureDescription(fieldName, "MIN");
				AggregatedMeasureDescription max = new AggregatedMeasureDescription(fieldName, "MAX");
				AggregatedMeasureDescription sq_sum = new AggregatedMeasureDescription(fieldName, "SQ_SUM");
				sq_sum.setVisible(false);
				
				// Shared formula expressions
				String sumExpression = "aggregatedValue[" + fieldName + ".SUM]";
				String squareSumExpression = "aggregatedValue[" + fieldName + ".SQ_SUM]";
				String countExpression = "aggregatedValue[contributors.COUNT]";
				String avgExpression = "aggregatedValue[" + fieldName + ".avg]";
				
				// Define a formula post processor to compute the average
				PostProcessorDescription avg = new PostProcessorDescription(fieldName + ".avg", "FORMULA", new Properties());
				String formula = sumExpression + ", " + countExpression + ", /";
				avg.getProperties().setProperty("formula", formula);
				
				// Define a formula post processor to compute the standard deviation
				PostProcessorDescription std = new PostProcessorDescription(fieldName + ".STD", "FORMULA", new Properties());
				String stdFormula = "(" + squareSumExpression + ", " + countExpression + ", /)";
				stdFormula += ", (" + avgExpression + ", " + avgExpression + ", *), -, SQRT";
				std.getProperties().setProperty("formula", stdFormula);

				// Put the measures for that field in one folder
				sum.setFolder(fieldName);
				sq_sum.setFolder(fieldName);
				avg.setFolder(fieldName);
				std.setFolder(fieldName);
				min.setFolder(fieldName);
				max.setFolder(fieldName);
				
				// Setup measure formatters
				String formatter = integers.contains(fieldType) ? INTEGER_FORMAT : DOUBLE_FORMAT;
				sum.setFormatter(formatter);
				sq_sum.setFormatter(formatter);
				min.setFormatter(formatter);
				max.setFormatter(formatter);
				avg.setFormatter(DOUBLE_FORMAT);
				std.setFormatter(DOUBLE_FORMAT);
				
				measures.add(sum);
				measures.add(min);
				measures.add(max);
				
				postProcessors.add(avg);
				
				// Add standard deviation only for floating point inputs
				if(decimals.contains(fieldType)) {
					measures.add(sq_sum);
					postProcessors.add(std);
				}

			}
		}


		// Add distinct count calculation for each level field
		for(int f = 0; f < format.getColumnCount(); f++) {
			String fieldName = format.getColumnName(f).trim();
			String fieldType = format.getColumnType(f);

			if(!numericsOnly.contains(fieldType)) {
				
				PostProcessorDescription dc = new PostProcessorDescription(fieldName + ".COUNT", IPostProcessorConstants.LEAF_COUNT_PLUGIN_KEY, new Properties());
				String leafExpression = fieldName + "@" + fieldName;
				dc.getProperties().setProperty(IPostProcessorConstants.LEAF_COUNT_PARAM_LEAF_LEVELS, leafExpression);
				dc.setFolder("Distinct Count");
				postProcessors.add(dc);
			}
		}
		
		
		measureDesc.setAggregatedMeasuresDescription(measures);
		measureDesc.setPostProcessorsDescription(postProcessors);

		// Configure "count" native measure
		List<INativeMeasureDescription> nativeMeasures = new ArrayList<>();
		INativeMeasureDescription countMeasure = new NativeMeasureDescription(IMeasureHierarchy.COUNT_ID, "Count");
		countMeasure.setFormatter(INTEGER_FORMAT);
		nativeMeasures.add(countMeasure);
		
		// Hide the last update measure that does not work Just In Time
		INativeMeasureDescription lastUpdateMeasure = new NativeMeasureDescription(IMeasureHierarchy.TIMESTAMP_ID);
		lastUpdateMeasure.setVisible(false);
		nativeMeasures.add(lastUpdateMeasure);

		measureDesc.setNativeMeasures(nativeMeasures);
		desc.setMeasuresDescription(measureDesc);

		// Aggregate cache configuration
		if(env.containsProperty("pivot.cache.size")) {
			Integer cacheSize = env.getProperty("pivot.cache.size", Integer.class);
			LOGGER.info(()->"Configuring aggregate cache of size " + cacheSize);
			IAggregatesCacheDescription cacheDescription = new AggregatesCacheDescription();
			cacheDescription.setSize(cacheSize);
			desc.setAggregatesCacheDescription(cacheDescription);
		}

		return desc;
	}
	
	/**
	 * 
	 * @param format format of the CSV file
	 * @param env spring environment
	 * @return schema description
	 */
	public IActivePivotSchemaDescription createActivePivotSchemaDescription(CSVFormat format, Environment env) {
		ActivePivotSchemaDescription desc = new ActivePivotSchemaDescription();

		// Datastore selection
		List<AliasedField> fields = new ArrayList<>();
		for(int f = 0; f < format.getColumnCount(); f++) {
			String fieldName = format.getColumnName(f);
			String fieldType = format.getColumnType(f);
			fields.add(AliasedField.fromFieldName(fieldName));
			
			if(isDate(fieldType)) {
				fields.add(AliasedField.fromFieldName(fieldName + YEAR));
				fields.add(AliasedField.fromFieldName(fieldName + MONTH));
				fields.add(AliasedField.fromFieldName(fieldName + DAY));
			}
		}
		SelectionDescription selection = new SelectionDescription(BASE_STORE, fields);
		
		// ActivePivot instance
		IActivePivotDescription pivot = createActivePivotDescription(format, env);
		IActivePivotInstanceDescription instance = new ActivePivotInstanceDescription(PIVOT, pivot);

		desc.setDatabaseSelection(selection);
		desc.setActivePivotInstanceDescriptions(Collections.singletonList(instance));
		
		return desc;
	}
	
	
	/**
	 * 
	 * Generate a complete ActivePivot Manager description, with one catalog,
	 * one schema and one cube, based on the provided input data format.
	 * 
	 * @param format input data format
	 * @return ActivePivot Manager description
	 */
	public IActivePivotManagerDescription createActivePivotManagerDescription(CSVFormat format, Environment env) {

		ICatalogDescription catalog = new CatalogDescription(PIVOT + "_CATALOG", Arrays.asList(PIVOT));
		IActivePivotSchemaDescription schema = createActivePivotSchemaDescription(format, env);
		IActivePivotSchemaInstanceDescription instance = new ActivePivotSchemaInstanceDescription(PIVOT + "_SCHEMA", schema);
		
		ActivePivotManagerDescription desc = new ActivePivotManagerDescription();
		desc.setCatalogs(Arrays.asList(catalog));
		desc.setSchemas(Arrays.asList(instance));
		return desc;
	}
	
}
