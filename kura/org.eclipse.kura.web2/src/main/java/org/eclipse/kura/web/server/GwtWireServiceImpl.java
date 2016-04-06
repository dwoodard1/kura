package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.core.wire.WireUtils;
import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtWireConfig;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.wires.WireEmitter;
import org.eclipse.kura.wires.WireReceiver;
import org.eclipse.kura.wires.WireService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtWireServiceImpl extends OsgiRemoteServiceServlet implements GwtWireService {

	private static final long serialVersionUID = 388917916072453894L;

	private static final Logger s_logger = LoggerFactory.getLogger(GwtWireServiceImpl.class);
			
	@Override
	public void updateWireConfiguration(GwtXSRFToken xsrfToken, GwtWireConfig wireConfig) throws GwtKuraException {
		// TODO Auto-generated method stub

	}

	@Override
	public GwtWireConfig getWireConfiguration(GwtXSRFToken xsrfToken) throws GwtKuraException {
		checkXSRFToken(xsrfToken);
		
		WireService wireService = ServiceLocator.getInstance().getService(WireService.class);
		ConfigurationService configService= ServiceLocator.getInstance().getService(ConfigurationService.class);
		String options = wireService.getConfigurationOptions();
		GwtWireConfig config = new GwtWireConfig();
		config.setWireOptions(options);
;
		Set<String> factoryPids = configService.getComponentFactoryPids();
		List<String> emitters = new ArrayList<String>();
		List<String> receivers = new ArrayList<String>();
		
		for (String factoryPid : factoryPids) {
			emitters.addAll(WireUtils.getFactoriesAndInstances(Console.getBundleContext(), factoryPid, WireEmitter.class));
			receivers.addAll(WireUtils.getFactoriesAndInstances(Console.getBundleContext(), factoryPid, WireReceiver.class));
		}
		for (String test : emitters)
			s_logger.info("emitter: " + test);
		for (String test : receivers)
			s_logger.info("receiver: " + test);
		config.setWireEmitterPids(emitters);
		config.setWireReceiverPids(receivers);
		
		return config;

	}

}
