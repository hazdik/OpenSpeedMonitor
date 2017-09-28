<%@ page import="de.iteratec.osm.result.Threshold" %>
<%@ page import="de.iteratec.osm.measurement.schedule.JobGroup" %>
<%@ page import="de.iteratec.osm.result.Measurand" %>
<div class="form-group" id="">
    <label>
        <g:message code="job.Thresholds.label" default="Thresholds"/>
    </label>
</div>

<div id="threshold">
    <div>
        <div id="thresholdList">
            <div>
                <ul>
                    <li v-for="threshold in thresholds">
                        <div v-if="threshold.edit">Die
                            <select id="measurandEdit" name="measurand" class="measurand-event-select"
                                    v-model="threshold.threshold.measurand">
                                <option v-for="measurand in measurands" :value="measurand">{{measurand.name}}</option>
                            </select>
                            Messung von
                            <select id="measuredEventEdit" name="measuredEvent" class="measured-event-select"
                                    v-model="threshold.threshold.measuredEvent">
                                <option v-for="measuredEvent in measuredEvents"
                                        :value="measuredEvent">{{measuredEvent.name}}</option>
                            </select>
                            sollte schneller als
                            <input id="lowerBoundaryEdit" class="" type="number" min="1" name="lowerBoundary" cols="40"
                                   rows="5"
                                   maxlength="255"
                                   v-model="threshold.threshold.lowerBoundary"/>
                            ms, aber nicht langsamer als
                            <input id="upperBoundaryEdit" class="" type="number" min="1" name="upperBoundary" cols="40"
                                   rows="5"
                                   maxlength="255"
                                   v-model="threshold.threshold.upperBoundary"/>
                            ms sein. <button type="button"
                                             @click="updateThreshold(threshold, '${g.createLink([controller: 'threshold', action: 'updateAsync'])}')">submit</button>
                            <button type="button" @click="changeEditMode(threshold, false)">discard</button>
                        </div>

                        <div v-else>Die {{ threshold.threshold.measurand.name }} Messung von {{ threshold.threshold.measuredEvent.name }}
                        sollte schneller als {{ threshold.threshold.lowerBoundary }} ms, aber nicht langsamer als {{ threshold.threshold.upperBoundary }} ms sein.
                            <button type="button" @click="changeEditMode(threshold, true)">edit</button>
                            <button type="button"
                                                 @click="deleteThreshold(threshold, '${g.createLink([controller: 'threshold', action: 'deleteAsync'])}')">delete</button>
                        </div>

                    </li>
                </ul>
            </div>
        </div>
    </div>


    <div>Die
        <select id="measurand" name="measurand" class="measurand-event-select" v-model="newThreshold.measurand">
            <option v-for="measurand in measurands" :value="measurand">{{measurand.name}}</option>
        </select>
        Messung von
        <select id="measuredEvent" name="measuredEvent" class="measured-event-select"
                v-model="newThreshold.measuredEvent">
            <option v-for="measuredEvent in measuredEvents" :value="measuredEvent">{{measuredEvent.name}}</option>
        </select>
        sollte schneller als
        <input id="lowerBoundary" class="" type="number" min="1" name="lowerBoundary" cols="30" rows="5"
               maxlength="150"
               v-model="newThreshold.lowerBoundary"/>
        ms, aber nicht langsamer als
        <input id="upperBoundary" class="" type="number" min="1" name="upperBoundary" cols="40" rows="5"
               maxlength="150"
               v-model="newThreshold.upperBoundary"/>
        ms sein. <a>add</a>  <a>clear</a>
    </div>


    <div>
        <button class="btn btn-default" type="button" id="copyToClipboard">
            <g:message code="job.threshold.copyToClipboard" default="Copy To Clipboard"/>
        </button>

        <button id="threshold_button_create" class="btn btn-default" type="button"
                @click="addThreshold('${job}', '${g.createLink([controller: 'threshold', action: 'createAsync'])}')">
            <g:message code="job.threshold.create.new" default="Add Threshold"/>
        </button>
    </div>
</div>


<asset:script type="text/javascript">
    OpenSpeedMonitor.postLoader.loadJavascript('<g:assetPath src="job/thresholdForJob.js"/>');
    $(window).load(function() {
      OpenSpeedMonitor.thresholdforJobs.initVue({jobId: "${job?.id}", scriptId: "${job?.script?.id}"});
    });
</asset:script>