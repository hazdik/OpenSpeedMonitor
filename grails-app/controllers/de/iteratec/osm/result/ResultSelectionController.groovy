package de.iteratec.osm.result

import de.iteratec.osm.util.ControllerUtils
import de.iteratec.osm.util.PerformanceLoggingService
import de.iteratec.osm.util.PerformanceLoggingService.IndentationDepth
import grails.converters.JSON
import org.hibernate.exception.GenericJDBCException
import org.hibernate.type.StandardBasicTypes
import org.joda.time.DateTime
import org.joda.time.Days
import org.springframework.http.HttpStatus

import static de.iteratec.osm.util.PerformanceLoggingService.LogLevel.DEBUG

class ResultSelectionController {
    private final static MAX_RESULT_COUNT = 50000
    private final static RESULT_COUNT_MAX_SECONDS = 1

    PerformanceLoggingService performanceLoggingService

    enum MetaConnectivityProfileId {
        Custom("custom"), Native("native")

        MetaConnectivityProfileId(String value) {
            this.value = value
        }
        String value
    }

    enum ResultSelectionType {
        JobGroups,
        MeasuredEvents,
        Locations,
        ConnectivityProfiles,
        Results
    }

    def getResultCount(ResultSelectionCommand command) {
        if (command.hasErrors()) {
            sendError(command)
            return
        }
        def count = performanceLoggingService.logExecutionTime(DEBUG, "getResultCount for ${command as JSON}", IndentationDepth.NULL, {
            // we select static '1' for up to MAX_RESULT_COUNT records and count them afterwards
            // counting directly is slower, as we can't easily set a limit *before* counting the rows with GORM
            try {
                return EventResult.createCriteria().list {
                    applyResultSelectionFilters(delegate, command.from, command.to, command, ResultSelectionType.Results)
                    maxResults MAX_RESULT_COUNT
                    timeout RESULT_COUNT_MAX_SECONDS
                    projections {
                        sqlProjection '1 as c', 'c', StandardBasicTypes.INTEGER
                    }
                }.size()
            } catch (GenericJDBCException ignored) {
                return -1 // on timeout
            }
        })
        def result = count < MAX_RESULT_COUNT ? count : -1
        ControllerUtils.sendSimpleResponseAsStream(response, HttpStatus.OK, result.toString())
    }

    def getJobGroups(ResultSelectionCommand command) {
        if (command.hasErrors()) {
            sendError(command)
            return
        }
        def dtos = performanceLoggingService.logExecutionTime(DEBUG, "getJobGroups for ${command as JSON}", IndentationDepth.NULL, {
            def jobGroups = query(command, ResultSelectionType.JobGroups, { existing ->
                if (existing) {
                    not { 'in'('jobGroup', existing) }
                }
                projections {
                    distinct('jobGroup')
                }
            })
            return jobGroups.collect {[
                id: it.id,
                name: it.name
            ]}
        })
        ControllerUtils.sendObjectAsJSON(response, dtos)
    }

    def getMeasuredEvents(ResultSelectionCommand command) {
        if (command.hasErrors()) {
            sendError(command)
            return
        }

        def dtos = performanceLoggingService.logExecutionTime(DEBUG, "getMeasuredEvents for ${command as JSON}", IndentationDepth.NULL, {
            def measuredEvents = query(command, ResultSelectionType.MeasuredEvents, { existing ->
                if (existing) {
                    or {
                        not { 'in'('measuredEvent', existing.collect { it[0] }) }
                        not { 'in'('page', existing.collect { it[1] }) }
                    }
                }
                projections {
                    distinct('measuredEvent')
                    property('page')
                }
            })
            return measuredEvents.collect {[
                id: it[0].id,
                name: it[0].name,
                parent: [id: it[1].id, name: it[1].name]
            ]}
        })
        ControllerUtils.sendObjectAsJSON(response, dtos)
    }

    def getLocations(ResultSelectionCommand command) {
        if (command.hasErrors()) {
            sendError(command)
            return
        }

        def dtos = performanceLoggingService.logExecutionTime(DEBUG, "getLocations for ${command as JSON}", IndentationDepth.NULL, {
            def locations = query(command, ResultSelectionType.Locations, { existing ->
                if (existing) {
                    or {
                        not { 'in'('location', existing.collect { it[0] }) }
                        not { 'in'('browser', existing.collect { it[1] }) }
                    }
                }
                projections {
                    distinct('location')
                    property('browser')
                }
            })
            return locations.collect {[
                id    : it[0].id,
                name  : it[0].toString(),
                parent: [id: it[1].id, name: it[1].name]
            ]}
        })
        ControllerUtils.sendObjectAsJSON(response, dtos)
    }

    def getConnectivityProfiles(ResultSelectionCommand command) {
        if (command.hasErrors()) {
            sendError(command)
            return
        }

        def dtos = performanceLoggingService.logExecutionTime(DEBUG, "getConnectivityProfiles all for ${command as JSON}", IndentationDepth.NULL, {
            def dtos = getPredefinedConnectivityProfiles(command)
            dtos += getCustomConnectivity(command)
            dtos += getNativeConnectivity(command)
            return dtos
        })

        ControllerUtils.sendObjectAsJSON(response, dtos)
    }

    private getPredefinedConnectivityProfiles(ResultSelectionCommand command) {
        return performanceLoggingService.logExecutionTime(DEBUG, "getConnectivityProfiles predefined for ${command as JSON}", IndentationDepth.ONE, {
            def connectivityProfiles = query(command, ResultSelectionType.ConnectivityProfiles, { existing ->
                isNotNull('connectivityProfile')
                if (existing) {
                    not { 'in'('connectivityProfile', existing) }
                }
                projections {
                    distinct('connectivityProfile')
                }
            })
            return connectivityProfiles.collect {[
                id  : it.id,
                name: it.toString()
            ]}
        })
    }

    private getCustomConnectivity(ResultSelectionCommand command) {
        return performanceLoggingService.logExecutionTime(DEBUG, "getConnectivityProfiles custom for ${command as JSON}", IndentationDepth.ONE, {
            def customProfiles = query(command, ResultSelectionType.ConnectivityProfiles, { existing ->
                isNotNull('customConnectivityName')

                if (existing) {
                    not { 'in'('customConnectivityName', existing) }
                }

                projections {
                    distinct('customConnectivityName')
                }
            })
            return customProfiles.collect {[
                id  : MetaConnectivityProfileId.Custom.value,
                name: it
            ]}
        })
    }

    private getNativeConnectivity(ResultSelectionCommand command) {
        return performanceLoggingService.logExecutionTime(DEBUG, "getConnectivityProfiles native for ${command as JSON}", IndentationDepth.ONE, {
            def nativeConnectivity = query(command, ResultSelectionType.ConnectivityProfiles, {
                eq('noTrafficShapingAtAll', true)
                maxResults 1
                projections {
                    distinct('noTrafficShapingAtAll')
                }
            })
            return nativeConnectivity ? [[id: MetaConnectivityProfileId.Native.value, name: "Native"]] : []
        })
    }

    private def sendError(ResultSelectionCommand command) {
        ControllerUtils.sendSimpleResponseAsStream(response, HttpStatus.BAD_REQUEST,
            "Invalid parameters: " + command.getErrors().fieldErrors.each{it.field}.join(", "))
    }

    private def query(ResultSelectionCommand command, ResultSelectionType type, Closure projection) {
        boolean isStartOfDay = isStartOfDay(command.from)
        def fromFullDay = command.from.withTimeAtStartOfDay()
        if (!isStartOfDay) {
            fromFullDay = fromFullDay.plusDays(1)
        }

        boolean isEndOfDay = isEndOfDay(command.to)
        def toFullDay = command.to.withTimeAtStartOfDay()
        if (isEndOfDay) {
            toFullDay = toFullDay.plusDays(1)
        }

        boolean isFullDayBetween = Days.daysBetween(fromFullDay, toFullDay).getDays() > 0

        if (!isFullDayBetween) {
            return queryEventTable(command.from, command.to, command, type, projection, null)
        }

        def results = queryResultSelectionTable(fromFullDay, toFullDay, command, type, projection, null)
        if (!isStartOfDay) {
            results += queryEventTable(command.from, fromFullDay, command, type, projection, results)
        }
        if (!isEndOfDay) {
            results += queryEventTable(toFullDay, command.to, command, type, projection, results)
        }
        return results
    }

    private def queryEventTable(DateTime from, DateTime to, ResultSelectionCommand command, ResultSelectionType type, Closure projection, Object existingResults) {
        return EventResult.createCriteria().list {
            applyResultSelectionFilters(delegate, from, to, command, type)
            projection.delegate = delegate
            projection(existingResults)
        }
    }

    private def queryResultSelectionTable(DateTime from, DateTime to, ResultSelectionCommand command, ResultSelectionType type, Closure projection, Object existingResults) {
        return ResultSelectionInformation.createCriteria().list {
            applyResultSelectionFilters(delegate, from, to, command, type)
            projection.delegate = delegate
            projection(existingResults)
        }
    }

    private void applyResultSelectionFilters(Object criteriaBuilder, DateTime from, DateTime to, ResultSelectionCommand command, ResultSelectionType resultSelectionType) {
        def filterClosure = {
            and {
                between("jobResultDate", from.toDate(), to.toDate())
                if (resultSelectionType != ResultSelectionType.JobGroups && command.jobGroupIds) {
                    jobGroup {
                        'in'("id", command.jobGroupIds)
                    }
                }

                if (resultSelectionType != ResultSelectionType.MeasuredEvents && command.measuredEventIds) {
                    measuredEvent {
                        'in'("id", command.measuredEventIds)
                    }
                }

                if (resultSelectionType != ResultSelectionType.MeasuredEvents && command.pageIds) {
                    page {
                        'in'("id", command.pageIds)
                    }
                }

                if (resultSelectionType != ResultSelectionType.Locations && command.locationIds) {
                    location {
                        'in'("id", command.locationIds)
                    }
                }

                if (resultSelectionType != ResultSelectionType.Locations && command.browserIds) {
                    browser {
                        'in'("id", command.browserIds)
                    }
                }

                if (resultSelectionType != ResultSelectionType.ConnectivityProfiles) {
                    or {
                        if (command.connectivityIds) {
                            connectivityProfile {
                                'in'("id", command.connectivityIds)
                            }
                        }

                        if (command.nativeConnectivity) {
                            and {
                                isNull("connectivityProfile")
                                eq("noTrafficShapingAtAll", true)
                            }
                        }
                        if (command.customConnectivities) {
                            and {
                                isNull("connectivityProfile")
                                'in'("customConnectivityName", command.customConnectivities)
                            }
                        }
                    }
                }
            }
        }
        filterClosure.delegate = criteriaBuilder
        filterClosure()
    }

    private boolean isStartOfDay(DateTime dateTime) {
        return dateTime.getMillisOfDay() <= 1000
    }

    private boolean isEndOfDay(DateTime dateTime) {
        def millisOfDay = dateTime.getMillisOfDay()
        def maxMillis = dateTime.millisOfDay().withMaximumValue().getMillisOfDay()
        return (maxMillis - millisOfDay) <= 1000
    }
}

class ResultSelectionCommand {
    DateTime from
    DateTime to
    List<Long> jobGroupIds
    List<Long> pageIds
    List<Long> measuredEventIds
    List<Long> browserIds
    List<Long> locationIds
    List<Long> connectivityIds
    Boolean nativeConnectivity
    List<String> customConnectivities

    static constraints = {
        from(blank: false, nullable: false)
        to(blank: false, nullable: false, validator: { val, obj ->
            if (!val.isAfter(obj.from)) {
               return ['datePriorTo', val.toString(), obj.from.toString()]
            }
        })
        nativeConnectivity(blank: true, nullable: true)
    }
}