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
package org.mule.devkit.model;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;

public class DefaultDevKitExecutableElement extends DefaultDevKitElement<ExecutableElement, DevKitTypeElement> implements DevKitExecutableElement {
    public DefaultDevKitExecutableElement(ExecutableElement element, DevKitTypeElement parent, Types types) {
        super(element, parent, types);
    }

    @Override
    public List<DevKitParameterElement> getParameters() {
        List<DevKitParameterElement> parameters = new ArrayList<DevKitParameterElement>();
        for(VariableElement variableElement : innerElement.getParameters() ) {
            parameters.add(new DefaultDevKitParameterElement(variableElement, this, types));
        }

        return parameters;
    }

    @Override
    public List<? extends TypeMirror> getThrownTypes() {
        return innerElement.getThrownTypes();
    }

    @Override
    public TypeMirror getReturnType() {
        return innerElement.getReturnType();
    }

    @Override
    public List<? extends TypeParameterElement> getTypeParameters() {
        return innerElement.getTypeParameters();
    }
}
