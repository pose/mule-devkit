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

public interface Typeable {
    boolean isXmlType();

    boolean isAbstract();

    boolean isFinal();

    boolean isStatic();

    boolean isNestedProcessor();

    boolean isArrayOrList();

    boolean isMap();

    boolean isEnum();

    boolean isCollection();


    boolean isString();

    boolean isBoolean();

    boolean isInteger();

    boolean isLong();

    boolean isFloat();

    boolean isDouble();

    boolean isChar();

    boolean isHttpCallback();

    boolean isURL();

    boolean isDate();

    boolean isBigDecimal();

    boolean isBigInteger();
}
