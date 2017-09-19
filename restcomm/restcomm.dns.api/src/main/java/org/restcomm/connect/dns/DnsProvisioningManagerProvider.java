/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.connect.dns;

import javax.servlet.ServletContext;

import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.loader.ObjectFactory;
import org.restcomm.connect.commons.loader.ObjectInstantiationException;

/**
 * A single place to create the dns provisioning manager
 *
 * @author maria farooq
 */
public class DnsProvisioningManagerProvider {
    Configuration configuration;
    ServletContext context;

    public DnsProvisioningManagerProvider(Configuration configuration, ServletContext context) {
        this.configuration = configuration;
        this.context = context;
    }

    public DnsProvisioningManager create() {
        final String dnsProvisioningManagerClass = configuration.getString("dns-provisioning[@class]");
        Configuration dnsProvisioningConfiguration = configuration.subset("dns-provisioning");
        DnsProvisioningManager dnsProvisioningManager;
        try {
            dnsProvisioningManager = (DnsProvisioningManager) new ObjectFactory(getClass().getClassLoader())
                    .getObjectInstance(dnsProvisioningManagerClass);
            dnsProvisioningManager.init(dnsProvisioningConfiguration);
        } catch (ObjectInstantiationException e) {
            throw new RuntimeException(e);
        }
        return dnsProvisioningManager;
    }

    /**
     * Tries to retrieve the manager from the Servlet context. If it's not there it creates is, stores it
     * in the context and also returns it.
     *
     * @param context
     * @return
     */
    public DnsProvisioningManager get() {
        DnsProvisioningManager manager = (DnsProvisioningManager) context.getAttribute("DnsProvisioningManager");
        if (manager != null) // ok, it's already in the context. Return it
            return manager;
        manager = create();
        // put it into the context for next time that is requested
        context.setAttribute("DnsProvisioningManager", manager);
        return manager;
    }

}
