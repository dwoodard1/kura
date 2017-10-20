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

import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.iot.unide.ppmp.PPMPPackager;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.eclipse.kura.wire.ppmp.component.localization.PpmpMessages;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;

public class PpmpProvider implements WireEmitter, WireReceiver, ConfigurableComponent {
	
	private static final Logger logger = LoggerFactory.getLogger(PpmpProvider.class);
	private static final PpmpMessages messages = LocalizationAdapter.adapt(PpmpMessages.class);
    
	private static final String PPMP_CONFIG_KEY = "ppmp.config";
	private static final String PPMP_POLL_INTERVAL_KEY = "ppmp.poll.interval";
	private static final String PPMP_REST_MEASUREMENT_URL_KEY = "ppmp.rest.measurement.url";
	
	private WireHelperService wireHelperService;
	private WireSupport wireSupport;
	
	private JsonObject ppmpConfig;
	private List<KuraPpmpMeasurement> measurements;
	
	private int pollInterval = 5;
	private int pollCount;
	private String restMeasurementUrl;
	
	public void bindWireHelperService(WireHelperService wireHelperService) {
		if (this.wireHelperService == null) {
			this.wireHelperService = wireHelperService;
		}
	}
	
	public void unbindWireHelperService(WireHelperService wireHelperService) {
		if (this.wireHelperService == wireHelperService) {
			this.wireHelperService = null;
		}
	}
	
	public void activate(Map<String, Object> properties) {
		logger.info(messages.activating());
		this.wireSupport = this.wireHelperService.newWireSupport(this);
		
		updated(properties);
		
		logger.info(messages.activatingDone());
	}
	
	public void deactivate() {
		logger.info(messages.deactivating());
		logger.info(messages.deactivatingDone());
	}
	
	public void updated(Map<String, Object> properties) {
		logger.info(messages.updating());
		
		this.ppmpConfig = Json.parse((String) properties.get(PPMP_CONFIG_KEY)).asObject();
		this.pollCount = (int) properties.get(PPMP_POLL_INTERVAL_KEY);
		this.restMeasurementUrl = (String) properties.get(PPMP_REST_MEASUREMENT_URL_KEY);
		this.measurements = new ArrayList<KuraPpmpMeasurement>();
		parsePpmpConfig();

		logger.info(messages.updatingDone());
	}
	
	@Override
	public void onWireReceive(WireEnvelope wireEnvelope) {
		final List<WireRecord> wireRecords = wireEnvelope.getRecords();
		requireNonNull(wireRecords, messages.wireRecordsNonNull());

		for (WireRecord dataRecord : wireRecords) {
			updatePpmp(dataRecord);
		}
		if (this.pollCount >= this.pollInterval) {
			publishPpmpRecords();
			this.measurements.forEach(measurement->measurement.resetSeriesValues());
			this.pollCount = 1;
		}
		else {
			this.pollCount++;
		}
		
	}
	
	@Override
    public Object polled(Wire wire) {
        return this.wireSupport.polled(wire);
    }

    @Override
    public void consumersConnected(Wire[] wires) {
        this.wireSupport.consumersConnected(wires);
    }

	@Override
	public void updated(Wire wire, Object value) {
		this.wireSupport.updated(wire, value);
	}

	@Override
	public void producersConnected(Wire[] wires) {
		this.wireSupport.producersConnected(wires);
	}
	
	private void parsePpmpConfig() {

		try {
			// TODO: Parse other PPMP payloads (MachineMessage, ProcessMessage)
			JsonArray measurements = this.ppmpConfig.get("mm").asArray();
			JsonObject obj;
			
			for (JsonValue measurement : measurements) {
				KuraPpmpMeasurement tmpMeasurement = new KuraPpmpMeasurement();
				
				// Device info
				obj = measurement.asObject().get("device").asObject();
				tmpMeasurement.setDeviceId(obj.get("deviceId").asString());
				tmpMeasurement.setOperationalStatus(obj.getString("deviceStatus", "UNKNOWN"));
	
				// Part info
				obj = measurement.asObject().get("part").asObject();
				tmpMeasurement.setPartCode(obj.getString("partCode", "UNKNOWN"));
				tmpMeasurement.setPartId(obj.getString("partId", "UNKNOWN"));
				tmpMeasurement.setPartTypeId(obj.getString("partTypeId", "UNKNOWN"));
				
				// Measurement info
				obj = measurement.asObject().get("measurements").asObject();
				JsonArray jArray = obj.get("series").asArray();
				for (JsonValue series : jArray) {
					JsonArray seriesNames = series.asObject().get("seriesNames").asArray();
					List<String> namesList = new ArrayList<String>();
					for (JsonValue seriesName : seriesNames) {
						namesList.add(seriesName.asString());
					}
					tmpMeasurement.addSeries(namesList);
				}
				
				this.measurements.add(tmpMeasurement);
			}
		} catch (Exception e) {
			logger.error(messages.errorParsingConfig(), e);
		}
	}
	
	private void updatePpmp(WireRecord dataRecord) {
		for (KuraPpmpMeasurement m : this.measurements) {
			m.updateValue(dataRecord.getProperties());
		}
	}
	
	private void publishPpmpRecords() {
		for (KuraPpmpMeasurement m : this.measurements) {
			PPMPPackager packager = new PPMPPackager();

			String postBody;
			try {
				postBody = packager.getMessage(m.getPpmpMeasurementWrapper(), true);
				logger.debug(postBody);
				doRestMeasurementPublish(postBody);
			} catch (JsonProcessingException e) {
				logger.error(messages.errorPpmpToJson(), e);
			} 
		}
	}
	
	private void doRestMeasurementPublish(String postBody) {
		
		//TODO: Implement basic auth for REST calls
		Authenticator.setDefault (new Authenticator() {
		    protected PasswordAuthentication getPasswordAuthentication() {
		        return new PasswordAuthentication ("username", "password".toCharArray());
		    }
		});
		
		URL url;
		try {
			url = new URL(this.restMeasurementUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			
			OutputStream os = conn.getOutputStream();
			os.write(postBody.getBytes());
			os.flush();

			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

			String output;
			while ((output = br.readLine()) != null) {
				logger.debug(output);
			}

			conn.disconnect();
		} catch (IOException e) {
			logger.error(messages.errorPpmpRest(), e);
		}
		
	}

}
