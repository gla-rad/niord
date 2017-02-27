/*
 * Copyright 2017 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.niord.web;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.security.annotation.SecurityDomain;
import org.niord.core.batch.AbstractBatchableRestService;
import org.niord.core.template.Template;
import org.niord.core.template.TemplateService;
import org.niord.core.template.vo.TemplateVo;
import org.niord.core.user.Roles;
import org.niord.core.user.UserService;
import org.slf4j.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * REST interface for accessing templates.
 */
@Path("/templates")
@Stateless
@SecurityDomain("keycloak")
@RolesAllowed(Roles.SYSADMIN)
@SuppressWarnings("unused")
public class TemplateRestService extends AbstractBatchableRestService {

    @Inject
    Logger log;

    @Inject
    UserService userService;

    @Inject
    TemplateService templateService;

    
    /** Returns all templates */
    @GET
    @Path("/all")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @PermitAll
    @NoCache
    public List<TemplateVo> getTemplates() {

        // If a ticket is defined, check if programmatically
        if (!userService.isCallerInRole(Roles.ADMIN)) {
            throw new WebApplicationException(403);
        }

        return templateService.findAll().stream()
                .map(Template::toVo)
                .collect(Collectors.toList());
    }


    /** Returns all template for the given category and current domain */
    @GET
    @Path("/category/{categoryId}")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @PermitAll
    @NoCache
    public List<TemplateVo> getTemplatesForCategory(@PathParam("categoryId") Integer categoryId) {

        return templateService.findByCategoryAndCurrentDomain(categoryId).stream()
                .map(Template::toVo)
                .collect(Collectors.toList());
    }


    /** Returns the template with the given ID */
    @GET
    @Path("/template/{id}")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public TemplateVo getTemplate(@PathParam("id") Integer id) throws Exception {
        return templateService.findById(id)
                .toVo();
    }


    /** Creates a new template */
    @POST
    @Path("/template/")
    @Consumes("application/json;charset=UTF-8")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public TemplateVo createTemplate(TemplateVo template) throws Exception {
        log.info("Creating template " + template);
        return templateService.createTemplate(new Template(template))
                .toVo();
    }


    /** Updates an existing template */
    @PUT
    @Path("/template/{id}")
    @Consumes("application/json;charset=UTF-8")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public TemplateVo updateTemplate(@PathParam("id") Integer id, TemplateVo template) throws Exception {
        if (!Objects.equals(id, template.getId())) {
            throw new WebApplicationException(400);
        }

        log.info("Updating template " + template);
        return templateService.updateTemplate(new Template(template))
                .toVo();
    }


    /** Deletes an existing template */
    @DELETE
    @Path("/template/{id}")
    @GZIP
    @NoCache
    public void deleteTemplate(@PathParam("id") Integer id) throws Exception {
        log.info("Deleting template " + id);
        templateService.deleteTemplate(id);
    }


    /**
     * Imports an uploaded templates json file
     *
     * @param request the servlet request
     * @return a status
     */
    @POST
    @Path("/upload-templates")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("text/plain")
    @RolesAllowed(Roles.SYSADMIN)
    public String importTemplates(@Context HttpServletRequest request) throws Exception {
        return executeBatchJobFromUploadedFile(request, "template-import");
    }

}
