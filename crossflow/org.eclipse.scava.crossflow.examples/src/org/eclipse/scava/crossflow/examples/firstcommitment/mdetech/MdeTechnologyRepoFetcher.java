/**
 * 
 */
package org.eclipse.scava.crossflow.examples.firstcommitment.mdetech;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.scava.crossflow.restmule.client.github.api.IGitHubApi;
import org.eclipse.scava.crossflow.restmule.client.github.model.SearchRepositories;
import org.eclipse.scava.crossflow.restmule.client.github.util.GitHubUtils;
import org.eclipse.scava.crossflow.restmule.core.data.IDataSet;

public class MdeTechnologyRepoFetcher extends MdeTechnologyRepoFetcherBase {

	
	protected final int MAX_NUMBER_OF_COMMITMENTS = 128;
	
	protected Set<String> alreadySeenJobs = new HashSet<String>();
	
	// < repository-url, number-of-repository-occurrence >
	protected Map<String, Integer> committedRepoMap = new HashMap<String, Integer>(); 
	
	/**
	 * 
	 */
	public MdeTechnologyRepoFetcher() {
		// do nothing
	}

	/* (non-Javadoc)
	 * @see org.eclipse.scava.crossflow.examples.firstcommitment.mdetech.MdeTechnologiesConsumer#consumeMdeTechnologies(org.eclipse.scava.crossflow.examples.firstcommitment.mdetech.StringStringTuple)
	 */
	@Override
	public void consumeMdeTechnologies(StringStringTuple stringStringTuple) {
		// Use MDE extension-keyword tuple to issue API calls using generated RestMule client for GitHub
		
		// Instantiate API client
		IGitHubApi client = GitHubUtils.getOAuthClient();
		
		// Construct query parameters
		String q = MDE.query(stringStringTuple.getField0(), stringStringTuple.getField1()); //"figure extension:gmfgraph";
		String order = "asc";
		String sort = "stars";
		
		// Submit API query
		IDataSet<SearchRepositories> searchRepositories = client.getSearchRepositories(order, q, sort);
		
		// Observe search repositories data set
		searchRepositories.observe()
		
			.doOnNext(repo -> {
				StringStringIntegerTuple extensionUrlStarsTuple = new StringStringIntegerTuple();
				extensionUrlStarsTuple.setField0(stringStringTuple.field0);
				extensionUrlStarsTuple.setField1(repo.getHtmlUrl());
				extensionUrlStarsTuple.setField2(repo.getStargazersCount());
				
				getMdeTechnologyRepoEntries().send(extensionUrlStarsTuple);
				
				System.out.println("\n" + "[" + workflow.getName() + "] " + "Consuming " + extensionUrlStarsTuple.getField1() + " (search " + searchRepositories.percentage() + "% completed)");
			})
			
			.doOnError(e -> {
				System.err.println("\n" + "[" + workflow.getName() + "] " + "Failure occurred during repository search: " + e.getMessage() );
			})
			
			.doOnComplete(() -> {
				System.out.println("\n" + "[" + workflow.getName() + "] " + "Completed task: " + searchRepositories.id());
			})
			
			.blockingSubscribe();
	
		Long count = searchRepositories.observe().count().blockingGet();
		System.out.println("\n" + "[" + workflow.getName() + "] " + "Final observe count of task " + searchRepositories.id() + ": " + count);

	}

}
