package de.iteratec.osm.measurement.environment

import de.iteratec.osm.OsmConfiguration
import de.iteratec.osm.csi.TestDataUtil
import de.iteratec.osm.measurement.environment.wptserverproxy.HttpRequestService
import de.iteratec.osm.measurement.schedule.Job
import de.iteratec.osm.measurement.schedule.JobGroup
import de.iteratec.osm.measurement.schedule.JobService
import de.iteratec.osm.measurement.script.Script
import de.iteratec.osm.report.chart.CsiAggregationInterval
import de.iteratec.osm.util.ServiceMocker
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.joda.time.DateTime
import org.junit.Rule
import software.betamax.Configuration
import software.betamax.ProxyConfiguration
import software.betamax.junit.Betamax
import software.betamax.junit.RecorderRule
import spock.lang.Ignore
import spock.lang.Specification

@TestFor(QueueAndJobStatusService)
@Mock([WebPageTestServer, Location, Job, Browser, BrowserAlias, JobGroup, Script, OsmConfiguration, CsiAggregationInterval])
class QueueAndJobStatusServiceSpec extends Specification {

    //TODO: Re-Write these tests without mocking http requests (e.g. without betamax or similar libray)

//    public Recorder recorder = new Recorder(new ConfigSlurper().parse(new File('grails-app/conf/betamaxrties').toURL()).toProperties())
    Configuration configuration = ProxyConfiguration.builder().tapeRoot(new File("src/test/resources/betamax_tapes")).ignoreLocalhost(false).build();
    @Rule public RecorderRule recorder = new RecorderRule(configuration)

    public static final String WPTSERVER_URL = 'dev.server01.wpt.iteratec.de'

    public static final String WPTSERVER2_URL = 'dev.server02.wpt.iteratec.de'

    QueueAndJobStatusService serviceUnderTest

    ServiceMocker mocker

    WebPageTestServer server1
    WebPageTestServer server2

    private String labelJobWithExecutionSchedule = 'BV1 - Step 01'
    String jobGroupName

    def doWithSpring = {
        httpRequestService(HttpRequestService)
    }
    @Ignore
    @Betamax(tape = 'CreateChartData_creates_a_map_entry_per_server')
    def "CreateChartData creates a map entry per server"() {
        given:
        def start = new DateTime()
        def end = start.plusDays(1)
        mockServices()
        createTestDataCommonForAllTests()

        when:
        def resultMap = serviceUnderTest.createChartData(start, end)

        then:
        resultMap.keySet().size() == 2
    }

    @Ignore
    @Betamax(tape = 'CreateChartData creates entry for each location')
    def "CreateChartData creates entry for each location"() {
        given:
        def start = new DateTime()
        def end = start.plusDays(1)
        mockServices()
        createTestDataCommonForAllTests()

        when:
        def resultMap = serviceUnderTest.createChartData(start, end)

        then:
        resultMap.get(server1).size() == 11 // on 16.11.15 (saved in betamax tape)
        resultMap.get(server2).size() == 8 // on 16.11.15 (saved in betamax tape)
    }

    private void mockServices() {
        serviceUnderTest = service
        mocker = ServiceMocker.create()
        mocker.mockI18nService(serviceUnderTest)
        serviceUnderTest.jobService = Mock(JobService)
    }

    private void createTestDataCommonForAllTests() {
        TestDataUtil.createOsmConfig()
        TestDataUtil.createCsiAggregationIntervals()
        server1 = TestDataUtil.createWebPageTestServer(WPTSERVER_URL, WPTSERVER_URL, true, "http://${WPTSERVER_URL}/")
        server2 = TestDataUtil.createWebPageTestServer(WPTSERVER2_URL, WPTSERVER2_URL, true, "http://${WPTSERVER2_URL}/")

    }
}
