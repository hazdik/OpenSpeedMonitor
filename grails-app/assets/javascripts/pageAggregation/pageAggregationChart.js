//= require /bower_components/d3/d3.min.js
//= require /chartComponents/chartBars.js
//= require /chartComponents/chartBarScore.js
//= require /d3/chartColorProvider.js
//= require /chartComponents/chartLegend.js
//= require /chartComponents/chartSideLabels.js
//= require /chartComponents/chartHeader.js
//= require /d3/chartLabelUtil.js
//= require_self

"use strict";

var OpenSpeedMonitor = OpenSpeedMonitor || {};
OpenSpeedMonitor.ChartModules = OpenSpeedMonitor.ChartModules || {};

OpenSpeedMonitor.ChartModules.PageAggregation = (function (selector) {
    var svg = d3.select(selector);
    var chartBarsComponents = {};
    var chartLegendComponent = OpenSpeedMonitor.ChartComponents.ChartLegend();
    var chartBarScoreComponent = OpenSpeedMonitor.ChartComponents.ChartBarScore();
    var chartSideLabelsComponent = OpenSpeedMonitor.ChartComponents.ChartSideLabels();
    var chartHeaderComponent = OpenSpeedMonitor.ChartComponents.ChartHeader();
    var componentMargin = 15;
    var transitionDuration = 500;
    var chartSideLabelsWidth = 200;
    var chartBarsWidth = 700;
    var fullWidth = chartSideLabelsWidth + chartBarsWidth;
    var chartBarsHeight = 400;
    var measurandDataEntries = {};
    var measurandGroupDataMap = {};
    var sideLabelData = [];
    var headerText = "";
    var stackBars = false;
    var autoWidth = true;

    chartLegendComponent.on("select", function (selectEvent) {
        toggleBarComponentHighlight(selectEvent.id, selectEvent.anySelected, selectEvent.selected);
        render();
    });

    chartLegendComponent.on("highlight", function (highlightEvent) {
        toggleBarComponentHighlight(highlightEvent.id, highlightEvent.anyHighlighted, highlightEvent.highlighted);
        render();
    });

    var setData = function (data) {
        if (data.series) {
            var chartLabelUtils = OpenSpeedMonitor.ChartModules.ChartLabelUtil(data.series, data.i18nMap);
            headerText = chartLabelUtils.getCommonLabelParts(true);
            sideLabelData = chartLabelUtils.getSeriesWithShortestUniqueLabels(true).map(function (s) { return s.label;});
            measurandDataEntries = extractMeasurandData(data.series);
            measurandGroupDataMap = extractMeasurandGroupData(data.series);
        }
        stackBars = data.stackBars !== undefined ? data.stackBars : stackBars;
        fullWidth = data.width || fullWidth;
        autoWidth = data.autoWidth !== undefined ? data.autoWidth : autoWidth;
        fullWidth = autoWidth ? getActualSvgWidth() : fullWidth;
        chartSideLabelsWidth = chartSideLabelsComponent.estimateWidth(svg, sideLabelData);
        chartBarsWidth = fullWidth - componentMargin - chartSideLabelsWidth;
        chartBarsHeight = calculateChartBarsHeight();

        setDataForHeader();
        setDataForLegend();
        setDataForBarScore();
        setDataForSideLabels();
        setDataForBars();
    };

    var setDataForHeader = function () {
        chartHeaderComponent.setData({
            width: fullWidth,
            text: headerText
        });
    };

    var setDataForBarScore = function () {
        chartBarScoreComponent.setData({
            width: chartBarsWidth,
            max: measurandGroupDataMap["LOAD_TIMES"] ? measurandGroupDataMap["LOAD_TIMES"].max : 0
        });
    };

    var setDataForLegend = function () {
        chartLegendComponent.setData({
            entries: measurandDataEntries.map(function (measurandNestEntry) {
                var measurandValue = measurandNestEntry.values;
                return {
                    id: measurandValue.id,
                    color: measurandValue.color,
                    label: measurandValue.label
                };
            }),
            width: chartBarsWidth
        });
    };

    var setDataForSideLabels = function () {
      chartSideLabelsComponent.setData({
         height: chartBarsHeight,
         labels: sideLabelData
      });
    };

    var setDataForBars = function () {
        var componentsToRender = {};
        measurandDataEntries.forEach(function (entry) {
            if (!chartBarsComponents[entry.key]) {
                var component = OpenSpeedMonitor.ChartComponents.ChartBars();
                component.on("mouseover", function () {chartLegendComponent.mouseOverEntry({id: entry.key});});
                component.on("mouseout", function () {chartLegendComponent.mouseOutEntry({id: entry.key});});
                component.on("click", function () {chartLegendComponent.clickEntry({id: entry.key});});
                chartBarsComponents[entry.key] = component;
            }
            componentsToRender[entry.key] = chartBarsComponents[entry.key];
            componentsToRender[entry.key].setData({
                id: entry.values.id,
                values: entry.values.series,
                color: entry.values.color,
                min: measurandGroupDataMap[entry.values.measurandGroup].min > 0 ? 0 : measurandGroupDataMap[entry.values.measurandGroup].min,
                max: measurandGroupDataMap[entry.values.measurandGroup].max,
                height: chartBarsHeight,
                width: chartBarsWidth
            });
        });
        chartBarsComponents = componentsToRender;
    };

    var extractMeasurandData = function (series) {
        var colorProvider = OpenSpeedMonitor.ChartColorProvider();
        var colorScales = {};
        return d3.nest()
            .key(function(d) { return d.measurand; })
            .rollup(function (seriesOfMeasurand) {
                var firstValue = seriesOfMeasurand[0];
                var unit = firstValue.unit;
                colorScales[unit] = colorScales[unit] || colorProvider.getColorscaleForMeasurandGroup(unit);
                seriesOfMeasurand.forEach(function(pageData) {
                    pageData.id = pageData.page + ";" + pageData.jobGroup
                });
                return {
                    id: firstValue.measurand,
                    label: firstValue.measurandLabel,
                    measurandGroup: firstValue.measurandGroup,
                    color: colorScales[unit](firstValue.measurand),
                    series: seriesOfMeasurand
                };
            })
            .entries(series);
    };

    var extractMeasurandGroupData = function (series) {
        return d3.nest()
            .key(function(d) { return d.measurandGroup; })
            .rollup(function (seriesOfMeasurandGroup) {
                var extent = d3.extent(seriesOfMeasurandGroup, function(entry) { return entry.value; });
                return {
                    min: extent[0],
                    max: extent[1]
                };
            }).map(series);
    };

    var calculateChartBarsHeight = function () {
        var barBand = OpenSpeedMonitor.ChartComponents.ChartBars.BarBand;
        var barGap = OpenSpeedMonitor.ChartComponents.ChartBars.BarGap;
        var numberOfMeasurands = measurandDataEntries.length;
        var numberOfPages = d3.max(measurandDataEntries.map(function(d) { return d.values.series.length; }));
        var numberOfBars = numberOfPages * (stackBars ? 1 : numberOfMeasurands);
        var gapSize = barGap * ((stackBars || numberOfMeasurands < 2) ? 1 : 2);
        return ((numberOfPages - 1) * gapSize) + numberOfBars * barBand;
    };

    var render = function () {
        var shouldShowScore = !!measurandGroupDataMap["LOAD_TIMES"];
        var headerHeight = OpenSpeedMonitor.ChartComponents.ChartHeader.Height + componentMargin;
        var barScorePosY = chartBarsHeight + componentMargin;
        var barScoreHeight = shouldShowScore ? OpenSpeedMonitor.ChartComponents.ChartBarScore.BarHeight + componentMargin : 0;
        var legendPosY = barScorePosY + barScoreHeight;
        var legendHeight = chartLegendComponent.estimateHeight(svg) + componentMargin;
        var chartHeight = legendPosY + legendHeight + headerHeight;

        svg
            .transition()
            .duration(transitionDuration)
            .style("height", chartHeight)
            .each("end", rerenderIfWidthChanged);

        renderHeader(svg);
        renderSideLabels(svg, headerHeight);

        var contentGroup = svg.selectAll(".bars-content-group").data([1]);
        contentGroup.enter()
            .append("g")
            .classed("bars-content-group", true);
        contentGroup
            .transition()
            .duration(transitionDuration)
            .attr("transform", "translate(" + (chartSideLabelsWidth + componentMargin) +", " + headerHeight +")");
        renderBars(contentGroup);
        renderBarScore(contentGroup, shouldShowScore, barScorePosY);
        renderLegend(contentGroup, legendPosY);
    };

    var renderHeader = function (svg) {
        var header = svg.selectAll(".header-group").data([chartHeaderComponent]);
        header.exit()
            .remove();
        header.enter()
            .append("g")
            .classed("header-group", true);
        header.call(chartHeaderComponent.render);
    };

    var renderSideLabels = function(svg, posY) {
        var sideLabels = svg.selectAll(".side-labels-group").data([chartSideLabelsComponent]);
        sideLabels.exit()
            .remove();
        sideLabels.enter()
            .append("g")
            .classed("side-labels-group", true);
        sideLabels
            .attr("transform", "translate(0, " + posY + ")")
            .call(chartSideLabelsComponent.render)
    };

    var renderBarScore = function (svg, shouldShowScore, posY) {
        var barScore = svg.selectAll(".chart-score-group").data([chartBarScoreComponent]);
        barScore.exit()
            .remove();
        barScore.enter()
            .append("g")
            .attr("class", "chart-score-group")
            .attr("transform", "translate(0, " + posY + ")");
        barScore
            .call(chartBarScoreComponent.render)
            .transition()
            .style("opacity", shouldShowScore ? 1 : 0)
            .duration(transitionDuration)
            .attr("transform", "translate(0, " + posY + ")");
    };

    var renderLegend = function (svg, posY) {
        var legend = svg.selectAll(".chart-legend-group").data([chartLegendComponent]);
        legend.exit()
            .remove();
        legend.enter()
            .append("g")
            .attr("class", "chart-legend-group")
            .attr("transform", "translate(0, " + posY + ")");
        legend.call(chartLegendComponent.render)
            .transition()
            .duration(transitionDuration)
            .attr("transform", "translate(0, " + posY + ")");
    };

    var renderBars = function (svg) {
        var chartBarsGroup = svg.selectAll(".chart-bar-group").data([1]);
        chartBarsGroup.enter()
            .append("g")
            .attr("class", "chart-bar-group");

        var chartBars = chartBarsGroup.selectAll(".chart-bars").data(Object.values(chartBarsComponents));
        chartBars.exit()
            .remove();
        chartBars.enter()
            .append("g")
            .attr("class", "chart-bars");
        chartBars
            .attr("transform", function (_, i) {
                return "translate(0, " + (i * OpenSpeedMonitor.ChartComponents.ChartBars.BarBand) + ")";
            })
            .each(function(chartBarsComponent) {
           chartBarsComponent.render(this);
        });

    };

    var rerenderIfWidthChanged = function () {
        if (autoWidth && (Math.abs(getActualSvgWidth() - fullWidth) >= 1)) {
            setData({autoWidth: true});
            render();
        }
    };

    var getActualSvgWidth = function() {
        return svg.node().getBoundingClientRect().width;
    };

    var toggleBarComponentHighlight = function (measurandToHighlight, anyHighlighted, doHighlight) {
        Object.keys(chartBarsComponents).forEach(function(measurand) {
            var isRestrained = anyHighlighted && !(doHighlight && measurand === measurandToHighlight);
            chartBarsComponents[measurand].setData({isRestrained: isRestrained});
        });
        render();
    };

    return {
        render: render,
        setData: setData
    };

});
