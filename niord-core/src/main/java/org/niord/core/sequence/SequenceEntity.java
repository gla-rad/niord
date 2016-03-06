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
package org.niord.core.sequence;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Used internally by {@linkplain SequenceService} to
 * manage sequences.
 */
@Entity
@Table(name="Sequence",
    indexes = {
        @Index(name = "sequence_name", columnList="name", unique = true)
})
public class SequenceEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    String name;

    long lastValue;

    @Id
    @Column(unique = true, nullable = false)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getLastValue() { return lastValue; }
    public void setLastValue(long lastValue) { this.lastValue = lastValue; }
}