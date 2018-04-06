/*
###############################################################################
#                                                                             # 
#    Copyright 2016, AdeptJ (http://www.adeptj.com)                           #
#                                                                             #
#    Licensed under the Apache License, Version 2.0 (the "License");          #
#    you may not use this file except in compliance with the License.         #
#    You may obtain a copy of the License at                                  #
#                                                                             #
#        http://www.apache.org/licenses/LICENSE-2.0                           #
#                                                                             #
#    Unless required by applicable law or agreed to in writing, software      #
#    distributed under the License is distributed on an "AS IS" BASIS,        #
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. #
#    See the License for the specific language governing permissions and      #
#    limitations under the License.                                           #
#                                                                             #
###############################################################################
*/

package io.adeptj.runtime.osgi;

import io.adeptj.runtime.common.OSGiUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;
import java.util.EventListener;

/**
 * This class is a modified version of FELIX EventDispatcherTracker and rectify the Invalid BundleContext issue.
 * <p>
 * Issue: When OSGi Framework is being restarted from FELIX web console, original EventDispatcherTracker still holds
 * the stale BundleContext and Framework tries to call the ServiceTracker.addingService method which in turn
 * uses the stale BundleContext for getting the EventDispatcher OSGi service and thus fails with following exception.
 * <p>
 * <em><b>java.lang.IllegalStateException: Invalid BundleContext</b></em>
 * <p>
 * To fix the above issue, we close the ServiceTracker in removedService method itself.
 * So that Framework initialize the new EventDispatcherTracker with a fresh BundleContext.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
public class EventDispatcherTracker extends ServiceTracker<EventListener, EventListener> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventDispatcherTracker.class);

    private static final String EVENT_DISPATCHER_FILTER = "(http.felix.dispatcher=*)";

    private HttpSessionListener sessionListener;

    private HttpSessionIdListener sessionIdListener;

    private HttpSessionAttributeListener sessionAttributeListener;

    EventDispatcherTracker(BundleContext context) {
        super(context, OSGiUtils.filter(context, EventListener.class, EVENT_DISPATCHER_FILTER), null);
    }

    @Override
    public EventListener addingService(ServiceReference<EventListener> reference) {
        LOGGER.info("Adding OSGi Service: [{}]", OSGiUtils.getServiceDesc(reference));
        EventListener listener = super.addingService(reference);
        this.handleListener(listener);
        return listener;
    }

    @Override
    public void removedService(ServiceReference<EventListener> reference, EventListener service) {
        LOGGER.info("Removing OSGi Service: [{}]", OSGiUtils.getServiceDesc(reference));
        super.removedService(reference, service);
        // NOTE: See class header why ServiceTracker is closed here.
        // ignore exceptions, anyway Framework is managing it as the EventDispatcher is being removed from service registry.
        ServiceTrackers.INSTANCE.closeQuietly(this);
    }

    HttpSessionListener getHttpSessionListener() {
        return this.sessionListener;
    }

    HttpSessionIdListener getHttpSessionIdListener() {
        return this.sessionIdListener;
    }

    HttpSessionAttributeListener getHttpSessionAttributeListener() {
        return this.sessionAttributeListener;
    }

    private void handleListener(EventListener listener) {
        this.initHttpSessionListener(listener);
        this.initHttpSessionIdListener(listener);
        this.initHttpSessionAttributeListener(listener);
    }

    private void initHttpSessionAttributeListener(EventListener listener) {
        if (listener instanceof HttpSessionAttributeListener) {
            this.sessionAttributeListener = (HttpSessionAttributeListener) listener;
        }
    }

    private void initHttpSessionIdListener(EventListener listener) {
        if (listener instanceof HttpSessionIdListener) {
            this.sessionIdListener = (HttpSessionIdListener) listener;
        }
    }

    private void initHttpSessionListener(EventListener listener) {
        if (listener instanceof HttpSessionListener) {
            this.sessionListener = (HttpSessionListener) listener;
        }
    }
}