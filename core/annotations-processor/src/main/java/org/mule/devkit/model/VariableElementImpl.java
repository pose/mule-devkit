package org.mule.devkit.model;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

public class VariableElementImpl implements VariableElement {

    private VariableElement variableElement;

    public VariableElementImpl(VariableElement variableElement) {
        this.variableElement = variableElement;
    }

    @Override
    public Object getConstantValue() {
        return variableElement.getConstantValue();
    }

    @Override
    public TypeMirror asType() {
        return variableElement.asType();
    }

    @Override
    public ElementKind getKind() {
        return variableElement.getKind();
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        return variableElement.getAnnotationMirrors();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> aClass) {
        return variableElement.getAnnotation(aClass);
    }

    @Override
    public Set<Modifier> getModifiers() {
        return variableElement.getModifiers();
    }

    @Override
    public Name getSimpleName() {
        return variableElement.getSimpleName();
    }

    @Override
    public Element getEnclosingElement() {
        return variableElement.getEnclosingElement();
    }

    @Override
    public List<? extends Element> getEnclosedElements() {
        return variableElement.getEnclosedElements();
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> rpElementVisitor, P p) {
        return variableElement.accept(rpElementVisitor, p);
    }
}
