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

package org.activiti.rest.api.process;

import java.util.List;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ManagementService;
import org.activiti.engine.runtime.Job;
import org.activiti.rest.api.ActivitiUtil;
import org.activiti.rest.api.SecuredResource;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.restlet.resource.Delete;

public class ProcessInstanceJobsResource extends SecuredResource {

	@Delete
	public ObjectNode deleteProcessInstanceJobs() {
		try {
			if (authenticate() == false)
				return null;

			String processInstanceId = (String) getRequest().getAttributes()
					.get("processInstanceId");

			if (processInstanceId == null) {
				throw new ActivitiException("No process instance id provided");
			}

			ManagementService managementService = ActivitiUtil
					.getManagementService();
			List<Job> jobs = managementService.createJobQuery()
					.processInstanceId(processInstanceId).list();
			for (Job job : jobs) {
				managementService.deleteJob(job.getId());
			}

			ObjectNode successNode = new ObjectMapper().createObjectNode();
			successNode.put("success", true);
			return successNode;

		} catch (Exception e) {
			throw new ActivitiException("Failed to delete process jobs", e);
		}
	}

}
