/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.niord.core.aton;

import org.hibernate.search.annotations.Field;
import org.niord.core.model.BaseEntity;
import org.niord.model.message.aton.AtonTagVo;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * An AtoN OSM seamark node tag entity.
 *
 * The AtoN model adheres to the OSM seamark specification, please refer to:
 * http://wiki.openstreetmap.org/wiki/Key:seamark
 * and sub-pages.
 */
@Entity
@Table(indexes = {
        @Index(name = "aton_tag_k", columnList="k"),
        @Index(name = "aton_tag_v", columnList="v")
})
@SuppressWarnings("unused")
public class AtonTag extends BaseEntity<Integer> {

    // Custom AtoN tags
    public static final String TAG_ATON_UID         = "seamark:ref";
    public static final String TAG_LIGHT_NUMBER     = "seamark:light:ref";
    public static final String TAG_INT_LIGHT_NUMBER = "seamark:light:int_ref";
    public static final String TAG_LOCALITY         = "seamark:locality";
    public static final String TAG_AIS_NUMBER       = "seamark:ais:ref";
    public static final String TAG_RACON_NUMBER     = "seamark:racon:ref";
    public static final String TAG_INT_RACON_NUMBER = "seamark:racon:int_ref";

    @NotNull
    String k;

    @Field
    String v;

    @ManyToOne
    @NotNull
    AtonNode atonNode;

    /** Constructor */
    public AtonTag() {
    }

    /** Constructor */
    public AtonTag(String k, String v) {
        this.k = k;
        this.v = v;
    }

    /** Constructor */
    public AtonTag(AtonTagVo tag, AtonNode atonNode) {
        Objects.requireNonNull(tag);
        this.k = tag.getK();
        this.v = tag.getV();
        this.atonNode = atonNode;
    }

    /** Converts this entity to a value object */
    public AtonTagVo toVo() {
        return new AtonTagVo(k, v);
    }

    /*************************/
    /** Getters and Setters **/
    /*************************/

    public String getK() {
        return k;
    }

    public void setK(String k) {
        this.k = k;
    }

    public String getV() {
        return v;
    }

    public void setV(String v) {
        this.v = v;
    }

    public AtonNode getAtonNode() {
        return atonNode;
    }

    public void setAtonNode(AtonNode atonNode) {
        this.atonNode = atonNode;
    }
}
