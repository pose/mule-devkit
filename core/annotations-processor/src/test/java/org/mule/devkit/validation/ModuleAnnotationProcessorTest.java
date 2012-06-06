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
package org.mule.devkit.validation;

import org.mule.devkit.Validator;
import org.mule.devkit.apt.AbstractAnnotationProcessorTest;
import org.mule.devkit.apt.DevKitAnnotationProcessor;
import org.mule.devkit.Generator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class ModuleAnnotationProcessorTest extends AbstractAnnotationProcessorTest {

    protected abstract Validator getValidatorToTest();

    @Override
    protected List<DevKitAnnotationProcessor> getProcessors() {
        List<Validator> validators = Arrays.asList(getValidatorToTest());
        List<Generator> generators = Collections.emptyList();
        return Arrays.asList(new DevKitAnnotationProcessor(validators, generators));
    }
}