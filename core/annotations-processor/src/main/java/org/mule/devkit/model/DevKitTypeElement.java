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

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.List;

public interface DevKitTypeElement extends DevKitElement<TypeElement, DevKitTypeElement> {

    boolean hasProcessorMethodWithParameter(Class<?> parameterType);

    boolean hasConfigurableWithType(Class<?> parameterType);

    boolean hasProcessorMethodWithParameterListOf(Class<?> listGenericType);

    boolean hasAnnotation(Class<? extends Annotation> annotation);

    List<DevKitExecutableElement> getMethodsAnnotatedWith(Class<? extends Annotation> annotation);

    List<DevKitExecutableElement> getMethodsWhoseParametersAreAnnotatedWith(Class<? extends Annotation> annotation);

    List<DevKitFieldElement> getFieldsAnnotatedWith(Class<? extends Annotation> annotation);

    boolean hasMethodsAnnotatedWith(Class<? extends Annotation> annotation);

    List<DevKitFieldElement> getFields();

    List<DevKitExecutableElement> getMethods();

    boolean hasFieldAnnotatedWith(Class<? extends Annotation> annotation);

    boolean isInterface();

    boolean isParametrized();

    boolean isModuleOrConnector();

    boolean isPoolable();

    String minMuleVersion();

    String namespace();

    String name();

    String schemaLocation();

    String schemaVersion();

    boolean usesConnectionManager();

    String friendlyName();

    String description();

    boolean needsConfig();

    Name getQualifiedName();

    String getPathToSourceFile();

    String getPackageName();

    String getClassName();
}