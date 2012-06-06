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
package org.mule.devkit.generation.adapter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.mule.api.Capabilities;
import org.mule.api.ConnectionManager;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.param.ConnectionKey;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.config.PoolingProfile;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.generation.NamingContants;
import org.mule.devkit.model.DevKitExecutableElement;
import org.mule.devkit.model.DevKitFieldElement;
import org.mule.devkit.model.DevKitParameterElement;
import org.mule.devkit.model.DevKitTypeElement;
import org.mule.devkit.model.code.Cast;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.ClassAlreadyExistsException;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.DefinedClassRoles;
import org.mule.devkit.model.code.Expression;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;

import java.util.Iterator;
import java.util.Map;

public class ConnectionManagerGenerator extends AbstractMessageGenerator {

    @Override
    public boolean shouldGenerate(DevKitTypeElement typeElement) {
        DevKitExecutableElement connectMethod = connectMethodForClass(typeElement);
        DevKitExecutableElement disconnectMethod = disconnectMethodForClass(typeElement);

        if (connectMethod == null || disconnectMethod == null) {
            return false;
        }

        return true;
    }

    @Override
    public void generate(DevKitTypeElement typeElement) throws GenerationException {
        DevKitExecutableElement connectMethod = connectMethodForClass(typeElement);
        DevKitExecutableElement disconnectMethod = disconnectMethodForClass(typeElement);
        DevKitExecutableElement validateConnectionMethod = validateConnectionMethodForClass(typeElement);

        DefinedClass connectionManagerClass = getConnectionManagerAdapterClass(typeElement);

        // generate fields for each connection parameters
        Map<String, AbstractMessageGenerator.FieldVariableElement> fields = generateStandardFieldForEachParameter(connectionManagerClass, connectMethod);

        // generate fields for each configurable field
        for (DevKitFieldElement field : typeElement.getFieldsAnnotatedWith(Configurable.class)) {
            FieldVariable configField = connectionManagerClass.field(Modifier.PRIVATE, ref(field.asType()), field.getSimpleName().toString());
            generateSetter(connectionManagerClass, configField);
            generateGetter(connectionManagerClass, configField);
        }
        
        // logger field
        FieldVariable logger = generateLoggerField(connectionManagerClass);        

        // standard fields
        FieldVariable muleContext = generateFieldForMuleContext(connectionManagerClass);
        FieldVariable flowConstruct = generateFieldForFlowConstruct(connectionManagerClass);

        // generate field for connection pool
        FieldVariable connectionPool = generateFieldForConnectionPool(connectionManagerClass);
        FieldVariable poolingProfile = connectionManagerClass.field(Modifier.PROTECTED, ref(PoolingProfile.class), "connectionPoolingProfile");

        // generate getter and setter for pooling profile
        generateSetter(connectionManagerClass, poolingProfile);
        generateGetter(connectionManagerClass, poolingProfile);

        // generate setters for all parameters
        for (String fieldName : fields.keySet()) {
            generateSetter(connectionManagerClass, fields.get(fieldName).getField());
            generateGetter(connectionManagerClass, fields.get(fieldName).getField());
        }

        // standard fields setters
        generateSetFlowConstructMethod(connectionManagerClass, flowConstruct);
        generateSetMuleContextMethod(connectionManagerClass, muleContext);

        DefinedClass connectionKeyClass = getConnectionParametersClass(typeElement, connectionManagerClass);

        // generate key fields
        Map<String, AbstractMessageGenerator.FieldVariableElement> keyFields = generateStandardFieldForEachParameter(connectionKeyClass, connectMethod);

        // generate constructor for key
        generateKeyConstructor(connectMethod, connectionKeyClass, keyFields);

        // generate setters for all keys
        for (String fieldName : keyFields.keySet()) {
            generateSetter(connectionKeyClass, keyFields.get(fieldName).getField());
            generateGetter(connectionKeyClass, keyFields.get(fieldName).getField());
        }

        generateConnectionKeyHashCodeMethod(connectMethod, connectionKeyClass);
        generateConnectionKeyEqualsMethod(connectMethod, connectionKeyClass);

        DefinedClass connectionFactoryClass = getConnectorFactoryClass(connectionManagerClass);

        FieldVariable connectionManagerInFactory = connectionFactoryClass.field(Modifier.PRIVATE,
                connectionManagerClass, "connectionManager");

        Method connectionFactoryConstructor = connectionFactoryClass.constructor(Modifier.PUBLIC);
        Variable constructorConnectionManager = connectionFactoryConstructor.param(connectionManagerClass, "connectionManager");
        connectionFactoryConstructor.body().assign(ExpressionFactory._this().ref(connectionManagerInFactory),
                constructorConnectionManager);

        generateMakeObjectMethod(typeElement, connectMethod, connectionFactoryClass, connectionKeyClass, connectionManagerInFactory);
        generateDestroyObjectMethod(connectMethod, disconnectMethod, connectionKeyClass, connectionFactoryClass);
        generateValidateObjectMethod(connectionFactoryClass, logger, validateConnectionMethod);
        generateActivateObjectMethod(connectionFactoryClass, validateConnectionMethod, connectMethod, keyFields, connectionKeyClass);
        generatePassivateObjectMethod(connectionFactoryClass);

        generateInitialiseMethod(connectionManagerClass, connectionPool, poolingProfile, connectionFactoryClass);

        generateBorrowConnectionMethod(connectMethod, connectionManagerClass, connectionPool, connectionKeyClass);
        generateReturnConnectionMethod(connectMethod, connectionManagerClass, connectionPool, connectionKeyClass);
        generateDestroyConnectionMethod(connectMethod, connectionManagerClass, connectionPool, connectionKeyClass);

        generateIsCapableOf(typeElement, connectionManagerClass);
    }

    private void generateConnectionKeyHashCodeMethod(DevKitExecutableElement connect, DefinedClass connectionKeyClass) {
        Method hashCode = connectionKeyClass.method(Modifier.PUBLIC, ctx().getCodeModel().INT, "hashCode");
        Variable hash = hashCode.body().decl(ctx().getCodeModel().INT, "hash", ExpressionFactory.lit(1));

        for (DevKitParameterElement variable : connect.getParameters()) {
            if (variable.getAnnotation(ConnectionKey.class) == null) {
                continue;
            }

            String fieldName = variable.getSimpleName().toString();

            hashCode.body().assign(hash,
                    Op.plus(
                            Op.mul(hash, ExpressionFactory.lit(31)),
                            ExpressionFactory._this().ref(fieldName).invoke("hashCode")
                    )
            );
        }

        hashCode.body()._return(
                hash
        );
    }

    private void generateConnectionKeyEqualsMethod(DevKitExecutableElement connect, DefinedClass connectionKey) {
        Method equals = connectionKey.method(Modifier.PUBLIC, ctx().getCodeModel().BOOLEAN, "equals");
        Variable obj = equals.param(ref(Object.class), "obj");
        Expression areEqual = Op._instanceof(obj, connectionKey);

        for (DevKitParameterElement variable : connect.getParameters()) {
            if (variable.getAnnotation(ConnectionKey.class) == null) {
                continue;
            }

            String fieldName = variable.getSimpleName().toString();
            areEqual = Op.cand(areEqual, Op.eq(
                    ExpressionFactory._this().ref(fieldName),
                    ExpressionFactory.cast(connectionKey, obj).ref(fieldName)
            ));
        }

        equals.body()._return(
                areEqual
        );
    }

    private void generateBorrowConnectionMethod(DevKitExecutableElement connect, DefinedClass connectionManagerClass, FieldVariable connectionPool, DefinedClass connectionKeyClass) {
        DefinedClass connectorClass = ctx().getCodeModel()._class(DefinedClassRoles.CONNECTOR_OBJECT, ref(connect.parent()));
        Method borrowConnector = connectionManagerClass.method(Modifier.PUBLIC, connectorClass, "acquireConnection");
        Variable key = borrowConnector.param(connectionKeyClass, "key");
        borrowConnector._throws(ref(Exception.class));

        borrowConnector.body()._return(
                ExpressionFactory.cast(connectorClass,
                        connectionPool.invoke("borrowObject").arg(
                                key
                        ))
        );
    }

    private void generateReturnConnectionMethod(DevKitExecutableElement connect, DefinedClass connectionManagerClass, FieldVariable connectionPool, DefinedClass connectionKeyClass) {
        DefinedClass connectorClass = ctx().getCodeModel()._class(DefinedClassRoles.CONNECTOR_OBJECT, ref(connect.parent()));
        Method returnConnector = connectionManagerClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "releaseConnection");
        Variable key = returnConnector.param(connectionKeyClass, "key");
        returnConnector._throws(ref(Exception.class));
        Variable connection = returnConnector.param(connectorClass, "connection");
        returnConnector.body().add(
                connectionPool.invoke("returnObject").arg(
                        key
                ).arg(connection)
        );
    }

    private void generateDestroyConnectionMethod(DevKitExecutableElement connect, DefinedClass connectionManagerClass, FieldVariable connectionPool, DefinedClass connectionKeyClass) {
        DefinedClass connectorClass = ctx().getCodeModel()._class(DefinedClassRoles.CONNECTOR_OBJECT, ref(connect.parent()));
        Method destroyConnector = connectionManagerClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "destroyConnection");
        Variable key = destroyConnector.param(connectionKeyClass, "key");
        destroyConnector._throws(ref(Exception.class));
        Variable connection = destroyConnector.param(connectorClass, "connection");
        destroyConnector.body().add(
                connectionPool.invoke("invalidateObject").arg(
                        key
                ).arg(connection)
        );
    }

    private void generateInitialiseMethod(DefinedClass connectionManagerClass, FieldVariable connectionPool, FieldVariable connectionPoolingProfile, DefinedClass connectionFactoryClass) {
        Method initialisableMethod = connectionManagerClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "initialise");

        Variable config = initialisableMethod.body().decl(ref(GenericKeyedObjectPool.Config.class), "config",
                ExpressionFactory._new(ref(GenericKeyedObjectPool.Config.class)));

        Conditional ifNotNull = initialisableMethod.body()._if(Op.ne(connectionPoolingProfile, ExpressionFactory._null()));
        ifNotNull._then().assign(config.ref("maxIdle"), connectionPoolingProfile.invoke("getMaxIdle"));
        ifNotNull._then().assign(config.ref("maxActive"), connectionPoolingProfile.invoke("getMaxActive"));
        ifNotNull._then().assign(config.ref("maxWait"), connectionPoolingProfile.invoke("getMaxWait"));
        ifNotNull._then().assign(config.ref("whenExhaustedAction"), ExpressionFactory.cast(ctx().getCodeModel().BYTE, connectionPoolingProfile.invoke("getExhaustedAction")));

        Invocation newObjectFactory = ExpressionFactory._new(connectionFactoryClass);
        newObjectFactory.arg(ExpressionFactory._this());
        initialisableMethod.body().assign(connectionPool, ExpressionFactory._new(ref(GenericKeyedObjectPool.class)).arg(
                newObjectFactory
        ).arg(config));
    }

    private void generateActivateObjectMethod(DefinedClass connectionFactoryClass, DevKitExecutableElement validateConnectionMethod, DevKitExecutableElement connect, Map<String, FieldVariableElement> keyFields, DefinedClass connectionKeyClass) {
        DefinedClass connectorClass = ctx().getCodeModel()._class(DefinedClassRoles.CONNECTOR_OBJECT, ref(validateConnectionMethod.parent()));
        Method activateObject = connectionFactoryClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "activateObject");
        activateObject._throws(ref(Exception.class));
        Variable key = activateObject.param(Object.class, "key");
        Variable obj = activateObject.param(Object.class, "obj");

        Conditional ifNotKey = activateObject.body()._if(Op.not(Op._instanceof(key, connectionKeyClass)));
        ifNotKey._then()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("Invalid key type"));

        Conditional ifNotObj = activateObject.body()._if(Op.not(Op._instanceof(obj, connectorClass)));
        ifNotObj._then()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("Invalid connector type"));
        
        Cast casterConnector = ExpressionFactory.cast(connectorClass, obj);
        TryStatement tryDisconnect = activateObject.body()._try();
        Conditional ifNotConnected = tryDisconnect.body()._if(Op.not(casterConnector.invoke(validateConnectionMethod.getSimpleName().toString())));
        Cast castedConnectionKey = ExpressionFactory.cast(connectionKeyClass, key);
        Invocation connectInvoke = ExpressionFactory.cast(connectorClass, obj).invoke(connect.getSimpleName().toString());
        for (DevKitParameterElement variable : connect.getParameters()) {
            String fieldName = variable.getSimpleName().toString();
            connectInvoke.arg(castedConnectionKey.invoke("get" + StringUtils.capitalize(keyFields.get(fieldName).getField().name())));
        }
        ifNotConnected._then().add(connectInvoke);

        
        CatchBlock catchAndRethrow = tryDisconnect._catch(ref(Exception.class));
        Variable e = catchAndRethrow.param("e");
        catchAndRethrow.body()._throw(e);
        
    }

    private void generatePassivateObjectMethod(DefinedClass connectionFactoryClass) {
        Method passivateObject = connectionFactoryClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "passivateObject");
        passivateObject._throws(ref(Exception.class));
        passivateObject.param(Object.class, "key");
        passivateObject.param(Object.class, "obj");
    }

    private void generateValidateObjectMethod(DefinedClass connectionFactoryClass, FieldVariable logger, DevKitExecutableElement validateConnectionMethod) {
        DefinedClass connectorClass = ctx().getCodeModel()._class(DefinedClassRoles.CONNECTOR_OBJECT, ref(validateConnectionMethod.parent()));
        Method validateObject = connectionFactoryClass.method(Modifier.PUBLIC, ctx().getCodeModel().BOOLEAN, "validateObject");
        validateObject.param(Object.class, "key");
        Variable obj = validateObject.param(Object.class, "obj");

        Conditional ifNotObj = validateObject.body()._if(Op.not(Op._instanceof(obj, connectorClass)));
        ifNotObj._then()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("Invalid connector type"));

        Cast casterConnector = ExpressionFactory.cast(connectorClass, obj);
        TryStatement tryDisconnect = validateObject.body()._try();
        tryDisconnect.body()._return(casterConnector.invoke(validateConnectionMethod.getSimpleName().toString()));
        CatchBlock catchAndRethrow = tryDisconnect._catch(ref(Exception.class));
        Variable e = catchAndRethrow.param("e");
        catchAndRethrow.body().add(logger.invoke("error").arg(e.invoke("getMessage")).arg(e));
        catchAndRethrow.body()._return(ExpressionFactory.FALSE);
    }

    private void generateDestroyObjectMethod(DevKitExecutableElement connect, DevKitExecutableElement disconnect, DefinedClass connectionKeyClass, DefinedClass connectionFactoryClass) {
        DefinedClass connectorClass = ctx().getCodeModel()._class(DefinedClassRoles.CONNECTOR_OBJECT, ref(connect.parent()));

        Method destroyObject = connectionFactoryClass.method(Modifier.PUBLIC, ctx().getCodeModel().VOID, "destroyObject");
        destroyObject._throws(ref(Exception.class));
        Variable key = destroyObject.param(Object.class, "key");
        Variable obj = destroyObject.param(Object.class, "obj");
        Conditional ifNotKey = destroyObject.body()._if(Op.not(Op._instanceof(key, connectionKeyClass)));
        ifNotKey._then()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("Invalid key type"));

        Conditional ifNotObj = destroyObject.body()._if(Op.not(Op._instanceof(obj, connectorClass)));
        ifNotObj._then()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("Invalid connector type"));

        Cast casterConnector = ExpressionFactory.cast(connectorClass, obj);
        TryStatement tryDisconnect = destroyObject.body()._try();
        tryDisconnect.body().add(casterConnector.invoke(disconnect.getSimpleName().toString()));
        CatchBlock catchAndRethrow = tryDisconnect._catch(ref(Exception.class));
        Variable e = catchAndRethrow.param("e");
        catchAndRethrow.body()._throw(e);
        tryDisconnect._finally()._if(Op._instanceof(casterConnector, ref(Stoppable.class)))._then().add(casterConnector.invoke("stop"));
        tryDisconnect._finally()._if(Op._instanceof(casterConnector, ref(Disposable.class)))._then().add(casterConnector.invoke("dispose"));
    }

    private void generateMakeObjectMethod(DevKitTypeElement typeElement, DevKitExecutableElement connect, DefinedClass connectionFactoryClass, DefinedClass connectionKey, FieldVariable connectionManagerInFactory) {
        DefinedClass connectorClass = ctx().getCodeModel()._class(DefinedClassRoles.CONNECTOR_OBJECT, ref(connect.parent()));
        Method makeObject = connectionFactoryClass.method(Modifier.PUBLIC, Object.class, "makeObject");
        makeObject._throws(ref(Exception.class));
        Variable key = makeObject.param(Object.class, "key");
        Conditional ifNotKey = makeObject.body()._if(Op.not(Op._instanceof(key, connectionKey)));
        ifNotKey._then()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("Invalid key type"));

        Variable connector = makeObject.body().decl(connectorClass, "connector", ExpressionFactory._new(connectorClass));

        for (DevKitFieldElement field : typeElement.getFieldsAnnotatedWith(Configurable.class)) {
            makeObject.body().add(connector.invoke("set" + StringUtils.capitalize(field.getSimpleName().toString()))
                    .arg(connectionManagerInFactory.invoke("get" + StringUtils.capitalize(field.getSimpleName().toString()))));
        }

        makeObject.body()._if(Op._instanceof(connector, ref(Initialisable.class)))._then().add(connector.invoke("initialise"));
        makeObject.body()._if(Op._instanceof(connector, ref(Startable.class)))._then().add(connector.invoke("start"));

        setMuleContextToConnectorIfNecessary(connectionManagerInFactory, connectorClass, makeObject, connector);

        makeObject.body()._return(connector);
    }

    private void setMuleContextToConnectorIfNecessary(FieldVariable connectionManagerInFactory, DefinedClass connectorClass, Method makeObject, Variable connector) {
        Iterator<TypeReference> implementsIterator = connectorClass._implements();
        while (implementsIterator.hasNext()) {
            TypeReference implementedInterface = implementsIterator.next();
            if(implementedInterface.equals(ref(MuleContextAware.class))) {
                makeObject.body()._if(Op._instanceof(connector, ref(MuleContextAware.class)))._then().add(connector.invoke("setMuleContext").arg(ExpressionFactory.direct(connectionManagerInFactory.name() + "." + MULE_CONTEXT_FIELD_NAME)));
            }
        }
    }

    private void generateKeyConstructor(DevKitExecutableElement connect, DefinedClass connectionKeyClass, Map<String, FieldVariableElement> keyFields) {
        Method keyConstructor = connectionKeyClass.constructor(Modifier.PUBLIC);
        for (DevKitParameterElement variable : connect.getParameters()) {
            String fieldName = variable.getSimpleName().toString();
            Variable parameter = keyConstructor.param(ref(variable.asType()), fieldName);
            keyConstructor.body().assign(ExpressionFactory._this().ref(keyFields.get(fieldName).getField()), parameter);
        }
    }

    private FieldVariable generateFieldForConnectionPool(DefinedClass connectionManagerClass) {
        FieldVariable connectionPool = connectionManagerClass.field(Modifier.PRIVATE, ref(GenericKeyedObjectPool.class), "connectionPool");
        connectionPool.javadoc().add("Connector Pool");

        return connectionPool;
    }

    private DefinedClass getConnectionManagerAdapterClass(DevKitTypeElement typeElement) {
        String connectionManagerName = ctx().getNameUtils().generateClassName(typeElement, NamingContants.ADAPTERS_NAMESPACE, NamingContants.CONNECTION_MANAGER_ADAPTER_CLASS_NAME_SUFFIX);
        org.mule.devkit.model.code.Package pkg = ctx().getCodeModel()._package(ctx().getNameUtils().getPackageName(connectionManagerName));

        DefinedClass classToExtend = ctx().getCodeModel()._class(DefinedClassRoles.MODULE_OBJECT, ref(typeElement));
        classToExtend.role(DefinedClassRoles.CONNECTOR_OBJECT, ref(typeElement));

        DefinedClass connectionManagerClass = pkg._class(ctx().getNameUtils().getClassName(connectionManagerName));
        connectionManagerClass._implements(ref(Initialisable.class));
        connectionManagerClass._implements(ref(Capabilities.class));
        connectionManagerClass._implements(ref(MuleContextAware.class));
        connectionManagerClass._implements(ref(ConnectionManager.class).narrow(getConnectionParametersClass(typeElement, connectionManagerClass)).narrow(classToExtend));

        connectionManagerClass.role(DefinedClassRoles.MODULE_OBJECT, ref(typeElement));

        connectionManagerClass.javadoc().add("A {@code " + connectionManagerClass.name() + "} is a wrapper around ");
        connectionManagerClass.javadoc().add(ref(typeElement.asType()));
        connectionManagerClass.javadoc().add(" that adds connection management capabilities to the pojo.");

        return connectionManagerClass;
    }

    private DefinedClass getConnectionParametersClass(DevKitTypeElement typeElement, DefinedClass connectionManagerClass) {
        try {
            DefinedClass connectionKey = connectionManagerClass._class(Modifier.PUBLIC | Modifier.STATIC, NamingContants.CONNECTION_KEY_CLASS_NAME_SUFFIX);
            connectionKey.javadoc().add("A tuple of connection parameters");
            connectionKey.role(DefinedClassRoles.CONNECTION_PARAMETERS, ref(typeElement));
            return connectionKey;
        } catch (ClassAlreadyExistsException e) {
            return e.getExistingClass();
        }
    }

    private DefinedClass getConnectorFactoryClass(DefinedClass connectorManagerClass) {
        try {
            DefinedClass objectFactory = connectorManagerClass._class(Modifier.PRIVATE | Modifier.STATIC, NamingContants.CONNECTION_FACTORY_CLASS_NAME_SUFFIX);
            objectFactory._implements(KeyedPoolableObjectFactory.class);
            return objectFactory;
        } catch (ClassAlreadyExistsException e) {
            return e.getExistingClass();
        }
    }
}