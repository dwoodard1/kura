/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.wire.ppmp.component.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.iot.unide.ppmp.measurements.Measurements.Result;

public class KuraPpmpSeries {
	
	public static final String DISCRETE_TIME_KEY = "$_time";

	// Series metrics
	private long ts;
	private Result measurementResult;
	private String measurementCode;
	private Map<String, List<Number>> seriesMap;
	
	private boolean intervalAdded = false;
	
	public KuraPpmpSeries(List<String> nameList) {
		this.seriesMap = new HashMap<String, List<Number>>();
		this.seriesMap.put(DISCRETE_TIME_KEY, new ArrayList<Number>());
		
		nameList.forEach(name->this.seriesMap.put(name, new ArrayList<Number>()));
		this.ts = 0L;
	}
	
	public void resetSeriesMap() {
		this.seriesMap.keySet().forEach(name->{
			this.seriesMap.put(name, new ArrayList<Number>());
		});
		this.ts = 0L;
	}
	
	public void addSeriesValue(String name, Number value) {
		this.seriesMap.get(name).add(value);
	}
	
	public void addSeriesTime(int time) {
		if (!this.intervalAdded)
			this.seriesMap.get(DISCRETE_TIME_KEY).add(time);
	}
	
	public Set<String> getSeriesNames() {
		return this.seriesMap.keySet();
	}
	
	public Map<String, List<Number>> getSeriesMap() {
		return this.seriesMap;
	}
	
	public long getTimestamp() {
		return this.ts;
	}
	
	public void setTimestamp(long ts) {
		this.ts = ts;
	}
	
	public void setIntervalAdded(Boolean intervalAdded) {
		this.intervalAdded = intervalAdded;
	}
	
	public Result getMeasurementResult() {
		return this.getMeasurementResult();
	}
	
	public void setMeasurementResult(Result result) {
		this.measurementResult = result;
	}
	
	public String getMeasurementCode() {
		return this.getMeasurementCode();
	}
	
	public void setMeasurementCode(String measurementCode) {
		this.measurementCode = measurementCode;
	}
}
