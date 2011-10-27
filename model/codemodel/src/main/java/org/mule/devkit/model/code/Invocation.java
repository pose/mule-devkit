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

import java.util.ArrayList;
import java.util.List;


/**
 * Method invocation
 */
public final class Invocation extends AbstractExpression implements Statement {

    /**
     * Object expression upon which this method will be invoked, or null if
     * this is a constructor invocation
     */
    private Generable object;

    /**
     * Name of the method to be invoked.
     * Either this field is set, or {@link #method}, or {@link #type} (in which case it's a
     * constructor invocation.)
     * This allows {@link Method#name(String) the name of the method to be changed later}.
     */
    private String name;

    private Method method;

    private boolean isConstructor;

    /**
     * List of argument expressions for this method invocation
     */
    private List<Expression> args = new ArrayList<Expression>();

    /**
     * If isConstructor==true, this field keeps the type to be created.
     */
    private Type type;

    /**
     * Invokes a method on an object.
     *
     * @param object Expression for the object upon which
     *               the named method will be invoked,
     *               or null if none
     * @param name   Name of method to invoke
     */
    Invocation(Expression object, String name) {
        this((Generable) object, name);
    }

    Invocation(Expression object, Method method) {
        this((Generable) object, method);
    }

    /**
     * Invokes a static method on a class.
     */
    Invocation(TypeReference type, String name) {
        this((Generable) type, name);
    }

    Invocation(TypeReference type, Method method) {
        this((Generable) type, method);
    }

    private Invocation(Generable object, String name) {
        this.object = object;
        if (name.indexOf('.') >= 0) {
            throw new IllegalArgumentException("method name contains '.': " + name);
        }
        this.name = name;
    }

    private Invocation(Generable object, Method method) {
        this.object = object;
        this.method = method;
    }

    /**
     * Invokes a constructor of an object (i.e., creates
     * a new object.)
     *
     * @param c Type of the object to be created. If this type is
     *          an array type, added arguments are treated as array
     *          initializer. Thus you can create an expression like
     *          <code>new int[]{1,2,3,4,5}</code>.
     */
    Invocation(Type c) {
        this.isConstructor = true;
        this.type = c;
    }

    /**
     * Add an expression to this invocation's argument list
     *
     * @param arg Argument to add to argument list
     */
    public Invocation arg(Expression arg) {
        if (arg == null) {
            throw new IllegalArgumentException();
        }
        args.add(arg);
        return this;
    }

    /**
     * Adds a literal argument.
     * <p/>
     * Short for {@code arg(JExpr.lit(v))}
     */
    public Invocation arg(String v) {
        return arg(ExpressionFactory.lit(v));
    }

    /**
     * Returns all arguments of the invocation.
     *
     * @return If there's no arguments, an empty array will be returned.
     */
    public Expression[] listArgs() {
        return args.toArray(new Expression[args.size()]);
    }

    public void generate(Formatter f) {
        if(isConstructor && type == null) {
            throw new IllegalStateException("Cannot generate this invocation: " + this);
        }
        if (isConstructor && type.isArray()) {
            // [RESULT] new T[]{arg1,arg2,arg3,...};
            f.p("new").g(type).p('{');
        } else {
            if (isConstructor) {
                f.p("new").g(type).p('(');
            } else {
                String name = this.name;
                if (name == null) {
                    name = this.method.name();
                }

                if (object != null) {
                    f.g(object).p('.').p(name).p('(');
                } else {
                    f.id(name).p('(');
                }
            }
        }

        f.g(args);

        if (isConstructor && type.isArray()) {
            f.p('}');
        } else {
            f.p(')');
        }

        if (type instanceof DefinedClass && ((DefinedClass) type).isAnonymous()) {
            ((AnonymousClass) type).declareBody(f);
        }
    }

    public void state(Formatter f) {
        f.g(this).p(';').nl();
    }

    @Override
    public String toString() {
        return "Invocation{" +
                "object=" + object +
                ", name='" + name + '\'' +
                ", method=" + method +
                ", isConstructor=" + isConstructor +
                ", args=" + args +
                ", type=" + type +
                '}';
    }
}