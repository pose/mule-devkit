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

package org.mule.devkit.generation.api;

import org.mule.devkit.model.Type;

/**
 * An annotation verifier like its implies verifiers that the annotations used
 * are correctly used.
 */
public interface AnnotationVerifier {

    /**
     * Should this verifier be executed for the given type?
     *
     * @param type The type to test
     * @return true if it should be verified, false otherwise
     */
    boolean shouldVerify(Type type);

    /**
     * Verify the annotations on this type
     *
     * @param type The type to be verified
     * @throws AnnotationVerificationException if the verification fails
     */
    void verify(Type type) throws AnnotationVerificationException;
}