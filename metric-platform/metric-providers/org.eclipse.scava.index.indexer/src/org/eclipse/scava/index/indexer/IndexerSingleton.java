package org.eclipse.scava.index.indexer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.conn.ssl.*;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

class IndexerSingleton {

	private static IndexerSingleton singleton = null;
	
//	When running the platform locally using a different elasticsearch instance
//	private  static String hostname = "localhost";//"elasticsearch";
//	private  static String clustername = "elasticsearch";//"bitergia_elasticsearch";

//	When running inside CROSSMINER  
	private  static String hostname = "elasticsearch";
	private  static String clustername = "bitergia_elasticsearch";

// Ports  and Scheme are the same in both
	private  static String scheme = "http";
	private  static int port = 9200;
	private  static int clusterport = 9300;
	private  RestHighLevelClient highLevelclient;
	private  Client adminClient;
	
	
	private IndexerSingleton() {

		try {
			highLevelclient = createHighLevelClient(); // This client handles 'High Level requests' such as indexing docs
			adminClient = createAdminClient(); // this client is used to perform administration tasks on ES
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	

//	/**
//	 * Reads Elasticsearch configuration properties from properties file
//	 * 
//	 * @return indexProperties
//	 */
//	private Properties loadIndexProperties() {
//
//		Properties properties = new Properties();
//		InputStream iStream = null;
//		try {
//			// Loading properties file from the path (relative path given here)
//			iStream = new FileInputStream(locateProperties());
//			properties.load(iStream);
//
//		} catch (IOException e) {
//
//			e.printStackTrace();
//		} finally {
//			try {
//				if (iStream != null) {
//					iStream.close();
//					return properties;
//				}
//			} catch (IOException e) {
//
//				e.printStackTrace();
//			}
//		}
//		return properties;
//	}

	/**
	 * 
	 * Builds an Elasticsearch HighLevel REST client
	 * 
	 * @return RestHighLevelClient
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 */
	@SuppressWarnings("deprecation")
	private RestHighLevelClient createHighLevelClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {

		RestHighLevelClient highLevelClient = null;
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY,new UsernamePasswordCredentials("admin", "admin"));

	;
		
		SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
			public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				return true;
			}
		}).build();
	
		
		
		RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", 9200), new HttpHost(hostname, port+1, scheme))
		        .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
		            @Override
		            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
		            	httpClientBuilder.setSSLContext(sslContext);
		                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
		            }

		        });
	
		highLevelClient = new RestHighLevelClient(builder);

				
		return  highLevelClient;

	}
	/**
	 * Creates an Elasticsearch Admin Client 
	 * @return Client
	 */
	
	private Client createAdminClient() {

		Client client = null;

		Settings settings = Settings.builder().put("client.transport.sniff", true).put("cluster.name", clustername)
				.build();

		try {
			client = new PreBuiltTransportClient(settings)
				.addTransportAddress(new TransportAddress(InetAddress.getByName(hostname), clusterport));
		
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	

		return client;
	}

	// ------------------------------------------------------------------------------------------
	// ------------------------------------------------------------------------------------------
	
		// MISC HELPER METHODS
	
	// ------------------------------------------------------------------------------------------
	// ------------------------------------------------------------------------------------------
	
//	/**
//	 * Locates the elasticsearch.properties file within the 'prefs' directory and returns a file path
//	 * 
//	 * @return String 
//	 * @throws IllegalArgumentException
//	 * @throws IOException
//	 */
//	private String locateProperties() throws IllegalArgumentException, IOException {
//		
//		String path = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
//		if (path.endsWith("bin/"))
//			path = path.substring(0, path.lastIndexOf("bin/"));
//		File file = new File(path + "prefs/elasticsearch.properties");
//		checkPropertiesFilePath(file.toPath());
//
//		return file.getPath();
//		
//	
//	}


	
	
//	/**
//	 * Checks if a file exists
//	 * 
//	 * @param path
//	 */
//	private void checkPropertiesFilePath(Path path) {
//		
//		if (!Files.exists(path)) {
//			System.err.println("The file " + path + " has not been found");
//		}
//	}

	// ------------------------------------------------------------------------------------------
	// ------------------------------------------------------------------------------------------
	
		//PUBLIC GETTERS 
	
	// ------------------------------------------------------------------------------------------
	// ------------------------------------------------------------------------------------------
	

	public static IndexerSingleton getInstance() {
		
		if (singleton == null) {

			synchronized (IndexerSingleton.class) {
				
				if (singleton == null) {

					singleton = new IndexerSingleton();
				}
			}
			
		}
		
		return singleton;
	}

	
	
	public  RestHighLevelClient getHighLevelclient() {
		return highLevelclient;
	}

	public  Client getAdminClient() {
		return adminClient;
	}
	
}
