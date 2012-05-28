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

import java.math.BigInteger;
import java.math.BigDecimal;

public class BasicModuleTest extends AbstractModuleTest {

    public BasicModuleTest() {
        System.setProperty("my.int", "3 ");
        System.setProperty("my.long", "4 ");
    }

    @Override
    protected String getConfigResources() {
        return "basic.xml";
    }

    public void testPollString() throws Exception {
        runFlow("pollPassthruStringFlow", "mulesoft");
    }

    public void testString() throws Exception {
        runFlow("passthruStringFlow", "mulesoft");
    }

    public void testInteger() throws Exception {
        runFlow("passthruIntegerFlow", 3);
    }

    public void testFloat() throws Exception {
    }

    public void testBoolean() throws Exception {
        runFlow("passthruBooleanFlow", true);
    }

    public void testLong() throws Exception {
        runFlow("passthruLongFlow", 3456443463342345734L);
    }

    public void testEnum() throws Exception {
        runFlow("passthruEnumFlow", "In");
    }

    public void testComplexRef() throws Exception {
        runFlow("passthruComplexRef", "MuleSoft$");
    }

    public void testBigDecimal() throws Exception {
        runFlow("passthruBigDecimal", new BigDecimal("10.2"));
    }

    public void testBigInteger() throws Exception {
        runFlow("passthruBigInteger", BigInteger.valueOf(18));
    }

}