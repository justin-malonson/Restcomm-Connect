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
package org.mobicents.servlet.restcomm.mgcp;

import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import akka.actor.ActorRef;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class ConferenceEndpoint extends GenericEndpoint {

    public ConferenceEndpoint(final ActorRef gateway, final MediaSession session, final NotifiedEntity agent,
            final String domain, long timeout) {
        super(gateway, session, agent, new EndpointIdentifier("mobicents/cnf/$", domain), timeout);
    }

}
