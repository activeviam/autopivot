/*
 * (C) ActiveViam 2017
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.av.autopivot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.av.csv.CSVFormat;
import com.qfs.desc.IFieldDescription;
import com.qfs.desc.IOptimizationDescription;
import com.qfs.desc.IOptimizationDescription.Optimization;
import com.qfs.desc.IStoreDescription;
import com.qfs.desc.impl.FieldDescription;
import com.qfs.desc.impl.OptimizationDescription;
import com.qfs.desc.impl.StoreDescription;
import com.qfs.store.selection.ISelectionField;
import com.qfs.store.selection.impl.SelectionField;
import com.qfs.util.impl.QfsArrays;
import com.quartetfs.biz.pivot.cube.dimension.IDimension.DimensionType;
import com.quartetfs.biz.pivot.cube.hierarchy.ILevelInfo.LevelType;
import com.quartetfs.biz.pivot.cube.hierarchy.measures.IMeasureHierarchy;
import com.quartetfs.biz.pivot.definitions.IActivePivotDescription;
import com.quartetfs.biz.pivot.definitions.IActivePivotInstanceDescription;
import com.quartetfs.biz.pivot.definitions.IActivePivotSchemaDescription;
import com.quartetfs.biz.pivot.definitions.IAggregatedMeasureDescription;
import com.quartetfs.biz.pivot.definitions.IAxisHierarchyDescription;
import com.quartetfs.biz.pivot.definitions.IAxisLevelDescription;
import com.quartetfs.biz.pivot.definitions.INativeMeasureDescription;
import com.quartetfs.biz.pivot.definitions.impl.ActivePivotDescription;
import com.quartetfs.biz.pivot.definitions.impl.ActivePivotInstanceDescription;
import com.quartetfs.biz.pivot.definitions.impl.ActivePivotSchemaDescription;
import com.quartetfs.biz.pivot.definitions.impl.AggregatedMeasureDescription;
import com.quartetfs.biz.pivot.definitions.impl.AxisDimensionDescription;
import com.quartetfs.biz.pivot.definitions.impl.AxisDimensionsDescription;
import com.quartetfs.biz.pivot.definitions.impl.AxisHierarchyDescription;
import com.quartetfs.biz.pivot.definitions.impl.AxisLevelDescription;
import com.quartetfs.biz.pivot.definitions.impl.MeasuresDescription;
import com.quartetfs.biz.pivot.definitions.impl.NativeMeasureDescription;
import com.quartetfs.biz.pivot.definitions.impl.SelectionDescription;

/**
 * 
 * Describe the components of an ActivePivot application
 * automatically based on the input data format.
 * 
 * @author ActiveViam
 *
 */
public class AutoDescription {

	/** Default name of the base store */
	public static final String BASE_STORE = "DATA";

	/** Default name of the base pivot */
	public static final String PIVOT = "AUTOPIVOT";

	/** Default format for double measures */
	public static final String DOUBLE_FORMAT = "DOUBLE[#,###.00;-#,###.00]";
	
	/** Default format for integer measures */
	public static final String INTEGER_FORMAT = "INT[#,###;-#,###]";
	
	/** Default format for date levels */
	public static final String DATE_FORMAT = "DATE[yyyy-MM-dd]";
	
	/** Default format for time levels */
	public static final String TIME_FORMAT = "DATE[HH:mm:ss]";	
	
	/**
	 * 
	 * Generate a store description based on the discovery of the input data.
	 * 
	 * @param format
	 * @return store description
	 */
	public static IStoreDescription createStoreDescription(CSVFormat format) {

		List<IFieldDescription> fields = new ArrayList<>();
		List<IOptimizationDescription> optimizations = new ArrayList<>();

		for(int c = 0; c < format.getColumnCount(); c++) {
			String columnName = format.getColumnName(c);
			String columnType = format.getColumnType(c);
			FieldDescription desc = new FieldDescription(columnName, columnType);

			// For date fields automatically add YEAR - MONTH - DAY fields
			if(columnType.startsWith("DATE")) {
				FieldDescription year = new FieldDescription(columnName + ".YEAR", "int");
				optimizations.add(new OptimizationDescription(year.getName(), Optimization.DICTIONARY));
				FieldDescription month = new FieldDescription(columnName + ".MONTH", "string");
				optimizations.add(new OptimizationDescription(month.getName(), Optimization.DICTIONARY));
				FieldDescription day = new FieldDescription(columnName + ".DAY", "int");
				optimizations.add(new OptimizationDescription(day.getName(), Optimization.DICTIONARY));

				fields.add(year);
				fields.add(month);
				fields.add(day);
			}

			// Dictionarize objects and integers so they can be used
			// as ActivePivot levels.
			if(columnType.startsWith("DATE")
					|| "int".equalsIgnoreCase(columnType)
					|| "String".equalsIgnoreCase(columnType)) {
				optimizations.add(new OptimizationDescription(columnName, Optimization.DICTIONARY));
			}

			fields.add(desc);
		}

		@SuppressWarnings("unchecked")
		StoreDescription desc = new StoreDescription(
				BASE_STORE,
				Collections.EMPTY_LIST,
				fields,
				"COLUMN",
				null,
				optimizations,
				false);
		
		return desc;
	}
	
	
	/**
	 * 
	 * Create the description of an ActivePivot cube automatically,
	 * based on the description of the input dataset.
	 * 
	 * @param format
	 * @return AcivePivot description
	 */
	public static IActivePivotDescription createActivePivotDescription(CSVFormat format) {
		
		ActivePivotDescription desc = new ActivePivotDescription();
		
		// Hierarchies and dimensions
		AxisDimensionsDescription dimensions = new AxisDimensionsDescription();
		
		Set<String> numericsOnly = QfsArrays.mutableSet("double", "float", "long");
		for(int f = 0; f < format.getColumnCount(); f++) {
			String fieldName = format.getColumnName(f);
			String fieldType = format.getColumnType(f);
			

			if(!numericsOnly.contains(fieldType)) {
				AxisDimensionDescription dimension = new AxisDimensionDescription(fieldName);
				dimensions.addValues(Arrays.asList(dimension));
				
				// For date fields generate the YEAR-MONTH-DAY hierarchy
				if(fieldType.startsWith("DATE")) {
					dimension.setDimensionType(DimensionType.TIME);
					
					List<IAxisHierarchyDescription> hierarchies = new ArrayList<>();
					IAxisHierarchyDescription hierarchy = new AxisHierarchyDescription(fieldName);
					hierarchy.setDefaultHierarchy(true);
					IAxisLevelDescription dateLevel = new AxisLevelDescription(fieldName);
					dateLevel.setFormatter(DATE_FORMAT);
					dateLevel.setLevelType(LevelType.TIME);
					hierarchy.setLevels(Arrays.asList(dateLevel));
					hierarchies.add(hierarchy);
					
					IAxisHierarchyDescription ymd = new AxisHierarchyDescription(fieldName + "_YMD");
					List<IAxisLevelDescription> levels = new ArrayList<>();
					levels.add(new AxisLevelDescription("Year", fieldName + ".YEAR"));
					levels.add(new AxisLevelDescription("Month", fieldName + ".MONTH"));
					levels.add(new AxisLevelDescription("Day", fieldName + ".DAY"));
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
		
		Set<String> numerics = QfsArrays.mutableSet("double", "float", "int", "long");
		Set<String> integers = QfsArrays.mutableSet("int", "long");
		
		for(int f = 0; f < format.getColumnCount(); f++) {
			String fieldName = format.getColumnName(f);
			String fieldType = format.getColumnType(f);
			if(numerics.contains(fieldType) && !fieldName.endsWith("id") && !fieldName.endsWith("ID")) {
				
				// For each numerical input value, create aggregations for SUM, MIN, MAX and AVG
				AggregatedMeasureDescription sum = new AggregatedMeasureDescription(fieldName, "SUM");
				AggregatedMeasureDescription avg = new AggregatedMeasureDescription(fieldName, "AVG");
				AggregatedMeasureDescription min = new AggregatedMeasureDescription(fieldName, "MIN");
				AggregatedMeasureDescription max = new AggregatedMeasureDescription(fieldName, "MAX");

				sum.setFolder(fieldName);
				avg.setFolder(fieldName);
				min.setFolder(fieldName);
				max.setFolder(fieldName);
				
				// Setup measure formatters
				String formatter = integers.contains(fieldType) ? INTEGER_FORMAT : DOUBLE_FORMAT;
				sum.setFormatter(formatter);
				avg.setFormatter(DOUBLE_FORMAT);
				min.setFormatter(formatter);
				max.setFormatter(formatter);
				
				measures.add(sum);
				measures.add(avg);
				measures.add(min);
				measures.add(max);
			}
		}
		measureDesc.setAggregatedMeasuresDescription(measures);
		
		// Configure "native" measures
		List<INativeMeasureDescription> nativeMeasures = new ArrayList<>();
		INativeMeasureDescription countMeasure = new NativeMeasureDescription(IMeasureHierarchy.COUNT_ID, "Count");
		countMeasure.setFormatter(INTEGER_FORMAT);
		INativeMeasureDescription timestampMeasure = new NativeMeasureDescription(IMeasureHierarchy.TIMESTAMP_ID, "LastUpdate");
		timestampMeasure.setFormatter(TIME_FORMAT);
		measureDesc.setNativeMeasures(nativeMeasures);
		nativeMeasures.add(countMeasure);
		nativeMeasures.add(timestampMeasure);
		
		desc.setMeasuresDescription(measureDesc);
		
		return desc;
	}
	
	/**
	 * 
	 * @param storeDesc
	 * @return schema description
	 */
	public static IActivePivotSchemaDescription createActivePivotSchemaDescription(CSVFormat format) {
		ActivePivotSchemaDescription desc = new ActivePivotSchemaDescription();

		// Datastore selection
		List<ISelectionField> fields = new ArrayList<>();
		for(int f = 0; f < format.getColumnCount(); f++) {
			String fieldName = format.getColumnName(f);
			String fieldType = format.getColumnType(f);
			fields.add(new SelectionField(fieldName));
			
			if(fieldType.startsWith("DATE")) {
				fields.add(new SelectionField(fieldName + ".YEAR"));
				fields.add(new SelectionField(fieldName + ".MONTH"));
				fields.add(new SelectionField(fieldName + ".DAY"));
			}
		}
		SelectionDescription selection = new SelectionDescription(BASE_STORE, fields);
		
		// ActivePivot instance
		IActivePivotDescription pivot = createActivePivotDescription(format);
		IActivePivotInstanceDescription instance = new ActivePivotInstanceDescription(PIVOT, pivot);
		
		desc.setDatastoreSelection(selection);
		desc.setActivePivotInstanceDescriptions(Collections.singletonList(instance));
		
		return desc;
	}
	
}
