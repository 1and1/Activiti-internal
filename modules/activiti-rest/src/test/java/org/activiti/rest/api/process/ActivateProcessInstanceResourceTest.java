package org.activiti.rest.api.process;

import java.util.List;

import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.Deployment;
import org.activiti.rest.BaseRestTestCase;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

public class ActivateProcessInstanceResourceTest extends BaseRestTestCase {

	  @Deployment(resources={"org/activiti/rest/api/process/oneTaskProcess.bpmn20.xml"})
	  public void testActivateInstance() throws Exception {
    	  
    	ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    	ProcessInstance processInstance  = runtimeService.startProcessInstanceByKey(processDefinition.getKey()); 
    
        runtimeService.suspendProcessInstanceById(processInstance.getId());   
	    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
	    assertTrue(processInstance.isSuspended()); 
    	 
	    ClientResource client = getAuthenticatedClient("process-instance/" + processInstance.getId() + "/activate");
	    ObjectNode requestNode = objectMapper.createObjectNode();	
	    Representation response = client.post(requestNode);
	    JsonNode responseNode = objectMapper.readTree(response.getStream());
	    assertNotNull(responseNode);    
	
	    processInstance = runtimeService.createProcessInstanceQuery().singleResult();
	    assertFalse(processInstance.isSuspended());	      
	  }
	
}
