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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.iot.unide.ppmp.commons.Device;
import org.eclipse.iot.unide.ppmp.measurements.Measurements;
import org.eclipse.iot.unide.ppmp.measurements.Measurements.Result;
import org.eclipse.iot.unide.ppmp.measurements.MeasurementsWrapper;
import org.eclipse.iot.unide.ppmp.measurements.SeriesMap;
import org.eclipse.kura.type.BooleanValue;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.DoubleValue;
import org.eclipse.kura.type.FloatValue;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.kura.type.LongValue;
import org.eclipse.kura.type.TypedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KuraPpmpMeasurement {
	
	private static final Logger logger = LoggerFactory.getLogger(KuraPpmpMeasurement.class);
	
	private static final String TOPIC_PATTERN_STRING = "\\$([^\\s/]+)";
    private static final Pattern TOPIC_PATTERN = Pattern.compile(TOPIC_PATTERN_STRING);
    private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
	
	// Device metrics
	private String deviceId;
	private String operationalStatus;
	private Map<String, String> deviceMetaData;
	
	// Part metrics
	private String partTypeId;
	private String partId;
	private Result partResult;
	private String partCode;
	private Map<String, String> partMetaData;
	
	// Series metrics
	private List<KuraPpmpSeries> seriesValues;
	
	// Limits metrics
	private List<KuraPpmpLimits> limitsValues;

	/*
	 * This method attempts to update values based on data
	 * received in the WireRecord.
	 * 
	 */
	public void updateValue(Map<String, TypedValue<?>> properties) {
		//TODO: Update all values, not just series data
		for (KuraPpmpSeries s : this.seriesValues) {
			Set<String> seriesNames = s.getSeriesNames();
			s.setIntervalAdded(false);
			
			seriesNames.forEach(seriesName -> {
				Matcher matcher = TOPIC_PATTERN.matcher(seriesName);

				while (matcher.find()) {
					if (matcher.group(0).equals(KuraPpmpSeries.DISCRETE_TIME_KEY))
						continue;
					
					String regexKey = matcher.group(0);
					String key = matcher.group(1);
					logger.info("Key: {}", key);
					if (properties.containsKey(key)) {
						properties.get(key);
						s.addSeriesValue(regexKey, getTypedValue(properties.get(key).getType(), properties.get(key)));
						
						if (s.getTimestamp() == 0L) {
							s.setTimestamp(((LongValue) properties.get(key + "_timestamp")).getValue());
							s.addSeriesTime(0);
							s.setIntervalAdded(true);
						}
						else {
							long interval = System.currentTimeMillis() - s.getTimestamp();
							s.addSeriesTime((int) interval);
							s.setIntervalAdded(true);
						}
					}
				}
			});
		}
	}
	
	public void resetSeriesValues() {
		this.seriesValues.forEach(series->series.resetSeriesMap());
	}
	
	public MeasurementsWrapper getPpmpMeasurementWrapper() {
		MeasurementsWrapper wrapper = new MeasurementsWrapper();
		SeriesMap seriesMap = new SeriesMap();
		List<Measurements> measurementsList = new ArrayList<Measurements>();
		wrapper.setDevice(createDevice());
		
		for (KuraPpmpSeries series : this.seriesValues) {
			Measurements measurements = new Measurements();
			measurements.setTimestamp(OffsetDateTime.parse(df.format(series.getTimestamp())));
			
			seriesMap = new SeriesMap();
			seriesMap.setSeries(series.getSeriesMap());
			measurements.setSeriesMap(seriesMap);
			
			measurementsList.add(measurements);
		}
		
		wrapper.setMeasurements(measurementsList);
		
		return wrapper;
	}
	
	public String getOperationalStatus() {
		return this.operationalStatus;
	}

	public void setOperationalStatus(String operationalStatus) {
		this.operationalStatus = operationalStatus;
	}

	public Map<String, String> getDeviceMetaData() {
		return this.deviceMetaData;
	}

	public void setDeviceMetaData(Map<String, String> deviceMetaData) {
		this.deviceMetaData = deviceMetaData;
	}

	public String getPartTypeId() {
		return this.partTypeId;
	}

	public void setPartTypeId(String partTypeId) {
		this.partTypeId = partTypeId;
	}

	public String getPartId() {
		return this.partId;
	}

	public void setPartId(String partId) {
		this.partId = partId;
	}

	public Result getPartResult() {
		return this.partResult;
	}

	public void setPartResult(Result partResult) {
		this.partResult = partResult;
	}

	public String getPartCode() {
		return this.partCode;
	}

	public void setPartCode(String partCode) {
		this.partCode = partCode;
	}

	public Map<String, String> getPartMetaData() {
		return this.partMetaData;
	}

	public void setPartMetaData(Map<String, String> partMetaData) {
		this.partMetaData = partMetaData;
	}

	public List<KuraPpmpSeries> getSeriesValues() {
		return this.seriesValues;
	}

	public void addSeries(List<String> namesList) {
		if (this.seriesValues == null) {
			this.seriesValues = new ArrayList<KuraPpmpSeries>();
		}
		this.seriesValues.add(new KuraPpmpSeries(namesList));
	}

	public List<KuraPpmpLimits> getLimitsValues() {
		return this.limitsValues;
	}

	public void setLimitsValues(List<KuraPpmpLimits> limitsValues) {
		this.limitsValues = limitsValues;
	}

	public String getDeviceId() {
		return this.deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	
	private Device createDevice() {
		Device device = new Device();
		device.setDeviceID(this.deviceId);
		
		return device;
	}
	
	private Number getTypedValue(DataType dataType, TypedValue<?> value) {
		Number n;
		
		switch (dataType) {
        case FLOAT:
            n = ((FloatValue) value).getValue();
            break;
        case DOUBLE:
            n = ((DoubleValue) value).getValue();
            break;
        case INTEGER:
            n = ((IntegerValue) value).getValue();
            break;
        case LONG:
            n = ((LongValue) value).getValue();
            break;
        default:
        	    n = 0;
		}
		
		return n;	
	}

}
