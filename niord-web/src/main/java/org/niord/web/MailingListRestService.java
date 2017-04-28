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

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.security.annotation.SecurityDomain;
import org.niord.core.mail.IMailable;
import org.niord.core.mail.MailingList;
import org.niord.core.mail.MailingListSearchParams;
import org.niord.core.mail.MailingListService;
import org.niord.core.mail.vo.MailingListVo;
import org.niord.core.user.Contact;
import org.niord.core.user.ContactService;
import org.niord.core.user.Roles;
import org.niord.core.user.User;
import org.niord.core.user.UserService;
import org.niord.core.user.vo.ContactVo;
import org.niord.core.user.vo.UserVo;
import org.niord.model.DataFilter;
import org.niord.model.IJsonSerializable;
import org.slf4j.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST interface for accessing mailing lists.
 */
@Path("/mailing-lists")
@Stateless
@SecurityDomain("keycloak")
@RolesAllowed(Roles.ADMIN)
public class MailingListRestService {

    @Inject
    Logger log;

    @Inject
    MailingListService mailingListService;

    @Inject
    UserService userService;

    @Inject
    ContactService contactService;


    /***************************************/
    /** Mailing list retrieval            **/
    /***************************************/


    /** Returns all mailing lists that matches the search criteria */
    @GET
    @Path("/search")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public List<MailingListVo> search(
            @QueryParam("name") String name,
            @QueryParam("lang") @DefaultValue("en") String lang) {

        MailingListSearchParams params = new MailingListSearchParams()
                .language(lang)
                .name(name);

        DataFilter filter = MailingList.MESSAGE_DETAILS_FILTER.lang(lang);
        
        return mailingListService.searchMailingLists(params).stream()
                .map(m -> m.toVo(filter))
                .collect(Collectors.toList());
    }


    /**
     * Returns the details of the mailing list with the given id
     * @param mailingListId the ID of the mailing list
     */
    @GET
    @Path("/mailing-list/{mailingListId}")
    @Produces("application/json;charset=UTF-8")
    @NoCache
    public MailingListVo getMailingListDetails(@PathParam("mailingListId") String mailingListId) {

        log.info("Returning details of mailing list " + mailingListId);
        MailingList mailingList = mailingListService.findByMailingListId(mailingListId);
        if (mailingList == null) {
            throw new WebApplicationException("Mailing list " + mailingListId + " not found", 404);
        }
        return mailingList.toVo(MailingList.MESSAGE_DETAILS_AND_RECIPIENTS_FILTER);
    }


    /***************************************/
    /** Mailing list life cycle           **/
    /***************************************/

    /**
     * Creates a new mailing list
     * @param mailingList the template mailing list to add
     */
    @POST
    @Path("/mailing-list/")
    @Consumes("application/json;charset=UTF-8")
    @Produces("application/json;charset=UTF-8")
    @RolesAllowed(Roles.SYSADMIN)
    @NoCache
    public MailingListVo addMailingList(MailingListVo mailingList) throws Exception {
        log.info("Creating mailing list " + mailingList.getMailingListId());
        return mailingListService.createMailingList(new MailingList(mailingList)).toVo(DataFilter.get());
    }


    /**
     * Updates the mailing list  with the given id
     * @param mailingListId the ID of the mailing list
     * @param mailingList the template mailing list to update
     * @noinspection all
     */
    @PUT
    @Path("/mailing-list/{mailingListId}")
    @Consumes("application/json;charset=UTF-8")
    @Produces("application/json;charset=UTF-8")
    @RolesAllowed(Roles.SYSADMIN)
    @NoCache
    public MailingListVo updateMailingList(
            @PathParam("mailingListId") String mailingListId,
            MailingListVo mailingList) throws Exception {

        if (!Objects.equals(mailingListId, mailingList.getMailingListId())) {
            throw new WebApplicationException(400);
        }
        log.info("Updating mailing list " + mailingList.getMailingListId());
        return mailingListService.updateMailingList(new MailingList(mailingList)).toVo(DataFilter.get());
    }


    /**
     * Deletes the mailing list with the given id
     * @param mailingListId the mailingList to delete
     */
    @DELETE
    @Path("/mailing-list/{mailingListId}")
    @RolesAllowed(Roles.SYSADMIN)
    @NoCache
    public void deleteMailingList(@PathParam("mailingListId") String mailingListId) throws Exception {
        log.info("Deleting mailing list " + mailingListId);
        mailingListService.deleteMailingList(mailingListId);
    }


    /***************************************/
    /** Mailing list recipients           **/
    /***************************************/


    /** Returns recipient users and available users for the given mailing list */
    @GET
    @Path("/mailing-list/{mailingListId}/users")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public MailingListRecipients getRecipientUsers(@PathParam("mailingListId") String mailingListId) {
        log.info("Returning recipient users of mailing list " + mailingListId);
        MailingList mailingList = mailingListService.findByMailingListId(mailingListId);
        if (mailingList == null) {
            throw new WebApplicationException("Mailing list " + mailingListId + " not found", 404);
        }

        MailingListRecipients<UserVo> recipientData = new MailingListRecipients<>();

        // Compute selected recipients
        recipientData.setSelectedRecipients(mailingList.getUsers().stream()
            .map(User::toVo)
            .collect(Collectors.toList()));

        // Compute available recipients
        Set<String> emailLookup = recipientData.getSelectedRecipients().stream()
                .map(UserVo::getEmail)
                .collect(Collectors.toSet());
        recipientData.setAvailableRecipients(userService.allUsers().stream()
            .filter(u -> StringUtils.isNotBlank(u.getEmail()) && !emailLookup.contains(u.getEmail()))
            .map(User::toVo)
            .collect(Collectors.toList()));

        return recipientData.sortRecipients();
    }


    /** Update the recipient users of the given mailing list */
    @PUT
    @Path("/mailing-list/{mailingListId}/users")
    @Consumes("application/json;charset=UTF-8")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public MailingListRecipients updateRecipientUsers(@PathParam("mailingListId") String mailingListId, List<UserVo> users) throws Exception {

        // Update the recipient users
        log.info("Updating " + users.size() + " recipient users of mailing list " + mailingListId);
        MailingListVo mailingList = getMailingListDetails(mailingListId);
        mailingList.setUsers(users);
        updateMailingList(mailingListId, mailingList);

        // Returns the updated recipient users
        return getRecipientUsers(mailingListId);
    }


    /** Returns recipient contacts and available contacts for the given mailing list */
    @GET
    @Path("/mailing-list/{mailingListId}/contacts")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public MailingListRecipients getRecipientContacts(@PathParam("mailingListId") String mailingListId) {
        log.info("Returning recipient contacts of mailing list " + mailingListId);
        MailingList mailingList = mailingListService.findByMailingListId(mailingListId);
        if (mailingList == null) {
            throw new WebApplicationException("Mailing list " + mailingListId + " not found", 404);
        }

        MailingListRecipients<ContactVo> recipientData = new MailingListRecipients<>();

        // Compute selected recipients
        recipientData.setSelectedRecipients(mailingList.getContacts().stream()
            .map(Contact::toVo)
            .collect(Collectors.toList()));

        // Compute available recipients
        Set<String> emailLookup = recipientData.getSelectedRecipients().stream()
                .map(ContactVo::getEmail)
                .collect(Collectors.toSet());
        recipientData.setAvailableRecipients(contactService.getAllContacts().stream()
            .filter(u -> StringUtils.isNotBlank(u.getEmail()) && !emailLookup.contains(u.getEmail()))
            .map(Contact::toVo)
            .collect(Collectors.toList()));

        return recipientData.sortRecipients();
    }


    /** Update the recipient contacts of the given mailing list */
    @PUT
    @Path("/mailing-list/{mailingListId}/contacts")
    @Consumes("application/json;charset=UTF-8")
    @Produces("application/json;charset=UTF-8")
    @GZIP
    @NoCache
    public MailingListRecipients updateRecipientContacts(@PathParam("mailingListId") String mailingListId, List<ContactVo> contacts) throws Exception {

        // Update the recipient contacts
        log.info("Updating " + contacts.size() + " recipient contacts of mailing list " + mailingListId);
        MailingListVo mailingList = getMailingListDetails(mailingListId);
        mailingList.setContacts(contacts);
        updateMailingList(mailingListId, mailingList);

        // Returns the updated recipient contacts
        return getRecipientContacts(mailingListId);
    }


    /***************************************/
    /** Helper Classes                    **/
    /***************************************/


    @SuppressWarnings("unused")
    public static class MailingListRecipients<T extends IMailable> implements IJsonSerializable {
        List<T> selectedRecipients = new ArrayList<>();
        List<T> availableRecipients = new ArrayList<>();

        public MailingListRecipients<T> sortRecipients() {
            IMailable.sortByEmail(selectedRecipients);
            IMailable.sortByEmail(availableRecipients);
            return this;
        }

        public List<T> getSelectedRecipients() {
            return selectedRecipients;
        }

        public void setSelectedRecipients(List<T> selectedRecipients) {
            this.selectedRecipients = selectedRecipients;
        }

        public List<T> getAvailableRecipients() {
            return availableRecipients;
        }

        public void setAvailableRecipients(List<T> availableRecipients) {
            this.availableRecipients = availableRecipients;
        }
    }
}
