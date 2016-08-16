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
package org.niord.model.message;

import io.swagger.annotations.ApiModel;
import org.niord.model.IJsonSerializable;

import javax.xml.bind.annotation.XmlType;
import java.util.Date;

/**
 * Represents a single date interval for a message
 */
@ApiModel(value = "DateInterval", description = "Date interval")
@XmlType(propOrder = {
        "allDay", "fromDate", "toDate"
})
public class DateIntervalVo implements IJsonSerializable {

    Boolean allDay;
    Date fromDate;
    Date toDate;

    /*************************/
    /** Getters and Setters **/
    /*************************/

    public Boolean getAllDay() {
        return allDay;
    }

    public void setAllDay(Boolean allDay) {
        this.allDay = allDay;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }
}