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

package de.iteratec.osm.report.chart

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.web.servlet.support.RequestContextUtils

/**
 * EventController
 * A controller class handles incoming web requests and performs actions such as redirects, rendering views and so on.
 */
class EventController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def markdownService

    def index() {
        redirect(action: "list", params: params)
    }

    def list() {
        params.max = Math.min(params.max ? params.int('max') : 10, 100)
        [eventInstanceList: Event.list(params), eventInstanceTotal: Event.count()]
    }

    def create() {
        [eventInstance: new Event(params)]
    }

    def save() {
        combineDateAndTime(params)
        def eventInstance = new Event(params)
        println eventInstance.eventDate
        if (!eventInstance.save(flush: true)) {
            render(view: "create", model: [eventInstance: eventInstance])
            return
        }

		flash.message = message(code: 'default.created.message', args: [message(code: 'event.label', default: 'Event'), eventInstance.id])
        redirect(action: "show", id: eventInstance.id)
    }

    def show() {
        def eventInstance = Event.get(params.id)
        if (!eventInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'event.label', default: 'Event'), params.id])
            redirect(action: "list")
            return
        }

        [eventInstance: eventInstance]
    }

    def edit() {
        def eventInstance = Event.get(params.id)
        if (!eventInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'event.label', default: 'Event'), params.id])
            redirect(action: "list")
            return
        }

        [eventInstance: eventInstance]
    }

    def update() {
        def eventInstance = Event.get(params.id)
        if (!eventInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'event.label', default: 'Event'), params.id])
            redirect(action: "list")
            return
        }

        if (params.version) {
            def version = params.version.toLong()
            if (eventInstance.version > version) {
                eventInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                          [message(code: 'event.label', default: 'Event')] as Object[],
                          "Another user has updated this Event while you were editing")
                render(view: "edit", model: [eventInstance: eventInstance])
                return
            }
        }
        combineDateAndTime(params)
        eventInstance.properties = params

        if (!eventInstance.save(flush: true)) {
            render(view: "edit", model: [eventInstance: eventInstance])
            return
        }

		flash.message = message(code: 'default.updated.message', args: [message(code: 'event.label', default: 'Event'), eventInstance.id])
        redirect(action: "show", id: eventInstance.id)
    }

    def delete() {
        def eventInstance = Event.get(params.id)
        if (!eventInstance) {
			flash.message = message(code: 'default.not.found.message', args: [message(code: 'event.label', default: 'Event'), params.id])
            redirect(action: "list")
            return
        }

        try {
            eventInstance.delete(flush: true)
			flash.message = message(code: 'default.deleted.message', args: [message(code: 'event.label', default: 'Event'), params.id])
            redirect(action: "list")
        }
        catch (DataIntegrityViolationException e) {
			flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'event.label', default: 'Event'), params.id])
            redirect(action: "show", id: params.id)
        }
    }

    /**
     * Combine time and date within the param list, where time ist 'time' and date is 'eventDate'
     * @param params
     */
    private void combineDateAndTime(def params){
        //Convert english date format to german, for passing validation
        //The gsp passes the time and the date separately so we need to combine these two
        params['eventDate'] = params['eventDate']+" "+params['time']
        def formatter
        params.remove('time')
        def locale = RequestContextUtils.getLocale(request)
        switch (locale){
            case Locale.GERMANY:
                formatter =  "dd.MM.yyyy HH:mm"
                break;
            default:
                formatter = "yyyy-MM-dd HH:mm"
        }
        params['eventDate'] = Date.parse(formatter,params['eventDate'] as String)
    }
}
