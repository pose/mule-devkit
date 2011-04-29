package org.mule.devkit.apt.generator.spring;

import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import org.mule.config.spring.handlers.AbstractPojoNamespaceHandler;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.GenerationException;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;

public class NamespaceHandlerGenerator extends AbstractCodeGenerator {
    public NamespaceHandlerGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    public void generate(TypeElement type) throws GenerationException {
        String namespaceHandlerName = getContext().getElements().getBinaryName(type) + "NamespaceHandler";

        JDefinedClass namespaceHandlerClass = getOrCreateClass(namespaceHandlerName);
        namespaceHandlerClass._extends(AbstractPojoNamespaceHandler.class);

        JMethod init = namespaceHandlerClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "init");

        registerBeanDefinitionParserForEachProcessor(type, init);
    }

    private void registerBeanDefinitionParserForEachProcessor(TypeElement type, JMethod init) {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Processor processor = executableElement.getAnnotation(Processor.class);

            if (processor == null)
                continue;

            registerBeanDefinitionParser(init, executableElement);
        }
    }

    private void registerBeanDefinitionParser(JMethod init, ExecutableElement executableElement) {
        JDefinedClass beanDefinitionParser = getBeanDefinitionParserClass(executableElement);

        Processor processor = executableElement.getAnnotation(Processor.class);
        String elementName = executableElement.getSimpleName().toString();
        if (processor.name().length() != 0)
            elementName = processor.name();

        init.body().invoke("registerMuleBeanDefinitionParser").arg(JExpr.lit(elementName)).arg(JExpr._new(beanDefinitionParser));
    }

}