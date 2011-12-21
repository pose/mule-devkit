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

package org.mule.api.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation defines a class that will export its functionality as a Mule Cloud Connector.
 * <p/>
 * There are a few restrictions as to which types as valid for this annotation:
 * - It cannot be an interface
 * - It must be public
 * - It cannot have a typed parameter (no generic)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Connector {
    /**
     * The name of the connector.
     */
    String name();

    /**
     * The schema version of the connector. Defaults to 1.0.
     */
    String schemaVersion() default DEFAULT_VERSION;

    /**
     * Namespace of the connector
     */
    String namespace() default "";

    /**
     * Location URI for the schema
     */
    String schemaLocation() default "";

    /**
     * Minimum Mule version required
     */
    String minMuleVersion() default "3.2";

    /**
     * Provides a friendly name for the module.
     */
    String friendlyName() default "";

    /**
     * Short description about the annotated module.
     */
    String description() default "";

    String DEFAULT_VERSION = "1.0";
}