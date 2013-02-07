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

package org.activiti.engine.test.persistence.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.test.PluggableActivitiTestCase;
import org.activiti.engine.runtime.JobQuery;
import org.activiti.engine.test.Deployment;

/**
 * @author Clint Manning
 */
public class ExecutionEntityTest extends PluggableActivitiTestCase {

	private static final int PRIORITY_10 = 10;
	private static final int PRIORITY_5 = 5;
	private static final int DEFAULT_PRIORITY = 0;

	
	public void testStartProcessInstanceCheckPriorityInJob() {
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml")
		.deploy();
		
		
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("PRIO", PRIORITY_10);

		
		
		ExecutionEntity processInstance = (ExecutionEntity) runtimeService
				.startProcessInstanceByKey("oneTaskProcess", "123", variables);
		assertNotNull(processInstance);
		Object tmpPrio = processInstance.getVariable("PRIO");
		assertNotNull(tmpPrio);
		int processPriority = Integer.parseInt(tmpPrio.toString());
		assertEquals(PRIORITY_10, processPriority);
		
		repositoryService.deleteDeployment(repositoryService
				.createDeploymentQuery().singleResult().getId(), true);
	}

	
	public void testStartProcessInstanceNoPriority() {
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml")
		.deploy();
		
		ExecutionEntity processInstance = (ExecutionEntity) runtimeService
				.startProcessInstanceByKey("oneTaskProcess", "123");

		Object tmpPrio = processInstance.getVariable("PRIO");
		assertNull(tmpPrio);
		
		repositoryService.deleteDeployment(repositoryService
				.createDeploymentQuery().singleResult().getId(), true);
	}
	
	
	public void testStartProcessAsyncTaskCheckDefaultPriorityOnMessage() {
		final int maxNonExclusiveJobsPerAcquisition = 3;
		
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/bpmn/async/AsyncTaskTest.testAsycServiceNoListeners.bpmn20.xml")
		.deploy();		
		
		// start process
		ExecutionEntity processInstance = (ExecutionEntity) runtimeService.startProcessInstanceByKey("asyncService");
		// now there should be one job in the database:
		assertEquals(1, managementService.createJobQuery().count());

		// assert the JobEntity has been created and passed back to the process instance with the correct priority		
		assertEquals(DEFAULT_PRIORITY, processInstance.getJobs().get(0).getPriority());
				
	
		CommandExecutor commandExecutor = processEngineConfiguration
				.getCommandExecutorTxRequired();

		JobEntity job = commandExecutor.execute(new Command<JobEntity>() {
			public JobEntity execute(CommandContext commandContext) {
				return commandContext
						.getJobManager()
						.findNextJobsToExecute(
								new Page(0, maxNonExclusiveJobsPerAcquisition))
						.get(0);
			}
		});

		// job executor has not started. Job not invoked.
		assertEquals(DEFAULT_PRIORITY, job.getPriority());

		waitForJobExecutorToProcessAllJobs(10000L, 25L);
		// and the job is done
		assertEquals(0, managementService.createJobQuery().count());
		
		repositoryService.deleteDeployment(repositoryService
				.createDeploymentQuery().singleResult().getId(), true);
	}

	
	
	// Check the Job priority is correct once a job is sent to the DB and listed in the process instance
	public void testStartProcessAsyncTaskCheckPriorityOnMessage() {
		
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/bpmn/async/AsyncTaskTest.testAsycServiceNoListeners.bpmn20.xml")
		.deploy();		
		
		final int maxNonExclusiveJobsPerAcquisition = 3;
		
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("PRIO", PRIORITY_10);
		
		// start process
		ExecutionEntity processInstance = (ExecutionEntity) runtimeService.startProcessInstanceByKey("asyncService",variables);
		// now there should be one job in the database:
		assertEquals(1, managementService.createJobQuery().count());
		
		// assert the JobEntity has been created and passed back to the process instance with the correct priority		
		assertEquals(PRIORITY_10, processInstance.getJobs().get(0).getPriority());
		
		
		CommandExecutor commandExecutor = processEngineConfiguration
				.getCommandExecutorTxRequired();

		JobEntity job = commandExecutor.execute(new Command<JobEntity>() {
			public JobEntity execute(CommandContext commandContext) {
				return commandContext
						.getJobManager()
						.findNextJobsToExecute(
								new Page(0, maxNonExclusiveJobsPerAcquisition))
						.get(0);
			}
		});

		// job executor has not started. Job not invoked.
		assertEquals(PRIORITY_10, job.getPriority());

		waitForJobExecutorToProcessAllJobs(10000L, 25L);
		// and the job is done
		assertEquals(0, managementService.createJobQuery().count());
		
		repositoryService.deleteDeployment(repositoryService
				.createDeploymentQuery().singleResult().getId(), true);
	}	
	
	
	public void testRunProcessWithAsyncTaskCheckPriorityOnMessageMultipleTimes() {
		
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/bpmn/async/AsyncTaskTest.testAsycServiceNoListeners.bpmn20.xml")
		.deploy();		
		
		checkProcessWithAsyncTask(10);
		checkProcessWithAsyncTask(-1);
		
		repositoryService.deleteDeployment(repositoryService
				.createDeploymentQuery().singleResult().getId(), true);
	}	
	
	
	
	private void checkProcessWithAsyncTask(int expectedPriority){
	
		final int maxNonExclusiveJobsPerAcquisition = 3;
		
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("PRIO", expectedPriority);
		
		// start process
		ExecutionEntity processInstance = (ExecutionEntity) runtimeService.startProcessInstanceByKey("asyncService",variables);
		// now there should be one job in the database:
		assertEquals(1, managementService.createJobQuery().count());
		
		// assert the JobEntity has been created and passed back to the process instance with the correct priority		
		assertEquals(expectedPriority, processInstance.getJobs().get(0).getPriority());
		
		
		CommandExecutor commandExecutor = processEngineConfiguration
				.getCommandExecutorTxRequired();

		JobEntity job = commandExecutor.execute(new Command<JobEntity>() {
			public JobEntity execute(CommandContext commandContext) {
				return commandContext
						.getJobManager()
						.findNextJobsToExecute(
								new Page(0, maxNonExclusiveJobsPerAcquisition))
						.get(0);
			}
		});

		// job executor has not started. Job not invoked.
		assertEquals(expectedPriority, job.getPriority());

		waitForJobExecutorToProcessAllJobs(10000L, 25L);
		// and the job is done
		assertEquals(0, managementService.createJobQuery().count());
	}
	
	

}
