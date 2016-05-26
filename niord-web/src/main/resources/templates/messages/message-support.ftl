
<#macro messageStyles>
    <style type="text/css" media="all">

        @page {
            size: a4 portrait;
            margin: 1.5cm 1.5cm;
            padding:0;
        }

        body{
            font-size:10px;
            font-family: Helvetica;
            margin: 0;
            padding:0;
        }

        a {
            color: #000000;
        }

        .page-break  {
            clear: left;
            display:block;
            page-break-after:always;
            margin: 0;
            padding: 0;
            height: 0;
        }

        table.message-table tr, table.message-table tr {
            page-break-inside: avoid;
        }

        .table-image {
            vertical-align: top;
            padding: 10px;
            border-top: 1px solid lightgray;
        }

        .table-item {
            vertical-align: top;
            width: 100%;
            padding: 10px 10px 10px 40px;
            border-top: 1px solid lightgray;
        }

        .field-name {
            white-space: nowrap;
            vertical-align: top;
            font-style: italic;
            padding-right: 10px;
            text-align: right;
        }

        .field-value {
            vertical-align: top;
            width: 100%;
        }

        .field-value ol {
            padding-left: 0;
        }

    </style>
</#macro>


<#macro areaLineage area>
    <#if area??>
        <#if area.parent??>
            <@areaLineage area=area.parent /> -
        </#if>
        <#if area.descs?has_content>${area.descs[0].name}</#if>
    </#if>
</#macro>


<#macro renderMessage msg>
<!-- Title line -->
<#if msg.originalInformation?has_content && msg.originalInformation>
    <div>*</div>
</#if>

<#if msg.shortId?has_content>
    <div>
        <strong>${msg.shortId}.</strong>
    </div>
</#if>

<#if msg.descs?has_content && msg.descs[0].title?has_content>
    <div>
        <strong>
            <a href="${baseUri}/#/message/${msg.id?c}" target="_blank">
                ${msg.descs[0].title}
            </a>
        </strong>
    </div>
</#if>


<table>

    <!-- Reference lines -->
    <#if msg.references?has_content>
        <tr>
            <td class="field-name">
                ${text("msg.field.reference")}
            </td>
            <td class="field-value">
                <#list msg.references as ref>
                    <div>
                        <a href="${baseUri}/#/message/${ref.messageId}" target="_blank">${ref.messageId}</a>

                        <#if ref.type == 'REPETITION'>
                            ${text("msg.reference.repetition")}
                        <#elseif ref.type == 'CANCELLATION'>
                            ${text("msg.reference.cancelled")}
                        <#elseif ref.type == 'UPDATE'>
                            ${text("msg.reference.updated")}
                        </#if>

                        <#if ref.description?has_content>
                            ${ref.description}
                        </#if>
                    </div>
                </#list>
            </td>
        </tr>
    </#if>


    <!-- Time line -->
    <tr>
        <td class="field-name">${text("msg.field.time")}</td>
        <td class="field-value">
            <#if msg.descs?has_content && msg.descs[0].time?has_content>
                <div>${msg.descs[0].time}</div>
            <#elseif msg.dateIntervals?has_content>
                <#list msg.dateIntervals as dateInterval>
                    <div>
                        ${dateInterval.fromDate?datetime}
                        <#if dateInterval.toDate?has_content>
                            - ${dateInterval.toDate?datetime}
                        </#if>
                    </div>
                </#list>
            </#if>
        </td>
    </tr>


    <!-- Geometry line -->
    <#if msg.geometry?has_content && msg.geometry.features?has_content>
        <tr>
            <td class="field-name">${text("msg.field.positions")}</td>
            <td class="field-value">
                <#list msg.geometry.features as feature>
                    feature
                </#list>
            </td>
        </tr>
    </#if>


    <!-- Details line -->
    <#if msg.descs?has_content && msg.descs[0].description?has_content>
        <tr>
            <td class="field-name">${text("msg.field.details")}</td>
            <td class="field-value">
            ${msg.descs[0].description}
            </td>
        </tr>
    </#if>

    <!-- Note line -->
    <#if msg.descs?has_content && msg.descs[0].note?has_content>
        <tr>
            <td class="field-name">${text("msg.field.note")}</td>
            <td class="field-value">
            ${msg.descs[0].note}
            </td>
        </tr>
    </#if>

    <!-- Charts line -->
    <#if msg.charts?has_content>
        <tr>
            <td class="field-name">${text("msg.field.charts")}</td>
            <td class="field-value">
                <#list msg.charts as chart>
                ${chart.chartNumber}
                    <#if chart.internationalNumber?has_content>(INT ${chart.internationalNumber?c})</#if>
                    <#if chart_has_next>, </#if>
                </#list>
            </td>
        </tr>
    </#if>

    <!-- Publication line -->
    <#if msg.descs?has_content && msg.descs[0].publication?has_content>
        <tr>
            <td class="field-name">${text("msg.field.publication")}</td>
            <td class="field-value">
            ${msg.descs[0].publication}
            </td>
        </tr>
    </#if>

    <!-- Source line -->
    <#if msg.descs?has_content && msg.descs[0].source?has_content>
        <tr>
            <td class="field-name" colspan="2">
                (${msg.descs[0].source})
            </td>
        </tr>
    </#if>

</table>
</#macro>