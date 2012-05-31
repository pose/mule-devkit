package org.mule.devkit.model;

import javax.lang.model.element.ExecutableElement;

public class DefaultDevKitExecutableElement extends ExecutableElementImpl implements DevKitExecutableElement {
    public DefaultDevKitExecutableElement(ExecutableElement executableElement) {
        super(executableElement);
    }
}
