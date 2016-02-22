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
package org.niord.core.model;

import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.lang.StringUtils;
import org.niord.core.util.GeoJsonUtils;
import org.niord.model.vo.ChartVo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.validation.constraints.NotNull;

/**
 * Represents a chart
 */
@Entity
@SuppressWarnings("unused")
@NamedQueries({

        @NamedQuery(name  = "Chart.searchCharts",
                query = "select distinct c from Chart c where lower(c.chartNumber) like lower(:term) "
                        + "or str(c.internationalNumber) like lower(:term) "
                        + "or lower(c.name) like lower(:term) "),

        @NamedQuery(name  = "Chart.findAll",
                query = "select c from Chart c order by coalesce(scale, 99999999) asc, chartNumber"),

        @NamedQuery(name="Chart.findByChartNumber",
                query="SELECT chart FROM Chart chart where chart.chartNumber = :chartNumber"),

        @NamedQuery(name="Chart.findByChartNumbers",
                query="SELECT chart FROM Chart chart where chart.chartNumber IN :chartNumbers")
})
public class Chart extends VersionedEntity<Integer> {

    @NotNull
    @Column(unique = true)
    String chartNumber;

    Integer internationalNumber;

    @Column(columnDefinition = "GEOMETRY")
    Geometry geometry;

    String horizontalDatum;

    Integer scale;

    String name;

    /**
     * Constructor
     */
    public Chart() {
    }


    /**
     * Constructor
     */
    public Chart(String chartNumber, Integer internationalNumber) {
        this.chartNumber = chartNumber;
        this.internationalNumber = internationalNumber;
    }


    /**
     * Constructor
     */
    public Chart(ChartVo chart) {
        this.chartNumber = chart.getChartNumber();
        this.internationalNumber = chart.getInternationalNumber();
        this.geometry = GeoJsonUtils.toJts(chart.getGeometry());
        this.horizontalDatum = chart.getHorizontalDatum();
        this.scale = chart.getScale();
        this.name = chart.getName();
    }


    /** Converts this entity to a value object */
    public ChartVo toVo() {
        ChartVo chart = new ChartVo();
        chart.setChartNumber(chartNumber);
        chart.setInternationalNumber(internationalNumber);
        chart.setGeometry(GeoJsonUtils.fromJts(geometry));
        chart.setHorizontalDatum(horizontalDatum);
        chart.setScale(scale);
        chart.setName(name);
        return chart;
    }


    /**
     * Returns a string representation of the chart including chart number and international number
     * @return a string representation of the chart
     */
    public String toFullChartNumber() {
        return (internationalNumber == null)
                ? chartNumber
                : String.format("%s (INT %d)", chartNumber, internationalNumber);
    }


    /**
     * Computes a text search sort key for the given term.
     * Used to sort a list of matching charts, i.e. where one of "chartNumber", "internationalNumber" and "name"
     * is known to match. Use e.g. with search result from "Chart.searchCharts".
     * Prioritize, so that chartNumber rates higher than name match, which rates higher than internationalNumber match
     */
    public int computeSearchSortKey(String term) {
        if (StringUtils.isBlank(term)) {
            return 0;
        }
        term = term.toLowerCase();
        int indexChartNumber = chartNumber.toLowerCase().indexOf(term);
        int indexInternationalNumber = internationalNumber == null ? -1 : String.valueOf(internationalNumber).indexOf(term);
        int indexName = name == null ? -1 : name.toLowerCase().indexOf(term);

        int sortKey = 0;
        if (indexInternationalNumber != -1) {
            sortKey += indexInternationalNumber;
        } else if (indexName != -1) {
            sortKey += 5 * indexName;
        } else if (indexChartNumber != -1) {
            sortKey += 10 * indexChartNumber;
        }
        return -sortKey;
    }

    /*************************/
    /** Getters and Setters **/
    /*************************/

    public String getChartNumber() {
        return chartNumber;
    }

    public void setChartNumber(String chartNumber) {
        this.chartNumber = chartNumber;
    }

    public Integer getInternationalNumber() {
        return internationalNumber;
    }

    public void setInternationalNumber(Integer internationalNumber) {
        this.internationalNumber = internationalNumber;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public String getHorizontalDatum() {
        return horizontalDatum;
    }

    public void setHorizontalDatum(String horizontalDatum) {
        this.horizontalDatum = horizontalDatum;
    }

    public Integer getScale() {
        return scale;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
