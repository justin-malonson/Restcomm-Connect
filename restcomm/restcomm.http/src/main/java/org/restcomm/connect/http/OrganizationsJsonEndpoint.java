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
package org.restcomm.connect.http;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;

/**
 * @author maria farooq
 */
@Path("/Organizations.json")
@ThreadSafe
public final class OrganizationsJsonEndpoint extends OrganizationsEndpoint {
    public OrganizationsJsonEndpoint() {
        super();
    }

    @Path("/{organizationSid}")
    @GET
    public Response getOrganizationAsJson(@PathParam("organizationSid") final String organizationSid) {
        return getOrganization(organizationSid, APPLICATION_JSON_TYPE);
    }

    @GET
    public Response getOrganizations(@Context UriInfo info) {
        return getOrganizations(info, APPLICATION_JSON_TYPE);
    }

    @POST
    public Response putOrganization(@PathParam("domainName") final String domainName, final MultivaluedMap<String, String> data) {
        return putOrganization(domainName, data, APPLICATION_JSON_TYPE);
    }
}
