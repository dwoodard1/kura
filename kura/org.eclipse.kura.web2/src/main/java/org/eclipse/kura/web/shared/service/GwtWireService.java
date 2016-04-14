package org.eclipse.kura.web.shared.service;

import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtSslConfig;
import org.eclipse.kura.web.shared.model.GwtWireConfig;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("wires")
public interface GwtWireService extends RemoteService {

	public void updateWireConfiguration(GwtXSRFToken xsrfToken, GwtWireConfig wireConfig) throws GwtKuraException;

	public GwtWireConfig getWireConfiguration(GwtXSRFToken xsrfToken) throws GwtKuraException;
	
	public String createConfigComponent(GwtXSRFToken xsrfToken, String factoryPid, String instanceName) throws GwtKuraException;
	
	public GwtWireConfig updateWireConfiguration(GwtXSRFToken xsrfToken, String jsonModels) throws GwtKuraException;
	
	public void deleteConfigComponent(GwtXSRFToken xsrfToken, String pid) throws GwtKuraException;
	
	public void createWire(GwtXSRFToken xsrfToken, String emitterPid, String receiverPid) throws GwtKuraException;
	
	public void deleteWire(GwtXSRFToken xsrfToken, String emitterPid, String receiverPid) throws GwtKuraException;
}
