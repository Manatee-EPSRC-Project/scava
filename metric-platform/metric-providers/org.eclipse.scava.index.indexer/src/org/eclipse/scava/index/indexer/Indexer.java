/*******************************************************************************
 * Copyright (c) 2019 Edge Hill University
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.scava.index.indexer;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.eclipse.scava.platform.logging.OssmeterLogger;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;

public class Indexer {

	protected static OssmeterLogger logger;
	private static RestHighLevelClient highLevelClient;
	private static Pattern versionFinder;
	private static FetchSourceContext fetchSourceContext;

	static {
		IndexerSingleton singleton = IndexerSingleton.getInstance();
		highLevelClient = singleton.getHighLevelclient();
		logger = (OssmeterLogger) OssmeterLogger.getLogger("Indexer");
		versionFinder = Pattern.compile("\"mapping_version\"\\s*:\\s*\"([^\"]+)\"");
		fetchSourceContext = new FetchSourceContext(false);
	}

	/**
	 * Adds settings to index
	 * 
	 * @param index
	 *            - name of the index
	 * @param setting
	 *            - settings represented as a JSON String
	 * @throws IOException
	 */
	private static void addIndexSetting(String index, String setting) throws IOException {
		UpdateSettingsRequest updateSettingsRequest = new UpdateSettingsRequest();
		updateSettingsRequest.indices(index);
		updateSettingsRequest.settings(setting, XContentType.JSON);
		UpdateSettingsResponse updateSettingsResponse = highLevelClient.indices().putSettings(updateSettingsRequest,
				getWriteHeaders());
		if (updateSettingsResponse.isAcknowledged() == true) {
			System.out.println("[INDEXER] \tSettings have been added to " + index);
		}
	}
	
	

	/**
	 * Creates a new index
	 * 
	 * @param indexName
	 *            - name of the new index
	 * @throws IOException
	 */
	private static Boolean indexReady(String indexName, String documentType, MappingStorage mapping)  {
		
		// checks if index exists
		try {
			if (highLevelClient.indices().exists(new GetIndexRequest().indices(indexName), getReadHeaders())) {
				// do nothing already exists
				logger.info("A index for " + indexName + " already exists");
				if (mapping!=null) 
				{
					try {
						Response response = highLevelClient.getLowLevelClient().performRequest("GET", indexName+"/_mapping/"+documentType, getReadHeaders());
					
						Matcher m = versionFinder.matcher(EntityUtils.toString(response.getEntity()));
						if(m.find())
						{
							if(Float.valueOf(m.group(1))<mapping.getVersion())
								addMappingToIndex(indexName, documentType, mapping.getMapping());	
						
						}
					}
					catch (ResponseException e) {
						addMappingToIndex(indexName, documentType, mapping.getMapping());
					}
				}
				return true;

			} else {

				CreateIndexRequest request = new CreateIndexRequest(indexName);
				CreateIndexResponse createIndexResponse;

				try {
					createIndexResponse = highLevelClient.indices().create(request, getWriteHeaders());
					
					
					if (createIndexResponse.isAcknowledged() == true) {
						logger.info("The index " + indexName + " has been created");
						addMappingToIndex(indexName, documentType, mapping.getMapping());
						return true;
					}

				} catch (ElasticsearchStatusException e) {

					logger.error(indexName + "\tIndexResponse :" + e.getLocalizedMessage());

				} catch (IOException e) {

					logger.error("Issue whilst creating index", e);
				}
			}
			
			
		
		} catch(UnknownHostException e) {
			
			logger.error(e);
			
			
		} catch (IOException e) {
			
			logger.error("Issue whilst creating index", e);
		} 
		
		return false;
	}

	/**
	 * Adds a document map (mapping) to a specific index
	 * 
	 * @param index
	 *            - index name
	 * @param type
	 *            - doucment type
	 * @param mapping
	 *            - a JSON document represented as a string
	 */
	private static void addMappingToIndex(String indexName, String documentType, String mapping) {

		PutMappingRequest putMappingRequest = new PutMappingRequest(indexName.toLowerCase());
		putMappingRequest.source(mapping, XContentType.JSON).type(documentType.toLowerCase());
		PutMappingResponse putMappingResponse;

		try {

			putMappingResponse = highLevelClient.indices().putMapping(putMappingRequest, getWriteHeaders());

			if (putMappingResponse.isAcknowledged() == true) {

				logger.info("Mapping for " + documentType + " in " + indexName + " was added successfully");
			}

		} catch (IOException e) {

			logger.error(e);
		}
	}

	/**
	 * Returns a basic HTTP write headers
	 * 
	 * 
	 * @return headers
	 */
	private static Header[] getWriteHeaders() {

		Header[] headers = { new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"),
				new BasicHeader("Role", "Write")};
		return headers;
	}

	/**
	 * Returns a basic HTTP read headers
	 * 
	 * 
	 * @return headers
	 */
	private static Header[] getReadHeaders() {

		Header[] headers = { new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"),
				new BasicHeader("Role", "Read")};
		return headers;
	}

	/**
	 * @param reposiotry
	 *            - name of the repository. For example github, mantis
	 * @param documentType
	 *            - document type. For example article, bug, post, comment.
	 * @param knowledgeType
	 *            - type of knowledge. for example nlp
	 * @return indexName - generated name for the index
	 */
	public static String generateIndexName(String reposiotry, String documentType, String knowledgeType) {

		reposiotry = reposiotry.replaceAll(" ", "").trim().toLowerCase();
		documentType = documentType.replaceAll(" ", "").trim().toLowerCase();
		knowledgeType = knowledgeType.replace(" ", "").trim().toLowerCase();

		return reposiotry + "." + documentType + "." + knowledgeType;
	}

	/**
	 * Maps document Object to Json and creates and new index request
	 * 
	 * @param indexName
	 *            - index name
	 * @param documentType
	 *            - document type
	 * @param uid
	 *            - unique identifier
	 * @param object
	 *            - object that represents the structure of a document for indexing
	 * @return
	 * @return IndexResponse
	 * @throws IOException
	 */
	private static void index(String indexName, String documentType, String uid, String document) {

		try {
			
			GetRequest getRequest = new GetRequest(indexName, documentType, uid).fetchSourceContext(fetchSourceContext).storedFields("_none_");
			
			if(highLevelClient.exists(getRequest, getReadHeaders()))
			{
				UpdateRequest request = new UpdateRequest(indexName, documentType, uid);
				request.doc(document, XContentType.JSON);
				logger.info("Document (uid: " + uid + ") has been updated");
			}
			else
			{
				IndexRequest indexRequest = new IndexRequest();
				indexRequest .index(indexName).type(documentType).id(uid).source(document,XContentType.JSON);
					
				logger.info("Document (uid: " + uid + ") has been "	+
											highLevelClient.index(indexRequest, getWriteHeaders()).getResult().toString().toLowerCase());
			}
			
		} catch (IOException io) {
			logger.error("Method index has experienced an IO error\n" + io);
		}
	}

	/**
	 * Closes the HighLevel Rest Client's connection to the Elasticsearch index
	 * 
	 * @throws IOException
	 */
	private static void closeHighLevelClient() {

		try {
			highLevelClient.close();
			logger.info("High Level Client has been closed");
		} catch (IOException io) {

			logger.error(io);
		}
	}


	/**
	 * Closes all clients (HighLevelRest and AdminClients)
	 * 
	 */

	private static void closeAllClients() {

		closeHighLevelClient();

	}

	/**
	 * This method is used to index a document within an index.
	 * 
	 * @param indexName
	 * @param mapping
	 * @param documentType
	 * @param uid
	 * @param document
	 * @throws IOException 
	 */

	public static void indexDocument(String indexName, MappingStorage mapping, String documentType, String uid, String document)  {

		logger.info("Indexing tool has started - Attempting to add document to " + indexName);
		
		indexName=indexName.toLowerCase();
		documentType=documentType.toLowerCase();
			
		if (indexReady(indexName, documentType, mapping)) {
			index(indexName, documentType, uid, document);
		}else {		
			logger.info("ERROR - " + indexName + " was not ready or could not be created");
		}
	}
}
