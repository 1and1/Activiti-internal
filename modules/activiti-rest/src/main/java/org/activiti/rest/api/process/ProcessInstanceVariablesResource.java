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

import java.util.List;
import java.util.Map;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricVariableUpdate;
import org.activiti.rest.api.ActivitiUtil;
import org.activiti.rest.api.RequestUtil;
import org.activiti.rest.api.SecuredResource;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.restlet.resource.Get;

public class ProcessInstanceVariablesResource extends SecuredResource {

	@Get
	public ObjectNode getProcessInstanceVariables() {
		if (authenticate() == false) {
			return null;
		}

		String processInstanceId = (String) getRequest().getAttributes().get(
				"processInstanceId");

		if (processInstanceId == null) {
			throw new ActivitiException("No process instance id provided");
		}

		HistoricProcessInstance instance = ActivitiUtil.getHistoryService()
				.createHistoricProcessInstanceQuery()
				.processInstanceId(processInstanceId).singleResult();

		if (instance == null) {
			throw new ActivitiException("Process instance not found for id "
					+ processInstanceId);
		}

		ObjectNode responseJSON = new ObjectMapper().createObjectNode();
		responseJSON.put("processInstanceId", instance.getId());

		boolean processStillActive = instance.getEndTime() == null;
		addVariableList(processInstanceId, responseJSON, processStillActive);

		return responseJSON;
	}

	private void addVariableList(String processInstanceId,
			ObjectNode responseJSON, boolean processStillActive) {

		if (processStillActive) {
			try {
				Map<String, Object> variableMap = ActivitiUtil
						.getRuntimeService().getVariables(processInstanceId);

				if (variableMap != null && variableMap.size() > 0) {
					ArrayNode variablesJSON = new ObjectMapper()
							.createArrayNode();
					responseJSON.put("variables", variablesJSON);
					for (String key : variableMap.keySet()) {
						Object variableValue = variableMap.get(key);
						ObjectNode variableJSON = new ObjectMapper()
								.createObjectNode();
						variableJSON.put("variableName", key);
						if (variableValue != null) {
							if (variableValue instanceof Boolean) {
								variableJSON.put("variableValue",
										(Boolean) variableValue);
							} else if (variableValue instanceof Long) {
								variableJSON.put("variableValue",
										(Long) variableValue);
							} else if (variableValue instanceof Double) {
								variableJSON.put("variableValue",
										(Double) variableValue);
							} else if (variableValue instanceof Float) {
								variableJSON.put("variableValue",
										(Float) variableValue);
							} else if (variableValue instanceof Integer) {
								variableJSON.put("variableValue",
										(Integer) variableValue);
							} else {
								variableJSON.put("variableValue",
										variableValue.toString());
							}
						} else {
							variableJSON.putNull("variableValue");
						}
						variablesJSON.add(variableJSON);
					}
				}
			} catch (Exception e) {
				// Absorb possible error that the execution could not be found
			}
		}

		List<HistoricDetail> historyVariableList = ActivitiUtil
				.getHistoryService().createHistoricDetailQuery()
				.processInstanceId(processInstanceId).variableUpdates()
				.orderByTime().desc().list();

		if (historyVariableList != null && historyVariableList.size() > 0) {
			ArrayNode variablesJSON = new ObjectMapper().createArrayNode();
			responseJSON.put("historyVariables", variablesJSON);
			for (HistoricDetail historicDetail : historyVariableList) {
				HistoricVariableUpdate variableUpdate = (HistoricVariableUpdate) historicDetail;
				ObjectNode variableJSON = new ObjectMapper().createObjectNode();
				variableJSON.put("variableName",
						variableUpdate.getVariableName());
				if (variableUpdate.getValue() != null) {
					if (variableUpdate.getValue() instanceof Boolean) {
						variableJSON.put("variableValue",
								(Boolean) variableUpdate.getValue());
					} else if (variableUpdate.getValue() instanceof Long) {
						variableJSON.put("variableValue",
								(Long) variableUpdate.getValue());
					} else if (variableUpdate.getValue() instanceof Double) {
						variableJSON.put("variableValue",
								(Double) variableUpdate.getValue());
					} else if (variableUpdate.getValue() instanceof Float) {
						variableJSON.put("variableValue",
								(Float) variableUpdate.getValue());
					} else if (variableUpdate.getValue() instanceof Integer) {
						variableJSON.put("variableValue",
								(Integer) variableUpdate.getValue());
					} else {
						variableJSON.put("variableValue", variableUpdate
								.getValue().toString());
					}
				} else {
					variableJSON.putNull("variableValue");
				}
				variableJSON.put("variableType",
						variableUpdate.getVariableTypeName());
				variableJSON.put("revision", variableUpdate.getRevision());
				variableJSON.put("time",
						RequestUtil.dateToString(variableUpdate.getTime()));

				variablesJSON.add(variableJSON);
			}
		}
	}

}
