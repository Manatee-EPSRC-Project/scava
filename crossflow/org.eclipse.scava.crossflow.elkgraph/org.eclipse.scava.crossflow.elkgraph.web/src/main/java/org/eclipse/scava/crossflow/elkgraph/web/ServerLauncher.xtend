/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.scava.crossflow.elkgraph.web

import com.google.inject.Guice
import com.google.inject.Inject
import com.google.inject.Provider
import io.typefox.sprotty.server.xtext.websocket.LanguageServerEndpoint
import java.net.InetSocketAddress
import javax.websocket.Endpoint
import javax.websocket.server.ServerEndpointConfig
import org.eclipse.elk.alg.force.options.ForceMetaDataProvider
import org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider
import org.eclipse.elk.alg.mrtree.options.MrTreeMetaDataProvider
import org.eclipse.elk.alg.radial.options.RadialMetaDataProvider
import org.eclipse.elk.alg.force.options.StressMetaDataProvider
import org.eclipse.elk.alg.common.compaction.options.PolyominoOptions
import org.eclipse.elk.alg.disco.options.DisCoMetaDataProvider
import org.eclipse.elk.alg.spore.options.SporeMetaDataProvider
import org.eclipse.elk.core.data.LayoutMetaDataService
import org.eclipse.elk.graph.ElkGraphPackage
import org.eclipse.elk.graph.text.ElkGraphRuntimeModule
import org.eclipse.elk.graph.text.ide.ElkGraphIdeModule
import org.eclipse.elk.graph.text.ide.ElkGraphIdeSetup
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.log.Slf4jLog
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer
import org.eclipse.xtext.ide.server.ServerModule
import org.eclipse.xtext.resource.IResourceServiceProvider
import org.eclipse.xtext.util.Modules2

/**
 * Main class for launching the ELK Graph server.
 */
class ServerLauncher {
	
	def static void main(String[] args) {
		val injector = Guice.createInjector(Modules2.mixin(new ServerModule, [
			bind(Endpoint).to(LanguageServerEndpoint)
			bind(IResourceServiceProvider.Registry).toProvider(IResourceServiceProvider.Registry.RegistryProvider)
		]))
		val launcher = injector.getInstance(ServerLauncher)
		launcher.initialize()
		val rootPath = if (args.length >= 1) args.get(0) else '../..'
		launcher.start(rootPath)
	}

	@Inject Provider<Endpoint> endpointProvider
	
	def void initialize() {
		// Initialize ELK meta data
		LayoutMetaDataService.instance.registerLayoutMetaDataProviders(
			new ForceMetaDataProvider,
			new LayeredMetaDataProvider,
			new MrTreeMetaDataProvider,
			new RadialMetaDataProvider,
			new StressMetaDataProvider,
			new PolyominoOptions, 
			new DisCoMetaDataProvider,
			new SporeMetaDataProvider
		)
		
		// Initialize the ELK Graph Xtext language
		ElkGraphPackage.eINSTANCE.getNsURI
		new ElkGraphIdeSetup {
			override createInjector() {
				Guice.createInjector(Modules2.mixin(new ElkGraphRuntimeModule, new ElkGraphIdeModule, new ElkGraphDiagramModule))
			}
		}.createInjectorAndDoEMFRegistration()
	}
	
	def void start(String rootPath) {
		val log = new Slf4jLog(ServerLauncher.name)
		
		// Set up Jetty server
		val server = new Server(new InetSocketAddress(9090))
		val webAppContext = new WebAppContext => [
			resourceBase = rootPath + '/client/app'
			log.info('Serving client app from ' + resourceBase)
			welcomeFiles = #['index.html']
			setInitParameter('org.eclipse.jetty.servlet.Default.dirAllowed', 'false')
			setInitParameter('org.eclipse.jetty.servlet.Default.useFileMappedBuffer', 'false')
		]
		server.handler = webAppContext
		
		// Configure web socket
		val container = WebSocketServerContainerInitializer.configureContext(webAppContext)
		val endpointConfigBuilder = ServerEndpointConfig.Builder.create(LanguageServerEndpoint, '/elkgraph')
		endpointConfigBuilder.configurator(new ServerEndpointConfig.Configurator {
			override <T> getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
				endpointProvider.get as T
			}
		})
		container.addEndpoint(endpointConfigBuilder.build())
		
		// Start the server
		try {
			server.start()
			log.info('Sever successfully started...')
			server.join()
		} catch (Exception exception) {
			log.warn('Shutting down due to exception', exception)
			System.exit(1)
		}
	}
	
}
