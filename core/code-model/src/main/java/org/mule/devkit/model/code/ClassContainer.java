/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.mule.devkit.model.code;

import java.util.Iterator;

/**
 * The common aspect of a package and a class.
 */
public interface ClassContainer {

    /**
     * Returns true if the container is a class.
     */
    boolean isClass();

    /**
     * Returns true if the container is a package.
     */
    boolean isPackage();

    /**
     * Add a new class to this package/class.
     *
     * @param mods Modifiers for this class declaration
     * @param name Name of class to be added to this package
     * @return Newly generated class
     * @throws ClassAlreadyExistsException When the specified class/interface was already created.
     */
    DefinedClass _class(int mods, String name) throws ClassAlreadyExistsException;

    /**
     * Add a new public class to this class/package.
     *
     * @throws ClassAlreadyExistsException When the specified class/interface was already created.
     */
    public DefinedClass _class(String name) throws ClassAlreadyExistsException;

    /**
     * Add an interface to this class/package.
     *
     * @param mods Modifiers for this interface declaration
     * @param name Name of interface to be added to this package
     * @return Newly generated interface
     * @throws ClassAlreadyExistsException When the specified class/interface was already created.
     */
    public DefinedClass _interface(int mods, String name) throws ClassAlreadyExistsException;

    /**
     * Adds a public interface to this package.
     *
     * @throws ClassAlreadyExistsException When the specified class/interface was already created.
     */
    public DefinedClass _interface(String name) throws ClassAlreadyExistsException;

    /**
     * Create a new class or a new interface.
     *
     * @deprecated use {@link #_class(int, String, ClassType)}
     */
    public DefinedClass _class(int mods, String name, boolean isInterface)
            throws ClassAlreadyExistsException;

    /**
     * Creates a new class/enum/interface/annotation.
     */
    public DefinedClass _class(int mods, String name, ClassType kind)
            throws ClassAlreadyExistsException;


    /**
     * Returns an iterator that walks the nested classes defined in this
     * class.
     */
    public Iterator<DefinedClass> classes();

    /**
     * Parent ClassContainer.
     * <p/>
     * If this is a package, this method returns a parent package,
     * or null if this package is the root package.
     * <p/>
     * If this is an outer-most class, this method returns a package
     * to which it belongs.
     * <p/>
     * If this is an inner class, this method returns the outer
     * class.
     */
    public ClassContainer parentContainer();

    /**
     * Gets the nearest package parent.
     * <p/>
     * <p/>
     * If <tt>this.isPackage()</tt>, then return <tt>this</tt>.
     */
    public Package getPackage();

    /**
     * Get the root code model object.
     */
    public CodeModel owner();

    /**
     * Add an annotationType Declaration to this package
     *
     * @param name Name of the annotation Type declaration to be added to this package
     * @return newly created Annotation Type Declaration
     * @throws ClassAlreadyExistsException When the specified class/interface was already created.
     */
    public DefinedClass _annotationTypeDeclaration(String name) throws ClassAlreadyExistsException;

    /**
     * Add a public enum to this package
     *
     * @param name Name of the enum to be added to this package
     * @return newly created Enum
     * @throws ClassAlreadyExistsException When the specified class/interface was already created.
     */
    public DefinedClass _enum(String name) throws ClassAlreadyExistsException;

}
