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

import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.Transformer;
import org.mule.api.callback.SourceCallback;

/**
 * My module
 *
 * @author MuleSoft, inc
 */
@Module(name = "mymodule")
public class MyModule {

    /**
     * Some processor
     */
    @Processor
    public void processor() {
    }

    /**
     * Some transformer
     *
     * @param myString the transformer input
     * @return the transformed string
     */
    @Transformer(sourceTypes = {String.class})
    public static String transformer(String myString) {
        return "";
    }

    /**
     * Some source
     *
     * @param sourceCallback the callback
     */
    @Source
    public void source(SourceCallback sourceCallback) {
    }
}