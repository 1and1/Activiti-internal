package org.activiti.engine.test.persistence.entity;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.MessageEntity;
import org.activiti.engine.impl.persistence.entity.TimerEntity;


public class JobEntityTest extends JobTestCase {
	
	private static final int DEFAULT_PRIORITY = 0;

	private String[] testInputPriorities = { "10", "-5",
			String.valueOf(JobEntity.MAX_JOB_PRIO),
			String.valueOf(JobEntity.MIN_JOB_PRIO),
			String.valueOf(JobEntity.MAX_JOB_PRIO + 1),
			String.valueOf(JobEntity.MIN_JOB_PRIO - 1) };

	private String[] expectedPriorities = { "10", "-5",
			String.valueOf(JobEntity.MAX_JOB_PRIO),
			String.valueOf(JobEntity.MIN_JOB_PRIO),
			String.valueOf(JobEntity.MAX_JOB_PRIO),
			String.valueOf(JobEntity.MIN_JOB_PRIO) };

	// Check the default priority for a new TimerEntity
	public void testCreateTimerCheckDefaultPriority() {
		TimerEntity timer = createTestTimerEntity();
		assertEquals(DEFAULT_PRIORITY, timer.getPriority());
	}

	// Check the default priority for a new MessageEntity
	public void testCreateMessageCheckDefaultPriority() {
		MessageEntity message = createTestMessageEntity();
		assertEquals(DEFAULT_PRIORITY, message.getPriority());
	}

	// Check the priority for a new TimerEntity Object
	public void testCreateTimerCheckPriority() {
		TimerEntity jobEntity = null;
		Map<String, Object> variables = null;

		for (int i = 0, j = 0; i < testInputPriorities.length; i++, j++) {
			variables = new HashMap<String, Object>();
			variables.put("PRIO", testInputPriorities[i]);

			repositoryService
					.createDeployment()
					.addClasspathResource(
							"org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml")
					.deploy();

			ExecutionEntity processInstance = (ExecutionEntity) runtimeService
					.startProcessInstanceByKey("oneTaskProcess", "123",
							variables);
			jobEntity = createTestTimerEntity();
			jobEntity.setExecution(processInstance);
			assertEquals(expectedPriorities[j],
					String.valueOf(jobEntity.getPriority()));

			repositoryService.deleteDeployment(repositoryService
					.createDeploymentQuery().singleResult().getId(), true);
		}
	}

	// Check the priority for a new MessageEntity Object
	public void testCreateMessageCheckPriority() {
		MessageEntity jobEntity = null;
		Map<String, Object> variables = null;

		for (int i = 0, j = 0; i < testInputPriorities.length; i++, j++) {
			variables = new HashMap<String, Object>();
			variables.put("PRIO", testInputPriorities[i]);

			repositoryService
					.createDeployment()
					.addClasspathResource(
							"org/activiti/engine/test/api/oneTaskProcess.bpmn20.xml")
					.deploy();

			ExecutionEntity processInstance = (ExecutionEntity) runtimeService
					.startProcessInstanceByKey("oneTaskProcess", "123",
							variables);
			jobEntity = createTestMessageEntity();
			jobEntity.setExecution(processInstance);
			assertEquals(expectedPriorities[j],
					String.valueOf(jobEntity.getPriority()));

			repositoryService.deleteDeployment(repositoryService
					.createDeploymentQuery().singleResult().getId(), true);
		}
	}

}
