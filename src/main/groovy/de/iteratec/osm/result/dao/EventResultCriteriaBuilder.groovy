package de.iteratec.osm.result.dao

import de.iteratec.osm.result.EventResult
import grails.gorm.DetachedCriteria
import org.grails.datastore.mapping.query.Projections
import org.grails.datastore.mapping.query.Query

/**
 * Created by mwg on 20.09.2017.
 */
class EventResultCriteriaBuilder {
    List<String> projectedFields = []
    List<Query.Projection> projections = []
    DetachedCriteria query = new DetachedCriteria(EventResult)

    List<Query.Criterion> getFilters() {
        return query.criteria
    }

    void filterBetween(String propertyName, def from, def to) {
        if (from && to) {
            query.between(propertyName, from, to)
        }
    }

    void filterIn(String propertyName, List range, boolean groupBy) {
        if (range) {
            query.in(propertyName, range)
            if (groupBy) {
                addGroupByProjection(propertyName)
            }
        }
    }

    void filterEquals(String propertyName, def toBeEqualTo) {
        if (toBeEqualTo) {
            query.eq(propertyName, toBeEqualTo)
        }
    }

    void addPropertyProjection(String propertyName, String projectionName = null) {
        addProjection(Projections.property(propertyName), propertyName, projectionName)
    }

    void addAvgProjection(String propertyName, String projectionName = null) {
        addProjection(Projections.avg(propertyName), propertyName, projectionName)
    }

    void addGroupByProjection(String propertyName, String projectionName = null) {
        addProjection(Projections.groupProperty(propertyName), propertyName, projectionName)
    }


    private void addProjection(Query.Projection projection, String propertyName, String projectionName) {
        String projectedField = projectionName ?: propertyName
        if (!projectedFields.contains(projectedField)) {
            projections.add(projection)
            projectedFields.add(projectedField)
        }
    }

    List<Map> getResults() {
        query.projections.addAll(projections)
        return transformAggregations(query.list())
    }

    EventResultCriteriaBuilder mergeWith(EventResultCriteriaBuilder eventResultCriteriaBuilder) {
        eventResultCriteriaBuilder.filters.each {
            query.add(it)
        }
        projections += eventResultCriteriaBuilder.projections
        projectedFields += eventResultCriteriaBuilder.projectedFields
        return this
    }

    private List<Map> transformAggregations(def aggregations) {
        List<Map> result = []
        aggregations.each { aggregation ->
            Map transformed = [:]
            projectedFields.each {
                transformed.put(it, aggregation[projectedFields.indexOf(it)])
            }
            result.add(transformed)
        }
        return result
    }
}
