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
}
