/**
 * 
 */
package org.eclipse.scava.crossflow.examples.firstcommitment.mdetech;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.scava.crossflow.restmule.client.github.api.IGitHubApi;
import org.eclipse.scava.crossflow.restmule.client.github.model.Repo;
import org.eclipse.scava.crossflow.restmule.client.github.model.User;
import org.eclipse.scava.crossflow.restmule.client.github.util.GitHubUtils;
import org.eclipse.scava.crossflow.restmule.core.data.IData;

/**
 * @author blizzfire
 *
 */
public class MdeTechnologyRepoOwnerPopularityCounter extends MdeTechnologyRepoOwnerPopularityCounterBase {

	protected final int MAX_NUMBER_OF_COMMITMENTS = 128;
	
	protected Set<String> alreadySeenJobs = new HashSet<String>();
	
	// < repository-url, number-of-repository-occurrence >
	protected Map<String, Integer> committedRepoMap = new HashMap<String, Integer>(); 
	
	/**
	 * 
	 */
	public MdeTechnologyRepoOwnerPopularityCounter() {
		// do nothing
	}

	@Override
	public void consumeMdeTechnologyClonedRepoEntries(StringStringIntegerStringTuple stringStringIntegerStringTuple) {
		if ( committedRepoMap.size() == MAX_NUMBER_OF_COMMITMENTS ) {
			// do not commit to any more repositories - sending back
			workflow.getMdeTechnologyClonedRepoEntries().send( stringStringIntegerStringTuple );
		
		} else {
			// We still have space left for repositories to commit to - considering it
			if ( alreadySeenJobs.contains( stringStringIntegerStringTuple.getId() ) ) { 
				// We've seen this job before - assume no-one else wants it
				committedRepoMap.put( stringStringIntegerStringTuple.getField1(), 0 );
			
			} else {
				// We haven't seen this job before
				// Record it and send it back
				alreadySeenJobs.add( stringStringIntegerStringTuple.getId() );
				workflow.getMdeTechnologyClonedRepoEntries().send( stringStringIntegerStringTuple );
			}
			
			if ( committedRepoMap.containsKey( stringStringIntegerStringTuple.getField1() ) ) {
				
				committedRepoMap.replace( stringStringIntegerStringTuple.getField1(), committedRepoMap.get( stringStringIntegerStringTuple.getField1()) + 1 );
				
				int repoOwnerFollowerCount = count(stringStringIntegerStringTuple.getField1());
				
				StringStringIntegerStringIntegerTuple mdeTechnologyClonedRepoEntryOwnerFollowerCount = new StringStringIntegerStringIntegerTuple();
				mdeTechnologyClonedRepoEntryOwnerFollowerCount.setField0(stringStringIntegerStringTuple.field0); // file extension
				mdeTechnologyClonedRepoEntryOwnerFollowerCount.setField1(stringStringIntegerStringTuple.field1); // repository remote URL
				mdeTechnologyClonedRepoEntryOwnerFollowerCount.setField2(stringStringIntegerStringTuple.field2); // repository number of stars
				mdeTechnologyClonedRepoEntryOwnerFollowerCount.setField3(stringStringIntegerStringTuple.field3); // cloned repository local path
				mdeTechnologyClonedRepoEntryOwnerFollowerCount.setField4(repoOwnerFollowerCount); // repository owner follower count
				
				getMdeTechnologyRepoOwnerPopularityCountEntries().send(mdeTechnologyClonedRepoEntryOwnerFollowerCount);
			
			}
			
		}
		
	}

	private int count(String repoUrl) {
		AtomicInteger ownerFollowerOrSubscriberCount = new AtomicInteger();
		ownerFollowerOrSubscriberCount.set(-1);
		
		IGitHubApi client = GitHubUtils.getOAuthClient();
		IData<User> repoOwner = client.getUsersUserByUsername(CloneUtils.extractGhRepoOwner(repoUrl));
		
		repoOwner.observe()
        .doOnNext(o -> {
        	if ( o.getType().equals("User") ) {
        		ownerFollowerOrSubscriberCount.set( o.getFollowers() );   
        		
        	} else if ( o.getType().equals("Organization") ){
        		IData<Repo> repo = client.getReposRepoByRepo(CloneUtils.extractGhRepoOwner(repoUrl), CloneUtils.extractGhRepoName(repoUrl));
        		repo.observe()
		              .doOnNext(r -> {
		            	  ownerFollowerOrSubscriberCount.set(r.getSubscribersCount());
		              })
		              .blockingSubscribe();
        		
        	}
        		
        })
        .blockingSubscribe();
		
		return ownerFollowerOrSubscriberCount.intValue();
	}

	/**
	 * @return the alreadySeenJobs
	 */
	public Set<String> getAlreadySeenJobs() {
		return alreadySeenJobs;
	}

	/**
	 * @return the committedRepoMap
	 */
	public Map<String, Integer> getCommittedRepoMap() {
		return committedRepoMap;
	}
	
	public static void main(String args[]) throws IOException {
		MdeTechnologyRepoOwnerPopularityCounter counter = new MdeTechnologyRepoOwnerPopularityCounter();
//		String repoUrl = "https://github.com/torvalds/linux"; // user example (followers)
		String repoUrl = "https://github.com/eclipse/jgit"; // organization example (subscribers)
		System.out.println(repoUrl);
		int count = counter.count(repoUrl);
		System.out.println("COUNT: " + count);
	}
}
