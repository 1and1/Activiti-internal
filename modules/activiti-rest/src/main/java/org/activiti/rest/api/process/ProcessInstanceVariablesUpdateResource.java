/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.rest.api.process;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.rest.api.ActivitiUtil;
import org.activiti.rest.api.SecuredResource;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.BooleanNode;
import org.codehaus.jackson.node.DoubleNode;
import org.codehaus.jackson.node.IntNode;
import org.codehaus.jackson.node.LongNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;
import org.restlet.representation.Representation;
import org.restlet.resource.Put;

public class ProcessInstanceVariablesUpdateResource extends SecuredResource {

	@Put
	public ObjectNode updateProcessInstanceVariables(Representation entity) {
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

			Map<String, Object> variablesMap = new HashMap<String, Object>();

			String startParams = entity.getText();
			JsonNode startJSON = new ObjectMapper().readTree(startParams);
			ArrayNode variablesJSON = (ArrayNode) startJSON.get("variables");

			for (JsonNode variableJSON : variablesJSON) {
				String variableName = variableJSON.get("variableName")
						.getTextValue();
				Object variableValue = null;

				JsonNode variableValueNode = variableJSON.get("variableValue");
				if (variableValueNode instanceof BooleanNode) {
					variableValue = ((BooleanNode) variableValueNode)
							.getBooleanValue();
				} else if (variableValueNode instanceof LongNode) {
					variableValue = ((LongNode) variableValueNode)
							.getLongValue();
				} else if (variableValueNode instanceof DoubleNode) {
					variableValue = ((DoubleNode) variableValueNode)
							.getDoubleValue();
				} else if (variableValueNode instanceof IntNode) {
					variableValue = ((IntNode) variableValueNode).getIntValue();
				} else if (variableValueNode instanceof TextNode) {
					variableValue = ((TextNode) variableValueNode)
							.getTextValue();
				} else if (variableValueNode instanceof NullNode) {
					variableValue = null;
				}

				variablesMap.put(variableName, variableValue);
			}

			ActivitiUtil.getRuntimeService().setVariables(processInstanceId,
					variablesMap);

			ObjectNode successNode = new ObjectMapper().createObjectNode();
			successNode.put("success", true);
			return successNode;

		} catch (Exception e) {
			throw new ActivitiException("Failed to update process variables", e);
		}
	}

}
