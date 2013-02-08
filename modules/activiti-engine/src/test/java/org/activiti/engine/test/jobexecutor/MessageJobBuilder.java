package org.activiti.engine.test.jobexecutor;

import java.util.Date;

import org.activiti.engine.impl.persistence.entity.MessageEntity;

public class MessageJobBuilder extends JobBuilder {

	
	public static JobBuilder newBuilder(){
		return new MessageJobBuilder();
	}
	
	private MessageJobBuilder(){
		job = new MessageEntity();
	}
	
	@Override
	public JobBuilder createNewJob() {
		job = new MessageEntity();
		return this;
	}
	
	@Override
	public JobBuilder setRetries(int r) {
		job.setRetries(r);
		return this;
	}

	@Override
	public JobBuilder setId(String id) {
		job.setId(id);
		return this;
	}

	@Override
	public JobBuilder setExecutionId(String e) {
		job.setExecutionId(e);
		return this;
	}

	@Override
	public JobBuilder setPriority(int p) {
		job.setPriority(p);
		return this;
	}

	@Override
	public JobBuilder setDuedate(Date d) {
		job.setDuedate(d);
		return this;
	}

	@Override
	public JobBuilder setProcessDefinitionId(String pId) {
		job.setProcessDefinitionId(pId);
		return this;
	}

	@Override
	public JobBuilder setProcessInstanceId(String pI) {
		job.setProcessInstanceId(pI);
		return this;
	}
	
}
