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
package org.mule.devkit.apt.model;

import com.sun.source.util.Trees;
import org.apache.commons.lang.StringUtils;
import org.mule.devkit.model.Method;
import org.mule.devkit.model.Parameter;
import org.mule.devkit.model.Type;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;

public class AnnotationProcessorMethod extends AnnotationProcessorIdentifiable<ExecutableElement, Type> implements Method {
    public AnnotationProcessorMethod(ExecutableElement element, Type parent, Types types, Elements elements, Trees trees) {
        super(element, parent, types, elements, trees);
    }

    @Override
    public List<Parameter> getParameters() {
        List<Parameter> parameters = new ArrayList<Parameter>();
        for(VariableElement variableElement : innerElement.getParameters() ) {
            parameters.add(new AnnotationProcessorParameter(variableElement, this, types, elements, trees));
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

    @Override
    public boolean hasOnlyOneChildElement() {
        int requiredChildElements = 0;
        for (Parameter variable : getParameters()) {
            if (variable.shouldBeIgnored()) {
                continue;
            }
            if (variable.isNestedProcessor()) {
                requiredChildElements++;
            } else if (variable.isXmlType()) {
                requiredChildElements++;
            } else if (variable.isCollection()) {
                requiredChildElements++;
            }
        }

        return requiredChildElements == 1;
    }

    @Override
    public String getCapitalizedName() {
        return StringUtils.capitalize(innerElement.getSimpleName().toString());
    }
}
