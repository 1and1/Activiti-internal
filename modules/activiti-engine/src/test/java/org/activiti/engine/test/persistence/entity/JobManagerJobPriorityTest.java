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

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.runtime.Job;


/**
 * @author Clint Manning
 */
public class JobManagerJobPriorityTest extends JobTestCase {

	static final int DEFAULT_PRIORITY = 0;
	static final int maxNonExclusiveJobsPerAcquisition = 3;
	static final long SOME_TIME = 928374923546L;
	static final long SECOND = 1000;

	public void testDefaultPriorityWithMessage() {
		
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/bpmn/async/AsyncTaskTest.testAsycServiceNoListeners.bpmn20.xml")
		.deploy();		
		
		// start process
		runtimeService.startProcessInstanceByKey("asyncService");
		
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

		assertEquals(DEFAULT_PRIORITY, job.getPriority());
		
		repositoryService.deleteDeployment(repositoryService
				.createDeploymentQuery().singleResult().getId(), true);
	}	

	public void testDefaultPriorityWithTimer() {
		
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/bpmn/event/timer/IntermediateTimerEventTest.testCatchingTimerEvent.bpmn20.xml")
		.deploy();
		
		runtimeService.startProcessInstanceByKey("intermediateTimerEventExample");
		assertEquals(1, managementService.createJobQuery().count());
		Job jb = managementService.createJobQuery().singleResult();
		final String id = jb.getId();
		
		CommandExecutor commandExecutor = processEngineConfiguration
				.getCommandExecutorTxRequired();
		JobEntity job = commandExecutor.execute(new Command<JobEntity>() {
			public JobEntity execute(CommandContext commandContext) {		
				return commandContext.getJobManager().findJobById(id);
					
			}
		});

		assertEquals(DEFAULT_PRIORITY, job.getPriority());
		
		repositoryService.deleteDeployment(repositoryService
				.createDeploymentQuery().singleResult().getId(), true);
	}
	
	public void testPriorityWithMessage() {
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/bpmn/async/AsyncTaskTest.testAsycServiceNoListeners.bpmn20.xml")
		.deploy();		
		
		
		
		CommandExecutor commandExecutor = processEngineConfiguration
				.getCommandExecutorTxRequired();		
		
		String expectedPriority = "-5";
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("PRIO", expectedPriority);
		
		// start process
		runtimeService.startProcessInstanceByKey("asyncService",variables);

		JobEntity job = commandExecutor.execute(new Command<JobEntity>() {
			public JobEntity execute(CommandContext commandContext) {	
				return commandContext
						.getJobManager()
						.findNextJobsToExecute(
								new Page(0, maxNonExclusiveJobsPerAcquisition))
						.get(0);
			}
		});

		assertEquals(expectedPriority, String.valueOf(job.getPriority()));
		repositoryService.deleteDeployment(repositoryService
				.createDeploymentQuery().singleResult().getId(), true);
	}	

	public void testPriorityWithTimer() {
		
		repositoryService
		.createDeployment()
		.addClasspathResource(
				"org/activiti/engine/test/bpmn/event/timer/IntermediateTimerEventTest.testCatchingTimerEvent.bpmn20.xml")
		.deploy();
		
		String expectedPriority = "10";
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("PRIO", expectedPriority);
		
		
		runtimeService.startProcessInstanceByKey("intermediateTimerEventExample",variables);
		assertEquals(1, managementService.createJobQuery().count());
		Job jb = managementService.createJobQuery().singleResult();
		final String id = jb.getId();
		
		CommandExecutor commandExecutor = processEngineConfiguration
				.getCommandExecutorTxRequired();
		JobEntity job = commandExecutor.execute(new Command<JobEntity>() {
			public JobEntity execute(CommandContext commandContext) {		
				return commandContext.getJobManager().findJobById(id);
					
			}
		});

		assertEquals(expectedPriority,  String.valueOf(job.getPriority()));
		
		repositoryService.deleteDeployment(repositoryService
				.createDeploymentQuery().singleResult().getId(), true);
	}

}
