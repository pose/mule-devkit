/**
 * Mule Development Kit
 * Copyright 2010-2011 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mule.devkit.it.studio;

import org.mule.api.ConnectionException;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.ConnectionIdentifier;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Disconnect;
import org.mule.api.annotations.InvalidateConnectionOn;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.ValidateConnection;
import org.mule.api.annotations.param.ConnectionKey;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;

import java.net.URL;

/**
 * Connector class
 *
 * @author MuleSoft inc
 */
@Connector(name = "connector")
public class ConnectorModule {

    /**
     * a URL
     */
    @Configurable
    @Optional
    @Default("http://www.mulesoft.org")
    private URL url;
    private String username;
    private String password;

    /**
     * Processor method that invalidates connections
     */
    @Processor
    @InvalidateConnectionOn(exception = RuntimeException.class)
    public void invalidate() {
    }

    /**
     * returns the username
     *
     * @return the username
     */
    @Processor
    public String getUsername() {
        return username;
    }

    /**
     * Connect method
     *
     * @param username the username to use
     * @param password the password to use
     * @throws ConnectionException
     */
    @Connect
    public void connect(@ConnectionKey String username, String password) throws ConnectionException {
    }

    /**
     * Disconnect method
     */
    @Disconnect
    public void disconnect() {
    }

    /**
     * Connection identifier method
     *
     * @return the connection identifier
     */
    @ConnectionIdentifier
    public String connectionId() {
        return "";
    }

    /**
     * Is connected method
     *
     * @return whether it is connected
     */
    @ValidateConnection
    public boolean isConnected() {
        return true;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUrl(URL url) {
        this.url = url;
    }
}