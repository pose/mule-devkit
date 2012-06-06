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

package org.mule.devkit.utils;

import org.apache.commons.lang.StringUtils;
import org.mule.devkit.model.DevKitElement;
import org.mule.devkit.model.DevKitExecutableElement;
import org.mule.devkit.model.DevKitParameterElement;
import org.mule.devkit.model.DevKitTypeElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class NameUtils {
    private Elements elements;

    private static final List<Inflection> plural = new ArrayList<Inflection>();
    private static final List<Inflection> singular = new ArrayList<Inflection>();
    private static final List<String> uncountable = new ArrayList<String>();

    static {
        // plural is "singular to plural form"
        // singular is "plural to singular form"
        plural("$", "s");
        plural("s$", "s");
        plural("(ax|test)is$", "$1es");
        plural("(octop|vir)us$", "$1i");
        plural("(alias|status)$", "$1es");
        plural("(bu)s$", "$1ses");
        plural("(buffal|tomat)o$", "$1oes");
        plural("([ti])um$", "$1a");
        plural("sis$", "ses");
        plural("(?:([^f])fe|([lr])f)$", "$1$2ves");
        plural("(hive)$", "$1s");
        plural("([^aeiouy]|qu)y$", "$1ies");
        //plural("([^aeiouy]|qu)ies$", "$1y");
        plural("(x|ch|ss|sh)$", "$1es");
        plural("(matr|vert|ind)ix|ex$", "$1ices");
        plural("([m|l])ouse$", "$1ice");
        plural("^(ox)$", "$1en");
        plural("(quiz)$", "$1zes");

        singular("s$", "");
        singular("(n)ews$", "$1ews");
        singular("([ti])a$", "$1um");
        singular("((a)naly|(b)a|(d)iagno|(p)arenthe|(p)rogno|(s)ynop|(t)he)ses$", "$1$2sis");
        singular("(^analy)ses$", "$1sis");
        singular("([^f])ves$", "$1fe");
        singular("(hive)s$", "$1");
        singular("(tive)s$", "$1");
        singular("([lr])ves$", "$1f");
        singular("([^aeiouy]|qu)ies$", "$1y");
        singular("(s)eries$", "$1eries");
        singular("(m)ovies$", "$1ovie");
        singular("(x|ch|ss|sh)es$", "$1");
        singular("([m|l])ice$", "$1ouse");
        singular("(bus)es$", "$1");
        singular("(o)es$", "$1");
        singular("(shoe)s$", "$1");
        singular("(cris|ax|test)es$", "$1is");
        singular("(octop|vir)i$", "$1us");
        singular("(alias|status)es$", "$1");
        singular("^(ox)en", "$1");
        singular("(vert|ind)ices$", "$1ex");
        singular("(matr)ices$", "$1ix");
        singular("(quiz)zes$", "$1");

        // irregular
        irregular("person", "people");
        irregular("man", "men");
        irregular("child", "children");
        irregular("sex", "sexes");
        irregular("move", "moves");

        uncountable("equipment");
        uncountable("information");
        uncountable("rice");
        uncountable("money");
        uncountable("species");
        uncountable("series");
        uncountable("fish");
        uncountable("sheep");
    }

    public NameUtils(Elements elements) {
        this.elements = elements;
    }

    private static void plural(String pattern, String replacement) {
        plural.add(0, new Inflection(pattern, replacement));
    }

    private static void singular(String pattern, String replacement) {
        singular.add(0, new Inflection(pattern, replacement));
    }

    private static void irregular(String s, String p) {
        plural("(" + s.substring(0, 1) + ")" + s.substring(1) + "$", "$1" + p.substring(1));
        singular("(" + p.substring(0, 1) + ")" + p.substring(1) + "$", "$1" + s.substring(1));
    }

    private static void uncountable(String word) {
        uncountable.add(word);
    }

    public String camel(String uncamelCaseName) {
        String result = "";
        String[] parts = uncamelCaseName.split("-");

        for (int i = 0; i < parts.length; i++) {
            result += StringUtils.capitalize(parts[i].toLowerCase());
        }

        return result;
    }


    public String uncamel(String camelCaseName) {
        String result = "";
        String[] parts = camelCaseName.split("(?<!^)(?=[A-Z])");

        for (int i = 0; i < parts.length; i++) {
            result += parts[i].toLowerCase() + (i < parts.length - 1 ? "-" : "");
        }

        return result;
    }

    public String friendlyNameFromCamelCase(String camelCaseName) {
        return StringUtils.capitalize(uncamel(camelCaseName)).replaceAll("-", " ");
    }

    public String getClassName(String fullyQualifiedClassName) {
        int lastDot = fullyQualifiedClassName.lastIndexOf('.');
        return fullyQualifiedClassName.substring(lastDot + 1);
    }

    public String getPackageName(String fullyQualifiedClassName) {
        int lastDot = fullyQualifiedClassName.lastIndexOf('.');
        return fullyQualifiedClassName.substring(0, lastDot);
    }

    /**
     * Return the pluralized version of a word.
     *
     * @param word The word
     * @return The pluralized word
     */
    public String pluralize(String word) {
        if (isUncountable(word)) {
            return word;
        } else {
            for (Inflection inflection : plural) {
                if (inflection.match(word)) {
                    return inflection.replace(word);
                }
            }
            return word;
        }
    }

    /**
     * Return the singularized version of a word.
     *
     * @param word The word
     * @return The singularized word
     */
    public String singularize(String word) {
        if (isUncountable(word)) {
            return word;
        } else {
            for (Inflection inflection : singular) {
                if (inflection.match(word)) {
                    return inflection.replace(word);
                }
            }
        }
        return word;
    }

    /**
     * Return true if the word is uncountable.
     *
     * @param word The word
     * @return True if it is uncountable
     */
    public boolean isUncountable(String word) {
        for (String w : uncountable) {
            if (w.equalsIgnoreCase(word)) {
                return true;
            }
        }
        return false;
    }

    public String getBinaryName(DevKitTypeElement typeElement) {
        return elements.getBinaryName(typeElement.unwrap()).toString();
    }

    private static class Inflection {
        private String pattern;
        private String replacement;
        private boolean ignoreCase;

        public Inflection(String pattern) {
            this(pattern, null, true);
        }

        public Inflection(String pattern, String replacement) {
            this(pattern, replacement, true);
        }

        public Inflection(String pattern, String replacement, boolean ignoreCase) {
            this.pattern = pattern;
            this.replacement = replacement;
            this.ignoreCase = ignoreCase;
        }


        /**
         * Does the given word match?
         *
         * @param word The word
         * @return True if it matches the inflection pattern
         */
        public boolean match(String word) {
            int flags = 0;
            if (ignoreCase) {
                flags = flags | Pattern.CASE_INSENSITIVE;
            }
            return Pattern.compile(pattern, flags).matcher(word).find();
        }

        /**
         * Replace the word with its pattern.
         *
         * @param word The word
         * @return The result
         */
        public String replace(String word) {
            int flags = 0;
            if (ignoreCase) {
                flags = flags | Pattern.CASE_INSENSITIVE;
            }
            return Pattern.compile(pattern, flags).matcher(word).replaceAll(replacement);
        }
    }
}
