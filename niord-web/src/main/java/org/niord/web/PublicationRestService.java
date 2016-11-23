/*
 * Copyright 2016 Danish Maritime Authority.
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
import org.niord.core.publication.Publication;
import org.niord.core.publication.PublicationSearchParams;
import org.niord.core.publication.PublicationService;
import org.niord.model.publication.PublicationFileType;
import org.niord.core.publication.vo.SystemPublicationVo;
import org.niord.core.user.UserService;
import org.niord.core.publication.vo.MessagePublication;
import org.niord.model.publication.PublicationVo;
import org.niord.core.util.TextUtils;
import org.niord.model.DataFilter;
import org.slf4j.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * REST interface for accessing publications.
 */
@Path("/publications")
@Stateless
@SecurityDomain("keycloak")
@PermitAll
public class PublicationRestService extends AbstractBatchableRestService {

    @Inject
    Logger log;

    @Inject
    PublicationService publicationService;

    @Inject
    UserService userService;


    /** Searches publications based on the given search parameters */
    @GET
    @Path("/search")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public List<PublicationVo> searchPublications(
            @QueryParam("lang") String lang,
            @QueryParam("domain") String domain,
            @QueryParam("type") String type,
            @QueryParam("messagePublication") MessagePublication messagePublication,
            @QueryParam("fileType") PublicationFileType fileType,
            @QueryParam("title") @DefaultValue("") String title,
            @QueryParam("limit") @DefaultValue("100") int limit) {

        PublicationSearchParams params = new PublicationSearchParams()
                .language(lang)
                .domain(domain)
                .type(type)
                .messagePublication(messagePublication)
                .fileType(fileType)
                .title(title);
        params.maxSize(limit);

        DataFilter dataFilter = DataFilter.get().lang(lang);

        return publicationService.searchPublications(params).stream()
                .map(p -> p.toVo(PublicationVo.class, dataFilter))
                .sorted(publicationTitleComparator(lang))
                .collect(Collectors.toList());
    }


    /** Returns all publications up to the given limit */
    @GET
    @Path("/all")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public List<PublicationVo> getAllPublications(
            @QueryParam("lang") String lang,
            @QueryParam("limit") @DefaultValue("1000") int limit) {
        DataFilter dataFilter = DataFilter.get().lang(lang);
        return publicationService.getPublications().stream()
                .limit(limit)
                .map(p -> p.toVo(PublicationVo.class, dataFilter))
                .sorted(publicationTitleComparator(lang))
                .collect(Collectors.toList());
    }


    /** Returns the publication with the given ID */
    @GET
    @Path("/publication/{publicationId}")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public SystemPublicationVo getPublication(@PathParam("publicationId") String publicationId) throws Exception {
        return publicationService.findByPublicationId(publicationId)
                .toVo(SystemPublicationVo.class, DataFilter.get());
    }


    /** Creates a new publication */
    @POST
    @Path("/publication/")
    @Consumes("application/json;charset=UTF-8")
    @Produces("application/json;charset=UTF-8")
    @RolesAllowed({ "admin" })
    @GZIP
    @NoCache
    public SystemPublicationVo createPublication(SystemPublicationVo publication) throws Exception {
        log.info("Creating publication " + publication);
        return publicationService.createPublication(new Publication(publication))
                .toVo(SystemPublicationVo.class, DataFilter.get());
    }


    /** Updates an existing publication */
    @PUT
    @Path("/publication/{publicationId}")
    @Consumes("application/json;charset=UTF-8")
    @Produces("application/json;charset=UTF-8")
    @RolesAllowed({ "admin" })
    @GZIP
    @NoCache
    public SystemPublicationVo updatePublication(
            @PathParam("publicationId") String publicationId,
            SystemPublicationVo publication) throws Exception {

        if (!Objects.equals(publicationId, publication.getPublicationId())) {
            throw new WebApplicationException(400);
        }

        log.info("Updating publication " + publicationId);
        return publicationService.updatePublication(new Publication(publication))
                .toVo(SystemPublicationVo.class, DataFilter.get());
    }


    /** Deletes an existing publication */
    @DELETE
    @Path("/publication/{publicationId}")
    @Consumes("application/json;charset=UTF-8")
    @RolesAllowed({ "admin" })
    @GZIP
    @NoCache
    public void deletePublication(@PathParam("publicationId") String publicationId) throws Exception {
        log.info("Deleting publication " + publicationId);
        publicationService.deletePublication(publicationId);
    }


    /**
     * Returns all publications for export purposes
     *
     * If not called via Ajax, pass a ticket request parameter along, which
     * can be requested via Ajax call to /rest/tickets/ticket?role=admin
     */
    @GET
    @Path("/export")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public List<PublicationVo> exportPublications(
            @QueryParam("lang") String lang) {

        // If a ticket is defined, check if programmatically
        if (!userService.isCallerInRole("admin")) {
            throw new WebApplicationException(403);
        }

        DataFilter dataFilter = DataFilter.get().lang(lang);
        return publicationService.getPublications().stream()
                .map(p -> p.toVo(SystemPublicationVo.class, dataFilter))
                .sorted(publicationTitleComparator(lang))
                .collect(Collectors.toList());
    }


    /**
     * Imports an uploaded Publications json file
     *
     * @param request the servlet request
     * @return a status
     */
    @POST
    @Path("/upload-publications")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("text/plain")
    @RolesAllowed("admin")
    public String importPublications(@Context HttpServletRequest request) throws Exception {
        return executeBatchJobFromUploadedFile(request, "publication-import");
    }

    /** Returns a publication name comparator **/
    private Comparator<PublicationVo> publicationTitleComparator(String lang) {
        return (p1, p2) -> {
            String n1 = p1.getDesc(lang) != null ? p1.getDesc(lang).getTitle() : null;
            String n2 = p2.getDesc(lang) != null ? p2.getDesc(lang).getTitle() : null;
            return TextUtils.compareIgnoreCase(n1, n2);
        };
    }

}
