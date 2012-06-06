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

import org.mule.devkit.GeneratorContext;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Type;

public class StudioValidator extends JavaDocValidator {

    @Override
    public boolean shouldValidate(Type type, GeneratorContext context) {
        return !super.shouldValidate(type, context) && !context.isEnvOptionSet("skipStudioPluginPackage");
    }

    @Override
    public void validate(Type type, GeneratorContext context) throws ValidationException {
        try {
            super.validate(type, context);
        } catch (ValidationException e) {
            throw new ValidationException(type, "Cannot generate Mule Studio plugin if required javadoc comments are not present. " +
                    "If you want to skip the generation of the Mule Studio plugin use -Ddevkit.studio.package.skip=true. Error is: " + e.getMessage(), e);
        }
    }

    @Override
    protected boolean exampleDoesNotExist(GeneratorContext context, Method method) throws ValidationException {
        // do not check for example correctness
        return false;
    }
}