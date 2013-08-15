 package org.activiti.engine.impl.cmd;

import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.pvm.runtime.AtomicOperation;

public class MoveTokenCommand implements Command<Void> {

	  private String executionId;
	  private String targetActivityId;
	  
	  public MoveTokenCommand(String id, String activityId) {
		  executionId = id;
		  targetActivityId = activityId;
	  }
	
	 public Void execute(CommandContext commandContext) {

		    ExecutionEntity execution = commandContext.getExecutionManager().findExecutionById(this.executionId);

		    execution.setActivity(execution.getProcessDefinition().findActivity(this.targetActivityId));

		    AtomicOperation.TRANSITION_CREATE_SCOPE.execute(execution);

		    return null;
	 }	
}