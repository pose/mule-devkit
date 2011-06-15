
package org.mule.devkit.it;

import org.apache.commons.lang.StringUtils;
import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class ItemCountBeanDefinitionParser
    extends ChildDefinitionParser
{


    public ItemCountBeanDefinitionParser() {
        super("messageProcessor", ItemCountMessageProcessor.class);
    }

    protected Class getBeanClass(Element element) {
        return ItemCountMessageProcessor.class;
    }

    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder beanDefinitionBuilder) {
        if (!StringUtils.isEmpty(element.getAttribute(getTargetPropertyConfiguration().getAttributeAlias("config-ref")))) {
            beanDefinitionBuilder.addPropertyReference("object", element.getAttribute(getTargetPropertyConfiguration().getAttributeAlias("config-ref")));
        }
        BeanAssembler assembler;
        assembler = getBeanAssembler(element, beanDefinitionBuilder);
        postProcess(getParserContext(), assembler, element);
    }

    protected String getAttributeValue(Element element, String attributeName) {
        if (!StringUtils.isEmpty(element.getAttribute(attributeName))) {
            return element.getAttribute(attributeName);
        }
        return null;
    }

}