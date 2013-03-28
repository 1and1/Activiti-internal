package org.activiti.rest.api.process;

import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.Deployment;
import org.activiti.rest.BaseRestTestCase;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

public class SuspendProcessInstanceResourceTest extends BaseRestTestCase {

	
	
      @Deployment(resources={"org/activiti/rest/api/process/oneTaskProcess.bpmn20.xml"})
	  public void testSuspendInstance() throws Exception {
    	  
    	ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    	ProcessInstance processInstance  = runtimeService.startProcessInstanceByKey(processDefinition.getKey()); 
    
    //  runtimeService.suspendProcessInstanceById(processInstance.getId());    	 
    	 
	    ClientResource client = getAuthenticatedClient("process-instance/" + processInstance.getId() + "/suspend");
	    ObjectNode requestNode = objectMapper.createObjectNode();
	 // requestNode.put("processDefinitionKey", "simpleProcess");
	    Representation response = client.post(requestNode);
	    JsonNode responseNode = objectMapper.readTree(response.getStream());
	    assertNotNull(responseNode);
	    
	 // String processInstanceId = responseNode.get("processInstanceId").asText();
	 // assertNotNull(processInstanceId);   
	    
	    //suspend	 
	    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
	    assertTrue(processInstance.isSuspended());      
	    
	    //activate
	    runtimeService.activateProcessInstanceById(processInstance.getId());
	    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
	    assertFalse(processInstance.isSuspended());
	      
	  }
	
}
