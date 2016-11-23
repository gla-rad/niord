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

package org.niord.core.publication;

import org.niord.core.publication.vo.MessagePublication;
import org.niord.model.publication.PublicationFileType;
import org.niord.model.search.PagedSearchParamsVo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Defines the publication search parameters
 */
@SuppressWarnings("unused")
public class PublicationSearchParams extends PagedSearchParamsVo {

    String language;
    String title;
    String domain;    // If defined, only include publications for the given domain
    String type;
    PublicationFileType fileType;
    MessagePublication messagePublication;

    /**
     * Returns a string representation of the search criteria
     * @return a string representation of the search criteria
     */
    @Override
    public String toString() {
        List<String> desc = new ArrayList<>();
        if (messagePublication != null) { desc.add(String.format("MessagePublication: %s", messagePublication)); }
        if (fileType != null) { desc.add(String.format("FileType: %s", fileType)); }
        if (isNotBlank(language)) { desc.add(String.format("Language: %s", language)); }
        if (isNotBlank(domain)) { desc.add(String.format("Domain: %s", domain)); }
        if (isNotBlank(type)) { desc.add(String.format("Type: %s", type)); }
        if (isNotBlank(title)) { desc.add(String.format("Title: '%s'", title)); }
        if (isNotBlank(domain)) { desc.add(String.format("Domain: '%s'", domain)); }

        return desc.stream().collect(Collectors.joining(", "));
    }

    /*******************************************/
    /** Method chaining Getters and Setters   **/
    /*******************************************/


    public String getLanguage() {
        return language;
    }

    public PublicationSearchParams language(String language) {
        this.language = language;
        return this;
    }


    public String getTitle() {
        return title;
    }

    public PublicationSearchParams title(String title) {
        this.title = title;
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public PublicationSearchParams domain(String domain) {
        this.domain = domain;
        return this;
    }

    public String getType() {
        return type;
    }

    public PublicationSearchParams type(String type) {
        this.type = type;
        return this;
    }

    public MessagePublication getMessagePublication() {
        return messagePublication;
    }

    public PublicationSearchParams messagePublication(MessagePublication messagePublication) {
        this.messagePublication = messagePublication;
        return this;
    }

    public PublicationFileType getFileType() {
        return fileType;
    }

    public PublicationSearchParams fileType(PublicationFileType fileType) {
        this.fileType = fileType;
        return this;
    }
}
