/*********************************************************************
* Copyright c 2017 FrontEndART Software Ltd.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse PublicLicense 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/

package org.eclipse.scava.plugin.mvc.exampleimplementation;

import java.util.Random;

import org.eclipse.scava.plugin.mvc.controller.Controller;
import org.eclipse.scava.plugin.mvc.event.routed.RoutedEvent;

public class RandomNumberEvent extends RoutedEvent {

	private final int number;

	public RandomNumberEvent(Controller source) {
		super(source);

		number = new Random().nextInt(444444);
	}

	public int getNumber() {
		return number;
	}

}
