package org.mule.devkit.model;

import java.lang.annotation.Annotation;

/**
 * Created with IntelliJ IDEA.
 * User: emiliano
 * Date: 6/1/12
 * Time: 3:27 PM
 * To change this template use File | Settings | File Templates.
 */
public interface TestInterface {
    <A extends Annotation> A getAnnotation(Class<A> aClass);
}
