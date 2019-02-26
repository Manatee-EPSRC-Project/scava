/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.scava.crossflow.elkgraph.web

import io.typefox.sprotty.server.xtext.DefaultDiagramModule
import io.typefox.sprotty.server.xtext.IDiagramGenerator

/**
 * Guice bindings for the ELK diagram server.
 */
class ElkGraphDiagramModule extends DefaultDiagramModule {
	
	override bindILanguageServerExtension() {
		ElkGraphLanguageServerExtension
	}
	
	override bindIDiagramServerProvider() {
		ElkGraphLanguageServerExtension
	}
	
	def Class<? extends IDiagramGenerator> bindIDiagramGenerator() {
		ElkGraphDiagramGenerator
	}
	
}