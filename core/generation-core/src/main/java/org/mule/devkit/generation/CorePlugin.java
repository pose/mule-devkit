package org.mule.devkit.generation;

import org.mule.devkit.generation.adapter.CapabilitiesAdapterGenerator;
import org.mule.devkit.generation.adapter.ConnectionManagerGenerator;
import org.mule.devkit.generation.adapter.HttpCallbackAdapterGenerator;
import org.mule.devkit.generation.adapter.InjectAdapterGenerator;
import org.mule.devkit.generation.adapter.LifecycleAdapterFactoryGenerator;
import org.mule.devkit.generation.adapter.LifecycleAdapterGenerator;
import org.mule.devkit.generation.adapter.OAuth1AdapterGenerator;
import org.mule.devkit.generation.adapter.OAuth2AdapterGenerator;
import org.mule.devkit.generation.adapter.PoolAdapterGenerator;
import org.mule.devkit.generation.adapter.RestAdapterGenerator;
import org.mule.devkit.generation.api.AnnotationVerifier;
import org.mule.devkit.generation.api.Generator;
import org.mule.devkit.generation.api.Plugin;
import org.mule.devkit.generation.callback.DefaultHttpCallbackGenerator;
import org.mule.devkit.generation.mule.MessageProcessorGenerator;
import org.mule.devkit.generation.mule.MessageSourceGenerator;
import org.mule.devkit.generation.mule.NestedProcessorChainGenerator;
import org.mule.devkit.generation.mule.NestedProcessorStringGenerator;
import org.mule.devkit.generation.mule.RegistryBootstrapGenerator;
import org.mule.devkit.generation.mule.expression.ExpressionEnricherGenerator;
import org.mule.devkit.generation.mule.expression.ExpressionEvaluatorGenerator;
import org.mule.devkit.generation.mule.oauth.AuthorizeBeanDefinitionParserGenerator;
import org.mule.devkit.generation.mule.oauth.AuthorizeMessageProcessorGenerator;
import org.mule.devkit.generation.mule.oauth.DefaultRestoreAccessTokenCallbackFactoryGenerator;
import org.mule.devkit.generation.mule.oauth.DefaultRestoreAccessTokenCallbackGenerator;
import org.mule.devkit.generation.mule.oauth.DefaultSaveAccessTokenCallbackFactoryGenerator;
import org.mule.devkit.generation.mule.oauth.DefaultSaveAccessTokenCallbackGenerator;
import org.mule.devkit.generation.mule.transfomer.EnumTransformerGenerator;
import org.mule.devkit.generation.mule.transfomer.JaxbTransformerGenerator;
import org.mule.devkit.generation.mule.transfomer.StringToBigDecimalTransformerGenerator;
import org.mule.devkit.generation.mule.transfomer.StringToBigIntegerTransformerGenerator;
import org.mule.devkit.generation.mule.transfomer.StringToClassTransformerGenerator;
import org.mule.devkit.generation.mule.transfomer.StringToDateTransformerGenerator;
import org.mule.devkit.generation.mule.transfomer.TransformerGenerator;
import org.mule.devkit.generation.spring.AbstractBeanDefinitionParserGenerator;
import org.mule.devkit.generation.spring.BeanDefinitionParserGenerator;
import org.mule.devkit.generation.spring.NamespaceHandlerGenerator;
import org.mule.devkit.generation.spring.SchemaGenerator;

import java.util.ArrayList;
import java.util.List;

public class CorePlugin implements Plugin {
    private List<AnnotationVerifier> annotationVerifiers;
    private List<Generator> generators;

    public CorePlugin() {
        generators = new ArrayList<Generator>();
        generators.add(new StringToDateTransformerGenerator());
        generators.add(new StringToBigDecimalTransformerGenerator());
        generators.add(new StringToBigIntegerTransformerGenerator());
        generators.add(new StringToClassTransformerGenerator());
        generators.add(new DefaultHttpCallbackGenerator());
        generators.add(new CapabilitiesAdapterGenerator());
        generators.add(new LifecycleAdapterGenerator());
        generators.add(new InjectAdapterGenerator());
        generators.add(new RestAdapterGenerator());
        generators.add(new HttpCallbackAdapterGenerator());
        generators.add(new OAuth1AdapterGenerator());
        generators.add(new OAuth2AdapterGenerator());
        generators.add(new LifecycleAdapterFactoryGenerator());
        generators.add(new ConnectionManagerGenerator()); // this should be the last on the chain of adapters
        generators.add(new PoolAdapterGenerator());
        generators.add(new JaxbTransformerGenerator());
        generators.add(new TransformerGenerator());
        generators.add(new EnumTransformerGenerator());
        generators.add(new NestedProcessorChainGenerator());
        generators.add(new NestedProcessorStringGenerator());
        generators.add(new DefaultSaveAccessTokenCallbackGenerator());
        generators.add(new DefaultRestoreAccessTokenCallbackGenerator());
        generators.add(new DefaultRestoreAccessTokenCallbackFactoryGenerator());
        generators.add(new DefaultSaveAccessTokenCallbackFactoryGenerator());
        generators.add(new AbstractBeanDefinitionParserGenerator());
        generators.add(new MessageSourceGenerator());
        generators.add(new MessageProcessorGenerator());
        generators.add(new AuthorizeMessageProcessorGenerator());
        generators.add(new AuthorizeBeanDefinitionParserGenerator());
        generators.add(new BeanDefinitionParserGenerator());
        generators.add(new NamespaceHandlerGenerator());
        generators.add(new ExpressionEvaluatorGenerator());
        generators.add(new ExpressionEnricherGenerator());
        generators.add(new RegistryBootstrapGenerator());
        generators.add(new SchemaGenerator());

        annotationVerifiers = new ArrayList<AnnotationVerifier>();
        annotationVerifiers.add(new BasicAnnotationVerifier());
        annotationVerifiers.add(new OAuthAnnotationVerifier());
        annotationVerifiers.add(new ProcessorAnnotationVerifier());
        annotationVerifiers.add(new ConnectorAnnotationVerifier());
        annotationVerifiers.add(new SourceAnnotationVerifier());
        annotationVerifiers.add(new TransformerAnnotationVerifier());
        annotationVerifiers.add(new InjectAnnotationVerifier());
        annotationVerifiers.add(new RestAnnotationVerifier());
    }

    @Override
    public String getOptionName() {
        return null;
    }

    @Override
    public List<AnnotationVerifier> getAnnotationVerifiers() {
        return annotationVerifiers;
    }

    @Override
    public List<Generator> getGenerators() {
        return generators;
    }
}
