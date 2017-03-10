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

import com.av.csv.discover.CSVDiscoveryResult;
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
import com.quartetfs.biz.pivot.definitions.IActivePivotDescription;
import com.quartetfs.biz.pivot.definitions.IActivePivotInstanceDescription;
import com.quartetfs.biz.pivot.definitions.IActivePivotSchemaDescription;
import com.quartetfs.biz.pivot.definitions.IAggregatedMeasureDescription;
import com.quartetfs.biz.pivot.definitions.IAxisHierarchyDescription;
import com.quartetfs.biz.pivot.definitions.IAxisLevelDescription;
import com.quartetfs.biz.pivot.definitions.impl.ActivePivotDescription;
import com.quartetfs.biz.pivot.definitions.impl.ActivePivotInstanceDescription;
import com.quartetfs.biz.pivot.definitions.impl.ActivePivotSchemaDescription;
import com.quartetfs.biz.pivot.definitions.impl.AggregatedMeasureDescription;
import com.quartetfs.biz.pivot.definitions.impl.AxisDimensionDescription;
import com.quartetfs.biz.pivot.definitions.impl.AxisDimensionsDescription;
import com.quartetfs.biz.pivot.definitions.impl.AxisHierarchyDescription;
import com.quartetfs.biz.pivot.definitions.impl.AxisLevelDescription;
import com.quartetfs.biz.pivot.definitions.impl.MeasuresDescription;
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

	/**
	 * 
	 * Generate a store description based on the discovery of the input data.
	 * 
	 * @param discovery
	 * @return store description
	 */
	public static IStoreDescription createStoreDescription(CSVDiscoveryResult discovery) {

		List<IFieldDescription> fields = new ArrayList<>();
		List<IOptimizationDescription> optimizations = new ArrayList<>();

		for(int c = 0; c < discovery.getColumnCount(); c++) {
			String columnName = discovery.getColumnName(c);
			String columnType = discovery.getColumnType(c);
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
			// as ActivePivot fields.
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
	
	
	public static IActivePivotDescription createActivePivotDescription(CSVDiscoveryResult discovery) {
		
		ActivePivotDescription desc = new ActivePivotDescription();
		
		// Hierarchies and dimensions
		AxisDimensionsDescription dimensions = new AxisDimensionsDescription();
		
		Set<String> numericsOnly = QfsArrays.mutableSet("double", "float", "long");
		for(int f = 0; f < discovery.getColumnCount(); f++) {
			String fieldName = discovery.getColumnName(f);
			String fieldType = discovery.getColumnType(f);
			

			if(!numericsOnly.contains(fieldType)) {
				AxisDimensionDescription dimension = new AxisDimensionDescription(fieldName);
				dimensions.addValues(Arrays.asList(dimension));
				
				// For date fields generate the YEAR-MONTH-DAY hierarchy
				if(fieldType.startsWith("DATE")) {
					List<IAxisHierarchyDescription> hierarchies = new ArrayList<>();
					IAxisHierarchyDescription hierarchy = new AxisHierarchyDescription(fieldName);
					hierarchy.setDefaultHierarchy(true);
					hierarchy.setLevels(Arrays.asList(new AxisLevelDescription(fieldName)));
					hierarchies.add(hierarchy);
					
					IAxisHierarchyDescription ymd = new AxisHierarchyDescription("Year-Month-Day");
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
		for(int f = 0; f < discovery.getColumnCount(); f++) {
			String fieldName = discovery.getColumnName(f);
			String fieldType = discovery.getColumnType(f);
			if(numerics.contains(fieldType)) {
				AggregatedMeasureDescription sum = new AggregatedMeasureDescription(fieldName, "SUM");
				AggregatedMeasureDescription avg = new AggregatedMeasureDescription(fieldName, "AVG");
				AggregatedMeasureDescription min = new AggregatedMeasureDescription(fieldName, "MIN");
				AggregatedMeasureDescription max = new AggregatedMeasureDescription(fieldName, "MAX");
				sum.setFolder(fieldName);
				avg.setFolder(fieldName);
				min.setFolder(fieldName);
				max.setFolder(fieldName);
				measures.add(sum);
				measures.add(avg);
				measures.add(min);
				measures.add(max);
			}
		}
		measureDesc.setAggregatedMeasuresDescription(measures);
		desc.setMeasuresDescription(measureDesc);
		
		return desc;
	}
	
	/**
	 * 
	 * @param storeDesc
	 * @return schema description
	 */
	public static IActivePivotSchemaDescription createActivePivotSchemaDescription(CSVDiscoveryResult discovery) {
		ActivePivotSchemaDescription desc = new ActivePivotSchemaDescription();

		// Datastore selection
		List<ISelectionField> fields = new ArrayList<>();
		for(int f = 0; f < discovery.getColumnCount(); f++) {
			String fieldName = discovery.getColumnName(f);
			String fieldType = discovery.getColumnType(f);
			fields.add(new SelectionField(fieldName));
			
			if(fieldType.startsWith("DATE")) {
				fields.add(new SelectionField(fieldName + ".YEAR"));
				fields.add(new SelectionField(fieldName + ".MONTH"));
				fields.add(new SelectionField(fieldName + ".DAY"));
			}
		}
		SelectionDescription selection = new SelectionDescription(BASE_STORE, fields);
		
		// ActivePivot instance
		IActivePivotDescription pivot = createActivePivotDescription(discovery);
		IActivePivotInstanceDescription instance = new ActivePivotInstanceDescription(PIVOT, pivot);
		
		desc.setDatastoreSelection(selection);
		desc.setActivePivotInstanceDescriptions(Collections.singletonList(instance));
		
		return desc;
	}
	
}
