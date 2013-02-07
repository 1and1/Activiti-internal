package org.activiti.engine.test.jobexecutor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.cmd.AcquireJobsCmd;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.jobexecutor.AcquiredJobs;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.test.PluggableActivitiTestCase;
import org.activiti.engine.repository.Deployment;



public class TestPrioritizingJobExecutor extends PluggableActivitiTestCase {

	
	public void testSingleJobAcquistion() {
		
		final int maxNonExclusiveJobsPerAcquisition = 3;
			
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/jobexecutor/PrioJobExecutorTest.testAsycServiceA.bpmn20.xml")
		.deploy();				
	
		
		ExecutionEntity processInstanceA = (ExecutionEntity) runtimeService.startProcessInstanceByKey("asyncServiceA");		
		assertEquals(1,managementService.createJobQuery().processInstanceId(processInstanceA.getId()).count());
		String jobIdA = managementService.createJobQuery().processInstanceId(processInstanceA.getId()).singleResult().getId();	
	
		
		// assert the number of jobs in the DB
		assertEquals(1, managementService.createJobQuery().count());	
		
		waitForJobExecutorToProcessAllJobs(10000L, 25L);			
		
		
		for(Deployment dep : repositoryService.createDeploymentQuery().list()){
			repositoryService.deleteDeployment(dep.getId(), true);	
		}	
		
		assertEquals(0, managementService.createJobQuery().count());	
	}
	
	public void testMultipleJobAcquistion() {	
			
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/jobexecutor/PrioJobExecutorTest.testAsycServiceA.bpmn20.xml")
		.deploy();		
		
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/jobexecutor/PrioJobExecutorTest.testAsycServiceB.bpmn20.xml")
		.deploy();		
		
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/jobexecutor/PrioJobExecutorTest.testAsycServiceC.bpmn20.xml")
		.deploy();		
		
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/jobexecutor/PrioJobExecutorTest.testAsycServiceD.bpmn20.xml")
		.deploy();		
		
		Map<String, Object> variablesA = new HashMap<String, Object>();
		variablesA.put("PRIO", "1");
		ExecutionEntity processInstanceA = (ExecutionEntity) runtimeService.startProcessInstanceByKey("asyncServiceA",variablesA);		
		assertEquals(1,managementService.createJobQuery().processInstanceId(processInstanceA.getId()).count());
		String jobIdA = managementService.createJobQuery().processInstanceId(processInstanceA.getId()).singleResult().getId();
	
	
		ExecutionEntity processInstanceB = (ExecutionEntity) runtimeService.startProcessInstanceByKey("asyncServiceB");		
		assertEquals(1,managementService.createJobQuery().processInstanceId(processInstanceB.getId()).count());
		String jobIdB = managementService.createJobQuery().processInstanceId(processInstanceB.getId()).singleResult().getId();
	
		Map<String, Object> variablesB = new HashMap<String, Object>();
		variablesB.put("PRIO", "-2");		
		ExecutionEntity processInstanceC = (ExecutionEntity) runtimeService.startProcessInstanceByKey("asyncServiceC",variablesB);		
		assertEquals(1,managementService.createJobQuery().processInstanceId(processInstanceC.getId()).count());
		String jobIdC = managementService.createJobQuery().processInstanceId(processInstanceC.getId()).singleResult().getId();
		
		Map<String, Object> variablesD = new HashMap<String, Object>();
		variablesD.put("PRIO", "15");
		ExecutionEntity processInstanceD = (ExecutionEntity) runtimeService.startProcessInstanceByKey("asyncServiceD",variablesD);		
		assertEquals(1,managementService.createJobQuery().processInstanceId(processInstanceD.getId()).count());
		String jobIdD = managementService.createJobQuery().processInstanceId(processInstanceD.getId()).singleResult().getId();
		
		// assert the number of jobs in the DB
		assertEquals(4, managementService.createJobQuery().count());
		
	
		// aquire the jobs according to priority
		JobExecutor jobExecutor =  processEngineConfiguration.getJobExecutor();
		jobExecutor.setAcquireJobsCmd(new AcquireJobsCmd(jobExecutor));
		AcquiredJobs acquiredJobs = jobExecutor.getCommandExecutor().execute(jobExecutor.getAcquireJobsCmd());
	
		// assert that only the highest priority were acquired
		
		assertEquals(acquiredJobs.getJobIdBatches().get(0).get(0),jobIdD);
		
		assertEquals(acquiredJobs.getJobIdBatches().get(1).get(0),jobIdA);
		
		assertEquals(acquiredJobs.getJobIdBatches().get(2).get(0),jobIdB);
		
		//waitForJobExecutorToProcessAllJobs(10000L, 25L);		
			
		
		for(Deployment dep : repositoryService.createDeploymentQuery().list()){
			repositoryService.deleteDeployment(dep.getId(), true);	
		}	
		
		assertEquals(0, managementService.createJobQuery().count());	
	}


	public void testMultipleJobAcquistion2() {	
		
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/jobexecutor/PrioJobExecutorTest.testAsycServiceA.bpmn20.xml")
		.deploy();		
		
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/jobexecutor/PrioJobExecutorTest.testAsycServiceB.bpmn20.xml")
		.deploy();		
		
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/jobexecutor/PrioJobExecutorTest.testAsycServiceC.bpmn20.xml")
		.deploy();		
		
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/jobexecutor/PrioJobExecutorTest.testAsycServiceD.bpmn20.xml")
		.deploy();		
		
		Map<String, Object> variablesA = new HashMap<String, Object>();
		variablesA.put("PRIO", "-20");
		ExecutionEntity processInstanceA = (ExecutionEntity) runtimeService.startProcessInstanceByKey("asyncServiceA",variablesA);		
		assertEquals(1,managementService.createJobQuery().processInstanceId(processInstanceA.getId()).count());
		String jobIdA = managementService.createJobQuery().processInstanceId(processInstanceA.getId()).singleResult().getId();
	
	
		ExecutionEntity processInstanceB = (ExecutionEntity) runtimeService.startProcessInstanceByKey("asyncServiceB");		
		assertEquals(1,managementService.createJobQuery().processInstanceId(processInstanceB.getId()).count());
		String jobIdB = managementService.createJobQuery().processInstanceId(processInstanceB.getId()).singleResult().getId();
	
		Map<String, Object> variablesB = new HashMap<String, Object>();
		variablesB.put("PRIO", "16");		
		ExecutionEntity processInstanceC = (ExecutionEntity) runtimeService.startProcessInstanceByKey("asyncServiceC",variablesB);		
		assertEquals(1,managementService.createJobQuery().processInstanceId(processInstanceC.getId()).count());
		String jobIdC = managementService.createJobQuery().processInstanceId(processInstanceC.getId()).singleResult().getId();
		
		Map<String, Object> variablesD = new HashMap<String, Object>();
		variablesD.put("PRIO", "14");
		ExecutionEntity processInstanceD = (ExecutionEntity) runtimeService.startProcessInstanceByKey("asyncServiceD",variablesD);		
		assertEquals(1,managementService.createJobQuery().processInstanceId(processInstanceD.getId()).count());
		String jobIdD = managementService.createJobQuery().processInstanceId(processInstanceD.getId()).singleResult().getId();
		
		// assert the number of jobs in the DB
		assertEquals(4, managementService.createJobQuery().count());
		
	
		// aquire the jobs according to priority
		JobExecutor jobExecutor =  processEngineConfiguration.getJobExecutor();
		jobExecutor.setAcquireJobsCmd(new AcquireJobsCmd(jobExecutor));
		AcquiredJobs acquiredJobs = jobExecutor.getCommandExecutor().execute(jobExecutor.getAcquireJobsCmd());
	
		// assert that only the highest priority were acquired
		
		assertEquals(acquiredJobs.getJobIdBatches().get(0).get(0),jobIdC);
		
		assertEquals(acquiredJobs.getJobIdBatches().get(1).get(0),jobIdD);
		
		assertEquals(acquiredJobs.getJobIdBatches().get(2).get(0),jobIdB);
		
		
			
		
		for(Deployment dep : repositoryService.createDeploymentQuery().list()){
			repositoryService.deleteDeployment(dep.getId(), true);	
		}	
		
		assertEquals(0, managementService.createJobQuery().count());	
	}

	
	
}
