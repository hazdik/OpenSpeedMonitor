/* 
* OpenSpeedMonitor (OSM)
* Copyright 2014 iteratec GmbH
* 
* Licensed under the Apache License, Version 2.0 (the "License"); 
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
* 	http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, 
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and 
* limitations under the License.
*/

package de.iteratec.osm.measurement.environment.wptserverproxy

import de.iteratec.osm.csi.Page
import de.iteratec.osm.measurement.environment.Browser
import de.iteratec.osm.measurement.environment.BrowserAlias
import de.iteratec.osm.measurement.environment.Location
import de.iteratec.osm.measurement.environment.WebPageTestServer
import de.iteratec.osm.measurement.schedule.Job
import de.iteratec.osm.measurement.schedule.JobDaoService
import de.iteratec.osm.measurement.schedule.JobGroup
import de.iteratec.osm.measurement.script.Script
import de.iteratec.osm.result.EventResult
import de.iteratec.osm.result.JobResult
import de.iteratec.osm.result.MeasuredEvent
import de.iteratec.osm.result.PageService
import de.iteratec.osm.util.PerformanceLoggingService
import grails.buildtestdata.mixin.Build
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Unroll

@TestFor(ResultPersisterService)
@Build([WebPageTestServer, Location, Job])
@Mock([WebPageTestServer, Browser, Location, Job, JobResult, EventResult, BrowserAlias, Page, MeasuredEvent, JobGroup, Script])
class PersistingNewEventResultsWithNoMedianOptionTestSpec extends Specification {

    def doWithSpring = {
        performanceLoggingService(PerformanceLoggingService)
        pageService(PageService)
        jobDaoService(JobDaoService)
    }

    @Unroll
    void "only persist median results if not set otherwise in job"(String jobLabel, boolean doPersistNonMedianResults, int expectedMedianResults, int expectedNonMedianResults, String fileName) {
        setup:
        File file = new File("src/test/resources/WptResultXmls/" + fileName)
        WptResultXml xmlResult = new WptResultXml(new XmlSlurper().parse(file))
        String locationIdentifier = xmlResult.responseNode.data.location.toString()

        WebPageTestServer wptServer = WebPageTestServer.build(baseUrl: "http://wpt.org")
        Location.build(uniqueIdentifierForServer: locationIdentifier, wptServer: wptServer)
        Job.build(label: jobLabel, persistNonMedianResults: doPersistNonMedianResults)

        when: "the services listens to the XML file"
        service.listenToResult(xmlResult, wptServer)

        then: "the correct number of event results are created, with or without median results"
        JobResult.count() == 1
        EventResult.count() == expectedMedianResults + expectedNonMedianResults
        EventResult.findAllByMedianValue(true).size() == expectedMedianResults
        EventResult.findAllByMedianValue(false).size() == expectedNonMedianResults

        where:
        jobLabel                | doPersistNonMedianResults | expectedMedianResults | expectedNonMedianResults | fileName
        "FF_Otto_multistep"     | true                      | 3                     | 12                       | "MULTISTEP_FORK_ITERATEC_5Runs_3Events_JustFirstView_WithVideo.xml"
        "FF_Otto_multistep"     | false                     | 3                     | 0                        | "MULTISTEP_FORK_ITERATEC_5Runs_3Events_JustFirstView_WithVideo.xml"
        "IE_otto_hp_singlestep" | true                      | 2                     | 8                        | "BEFORE_MULTISTEP_5Runs_WithVideo.xml"
        "IE_otto_hp_singlestep" | false                     | 2                     | 0                        | "BEFORE_MULTISTEP_5Runs_WithVideo.xml"
    }
}
