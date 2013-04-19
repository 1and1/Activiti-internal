package org.activiti.rest.api.process;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.rest.api.ActivitiUtil;
import org.activiti.rest.api.SecuredResource;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.restlet.resource.Post;

/**
 * @author Bilal Farraj <bfarraj@gmail.com>
 * 
 */
public class ProcessInstanceSuspendExecutionResource extends SecuredResource {

	@Post
	public ObjectNode suspendProcessInstance() {
		try {
			if (authenticate() == false) {
				return null;
			}

			String processInstanceId = (String) getRequest().getAttributes()
					.get("processInstanceId");

			if (processInstanceId == null) {
				throw new ActivitiException("No process instance id provided");
			}

			HistoricProcessInstance instance = ActivitiUtil.getHistoryService()
					.createHistoricProcessInstanceQuery()
					.processInstanceId(processInstanceId).singleResult();

			if (instance == null) {
				throw new ActivitiException(
						"Process instance not found for id "
								+ processInstanceId);
			}

			if (instance.getEndTime() != null) {
				throw new ActivitiException("Process instance with id "
						+ processInstanceId + " already ended");
			}

			ActivitiUtil.getRuntimeService().suspendProcessInstanceById(
					processInstanceId);

			ObjectNode successNode = new ObjectMapper().createObjectNode();
			successNode.put("success", true);
			return successNode;

		} catch (Exception e) {
			throw new ActivitiException("Failed to suspend process instance", e);
		}

	}
}