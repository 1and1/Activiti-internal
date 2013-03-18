package org.activiti.engine.test.jobexecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.cmd.AcquireJobsCmd;
import org.activiti.engine.impl.jobexecutor.AcquiredJobs;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.JobManager;
import org.activiti.engine.impl.test.PluggableActivitiTestCase;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;

import static org.mockito.Mockito.*;

public class TestPrioritizingJobExecutor extends PluggableActivitiTestCase {

	private static final String ASYNC_PROCESS_NAME_A = "asyncServiceA";
	private static final String ASYNC_PROCESS_NAME_B = "asyncServiceB";
	private static final String ASYNC_PROCESS_NAME_C = "asyncServiceC";
	private static final String ASYNC_PROCESS_NAME_D = "asyncServiceD";
	private static final String TEST_RES_PATH = "org/activiti/engine/test/jobexecutor/";
	private static final String ASYNC_PRCS_RES_A = "PrioJobExecutorTest.testAsycServiceA.bpmn20.xml";
	private static final String ASYNC_PRCS_RES_B = "PrioJobExecutorTest.testAsycServiceB.bpmn20.xml";
	private static final String ASYNC_PRCS_RES_C = "PrioJobExecutorTest.testAsycServiceC.bpmn20.xml";
	private static final String ASYNC_PRCS_RES_D = "PrioJobExecutorTest.testAsycServiceD.bpmn20.xml";
	private static final String TIMER_PRCS_RES_A = "PrioJobExecutorTest.testTimerAA.bpmn20.xml";
	private static final String TIMER_PRCS_RES_B = "PrioJobExecutorTest.testTimerB.bpmn20.xml";
	private static final String TIMER_PRCS_RES_C = "PrioJobExecutorTest.testTimerC.bpmn20.xml";
	private static final String TIMER_PRCS_RES_D = "PrioJobExecutorTest.testTimerD.bpmn20.xml";
	
	
	
	
	/*
	 * Notes Testing the job priority.
	 * 
	 * Process priority valid range is (-20, ..., +20), +20 = Highest priority;
	 * -20 = lowest priority If outside that range the minimum resp. maximum
	 * valid value is used (e.g -20 resp. +20).
	 */

	public void testAcquireJobsCmdNoExclusiveJobs() {
		JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();

		// Mock the database response
		List<JobEntity> mockedJobs = createMockedJobEntityListNonExclusive();
		CommandContext mockedCommandContext = mock(CommandContext.class);
		JobManager mockedJobManager = mock(JobManager.class);
		when(mockedJobManager.findNextJobsToExecute(any(Page.class)))
				.thenReturn(mockedJobs);
		when(mockedCommandContext.getJobManager()).thenReturn(mockedJobManager);

		AcquireJobsCmd acquireJobsCmd = new AcquireJobsCmd(jobExecutor);
		AcquiredJobs acquiredJobs = acquireJobsCmd
				.execute(mockedCommandContext);

		int expectedJobAcquistions = 3;
		assertEquals(expectedJobAcquistions, acquiredJobs.getJobIdBatches()
				.size());

		assertEquals(acquiredJobs.getJobIdBatches().get(0).get(0), "A");
		assertEquals(acquiredJobs.getJobIdBatches().get(1).get(0), "B");
		assertEquals(acquiredJobs.getJobIdBatches().get(2).get(0), "C");
	}

	public void testAcquireJobsCmdExclusiveJobs() {
		JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();

		// Mock the database response
		List<JobEntity> mockedJobs = createMockedJobEntityListMixedWithExclusiveJobs();
		List<JobEntity> mockedExclusiveJobs = createMockedJobEntityListExclusiveJobs();
		CommandContext mockedCommandContext = mock(CommandContext.class);
		JobManager mockedJobManager = mock(JobManager.class);
		when(mockedJobManager.findNextJobsToExecute(any(Page.class)))
				.thenReturn(mockedJobs);
		when(mockedJobManager.findExclusiveJobsToExecute(any(String.class)))
				.thenReturn(mockedExclusiveJobs);
		when(mockedCommandContext.getJobManager()).thenReturn(mockedJobManager);

		AcquireJobsCmd acquireJobsCmd = new AcquireJobsCmd(jobExecutor);
		AcquiredJobs acquiredJobs = acquireJobsCmd
				.execute(mockedCommandContext);

		int expectedJobAcquistions = 3;
		assertEquals(expectedJobAcquistions, acquiredJobs.getJobIdBatches()
				.size());

		assertEquals(acquiredJobs.getJobIdBatches().get(0).get(0), "A");
		assertEquals(acquiredJobs.getJobIdBatches().get(1).get(0), "B");
		assertEquals(acquiredJobs.getJobIdBatches().get(2).get(0), "C");
	}

	public void testSingleJobAcquistion() {

		repositoryService
				.createDeployment()
				.addClasspathResource(
						TEST_RES_PATH + ASYNC_PRCS_RES_A)
				.deploy();

		ExecutionEntity processInstanceA = (ExecutionEntity) runtimeService
				.startProcessInstanceByKey(ASYNC_PROCESS_NAME_A);
		assertEquals(
				1,
				managementService.createJobQuery()
						.processInstanceId(processInstanceA.getId()).count());

		// assert the number of jobs in the DB
		assertEquals(1, managementService.createJobQuery().count());

		waitForJobExecutorToProcessAllJobs(10000L, 25L);

		deploymentCleanup();
	}

	public void testFindNextJobsToExecute() {

		deployTestProcessesWithAsyncTasks();

		ExecutionEntity processInstanceA = startProcessWithPriority(
				ASYNC_PROCESS_NAME_A, "0");		
		String jobIdA = getJobId(processInstanceA);		
		
		ExecutionEntity processInstanceB = startProcessWithPriority(
				ASYNC_PROCESS_NAME_B, "5");
		String jobIdB = getJobId(processInstanceB);

		startProcessWithPriority(ASYNC_PROCESS_NAME_C, "-12");		
		
		ExecutionEntity processInstanceD = startProcessWithPriority(ASYNC_PROCESS_NAME_D, "20");
		String jobIdD = getJobId(processInstanceD);		
		
		// Fetch jobs in the same manner as the job executor		
		CommandExecutor commandExecutor = processEngineConfiguration
				.getCommandExecutorTxRequired();

		List<JobEntity> jobs = commandExecutor.execute(new Command<List<JobEntity>>() {
			public List<JobEntity> execute(CommandContext commandContext) {
				return commandContext.getJobManager().findNextJobsToExecute(
						new Page(0, processEngineConfiguration.getJobExecutor()
								.getMaxJobsPerAcquisition()));
			}
		});
				
		assertEquals(jobs.get(0).getId(), jobIdD);	
		assertEquals(jobs.get(1).getId(), jobIdB);	
		assertEquals(jobs.get(2).getId(), jobIdA);

		deploymentCleanup();
	}

	public void testFindNextJobsToExecuteAsc() {

		deployTestProcessesWithAsyncTasks();

		ExecutionEntity processInstanceA = startProcessWithPriority(
				ASYNC_PROCESS_NAME_A, "20");		
		String jobIdA = getJobId(processInstanceA);			
		
		ExecutionEntity processInstanceB = startProcessWithPriority(
				ASYNC_PROCESS_NAME_B, "-1");
		String jobIdB = getJobId(processInstanceB);

		ExecutionEntity processInstanceC = startProcessWithPriority(ASYNC_PROCESS_NAME_C, "-5");		
		String jobIdC = getJobId(processInstanceC);		
		
	    startProcessWithPriority(ASYNC_PROCESS_NAME_D, "-19");
	
		
		// Fetch jobs in the same manner as the job executor		
		CommandExecutor commandExecutor = processEngineConfiguration
				.getCommandExecutorTxRequired();

		List<JobEntity> jobs = commandExecutor.execute(new Command<List<JobEntity>>() {
			public List<JobEntity> execute(CommandContext commandContext) {
				return commandContext.getJobManager().findNextJobsToExecute(
						new Page(0, processEngineConfiguration.getJobExecutor()
								.getMaxJobsPerAcquisition()));
			}
		});
				
		assertEquals(jobs.get(0).getId(), jobIdA);	
		assertEquals(jobs.get(1).getId(), jobIdB);	
		assertEquals(jobs.get(2).getId(), jobIdC);

		deploymentCleanup();
	}
	
	public void testFindNextJobsToExecuteDesc() {

		deployTestProcessesWithAsyncTasks();

		ExecutionEntity processInstanceA = startProcessWithPriority(
				ASYNC_PROCESS_NAME_A, "20");		
		String jobIdA = getJobId(processInstanceA);		
		
		ExecutionEntity processInstanceB = startProcessWithPriority(
				ASYNC_PROCESS_NAME_B, "5");
		String jobIdB = getJobId(processInstanceB);

		ExecutionEntity processInstanceC = startProcessWithPriority(ASYNC_PROCESS_NAME_C, "-1");		
		String jobIdC = getJobId(processInstanceC);
		
		startProcessWithPriority(ASYNC_PROCESS_NAME_D, "-10");		
		
		// Fetch jobs in the same manner as the job executor		
		CommandExecutor commandExecutor = processEngineConfiguration
				.getCommandExecutorTxRequired();

		List<JobEntity> jobs = commandExecutor.execute(new Command<List<JobEntity>>() {
			public List<JobEntity> execute(CommandContext commandContext) {
				return commandContext.getJobManager().findNextJobsToExecute(
						new Page(0, processEngineConfiguration.getJobExecutor()
								.getMaxJobsPerAcquisition()));
			}
		});
				
		assertEquals(jobs.get(0).getId(), jobIdA);	
		assertEquals(jobs.get(1).getId(), jobIdB);	
		assertEquals(jobs.get(2).getId(), jobIdC);

		deploymentCleanup();
	}
	
	
	public void testMultipleJobAcquistionAllDefaultPrio() {

		deployTestProcessesWithAsyncTasks();

		// Start deployed processes, with or without a priority variable, check
		// if there is a job in the jobs table and retrieve the job ID
		ExecutionEntity processInstanceA = startProcessWithPriority(
				ASYNC_PROCESS_NAME_A, null);
		String jobIdA = getJobId(processInstanceA);
	
		ExecutionEntity processInstanceB = startProcessWithPriority(
				ASYNC_PROCESS_NAME_B, null);
		String jobIdB = getJobId(processInstanceB);

		ExecutionEntity processInstanceC = startProcessWithPriority(
				ASYNC_PROCESS_NAME_C, null);
		String jobIdC = getJobId(processInstanceC);

		startProcessWithPriority(ASYNC_PROCESS_NAME_D, null);

		
		assertEquals(4, managementService.createJobQuery().count());
		
		AcquiredJobs acquiredJobs = getAcquiredJobs();

		int expectedJobAcquistions = 3;
		assertEquals(expectedJobAcquistions, acquiredJobs.getJobIdBatches()
				.size());
		
		assertEquals(acquiredJobs.getJobIdBatches().get(0).get(0), jobIdA);
		assertEquals(acquiredJobs.getJobIdBatches().get(1).get(0), jobIdB);
		assertEquals(acquiredJobs.getJobIdBatches().get(2).get(0), jobIdC);

		deploymentCleanup();
	}

	public void testMultipleJobAcquistionAllSamePrio() {

		deployTestProcessesWithAsyncTasks();

		// Start deployed processes, with or without a priority variable, check
		// if there is a job in the jobs table and retrieve the job ID
		ExecutionEntity processInstanceA = startProcessWithPriority(
				ASYNC_PROCESS_NAME_A, "-5");
		String jobIdA = getJobId(processInstanceA);

		ExecutionEntity processInstanceB = startProcessWithPriority(
				ASYNC_PROCESS_NAME_B, "-5");
		String jobIdB = getJobId(processInstanceB);

		ExecutionEntity processInstanceC = startProcessWithPriority(
				ASYNC_PROCESS_NAME_C, "-5");
		String jobIdC = getJobId(processInstanceC);

		startProcessWithPriority(ASYNC_PROCESS_NAME_D, "-5");

		// assert the number of jobs in the DB
		assertEquals(4, managementService.createJobQuery().count());
		
		AcquiredJobs acquiredJobs = getAcquiredJobs();
		
		int expectedJobAcquistions = 3;
		assertEquals(expectedJobAcquistions, acquiredJobs.getJobIdBatches()
				.size());
	
		assertEquals(acquiredJobs.getJobIdBatches().get(0).get(0), jobIdA);
		assertEquals(acquiredJobs.getJobIdBatches().get(1).get(0), jobIdB);
		assertEquals(acquiredJobs.getJobIdBatches().get(2).get(0), jobIdC);

		deploymentCleanup();
	}

	public void testMultipleJobAcquistionMostSamePrio() {

		deployTestProcessesWithAsyncTasks();

		// Start deployed processes, with or without a priority variable, check
		// if there is a job in the jobs table and retrieve the job ID
		ExecutionEntity processInstanceA = startProcessWithPriority(
				ASYNC_PROCESS_NAME_A, "-5");
		String jobIdA = getJobId(processInstanceA);

		ExecutionEntity processInstanceB = startProcessWithPriority(
				ASYNC_PROCESS_NAME_B, "-5");
		String jobIdB = getJobId(processInstanceB);

		startProcessWithPriority(ASYNC_PROCESS_NAME_C, "-5");

		ExecutionEntity processInstanceD = startProcessWithPriority(
				ASYNC_PROCESS_NAME_D, "-4");
		String jobIdD = getJobId(processInstanceD);
	
		assertEquals(4, managementService.createJobQuery().count());
	
		AcquiredJobs acquiredJobs = getAcquiredJobs();
		
		int expectedJobAcquistions = 3;
		assertEquals(expectedJobAcquistions, acquiredJobs.getJobIdBatches()
				.size());

		assertEquals(acquiredJobs.getJobIdBatches().get(0).get(0), jobIdD);
		assertEquals(acquiredJobs.getJobIdBatches().get(1).get(0), jobIdA);
		assertEquals(acquiredJobs.getJobIdBatches().get(2).get(0), jobIdB);

		deploymentCleanup();
	}

	public void testMultipleJobAcquistionMostSamePrio2() {

		deployTestProcessesWithAsyncTasks();

		// Start deployed processes, with or without a priority variable, check
		// if there is a job in the jobs table and retrieve the job ID
		ExecutionEntity processInstanceA = startProcessWithPriority(
				ASYNC_PROCESS_NAME_A, "-5");
		String jobIdA = getJobId(processInstanceA);

		ExecutionEntity processInstanceB = startProcessWithPriority(
				ASYNC_PROCESS_NAME_B, "-5");
		String jobIdB = getJobId(processInstanceB);

		ExecutionEntity processInstanceC = startProcessWithPriority(
				ASYNC_PROCESS_NAME_C, "-5");
		String jobIdC = getJobId(processInstanceC);

		startProcessWithPriority(ASYNC_PROCESS_NAME_D, "-6");
		
		assertEquals(4, managementService.createJobQuery().count());
		AcquiredJobs acquiredJobs = getAcquiredJobs();
		int expectedJobAcquistions = 3;
		assertEquals(expectedJobAcquistions, acquiredJobs.getJobIdBatches()
				.size());

		assertEquals(acquiredJobs.getJobIdBatches().get(0).get(0), jobIdA);
		assertEquals(acquiredJobs.getJobIdBatches().get(1).get(0), jobIdB);
		assertEquals(acquiredJobs.getJobIdBatches().get(2).get(0), jobIdC);

		deploymentCleanup();
	}

	public void testMultipleJobAcquistionMixedPrioriy1() {

		deployTestProcessesWithAsyncTasks();

		// Start deployed processes, with or without a priority variable, check
		// if there is a job in the jobs table and retrieve the job ID
		ExecutionEntity processInstanceA = startProcessWithPriority(
				ASYNC_PROCESS_NAME_A, "1");
		String jobIdA = getJobId(processInstanceA);
		
		ExecutionEntity processInstanceB = startProcessWithPriority(
				ASYNC_PROCESS_NAME_B, null);
		String jobIdB = getJobId(processInstanceB);

		startProcessWithPriority(ASYNC_PROCESS_NAME_C, "-2");

		ExecutionEntity processInstanceD = startProcessWithPriority(
				ASYNC_PROCESS_NAME_D, "15");
		String jobIdD = getJobId(processInstanceD);
		
		assertEquals(4, managementService.createJobQuery().count());

		AcquiredJobs acquiredJobs = getAcquiredJobs();
		
		int expectedJobAcquistions = 3;
		assertEquals(expectedJobAcquistions, acquiredJobs.getJobIdBatches()
				.size());
		
		assertEquals(acquiredJobs.getJobIdBatches().get(0).get(0), jobIdD);
		assertEquals(acquiredJobs.getJobIdBatches().get(1).get(0), jobIdA);
		assertEquals(acquiredJobs.getJobIdBatches().get(2).get(0), jobIdB);

		deploymentCleanup();
	}

	public void testMultipleJobAcquistionMixedPrioriy2() {

		deployTestProcessesWithAsyncTasks();

		startProcessWithPriority(ASYNC_PROCESS_NAME_A, "-1");
		ExecutionEntity processInstanceB = startProcessWithPriority(
				ASYNC_PROCESS_NAME_B, "13");
		String jobIdB = getJobId(processInstanceB);

		ExecutionEntity processInstanceC = startProcessWithPriority(
				ASYNC_PROCESS_NAME_C, "14");
		String jobIdC = getJobId(processInstanceC);

		ExecutionEntity processInstanceD = startProcessWithPriority(
				ASYNC_PROCESS_NAME_D, "15");
		String jobIdD = getJobId(processInstanceD);
		assertEquals(4, managementService.createJobQuery().count());

		AcquiredJobs acquiredJobs = getAcquiredJobs();
		int expectedJobAcquistions = 3;
		assertEquals(expectedJobAcquistions, acquiredJobs.getJobIdBatches()
				.size());

		assertEquals(acquiredJobs.getJobIdBatches().get(0).get(0), jobIdD);
		assertEquals(acquiredJobs.getJobIdBatches().get(1).get(0), jobIdC);
		assertEquals(acquiredJobs.getJobIdBatches().get(2).get(0), jobIdB);

		deploymentCleanup();
	}

	public void testMultipleJobAcquistionMixedPrioriy3() {

		deployTestProcessesWithAsyncTasks();
		
		ExecutionEntity processInstanceA = startProcessWithPriority(
				ASYNC_PROCESS_NAME_A, "20");
		String jobIdA = getJobId(processInstanceA);

		ExecutionEntity processInstanceB = startProcessWithPriority(
				ASYNC_PROCESS_NAME_B, "59");
		String jobIdB = getJobId(processInstanceB);

		startProcessWithPriority(ASYNC_PROCESS_NAME_C, "1");

		ExecutionEntity processInstanceD = startProcessWithPriority(
				ASYNC_PROCESS_NAME_D, "2");
		String jobIdD = getJobId(processInstanceD);
		
		assertEquals(4, managementService.createJobQuery().count());
		AcquiredJobs acquiredJobs = getAcquiredJobs();
		int expectedJobAcquistions = 3;
		assertEquals(expectedJobAcquistions, acquiredJobs.getJobIdBatches()
				.size());

		assertEquals(acquiredJobs.getJobIdBatches().get(0).get(0), jobIdA);
		assertEquals(acquiredJobs.getJobIdBatches().get(1).get(0), jobIdB);
		assertEquals(acquiredJobs.getJobIdBatches().get(2).get(0), jobIdD);

		deploymentCleanup();
	}

	public void testMultipleJobAcquistionMixedPrioriy4() {

		deployTestProcessesWithAsyncTasks();

		ExecutionEntity processInstanceA = startProcessWithPriority(
				ASYNC_PROCESS_NAME_A, "20");
		String jobIdA = getJobId(processInstanceA);

		ExecutionEntity processInstanceB = startProcessWithPriority(
				ASYNC_PROCESS_NAME_B, "59");
		String jobIdB = getJobId(processInstanceB);

		ExecutionEntity processInstanceC = startProcessWithPriority(
				ASYNC_PROCESS_NAME_C, "-234");
		String jobIdC = getJobId(processInstanceC);

		startProcessWithPriority(ASYNC_PROCESS_NAME_D, "-20");

		assertEquals(4, managementService.createJobQuery().count());
		AcquiredJobs acquiredJobs = getAcquiredJobs();
		int expectedJobAcquistions = 3;
		assertEquals(expectedJobAcquistions, acquiredJobs.getJobIdBatches()
				.size());

		assertEquals(acquiredJobs.getJobIdBatches().get(0).get(0), jobIdA);
		assertEquals(acquiredJobs.getJobIdBatches().get(1).get(0), jobIdB);
		assertEquals(acquiredJobs.getJobIdBatches().get(2).get(0), jobIdC);

		deploymentCleanup();
	}

	public void testMultipleJobAcquistionMixedPriorityTimers() {

		deployTestProcessesTimers();
		
		ExecutionEntity processInstanceA = startProcessWithPriority(
				"intermediateTimerEventExample", "20");
		String jobIdA = getJobId(processInstanceA);

		ExecutionEntity processInstanceB = startProcessWithPriority(
				"intermediateTimerEventExampleB", "59");
		String jobIdB = getJobId(processInstanceB);

		ExecutionEntity processInstanceC = startProcessWithPriority(
				"intermediateTimerEventExampleC", "-234");
		String jobIdC = getJobId(processInstanceC);

		startProcessWithPriority("intermediateTimerEventExampleD", "-20");
		assertEquals(4, managementService.createJobQuery().count());

		AcquiredJobs acquiredJobs = getAcquiredJobs();

		int expectedJobAcquistions = 3;
		assertEquals(expectedJobAcquistions, acquiredJobs.getJobIdBatches()
				.size());

		assertEquals(acquiredJobs.getJobIdBatches().get(0).get(0), jobIdA);
		assertEquals(acquiredJobs.getJobIdBatches().get(1).get(0), jobIdB);
		assertEquals(acquiredJobs.getJobIdBatches().get(2).get(0), jobIdC);

		deploymentCleanup();
	}

	public void testMultipleJobAcquistionMixedPriorityTimersAndAsyncTasks() {

		deployTestProcessesMixAsyncAndTimers();
		ExecutionEntity processInstanceA = startProcessWithPriority(
				"intermediateTimerEventExample", "0");
		String jobIdA = getJobId(processInstanceA);

		startProcessWithPriority(ASYNC_PROCESS_NAME_A, "-3");

		ExecutionEntity processInstanceC = startProcessWithPriority(
				"intermediateTimerEventExampleB", "-2");
		String jobIdC = getJobId(processInstanceC);

		ExecutionEntity processInstanceD = startProcessWithPriority(
				ASYNC_PROCESS_NAME_B, "20");
		String jobIdD = getJobId(processInstanceD);

		assertEquals(4, managementService.createJobQuery().count());
		AcquiredJobs acquiredJobs = getAcquiredJobs();

		int expectedJobAcquistions = 3;
		assertEquals(expectedJobAcquistions, acquiredJobs.getJobIdBatches()
				.size());

		assertEquals(acquiredJobs.getJobIdBatches().get(0).get(0), jobIdD);
		assertEquals(acquiredJobs.getJobIdBatches().get(1).get(0), jobIdA);
		assertEquals(acquiredJobs.getJobIdBatches().get(2).get(0), jobIdC);

		deploymentCleanup();
	}

	public void testMultipleJobAcquistionMixedPriorityTimersOneDelayedTimer() {

		deployTestProcessesTimersOneDelayedTimer();

		ExecutionEntity processInstanceA = startProcessWithPriority(
				"intermediateTimerEventExample", "-1");
		String jobIdA = getJobId(processInstanceA);

		ExecutionEntity processInstanceB = startProcessWithPriority(
				"intermediateTimerEventExampleB", "13");
		String jobIdB = getJobId(processInstanceB);

		ExecutionEntity processInstanceC = startProcessWithPriority(
				"intermediateTimerEventExampleC", "-2");
		String jobIdC = getJobId(processInstanceC);

		startProcessWithPriority("intermediateTimerEventExampleE", "20");
		
		assertEquals(4, managementService.createJobQuery().count());

		AcquiredJobs acquiredJobs = getAcquiredJobs();

		int expectedJobAcquistions = 3;
		assertEquals(expectedJobAcquistions, acquiredJobs.getJobIdBatches()
				.size());

		assertEquals(acquiredJobs.getJobIdBatches().get(0).get(0), jobIdB);
		assertEquals(acquiredJobs.getJobIdBatches().get(1).get(0), jobIdA);
		assertEquals(acquiredJobs.getJobIdBatches().get(2).get(0), jobIdC);

		deploymentCleanup();
	}

	// Test Helpers

	private void deployTestProcessesWithAsyncTasks() {
		repositoryService
				.createDeployment()
				.addClasspathResource(
						TEST_RES_PATH + ASYNC_PRCS_RES_A)
				.deploy();

		repositoryService
				.createDeployment()
				.addClasspathResource(
						TEST_RES_PATH + ASYNC_PRCS_RES_B)
				.deploy();

		repositoryService
				.createDeployment()
				.addClasspathResource(
						TEST_RES_PATH + ASYNC_PRCS_RES_C)
				.deploy();

		repositoryService
				.createDeployment()
				.addClasspathResource(
						TEST_RES_PATH + ASYNC_PRCS_RES_D)
				.deploy();
	}

	private void deployTestProcessesMixAsyncAndTimers() {

		repositoryService
				.createDeployment()
				.addClasspathResource(
						TEST_RES_PATH + TIMER_PRCS_RES_A)
				.deploy();

		repositoryService
				.createDeployment()
				.addClasspathResource(
						TEST_RES_PATH + "PrioJobExecutorTest.testTimerB.bpmn20.xml")
				.deploy();

		repositoryService
				.createDeployment()
				.addClasspathResource(
						TEST_RES_PATH + ASYNC_PRCS_RES_A)
				.deploy();

		repositoryService
				.createDeployment()
				.addClasspathResource(
						TEST_RES_PATH + ASYNC_PRCS_RES_B)
				.deploy();

	}

	private void deployTestProcessesTimers() {

		repositoryService
				.createDeployment()
				.addClasspathResource(
						TEST_RES_PATH + TIMER_PRCS_RES_A)
				.deploy();

		repositoryService
				.createDeployment()
				.addClasspathResource(
						TEST_RES_PATH + "PrioJobExecutorTest.testTimerB.bpmn20.xml")
				.deploy();

		repositoryService
				.createDeployment()
				.addClasspathResource(
						TEST_RES_PATH + "PrioJobExecutorTest.testTimerC.bpmn20.xml")
				.deploy();

		repositoryService
				.createDeployment()
				.addClasspathResource(
						TEST_RES_PATH + "PrioJobExecutorTest.testTimerD.bpmn20.xml")
				.deploy();

	}

	private void deployTestProcessesTimersOneDelayedTimer() {

		repositoryService
				.createDeployment()
				.addClasspathResource(
						TEST_RES_PATH + TIMER_PRCS_RES_A)
				.deploy();

		repositoryService
				.createDeployment()
				.addClasspathResource(
						TEST_RES_PATH + "PrioJobExecutorTest.testTimerB.bpmn20.xml")
				.deploy();

		repositoryService
				.createDeployment()
				.addClasspathResource(
						TEST_RES_PATH + "PrioJobExecutorTest.testTimerC.bpmn20.xml")
				.deploy();

		repositoryService
				.createDeployment()
				.addClasspathResource(
						TEST_RES_PATH + "PrioJobExecutorTest.testTimerE_furutre.bpmn20.xml")
				.deploy();

	}

	private AcquiredJobs getAcquiredJobs() {
		// aquire the jobs
		JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();
		jobExecutor.setAcquireJobsCmd(new AcquireJobsCmd(jobExecutor));
		return jobExecutor.getCommandExecutor().execute(
				jobExecutor.getAcquireJobsCmd());
	}

	private void deploymentCleanup() {

		for (Deployment dep : repositoryService.createDeploymentQuery().list()) {
			repositoryService.deleteDeployment(dep.getId(), true);
		}

		// cleaned up - no more jobs?
		assertEquals(0, managementService.createJobQuery().count());
	}

	private ExecutionEntity startProcessWithPriority(String processKey,
			String prio) {
		ExecutionEntity processInstance = null;

		if (prio != null) {
			Map<String, Object> variables = new HashMap<String, Object>();
			variables.put("PRIO", prio);
			processInstance = (ExecutionEntity) runtimeService
					.startProcessInstanceByKey(processKey, variables);
		} else {
			processInstance = (ExecutionEntity) runtimeService
					.startProcessInstanceByKey(processKey);
		}

		// A process with an Async task must have an entry in the Jobs table
		assertEquals(
				1,
				managementService.createJobQuery()
						.processInstanceId(processInstance.getId()).count());
		return processInstance;
	}

	private String getJobId(ExecutionEntity processInstance) {

		return managementService.createJobQuery()
				.processInstanceId(processInstance.getId()).singleResult()
				.getId();

	}

	private List<JobEntity> createMockedJobEntityListNonExclusive() {

		List<JobEntity> jobs = new ArrayList<JobEntity>();

		JobEntity mockedJobEntityA = mock(JobEntity.class);
		when(mockedJobEntityA.getId()).thenReturn("A");

		JobEntity mockedJobEntityB = mock(JobEntity.class);
		when(mockedJobEntityB.getId()).thenReturn("B");

		JobEntity mockedJobEntityC = mock(JobEntity.class);
		when(mockedJobEntityC.getId()).thenReturn("C");

		jobs.add(mockedJobEntityA);
		jobs.add(mockedJobEntityB);
		jobs.add(mockedJobEntityC);

		return jobs;
	}

	private List<JobEntity> createMockedJobEntityListMixedWithExclusiveJobs() {

		List<JobEntity> jobs = new ArrayList<JobEntity>();

		JobEntity mockedJobEntityA = mock(JobEntity.class);
		when(mockedJobEntityA.isExclusive()).thenReturn(false);
		when(mockedJobEntityA.getProcessInstanceId()).thenReturn("1");
		when(mockedJobEntityA.getId()).thenReturn("A");

		JobEntity mockedJobEntityB = mock(JobEntity.class);
		when(mockedJobEntityB.isExclusive()).thenReturn(true);
		when(mockedJobEntityB.getProcessInstanceId()).thenReturn("2");
		when(mockedJobEntityB.getId()).thenReturn("B");

		JobEntity mockedJobEntityC = mock(JobEntity.class);
		when(mockedJobEntityC.isExclusive()).thenReturn(false);
		when(mockedJobEntityC.getProcessInstanceId()).thenReturn("3");
		when(mockedJobEntityC.getId()).thenReturn("C");

		jobs.add(mockedJobEntityA);
		jobs.add(mockedJobEntityB);
		jobs.add(mockedJobEntityC);

		return jobs;
	}

	private List<JobEntity> createMockedJobEntityListExclusiveJobs() {

		List<JobEntity> jobs = new ArrayList<JobEntity>();

		JobEntity mockedJobEntity = mock(JobEntity.class);
		when(mockedJobEntity.isExclusive()).thenReturn(true);
		when(mockedJobEntity.getProcessInstanceId()).thenReturn("2");
		when(mockedJobEntity.getId()).thenReturn("B");
		jobs.add(mockedJobEntity);
		return jobs;
	}

	private List<JobEntity> findAllJobsInDB() {

		CommandExecutor commandExecutor = processEngineConfiguration
				.getCommandExecutorTxRequired();

		return commandExecutor.execute(new Command<List<JobEntity>>() {
			public List<JobEntity> execute(CommandContext commandContext) {
				return commandContext.getJobManager().findNextJobsToExecute(
						new Page(0, processEngineConfiguration.getJobExecutor()
								.getMaxJobsPerAcquisition()));
			}
		});

	}

}
