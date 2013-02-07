package org.activiti.engine.test.jobexecutor;

import java.util.Date;

import org.activiti.engine.impl.persistence.entity.JobEntity;

public abstract class JobBuilder {

    protected JobEntity job;  
    
    public JobEntity getJobEntity() { return job; }  

    public abstract JobBuilder createNewJob();
    public abstract JobBuilder setRetries(int r);
    public abstract JobBuilder setId(String id);
    public abstract JobBuilder setExecutionId(String e);
    public abstract JobBuilder setPriority(int p);
    public abstract JobBuilder setDuedate(Date d);
    public abstract JobBuilder setProcessDefinitionId(String pId);
    public abstract JobBuilder setProcessInstanceId(String pI);
   
	
}
