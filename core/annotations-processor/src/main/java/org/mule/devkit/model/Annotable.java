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

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * This interface provides a way to interact this model elements that can
 * have annotations.
 */
public interface Annotable {

    /**
     * Returns the annotations that are directly present on this element.
     *
     * @return the annotations directly present on this element; an empty list if there are none
     */
    List<? extends AnnotationMirror> getAnnotationMirrors();

    /**
     * Returns this element's annotation for the specified type if such an annotation is present, else null. The
     * annotation may be either inherited or directly present on this element.
     *
     * The annotation returned by this method could contain an element whose value is of type Class. This value
     * cannot be returned directly: information necessary to locate and load a class (such as the class loader to
     * use) is not available, and the class might not be loadable at all. Attempting to read a Class object by
     * invoking the relevant method on the returned annotation will result in a MirroredTypeException, from which
     * the corresponding TypeMirror may be extracted. Similarly, attempting to read a Class[]-valued element will
     * result in a MirroredTypesException.

     * @param aClass the Class object corresponding to the annotation type
     * @param <A> the annotation type
     * @return this element's annotation for the specified annotation type if present on this element, else null
     */
    <A extends Annotation> A getAnnotation(Class<A> aClass);
}
