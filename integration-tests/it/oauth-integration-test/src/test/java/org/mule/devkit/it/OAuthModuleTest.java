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
package org.mule.devkit.it;

import org.junit.Test;
import org.mule.api.Capabilities;
import org.mule.api.Capability;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OAuthModuleTest extends FunctionalTestCase {

    @Override
    protected String getConfigResources() {
        return "oauth.xml";
    }

    @Test
    public void testEnsureCapability() throws Exception {
        Capabilities capabilities = (Capabilities) muleContext.getRegistry().lookupObject("default-oauth");

        assertTrue(capabilities.isCapableOf(Capability.OAUTH1_CAPABLE));
    }

    @Test
    public void testNonProtectedResource() throws Exception {
        MuleEvent responseEvent = runFlow("nonProtectedResource");
        assertEquals(OAuthModule.NON_PROTECTED_RESOURCE, responseEvent.getMessageAsString());
    }

    @Test(expected = MessagingException.class)
    public void testProtectedResourceWithoutAuthorization() throws Exception {
        runFlow("protectedResource");
    }

    @Test
    public void testProtectedResource() throws Exception {
        MuleEvent responseEvent = runFlow("authorize");
        String url = verifyUserIsRedirectedToAuthorizationUrl(responseEvent);
        simulateCallbackUponUserAuthorizingConsumer(url);
        responseEvent = runFlow("protectedResource");
        verifiyProtectedResourceWasAccessed(responseEvent);
    }

    @Test
    public void testProtectedResourceWithSave() throws Exception {
        MuleEvent responseEvent = runFlow("authorizeWithSave");
        String url = verifyUserIsRedirectedToAuthorizationUrl(responseEvent);
        simulateCallbackUponUserAuthorizingConsumer(url);
        responseEvent = runFlow("protectedResourceWithSave");
        verifiyProtectedResourceWasAccessed(responseEvent);

        assertEquals(SaveAccessTokenComponent.getAccessToken(), Constants.ACCESS_TOKEN);
        assertEquals(SaveAccessTokenComponent.getAccessTokenSecret(), Constants.ACCESS_TOKEN_SECRET);
    }

    @Test
    public void testProtectedResourceWithRestore() throws Exception {
        MuleEvent responseEvent = runFlow("protectedResourceWithRestore");
        verifiyProtectedResourceWasAccessed(responseEvent);
    }

    private void verifiyProtectedResourceWasAccessed(MuleEvent responseEvent) throws MuleException {
        assertEquals(OAuthModule.PROTECTED_RESOURCE, responseEvent.getMessageAsString());
        if (RequestTokenComponent.timesCalled < 1) {
            fail("Not enough times called");
        }
        if (RequestTokenComponent.timesCalled == 0) {
            fail("Not enough times called");
        }
    }

    private void simulateCallbackUponUserAuthorizingConsumer(String url) throws IOException {
        String callbackUrl = url.substring(url.indexOf("oauth_callback=") + "oauth_callback=".length()).replaceAll("%3A", ":").replaceAll("%2F", "/");
        callbackUrl += "?oauth_verifier=" + Constants.OAUTH_VERIFIER;
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(callbackUrl).openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.getInputStream();
    }

    private String verifyUserIsRedirectedToAuthorizationUrl(MuleEvent responseEvent) {
        assertEquals("302", responseEvent.getMessage().getOutboundProperty("http.status"));
        String url = responseEvent.getMessage().getOutboundProperty("Location");
        assertNotNull(url);
        if (RequestTokenComponent.timesCalled < 1) {
            fail("Not enough times called");
        }
        return url;
    }

    private Flow lookupFlowConstruct(String name) {
        return (Flow) muleContext.getRegistry().lookupFlowConstruct(name);
    }

    private MuleEvent runFlow(String flowName) throws Exception {
        Flow flow = lookupFlowConstruct(flowName);
        MuleEvent event = getTestEvent("");
        return flow.process(event);
    }
}