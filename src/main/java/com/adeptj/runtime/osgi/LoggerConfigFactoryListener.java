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

package com.adeptj.runtime.osgi;

import com.adeptj.runtime.common.LogbackManagerHolder;
import com.adeptj.runtime.common.OSGiUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.framework.ServiceEvent.REGISTERED;
import static org.osgi.framework.ServiceEvent.UNREGISTERING;

/**
 * OSGi {@link ServiceListener} for getting logger config properties from LoggerConfigFactory services.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
public class LoggerConfigFactoryListener implements ServiceListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerConfigFactoryListener.class);

    private final Lock lock;

    /**
     * List containing the service pids for which loggers have been configured.
     */
    private final List<String> loggerConfiguredPids;

    public LoggerConfigFactoryListener() {
        this.lock = new ReentrantLock();
        this.loggerConfiguredPids = new ArrayList<>();
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        this.lock.tryLock();
        try {
            ServiceReference<?> reference = event.getServiceReference();
            String pid = OSGiUtil.getString(reference, SERVICE_PID);
            switch (event.getType()) {
                case REGISTERED:
                    if (LogbackManagerHolder.getInstance().getLogbackManager().addOSGiLoggers(reference)) {
                        this.loggerConfiguredPids.add(pid);
                    }
                    break;
                case UNREGISTERING:
                    if (this.loggerConfiguredPids.remove(pid)) {
                        LogbackManagerHolder.getInstance().getLogbackManager().resetLoggerContext(reference);
                    }
                    break;
                default:
                    LOGGER.warn("Ignored ServiceEvent: [{}]", event.getType());
            }
        } finally {
            this.lock.unlock();
        }
    }
}
