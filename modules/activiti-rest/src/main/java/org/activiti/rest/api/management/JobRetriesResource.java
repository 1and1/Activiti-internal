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

package org.activiti.rest.api.management;

import org.activiti.engine.ActivitiException;
import org.activiti.rest.api.ActivitiUtil;
import org.activiti.rest.api.SecuredResource;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;

public class JobRetriesResource extends SecuredResource {

	@Post
	public ObjectNode updateJobRetriesOperation(Representation entity) {
		try {
			if (authenticate(SecuredResource.ADMIN) == false) {
				return null;
			}

			String jobId = (String) getRequest().getAttributes().get("jobId");
			int retries = new ObjectMapper().readTree(entity.getText())
					.get("retries").getIntValue();
			ActivitiUtil.getManagementService().setJobRetries(jobId, retries);
			
			ObjectNode successNode = new ObjectMapper().createObjectNode();
			successNode.put("success", true);
			return successNode;
			
		} catch (Exception e) {
			throw new ActivitiException("Failed to update job retries", e);
		}
	}
}
