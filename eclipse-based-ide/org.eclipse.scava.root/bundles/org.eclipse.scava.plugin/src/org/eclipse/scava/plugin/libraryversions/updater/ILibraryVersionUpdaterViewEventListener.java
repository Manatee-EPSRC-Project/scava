/*********************************************************************
* Copyright c 2017 FrontEndART Software Ltd.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse PublicLicense 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/

package org.eclipse.scava.plugin.libraryversions.updater;

import java.util.Map;

import org.eclipse.scava.plugin.libraryversions.Library;
import org.eclipse.scava.plugin.mvc.view.IViewEventListener;

public interface ILibraryVersionUpdaterViewEventListener extends IViewEventListener {
	void onInstall(Map<Library, Library> libraries, String jarPth);
}
