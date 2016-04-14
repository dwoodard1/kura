package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.util.GwtSafeHtmlUtils;

public class GwtWireConfig extends GwtBaseModel implements Serializable {

	private static final long serialVersionUID = 3242500082719035361L;
	
	private List<String> m_emitterFactoryPids;
	private List<String> m_receiverFactoryPids;
	private List<String> m_components;
	private String       m_wireOptions;
	private String       m_graph;

	public GwtWireConfig() {
		m_emitterFactoryPids = new ArrayList<String>();
		m_receiverFactoryPids = new ArrayList<String>();
		m_components = new ArrayList<String>();
	}
	
	@Override
	public void set(String name, Object value){
		if (value instanceof String) {
			value = (Object) GwtSafeHtmlUtils.htmlEscape((String) value);
		}
		super.set(name, value);
	}

	public String getWireOptions() {
		return m_wireOptions;
	}
	
	public void setWireOptions(String wires) {
		m_wireOptions = wires;
	}
	
	public String getGraph() {
		return m_graph;
	}
	
	public void setGraph(String graph) {
		m_graph = graph;
	}
	
	public List<String> getEmitterFactoryPids() {
		return m_emitterFactoryPids;
	}
	
	public void setWireEmitterPids(List<String> emitterFactoryPids) {
		m_emitterFactoryPids = emitterFactoryPids;
	}
	
	public List<String> getReceiverFactoryPids() {
		return m_receiverFactoryPids;
	}
	
	public void setWireReceiverPids(List<String> receiverFactoryPids) {
		m_receiverFactoryPids = receiverFactoryPids;
	}
	
	public List<String> getComponents() {
		return m_components;
	}
	
	public void setComponents(List<String> components) {
		m_components = components;
	}

}
