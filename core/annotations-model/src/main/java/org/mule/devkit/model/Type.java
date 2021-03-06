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

public interface Type extends Identifiable<TypeElement, Type> {

    boolean hasProcessorMethodWithParameter(Class<?> parameterType);

    boolean hasConfigurableWithType(Class<?> parameterType);

    boolean hasProcessorMethodWithParameterListOf(Class<?> listGenericType);

    boolean hasAnnotation(Class<? extends Annotation> annotation);

    List<Method> getMethodsAnnotatedWith(Class<? extends Annotation> annotation);

    /**
     * Get a list of all methods annotated with @Source
     *
     * @return A list of all methods annotated with @Source
     * @see {@link SourceMethod}
     */
    List<SourceMethod> getSourceMethods();

    List<Method> getMethodsWhoseParametersAreAnnotatedWith(Class<? extends Annotation> annotation);

    List<Field> getFieldsAnnotatedWith(Class<? extends Annotation> annotation);

    boolean hasMethodsAnnotatedWith(Class<? extends Annotation> annotation);

    List<Field> getFields();

    /**
     * Get all methods defined in this class
     *
     * @return A list containing all the methods defined in this class
     */
    List<Method> getMethods();

    boolean isParametrized();

    boolean hasFieldAnnotatedWith(Class<? extends Annotation> annotation);

    boolean isInterface();

    boolean isModuleOrConnector();

    boolean isPoolable();

    String getMinMuleVersion();

    String getXmlNamespace();

    String getModuleName();

    String getAnnotatedSchemaLocation();

    String getVersionedSchemaLocation();

    String getCurrentSchemaLocation();

    String getModuleSchemaVersion();

    boolean usesConnectionManager();

    String getFriendlyName();

    String getDescription();

    boolean needsConfig();

    Name getQualifiedName();

    String getPathToSourceFile();

    String getPackageName();

    String getClassName();
}