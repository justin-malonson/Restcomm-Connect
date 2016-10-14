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

package org.restcomm.connect.http.client.rcmlserver;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
import org.restcomm.connect.commons.common.http.CustomHttpClientBuilder;
import org.restcomm.connect.commons.configuration.sets.MainConfigurationSet;
import org.restcomm.connect.commons.configuration.sets.RcmlserverConfigurationSet;
import org.restcomm.connect.commons.util.SecurityUtils;
import org.restcomm.connect.commons.util.UriUtils;
import org.restcomm.connect.dao.entities.Account;
import org.restcomm.connect.http.exceptions.RcmlserverNotifyError;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class RcmlserverApi {
    static final Logger logger = Logger.getLogger(RcmlserverApi.class.getName());

    enum NotificationType {
        accountClosed
    }

    URI apiUrl;
    Integer timeout;
    MainConfigurationSet mainConfig;

    public RcmlserverApi(MainConfigurationSet mainConfig, RcmlserverConfigurationSet rcmlserverConfig) {
        try {
            // resolve() should be run lazily to work. Make sure this constructor is invoked after the JBoss connectors have been set up.
            apiUrl = UriUtils.resolve(new URI(rcmlserverConfig.getBaseUrl()));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        this.timeout = rcmlserverConfig.getTimeout();
        this.mainConfig = mainConfig;
    }

    public RcmlserverApi(MainConfigurationSet mainConfig, URI apiUrl, Integer timeout) {
        this.mainConfig = mainConfig;
        this.apiUrl = apiUrl;
        this.timeout = timeout;
    }

    public void transmitNotifications(List<JsonObject> notifications, String notifierUsername, String notifierPassword) throws RcmlserverNotifyError {
        String notificationUrl = apiUrl + "/notifications";
        HttpPost request = new HttpPost(notificationUrl);
        String authHeader;
        authHeader = SecurityUtils.buildBasicAuthHeader(notifierUsername, notifierPassword);
        request.setHeader("Authorization", authHeader );
        Gson gson = new Gson();
        String json = gson.toJson(notifications);
        request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        HttpClient httpClient = CustomHttpClientBuilder.build(mainConfig, timeout);
        try {
            HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RcmlserverNotifyError(true); // TODO refine critical-ity of exceptions thrown based on status code
            }
            logger.info("Transmitted a set of " + notifications.size() + " notification(s) to rcmlserver");
        } catch (IOException e) {
            // TODO throw serious exception if the error signifies problem reaching rcmlserver that would affect subsequent
            // request too
            throw new RcmlserverNotifyError("Transmission of " + notifications.size() + " notifications failed.",e,true);
        }
    }

    public JsonObject buildAccountClosingNotification(Account closedAccount) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", NotificationType.accountClosed.toString());
        jsonObject.addProperty("accountSid", closedAccount.getSid().toString());
        return jsonObject;
    }

}
