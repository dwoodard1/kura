package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.web.client.util.GwtSafeHtmlUtils;

public class GwtWireConfig extends GwtBaseModel implements Serializable {

	private static final long serialVersionUID = 3242500082719035361L;
	
	private List<String> m_wireEmitterPids;
	private List<String> m_wireReceiverPids;

	public GwtWireConfig() {
		m_wireEmitterPids = new ArrayList<String>();
		m_wireReceiverPids = new ArrayList<String>();
	}
	
	@Override
	public void set(String name, Object value){
		if (value instanceof String) {
			value = (Object) GwtSafeHtmlUtils.htmlEscape((String) value);
		}
		super.set(name, value);
	}

	public String getWireOptions() {
		return get("wires");
	}
	
	public void setWireOptions(String wires) {
		set("wires", wires);
	}
	
	public List<String> getWireEmitterPids() {
		return m_wireEmitterPids;
	}
	
	public void setWireEmitterPids(List<String> wireEmitterPids) {
		m_wireEmitterPids = wireEmitterPids;
	}
	
	public List<String> getWireReceiverPids() {
		return m_wireReceiverPids;
	}
	
	public void setWireReceiverPids(List<String> wireReceiverPids) {
		m_wireReceiverPids = wireReceiverPids;
	}

}
