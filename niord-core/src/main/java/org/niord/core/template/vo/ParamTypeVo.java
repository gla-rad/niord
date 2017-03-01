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

package org.niord.core.template.vo;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.niord.model.IJsonSerializable;

/**
 * Abstract value object super class for the {@code ParamType} derived model entity
 */
@JsonTypeInfo(use= JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
        //@JsonSubTypes.Type(value=BaseParamTypeVo.class, name="BASE"),
        //@JsonSubTypes.Type(value=CompositeParamTypeVo.class, name="COMPOSITE"),
        @JsonSubTypes.Type(value=ListParamTypeVo.class, name="LIST")
})
public abstract class ParamTypeVo implements IJsonSerializable {

    Integer id;
    String name;

    /**
     * Constructor
     */
    public ParamTypeVo() {
    }

    /*************************/
    /** Getters and Setters **/
    /*************************/

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}