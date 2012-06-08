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
import org.mule.api.annotations.Source;
import org.mule.api.annotations.SourceThreadingModel;
import org.mule.devkit.model.SourceMethod;
import org.mule.devkit.model.Type;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class AnnotationProcessorSourceMethod extends AnnotationProcessorMethod implements SourceMethod {

    public AnnotationProcessorSourceMethod(ExecutableElement element, Type parent, Types types, Elements elements, Trees trees) {
        super(element, parent, types, elements, trees);
    }

    @Override
    public boolean shouldRunOnPrimaryNodeOnly() {
        return getAnnotation(Source.class).primaryNodeOnly();
    }

    @Override
    public SourceThreadingModel getThreadingModel() {
        return getAnnotation(Source.class).threadingModel();
    }
}
