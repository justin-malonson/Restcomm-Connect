package org.mobicents.servlet.restcomm.identity.configuration;

import org.mobicents.servlet.restcomm.configuration.ConfigurationSet;

public interface IdentityConfigurationSet extends ConfigurationSet {
    String getRealmKey();
    String getRealmName();
    String getAuthServerUrl();
    String getIdentityProxyUrl();
    String getIdentityProxyUrl(String authServerUrlBase );
    String getIdentityInstanceId();
    String getRestcommClientSecret();

    void setAuthServerUrlBase(String urlBase);
    void setMode(IdentityMode mode);
    void setRestcommClientSecret(String secret);
    void setInstanceId(String instanceId);

    public enum IdentityMode {
        init,
        cloud,
        standalone
    }
}
