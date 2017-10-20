/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.wire.ppmp.component.localization;

import com.github.rodionmoiseev.c10n.annotations.En;

public interface PpmpMessages {

	@En("Activating PPMP Component.....")
    public String activating();
	
	@En("Activating PPMP Component.....Done")
    public String activatingDone();
	
	@En("Deactivating PPMP Component.....")
    public String deactivating();

	@En("Deactivating PPMP Component.....Done")
    public String deactivatingDone();

	@En("Updating PPMP Component.....")
    public String updating();

	@En("Updating PPMP Component.....Done")
    public String updatingDone();
	
	@En("Wire Records cannot be null")
	public String wireRecordsNonNull();
	
	@En("Error parsing JSON config")
	public String errorParsingConfig();
	
	@En("Error converting PPMP message to JSON")
	public String errorPpmpToJson();
	
	@En("Error issuing REST call")
	public String errorPpmpRest();
}
