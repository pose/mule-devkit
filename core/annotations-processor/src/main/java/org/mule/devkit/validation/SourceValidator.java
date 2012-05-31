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
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.model.DevKitTypeElement;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import java.util.List;

public class SourceValidator implements Validator {

    @Override
    public boolean shouldValidate(DevKitTypeElement typeElement, GeneratorContext context) {
        return typeElement.isModuleOrConnector() && typeElement.hasMethodsAnnotatedWith(Source.class);
    }

    @Override
    public void validate(DevKitTypeElement typeElement, GeneratorContext context) throws ValidationException {

        for (ExecutableElement method : typeElement.getMethodsAnnotatedWith(Source.class)) {

            if (method.getAnnotation(Source.class).primaryNodeOnly()) {
                String[] expectedMinVersion = new String[]{"3", "3"};
                String minMuleVersion = typeElement.minMuleVersion();
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

            if (method.getModifiers().contains(Modifier.STATIC)) {
                throw new ValidationException(method, "@Source cannot be applied to a static method");
            }

            if (!method.getTypeParameters().isEmpty()) {
                throw new ValidationException(method, "@Source cannot be applied to a generic method");
            }

            if (!method.getModifiers().contains(Modifier.PUBLIC)) {
                throw new ValidationException(method, "@Source cannot be applied to a non-public method");
            }

            // verify that every @Source receives a SourceCallback
            boolean containsSourceCallback = false;
            List<? extends VariableElement> parameters = method.getParameters();
            for (VariableElement parameter : parameters) {
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