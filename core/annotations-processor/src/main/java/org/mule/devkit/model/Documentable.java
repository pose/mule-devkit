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

/**
 * This interface provides a way to interact with model elements that can
 * have javadoc comments and tags.
 */
public interface Documentable {

    /**
     * Checks if the element contains the specified tag in this javadoc
     * comments.
     *
     * @param tagName The name of the javadoc tag to check
     * @return true if it contains it, false otherwise
     */
    boolean hasJavaDocTag(String tagName);

    String getJavaDocSummary();

    String getJavaDocTagContent(String tagName);

    String getJavaDocParameterSummary(String paramName);
}
