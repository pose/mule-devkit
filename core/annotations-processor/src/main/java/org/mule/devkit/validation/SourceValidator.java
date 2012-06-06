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

import org.mule.api.annotations.Source;
import org.mule.api.callback.SourceCallback;
import org.mule.devkit.Context;
import org.mule.devkit.ValidationException;
import org.mule.devkit.Validator;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Parameter;
import org.mule.devkit.model.Type;

import java.util.List;

public class SourceValidator implements Validator {

    @Override
    public boolean shouldValidate(Type type, Context context) {
        return type.isModuleOrConnector() && type.hasMethodsAnnotatedWith(Source.class);
    }

    @Override
    public void validate(Type type, Context context) throws ValidationException {

        for (Method method : type.getMethodsAnnotatedWith(Source.class)) {

            if (method.getAnnotation(Source.class).primaryNodeOnly()) {
                String[] expectedMinVersion = new String[]{"3", "3"};
                String minMuleVersion = type.getMinMuleVersion();
                if (minMuleVersion.contains("-")) {
                    minMuleVersion = minMuleVersion.split("-")[0];
                }
                String[] minMuleVersions = minMuleVersion.split("\\.");
                for (int i = 0; (i < expectedMinVersion.length); i++) {
                    try {
                        if (Integer.parseInt(minMuleVersions[i]) < Integer.parseInt(expectedMinVersion[i])) {
                            throw new ValidationException(method, "The attribute primaryNodeOnly works with Mule 3.3 only therefore you must set the minMuleVersion of your @Connector or @Module to \"3.3\". Example: @Module(minMuleVersion=\"3.3\")");
                        }
                    } catch (NumberFormatException nfe) {
                        throw new ValidationException(method, "Error parsing Mule version, verify that the minMuleVersion is in the correct format: X.X.X");
                    }
                }
            }

            if (method.isStatic()) {
                throw new ValidationException(method, "@Source cannot be applied to a static method");
            }

            if (!method.getTypeParameters().isEmpty()) {
                throw new ValidationException(method, "@Source cannot be applied to a generic method");
            }

            if (!method.isPublic()) {
                throw new ValidationException(method, "@Source cannot be applied to a non-public method");
            }

            // verify that every @Source receives a SourceCallback
            boolean containsSourceCallback = false;
            List<Parameter> parameters = method.getParameters();
            for (Parameter parameter : parameters) {
                if (parameter.asType().toString().startsWith(SourceCallback.class.getName())) {
                    containsSourceCallback = true;
                }
            }

            if (!containsSourceCallback) {
                throw new ValidationException(method, "@Source method must contain a SourceCallback as one of its parameters");
            }
        }
    }
}