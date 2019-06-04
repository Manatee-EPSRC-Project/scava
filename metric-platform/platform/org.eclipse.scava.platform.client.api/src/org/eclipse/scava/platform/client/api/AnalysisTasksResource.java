package org.eclipse.scava.platform.client.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scava.platform.analysis.AnalysisTaskService;
import org.eclipse.scava.platform.analysis.data.model.AnalysisTask;
import org.eclipse.scava.platform.analysis.data.model.MetricExecution;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class AnalysisTasksResource extends AbstractApiResource {
	
	@Override
	public Representation doRepresent() {
		AnalysisTaskService service = platform.getAnalysisRepositoryManager().getTaskService();
		
		List<AnalysisTask> tasks = service.getAnalysisTasks();
		ArrayNode listTasks = mapper.createArrayNode();
		
		for (AnalysisTask task : tasks) {
			try {
				task.getDbObject().removeField("_id");
				task.getDbObject().removeField("_type");
				task.getDbObject().removeField("project");
				
				List<Object> metricProviders = new ArrayList<>();
				for (MetricExecution metric : task.getMetricExecutions()) {
					Map<String, String> newMetric = new HashMap<>();
					newMetric.put("projectId", metric.getDbObject().get("projectId").toString());
					newMetric.put("metricProviderId", metric.getDbObject().get("metricProviderId").toString());
					newMetric.put("lastExecutionDate", metric.getDbObject().get("lastExecutionDate").toString());
					newMetric.put("hasVisualisation", metric.getDbObject().get("hasVisualisation").toString());
					metricProviders.add(newMetric);
				}
				task.getDbObject().put("metricExecutions", metricProviders);
				listTasks.add(mapper.readTree(task.getDbObject().toString()));
			} catch (IOException e) {
				e.printStackTrace();
				StringRepresentation rep = new StringRepresentation("");
				rep.setMediaType(MediaType.APPLICATION_JSON);
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return rep;
			}
		}
		StringRepresentation rep = new StringRepresentation(listTasks.toString());
		rep.setMediaType(MediaType.APPLICATION_JSON);
		getResponse().setStatus(Status.SUCCESS_OK);
		return rep;
	}
}
