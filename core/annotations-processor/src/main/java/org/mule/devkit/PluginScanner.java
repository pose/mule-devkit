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
package org.mule.devkit;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The plugin scanner is responsible for scanning the specified classpath
 * and discovering all the available plugins.
 */
public final class PluginScanner {
    /**
     * All discovered {@link org.mule.devkit.Plugin}s.
     * This is lazily parsed, so that we can take '-cp' option into account.
     *
     * @see #getAllPlugins(ClassLoader)
     */
    private List<Plugin> allPlugins;

    private static PluginScanner INSTANCE;

    static {
        INSTANCE = new PluginScanner();
    }

    private PluginScanner() {
    }

    public static PluginScanner getInstance() {
        return INSTANCE;
    }

    /**
     * Gets all the {@link Plugin}s discovered so far.
     * <p/>
     * A plugins are enumerated when this method is called for the first time,
     * by taking the specified ClassLoader into account.
     *
     * @param ucl The user defined class loader
     */
    public List<Plugin> getAllPlugins(ClassLoader ucl) throws MalformedURLException {
        if (allPlugins == null) {
            allPlugins = new ArrayList<Plugin>();
            for (Plugin aug : findServices(Plugin.class, ucl))
                allPlugins.add(aug);
        }

        return allPlugins;
    }

    /**
     * Looks for all "META-INF/services/[className]" files and
     * create one instance for each class name found inside this file.
     */
    private static <T> T[] findServices(Class<T> clazz, ClassLoader classLoader) {
        try {
            Class<?> serviceLoader = Class.forName("java.util.ServiceLoader");
            Iterable<T> itr = (Iterable<T>) serviceLoader.getMethod("load", Class.class, ClassLoader.class).invoke(null, clazz, classLoader);
            List<T> r = new ArrayList<T>();
            for (T t : itr)
                r.add(t);
            return r.toArray((T[]) Array.newInstance(clazz, r.size()));
        } catch (ClassNotFoundException e) {
            // fall through
        } catch (IllegalAccessException e) {
            Error x = new IllegalAccessError();
            x.initCause(e);
            throw x;
        } catch (InvocationTargetException e) {
            Throwable x = e.getTargetException();
            if (x instanceof RuntimeException)
                throw (RuntimeException) x;
            if (x instanceof Error)
                throw (Error) x;
            throw new Error(x);
        } catch (NoSuchMethodException e) {
            Error x = new NoSuchMethodError();
            x.initCause(e);
            throw x;
        }

        return null;
    }
}
