package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.wire.WireUtils;
import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtWireConfig;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.wires.WireEmitter;
import org.eclipse.kura.wires.WireReceiver;
import org.eclipse.kura.wires.WireService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtWireServiceImpl extends OsgiRemoteServiceServlet implements GwtWireService {

	private static final long serialVersionUID = 388917916072453894L;

	private static final Logger s_logger = LoggerFactory.getLogger(GwtWireServiceImpl.class);
	
	private static final String PROP_MULTITON_INSTANCE_NAME = "multiton.instance.name";
	private static final String PROP_SERVICE_FACTORY_PID 	= "service.factoryPid";
	private static final String WIRE_SERVICE_PID 			= "org.eclipse.kura.core.wire.WireServiceImpl";
			
	@Override
	public void updateWireConfiguration(GwtXSRFToken xsrfToken, GwtWireConfig wireConfig) throws GwtKuraException {
		// TODO Auto-generated method stub

	}
	
	@Override
	public GwtWireConfig getWireConfiguration(GwtXSRFToken xsrfToken) throws GwtKuraException {
		checkXSRFToken(xsrfToken);
		
		ConfigurationService configService= ServiceLocator.getInstance().getService(ConfigurationService.class);
		
		String sWires = null;
		String sGraph = null;
		
		//
		// Get wires and graph JSON from WireService
		try {
			Map<String, Object> wsProps = configService.getComponentConfiguration(WIRE_SERVICE_PID).getConfigurationProperties();
			sWires = (String) wsProps.get("wires");
			sGraph = (String) wsProps.get("graph");
			s_logger.info(sGraph);
			
			// Update PIDs of instances in graph as they may have changed on framework restart
			if (!sGraph.equals("{}")) {
				
				JSONObject jGraph = new JSONObject(sGraph);
				JSONArray jCells = jGraph.getJSONArray("cells");
				for (int i = 0; i < jCells.length(); i++) {
					if (jCells.getJSONObject(i).getString("type").equals("html.Element")) {
						String oldPid = jCells.getJSONObject(i).getString("pid");
						String newPid = configService.getCurrentComponentPid(oldPid);
						s_logger.info("Updating old PID: " + jCells.getJSONObject(i).getString("pid") + " with new PID: " + newPid);
						jCells.getJSONObject(i).put("pid", newPid);
					}
				}
				
				jGraph.put("cells", jCells);
				sGraph = jGraph.toString();
			}
			
		} catch (KuraException e) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		return createConfig(sWires, sGraph);

	}

	
	@Override
	public String createConfigComponent(GwtXSRFToken xsrfToken, String factoryPid, String instanceName) throws GwtKuraException {
		checkXSRFToken(xsrfToken);
		String pid = null;
		
		ConfigurationService configService= ServiceLocator.getInstance().getService(ConfigurationService.class);
		try {
			pid = configService.newConfigurableComponent(factoryPid, null, true, instanceName);
		} catch (KuraException e) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
		}
		
		return pid;
	}
	
	@Override
	public GwtWireConfig updateWireConfiguration(GwtXSRFToken xsrfToken, String jsonObj) throws GwtKuraException {
		checkXSRFToken(xsrfToken);
		
		JSONObject jObj = null;		// JSON object passed from wires JS
		JSONObject jGraph = null;	// JSON object representing JointJS graph
		JSONArray jCells = null;	// JSON array of cells within JointJS graph
		JSONArray jDelCells = null;	// JSON array of cells to be deleted
		String sWires = null;		// String representation of Kura Wires
		String sGraph = null;		// String representation of JointJS graph
		
		Map<String, String> idToPid = new HashMap<String, String>();
		
		ConfigurationService configService= ServiceLocator.getInstance().getService(ConfigurationService.class);
		WireService wireService = ServiceLocator.getInstance().getService(WireService.class);
		
		try {
			jObj = new JSONObject(jsonObj);
			jDelCells = jObj.getJSONArray("deleteCells");
			jGraph = jObj.getJSONObject("jointJs");
			jCells = jGraph.getJSONArray("cells");
			
			// Create new component instances
			for (int i = 0; i < jCells.length(); i++) {
				
				if (jCells.getJSONObject(i).getString("type").equals("html.Element")) {
					
					if (jCells.getJSONObject(i).getString("pid").equals("none")) {
						s_logger.info("Creating new component: Factory PID -> " + jCells.getJSONObject(i).getString("factoryPid") + " | Instance Name -> " + jCells.getJSONObject(i).getString("label"));
						String pid = configService.newConfigurableComponent(jCells.getJSONObject(i).getString("factoryPid"), null, false, jCells.getJSONObject(i).getString("label"));
						jCells.getJSONObject(i).put("pid", pid);
						idToPid.put(jCells.getJSONObject(i).getString("id"), jCells.getJSONObject(i).getString("pid"));
					}
					else {
						idToPid.put(jCells.getJSONObject(i).getString("id"), jCells.getJSONObject(i).getString("pid"));
					}
				}
			}
			
			jGraph.put("cells", jCells);
			
			// Create new wires
			for (int i = 0; i < jCells.length(); i++) {
				
				if (jCells.getJSONObject(i).getString("type").equals("customLink.Element") && jCells.getJSONObject(i).getBoolean("newWire")) {
					
					String prod = idToPid.get(jCells.getJSONObject(i).getString("producer"));
					String cons = idToPid.get(jCells.getJSONObject(i).getString("consumer"));
					s_logger.info("Creating new wire: Producer PID -> " + prod + " | Consumer PID -> " + cons);
					wireService.createWire(prod, cons);
					jCells.getJSONObject(i).put("newWire", false);
				}
			}
			
			// Delete wires
			for (int i = 0; i < jDelCells.length(); i++) {
				if (jDelCells.getJSONObject(i).getString("cellType").equals("wire")) {
					String prod = idToPid.get(jDelCells.getJSONObject(i).getString("p"));
					String cons = idToPid.get(jDelCells.getJSONObject(i).getString("c"));
					s_logger.info("Deleting wire: Producer PID -> " + prod + " | Consumer PID -> " + cons);
					wireService.removeWire(prod, cons);
				}
			}
			// Delete instances
			for (int i = 0; i < jDelCells.length(); i++) {
				if (jDelCells.getJSONObject(i).getString("cellType").equals("instance")) {
					s_logger.info("Deleting instance: PID -> " + jDelCells.getJSONObject(i).getString("pid"));
					configService.deleteConfigurableComponent(jDelCells.getJSONObject(i).getString("pid"));
				}
			}
			
			Map<String, Object> props = configService.getComponentConfiguration(WIRE_SERVICE_PID).getConfigurationProperties();
			props.put("graph", jGraph.toString());
			configService.updateConfiguration(WIRE_SERVICE_PID, props, true);
			
			sWires = (String) props.get("wires");
			sGraph = jGraph.toString();

		} catch (JSONException e) {
			s_logger.info(e.getLocalizedMessage());
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
		} catch (KuraException e) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
		} 
		
		return createConfig(sWires, sGraph);
		
	}
	

	@Override
	public void deleteConfigComponent(GwtXSRFToken xsrfToken, String pid) throws GwtKuraException {
		checkXSRFToken(xsrfToken);
		
		ConfigurationService configService= ServiceLocator.getInstance().getService(ConfigurationService.class);
		try {
			configService.deleteConfigurableComponent(pid);
		} catch (KuraException e) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
		}
	}
	

	@Override
	public void createWire(GwtXSRFToken xsrfToken, String emitterPid, String receiverPid) throws GwtKuraException {
		checkXSRFToken(xsrfToken);
		
		WireService wireService = ServiceLocator.getInstance().getService(WireService.class);
		wireService.createWire(emitterPid, receiverPid);
	}
	

	@Override
	public void deleteWire(GwtXSRFToken xsrfToken, String emitterPid, String receiverPid) throws GwtKuraException {
		checkXSRFToken(xsrfToken);
		
		WireService wireService = ServiceLocator.getInstance().getService(WireService.class);
		wireService.removeWire(emitterPid, receiverPid);
	}
	
	private GwtWireConfig createConfig(String wires, String graph) throws GwtKuraException {
		GwtWireConfig config = new GwtWireConfig();
		
		ConfigurationService configService= ServiceLocator.getInstance().getService(ConfigurationService.class);
		
		//
		// Get all Factory PIDs and Instances
		Set<String> factoryPids = configService.getComponentFactoryPids();
		List<String> emitters = new ArrayList<String>();
		List<String> receivers = new ArrayList<String>();
		List<String> components = new ArrayList<String>();
		
		for (String factoryPid : factoryPids) {
			
			// Get Emitter Factories and Instances
			List<String> tmpEmitters = WireUtils.getFactoriesAndInstances(Console.getBundleContext(), factoryPid, WireEmitter.class);
			
			for (String emitter : tmpEmitters) {
				
				String[] tokens = emitter.split("\\|");
				// Add Factories
				if (tokens[0].equals("FACTORY")) {
					emitters.add(tokens[1]);
				}
				// If Instance, get name of Instance and add
				else {
					ComponentConfiguration compConfig;
					try {
						compConfig = configService.getComponentConfiguration(tokens[1]);
						String multInstanceName = (String) compConfig.getConfigurationProperties().get(PROP_MULTITON_INSTANCE_NAME);
						String serviceFactPid = (String) compConfig.getConfigurationProperties().get(PROP_SERVICE_FACTORY_PID);
						if (!components.contains(serviceFactPid + "|" + tokens[1] + "|" + multInstanceName + "|producer") && !components.contains(serviceFactPid + "|" + tokens[1] + "|" + multInstanceName + "|both"))
							components.add(serviceFactPid + "|" + tokens[1] + "|" + multInstanceName + "|producer");
						
					} catch (KuraException e) {
						throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
					}
					
				}
			}
			
			// Get Receiver Factories and Instances
			List<String> tmpReceivers = WireUtils.getFactoriesAndInstances(Console.getBundleContext(), factoryPid, WireReceiver.class);
			for (String receiver : tmpReceivers) {
				String[] tokens = receiver.split("\\|");
				// Add Factories
				if (tokens[0].equals("FACTORY")) {
					receivers.add(tokens[1]);
				}
				// If Instance, get name of Instance and add
				else {
					ComponentConfiguration compConfig;
					try {
						compConfig = configService.getComponentConfiguration(tokens[1]);

						String multInstanceName = (String) compConfig.getConfigurationProperties().get(PROP_MULTITON_INSTANCE_NAME);
						String serviceFactPid = (String) compConfig.getConfigurationProperties().get(PROP_SERVICE_FACTORY_PID);
						// If instance already in list as emitter, update to indicate it can be emitter or receiver
						int index = components.indexOf(tokens[1] + "|" + multInstanceName + "|producer");
						if (index > -1) {
							if (!components.contains(serviceFactPid + "|" + tokens[1] + "|" + multInstanceName + "|both"))
								components.set(index, tokens[1] + "|" + multInstanceName + "|both");
						}
						else {
							if (!components.contains(serviceFactPid + "|" + tokens[1] + "|" + multInstanceName + "|consumer") && !components.contains(serviceFactPid + "|" + tokens[1] + "|" + multInstanceName + "|both"))
								components.add(serviceFactPid + "|" + tokens[1] + "|" + multInstanceName + "|consumer");
						}
					} catch (KuraException e) {
						throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
					}
					
				}
			}
			
		}

		config.setWireOptions(wires);
		config.setGraph(graph);
		config.setWireEmitterPids(emitters);
		config.setWireReceiverPids(receivers);
		config.setComponents(components);
		return config;
	}

}
