/*******************************************************************************
 * Copyright (c) 2017 University of Manchester
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.scava.factoid.bugs.responsetime;

import java.util.Arrays;
import java.util.List;

import org.eclipse.scava.metricprovider.historic.bugs.responsetime.ResponseTimeHistoricMetricProvider;
import org.eclipse.scava.metricprovider.historic.bugs.responsetime.model.BugsResponseTimeHistoricMetric;
import org.eclipse.scava.platform.AbstractFactoidMetricProvider;
import org.eclipse.scava.platform.Date;
import org.eclipse.scava.platform.IMetricProvider;
import org.eclipse.scava.platform.delta.ProjectDelta;
import org.eclipse.scava.platform.factoids.Factoid;
import org.eclipse.scava.platform.factoids.StarRating;
import org.eclipse.scava.repository.model.Project;

import com.googlecode.pongo.runtime.Pongo;

public class BugsChannelResponseTimeFactoid extends AbstractFactoidMetricProvider{

	protected List<IMetricProvider> uses;
	
	@Override
	public String getShortIdentifier() {
		return "factoid.bugs.responsetime";
	}

	@Override
	public String getFriendlyName() {
		return "Bug Tracker Response Time";
		// This method will NOT be removed in a later version.
	}

	@Override
	public String getSummaryInformation() {
		return "This plugin generates the factoid regarding response time for bug trackers. "
				+ "This could be a cummulative average, yearly average etc"; 
		// This method will NOT be removed in a later version.
	}

	@Override
	public boolean appliesTo(Project project) {
	    return !project.getBugTrackingSystems().isEmpty();	   
	}

	@Override
	public List<String> getIdentifiersOfUses() {
		return Arrays.asList(ResponseTimeHistoricMetricProvider.IDENTIFIER);
	}

	@Override
	public void setUses(List<IMetricProvider> uses) {
		this.uses = uses;
	}

	@Override
	public void measureImpl(Project project, ProjectDelta delta, Factoid factoid) {
//		factoid.setCategory(FactoidCategory.BUGS);
		factoid.setName(getFriendlyName());

		ResponseTimeHistoricMetricProvider responseTimeProvider = null;

		for (IMetricProvider m : this.uses) {
			if (m instanceof ResponseTimeHistoricMetricProvider) {
				responseTimeProvider = (ResponseTimeHistoricMetricProvider) m;
				continue;
			}
		}

		int eightHoursMilliSeconds = 8 * 60 * 60 * 1000, 
			dayMilliSeconds = 3 * eightHoursMilliSeconds,
			weekMilliSeconds = 7 * dayMilliSeconds;
		
		Date end = new Date();
		Date start = (new Date()).addDays(-365);
//		Date start=null, end=null;
//		try {
//			start = new Date("20050301");
//			end = new Date("20060301");
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		List<Pongo> responseTimeList = responseTimeProvider.getHistoricalMeasurements(context, project, start, end);
		
		long cumulativeAvgResponseTime = 0,
			 yearlyAvgResponseTime = 0;

		if ( responseTimeList.size() > 0 ) {
			BugsResponseTimeHistoricMetric responseTimeMetric = 
					(BugsResponseTimeHistoricMetric) responseTimeList.get(responseTimeList.size() - 1);
			cumulativeAvgResponseTime = responseTimeMetric.getCumulativeAvgResponseTime();
			yearlyAvgResponseTime = getYearlyAvgResponseTime(responseTimeList);
		}

		if ( ( yearlyAvgResponseTime > 0 ) && ( yearlyAvgResponseTime < eightHoursMilliSeconds ) ) {
			factoid.setStars(StarRating.FOUR);
		} else if ( ( yearlyAvgResponseTime > 0 ) && ( yearlyAvgResponseTime < dayMilliSeconds ) ) {
			factoid.setStars(StarRating.THREE);
		} else if ( ( yearlyAvgResponseTime > 0 ) && ( yearlyAvgResponseTime < weekMilliSeconds ) ) {
			factoid.setStars(StarRating.TWO);
		} else {
			factoid.setStars(StarRating.ONE);
		}
		
		StringBuffer stringBuffer = new StringBuffer();
		
		stringBuffer.append("Over the lifetime of the project, " +
							"requests have received a first response ");
		if ( cumulativeAvgResponseTime < eightHoursMilliSeconds ) {
			stringBuffer.append("very quickly (in less than 8 hours).\n");
		} else if ( cumulativeAvgResponseTime < dayMilliSeconds ) {
			stringBuffer.append("quickly (in less than a day).\n");
		} else if ( cumulativeAvgResponseTime < weekMilliSeconds ) {
			stringBuffer.append("fairly quickly (in less than a week).\n");
		} else
			stringBuffer.append("not so quickly (in more than a week).\n");

		if (yearlyAvgResponseTime > 0) {
			
			stringBuffer.append("Over the last year, requests receive a first response ");
			if ( yearlyAvgResponseTime < eightHoursMilliSeconds ) {
				stringBuffer.append("very quickly (in less than 8 hours).\n");
			} else if ( yearlyAvgResponseTime < dayMilliSeconds ) {
				stringBuffer.append("quickly (in less than a day).\n");
			} else if ( yearlyAvgResponseTime < weekMilliSeconds ) {
				stringBuffer.append("fairly quickly (in less than a week).\n");
			} else
				stringBuffer.append("not so quickly (in more than a week).\n");
			
			stringBuffer.append("Response speed has ");
			if ( Math.abs(cumulativeAvgResponseTime-yearlyAvgResponseTime) < eightHoursMilliSeconds )
				stringBuffer.append("not changed");
			else if ( cumulativeAvgResponseTime > yearlyAvgResponseTime )
				stringBuffer.append("improved");
			else 
				stringBuffer.append("deteriorated");
			stringBuffer.append(" over the last 12 months.\n");
		}
		
		factoid.setFactoid(stringBuffer.toString());

	}
	
	private long getYearlyAvgResponseTime(List<Pongo> responseTimeList) {
		long totalResponseTime = 0;
		int totalBugsConsidered = 0;
		for (Pongo pongo: responseTimeList) {
			BugsResponseTimeHistoricMetric responseTimePongo = (BugsResponseTimeHistoricMetric) pongo;
			totalBugsConsidered += responseTimePongo.getBugsConsidered();
			totalResponseTime += responseTimePongo.getAvgResponseTime();
		}
		if (totalBugsConsidered > 0)
			return totalResponseTime / totalBugsConsidered;
		else
			return 0;
	}

}
