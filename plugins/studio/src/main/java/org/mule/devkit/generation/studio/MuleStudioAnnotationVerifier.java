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

package org.mule.devkit.generation.studio;

import org.mule.devkit.generation.api.AnnotationVerificationException;
import org.mule.devkit.generation.api.Context;
import org.mule.devkit.generation.javadoc.JavaDocAnnotationVerifier;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Type;

public class MuleStudioAnnotationVerifier extends JavaDocAnnotationVerifier {

    @Override
    public boolean shouldVerify(Type type, Context context) {
        return !super.shouldVerify(type, context);
    }

    @Override
    public void verify(Type type, Context context) throws AnnotationVerificationException {
        try {
            super.verify(type, context);
        } catch (AnnotationVerificationException e) {
            throw new AnnotationVerificationException(type, "Cannot generate Mule Studio plugin if required javadoc comments are not present. " +
                    "If you want to skip the generation of the Mule Studio plugin use -Ddevkit.studio.package.skip=true. Error is: " + e.getMessage(), e);
        }
    }

    @Override
    protected boolean exampleDoesNotExist(Context context, Method method) throws AnnotationVerificationException {
        // do not check for example correctness
        return false;
    }
}