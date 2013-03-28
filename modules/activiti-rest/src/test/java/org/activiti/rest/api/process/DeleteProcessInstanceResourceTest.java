package org.activiti.rest.api.process;

import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.history.HistoryLevel;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.Deployment;
import org.activiti.rest.BaseRestTestCase;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

public class DeleteProcessInstanceResourceTest extends BaseRestTestCase {

	 @Deployment(resources={
	    "org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml"})
	  public void testDeleteProcessInstance() throws Exception {
	    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
	    assertEquals(1, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());
	    
	    String deleteReason = "testing instance deletion";	
	    ClientResource client = getAuthenticatedClient("process-instance/delete");
	    ObjectNode requestNode = objectMapper.createObjectNode();	
	    requestNode.put("processId", processInstance.getId());
	    requestNode.put("deleteReason", deleteReason);
	    Representation response = client.post(requestNode);
	    JsonNode responseNode = objectMapper.readTree(response.getStream());	    
	    
	    assertEquals(0, runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").count());    
	    
	    // test that the delete reason of the process instance shows up as delete reason of the task in history	   
	    if(processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
	      
	      HistoricTaskInstance historicTaskInstance = historyService
	              .createHistoricTaskInstanceQuery()
	              .processInstanceId(processInstance.getId())
	              .singleResult();
	      
	      assertEquals(deleteReason, historicTaskInstance.getDeleteReason());
	    }    
	  }
	
}
