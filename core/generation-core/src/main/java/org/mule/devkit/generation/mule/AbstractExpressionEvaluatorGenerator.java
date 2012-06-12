package org.mule.devkit.generation.mule;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.ExpressionLanguage;
import org.mule.api.annotations.Module;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.NamingConstants;
import org.mule.devkit.generation.api.GenerationException;
import org.mule.devkit.model.Type;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.ClassAlreadyExistsException;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.DefinedClassRoles;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.ForEach;
import org.mule.devkit.model.code.ForLoop;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.TemplateParser;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class AbstractExpressionEvaluatorGenerator extends AbstractMessageGenerator {

    public boolean hasBeenGenerated = false;

    @Override
    public boolean shouldGenerate(Type type) {
        return (type.hasAnnotation(Module.class) || type.hasAnnotation(Connector.class) || type.hasAnnotation(ExpressionLanguage.class)) &&
                hasBeenGenerated  == false;
    }

    @Override
    public void generate(Type type) throws GenerationException {
        DefinedClass abstractExpressionEvaluatorClass = getAbstractExpressionEvaluatorClass(type);

        // generate evaluate & transform methods and helpers
        generateComputeClassHierarchyMethod(abstractExpressionEvaluatorClass);
        generateIsListClassMethod(abstractExpressionEvaluatorClass);
        generateIsMapClassMethod(abstractExpressionEvaluatorClass);
        generateIsListMethod(abstractExpressionEvaluatorClass);
        generateIsMapMethod(abstractExpressionEvaluatorClass);
        generateIsAssignableFrom(abstractExpressionEvaluatorClass);
        generateEvaluateMethod(abstractExpressionEvaluatorClass);
        generateEvaluateAndTransformMethod(abstractExpressionEvaluatorClass);
        generateTransformMethod(abstractExpressionEvaluatorClass);

        hasBeenGenerated = true;
    }

    private DefinedClass getAbstractExpressionEvaluatorClass(Type type) {
        org.mule.devkit.model.code.Package pkg = ctx().getCodeModel()._package(type.getPackageName());
        DefinedClass clazz = null;
        try {
            clazz = pkg._class(Modifier.ABSTRACT | Modifier.PUBLIC, NamingConstants.ABSTRACT_EXPRESSION_EVALUATOR_CLASS_NAME_SUFFIX);
        } catch (ClassAlreadyExistsException e) {
            clazz = e.getExistingClass();
        }

        clazz.role(DefinedClassRoles.ABSTRACT_EXPRESSION_EVALUATOR);

        return clazz;
    }


    private void generateComputeClassHierarchyMethod(DefinedClass abstractExpressionEvaluatorClass) {
        org.mule.devkit.model.code.Method computeClassHierarchy = abstractExpressionEvaluatorClass.method(Modifier.PROTECTED, ctx().getCodeModel().VOID, "computeClassHierarchy");
        computeClassHierarchy.javadoc().add("Get all superclasses and interfaces recursively.");
        computeClassHierarchy.javadoc().addParam("clazz   The class to start the search with.");
        computeClassHierarchy.javadoc().addParam("classes List of classes to which to add all found super classes and interfaces.");
        org.mule.devkit.model.code.Variable clazz = computeClassHierarchy.param(Class.class, "clazz");
        org.mule.devkit.model.code.Variable classes = computeClassHierarchy.param(List.class, "classes");

        ForLoop iterateClasses = computeClassHierarchy.body()._for();
        org.mule.devkit.model.code.Variable current = iterateClasses.init(ref(Class.class), "current", clazz);
        iterateClasses.test(Op.ne(current, ExpressionFactory._null()));
        iterateClasses.update(current.assign(current.invoke("getSuperclass")));

        Block ifContains = iterateClasses.body()._if(classes.invoke("contains").arg(current))._then();
        ifContains._return();

        iterateClasses.body().add(classes.invoke("add").arg(current));

        ForEach iterateInterfaces = iterateClasses.body().forEach(ref(Class.class), "currentInterface", current.invoke("getInterfaces"));
        iterateInterfaces.body().invoke("computeClassHierarchy").arg(iterateInterfaces.var()).arg(classes);
    }

    private void generateIsListClassMethod(DefinedClass abstractExpressionEvaluatorClass) {
        org.mule.devkit.model.code.Method isListClass = abstractExpressionEvaluatorClass.method(Modifier.PROTECTED, ctx().getCodeModel().BOOLEAN, "isListClass");
        isListClass.javadoc().add("Checks whether the specified class parameter is an instance of ");
        isListClass.javadoc().add(ref(List.class));
        isListClass.javadoc().addParam("clazz <code>Class</code> to check.");
        isListClass.javadoc().addReturn("<code>true</code> is <code>clazz</code> is instance of a collection class, <code>false</code> otherwise.");

        org.mule.devkit.model.code.Variable clazz = isListClass.param(ref(Class.class), "clazz");
        org.mule.devkit.model.code.Variable classes = isListClass.body().decl(ref(List.class).narrow(ref(Class.class)), "classes", ExpressionFactory._new(ref(ArrayList.class).narrow(ref(Class.class))));
        isListClass.body().invoke("computeClassHierarchy").arg(clazz).arg(classes);

        isListClass.body()._return(classes.invoke("contains").arg(ref(List.class).dotclass()));
    }

    private void generateIsMapClassMethod(DefinedClass abstractExpressionEvaluatorClass) {
        org.mule.devkit.model.code.Method isMapClass = abstractExpressionEvaluatorClass.method(Modifier.PROTECTED, ctx().getCodeModel().BOOLEAN, "isMapClass");
        isMapClass.javadoc().add("Checks whether the specified class parameter is an instance of ");
        isMapClass.javadoc().add(ref(Map.class));
        isMapClass.javadoc().addParam("clazz <code>Class</code> to check.");
        isMapClass.javadoc().addReturn("<code>true</code> is <code>clazz</code> is instance of a collection class, <code>false</code> otherwise.");

        org.mule.devkit.model.code.Variable clazz = isMapClass.param(ref(Class.class), "clazz");
        org.mule.devkit.model.code.Variable classes = isMapClass.body().decl(ref(List.class).narrow(ref(Class.class)), "classes", ExpressionFactory._new(ref(ArrayList.class).narrow(ref(Class.class))));
        isMapClass.body().invoke("computeClassHierarchy").arg(clazz).arg(classes);

        isMapClass.body()._return(classes.invoke("contains").arg(ref(Map.class).dotclass()));
    }

    private void generateIsListMethod(DefinedClass abstractExpressionEvaluatorClass) {
        org.mule.devkit.model.code.Method isList = abstractExpressionEvaluatorClass.method(Modifier.PROTECTED, ctx().getCodeModel().BOOLEAN, "isList");
        org.mule.devkit.model.code.Variable type = isList.param(ref(java.lang.reflect.Type.class), "type");

        Conditional isClass = isList.body()._if(Op.cand(Op._instanceof(type, ref(Class.class)),
                ExpressionFactory.invoke("isListClass").arg(ExpressionFactory.cast(ref(Class.class), type))));
        isClass._then()._return(ExpressionFactory.TRUE);

        Conditional isParameterizedType = isList.body()._if(Op._instanceof(type, ref(ParameterizedType.class)));
        isParameterizedType._then()._return(
                ExpressionFactory.invoke("isList").arg(
                        ExpressionFactory.cast(ref(ParameterizedType.class), type).invoke("getRawType")
                )
        );

        Conditional isWildcardType = isList.body()._if(Op._instanceof(type, ref(WildcardType.class)));
        org.mule.devkit.model.code.Variable upperBounds = isWildcardType._then().decl(ref(java.lang.reflect.Type.class).array(), "upperBounds",
                ExpressionFactory.cast(ref(WildcardType.class), type).invoke("getUpperBounds"));
        isWildcardType._then()._return(Op.cand(
                Op.ne(upperBounds.ref("length"), ExpressionFactory.lit(0)),
                ExpressionFactory.invoke("isList").arg(upperBounds.component(ExpressionFactory.lit(0)))
        ));

        isList.body()._return(ExpressionFactory.FALSE);
    }

    private void generateIsMapMethod(DefinedClass abstractExpressionEvaluatorClass) {
        org.mule.devkit.model.code.Method isMap = abstractExpressionEvaluatorClass.method(Modifier.PROTECTED, ctx().getCodeModel().BOOLEAN, "isMap");
        org.mule.devkit.model.code.Variable type = isMap.param(ref(java.lang.reflect.Type.class), "type");

        Conditional isClass = isMap.body()._if(Op.cand(Op._instanceof(type, ref(Class.class)),
                ExpressionFactory.invoke("isMapClass").arg(ExpressionFactory.cast(ref(Class.class), type))));
        isClass._then()._return(ExpressionFactory.TRUE);

        Conditional isParameterizedType = isMap.body()._if(Op._instanceof(type, ref(ParameterizedType.class)));
        isParameterizedType._then()._return(
                ExpressionFactory.invoke("isMap").arg(
                        ExpressionFactory.cast(ref(ParameterizedType.class), type).invoke("getRawType")
                )
        );

        Conditional isWildcardType = isMap.body()._if(Op._instanceof(type, ref(WildcardType.class)));
        org.mule.devkit.model.code.Variable upperBounds = isWildcardType._then().decl(ref(java.lang.reflect.Type.class).array(), "upperBounds",
                ExpressionFactory.cast(ref(WildcardType.class), type).invoke("getUpperBounds"));
        isWildcardType._then()._return(Op.cand(
                Op.ne(upperBounds.ref("length"), ExpressionFactory.lit(0)),
                ExpressionFactory.invoke("isMap").arg(upperBounds.component(ExpressionFactory.lit(0)))
        ));

        isMap.body()._return(ExpressionFactory.FALSE);
    }

    private void generateIsAssignableFrom(DefinedClass abstractExpressionEvaluatorClass) {
        org.mule.devkit.model.code.Method isAssignableFrom = abstractExpressionEvaluatorClass.method(Modifier.PROTECTED, ctx().getCodeModel().BOOLEAN, "isAssignableFrom");
        org.mule.devkit.model.code.Variable expectedType = isAssignableFrom.param(ref(java.lang.reflect.Type.class), "expectedType");
        org.mule.devkit.model.code.Variable clazz = isAssignableFrom.param(ref(Class.class), "clazz");

        Block isClass = isAssignableFrom.body()._if(Op._instanceof(expectedType, ref(Class.class)))._then();
        Conditional isPrimitive = isClass._if(ExpressionFactory.cast(ref(Class.class), expectedType).invoke("isPrimitive"));
        isPrimitive._then()._if(Op.cand(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("getName").invoke("equals").arg(ExpressionFactory.lit("boolean")),
                Op.eq(clazz, ref(Boolean.class).dotclass())
        ))._then()._return(ExpressionFactory.TRUE);

        isPrimitive._then()._if(Op.cand(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("getName").invoke("equals").arg(ExpressionFactory.lit("byte")),
                Op.eq(clazz, ref(Byte.class).dotclass())
        ))._then()._return(ExpressionFactory.TRUE);

        isPrimitive._then()._if(Op.cand(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("getName").invoke("equals").arg(ExpressionFactory.lit("short")),
                Op.eq(clazz, ref(Short.class).dotclass())
        ))._then()._return(ExpressionFactory.TRUE);

        isPrimitive._then()._if(Op.cand(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("getName").invoke("equals").arg(ExpressionFactory.lit("char")),
                Op.eq(clazz, ref(Character.class).dotclass())
        ))._then()._return(ExpressionFactory.TRUE);

        isPrimitive._then()._if(Op.cand(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("getName").invoke("equals").arg(ExpressionFactory.lit("int")),
                Op.eq(clazz, ref(Integer.class).dotclass())
        ))._then()._return(ExpressionFactory.TRUE);

        isPrimitive._then()._if(Op.cand(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("getName").invoke("equals").arg(ExpressionFactory.lit("float")),
                Op.eq(clazz, ref(Float.class).dotclass())
        ))._then()._return(ExpressionFactory.TRUE);

        isPrimitive._then()._if(Op.cand(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("getName").invoke("equals").arg(ExpressionFactory.lit("long")),
                Op.eq(clazz, ref(Long.class).dotclass())
        ))._then()._return(ExpressionFactory.TRUE);

        isPrimitive._then()._if(Op.cand(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("getName").invoke("equals").arg(ExpressionFactory.lit("double")),
                Op.eq(clazz, ref(Double.class).dotclass())
        ))._then()._return(ExpressionFactory.TRUE);

        isPrimitive._then()._return(ExpressionFactory.FALSE);

        isPrimitive._else()._return(
                ExpressionFactory.cast(ref(Class.class), expectedType).invoke("isAssignableFrom").arg(clazz)
        );

        Block isParameterizedType = isAssignableFrom.body()._if(
                Op._instanceof(expectedType, ref(ParameterizedType.class)))._then();
        isParameterizedType._return(
                ExpressionFactory.invoke("isAssignableFrom").arg(
                        ExpressionFactory.cast(ref(ParameterizedType.class), expectedType).invoke("getRawType")
                ).arg(
                        clazz
                )
        );

        Block isWildcardType = isAssignableFrom.body()._if(
                Op._instanceof(expectedType, ref(WildcardType.class)))._then();
        org.mule.devkit.model.code.Variable upperBounds = isWildcardType.decl(ref(java.lang.reflect.Type.class).array(), "upperBounds",
                ExpressionFactory.cast(ref(WildcardType.class), expectedType).invoke("getUpperBounds"));
        Block ifHasUpperBounds = isWildcardType._if(Op.ne(upperBounds.ref("length"), ExpressionFactory.lit(0)))._then();
        ifHasUpperBounds._return(
                ExpressionFactory.invoke("isAssignableFrom").arg(
                        upperBounds.component(ExpressionFactory.lit(0))).arg(clazz));

        isAssignableFrom.body()._return(ExpressionFactory.FALSE);
    }

    private void generateEvaluateMethod(DefinedClass abstractExpressionEvaluatorClass) {
        org.mule.devkit.model.code.Method evaluate = abstractExpressionEvaluatorClass.method(Modifier.PROTECTED, ref(Object.class), "evaluate");
        org.mule.devkit.model.code.Variable patternInfo = evaluate.param(ref(TemplateParser.PatternInfo.class), "patternInfo");
        org.mule.devkit.model.code.Variable expressionManager = evaluate.param(ref(ExpressionManager.class), "expressionManager");
        org.mule.devkit.model.code.Variable muleMessage = evaluate.param(ref(MuleMessage.class), "muleMessage");
        org.mule.devkit.model.code.Variable source = evaluate.param(ref(Object.class), "source");

        Block ifString = evaluate.body()._if(Op._instanceof(source, ref(String.class)))._then();
        org.mule.devkit.model.code.Variable stringSource = ifString.decl(ref(String.class), "stringSource", ExpressionFactory.cast(ref(String.class), source));
        Conditional isPattern = ifString._if(Op.cand(
                stringSource.invoke("startsWith").arg(patternInfo.invoke("getPrefix")),
                stringSource.invoke("endsWith").arg(patternInfo.invoke("getSuffix"))
        ));

        isPattern._then()._return(expressionManager.invoke("evaluate").arg(stringSource).arg(muleMessage));
        isPattern._else()._return(expressionManager.invoke("parse").arg(stringSource).arg(muleMessage));

        evaluate.body()._return(source);
    }

    private void generateEvaluateAndTransformMethod(DefinedClass abstractExpressionEvaluatorClass) {
        org.mule.devkit.model.code.Method evaluateAndTransform = abstractExpressionEvaluatorClass.method(Modifier.PROTECTED, ref(Object.class), "evaluateAndTransform");
        evaluateAndTransform._throws(ref(TransformerException.class));
        org.mule.devkit.model.code.Variable muleContext = evaluateAndTransform.param(ref(MuleContext.class), "muleContext");
        org.mule.devkit.model.code.Variable muleMessage = evaluateAndTransform.param(ref(MuleMessage.class), "muleMessage");
        org.mule.devkit.model.code.Variable expectedType = evaluateAndTransform.param(ref(java.lang.reflect.Type.class), "expectedType");
        org.mule.devkit.model.code.Variable expectedMimeType = evaluateAndTransform.param(ref(String.class), "expectedMimeType");
        org.mule.devkit.model.code.Variable source = evaluateAndTransform.param(ref(Object.class), "source");

        evaluateAndTransform.body()._if(Op.eq(source, ExpressionFactory._null()))._then()._return(source);

        org.mule.devkit.model.code.Variable target = evaluateAndTransform.body().decl(ref(Object.class), "target", ExpressionFactory._null());
        Conditional isList = evaluateAndTransform.body()._if(
                ExpressionFactory.invoke("isList").arg(source.invoke("getClass")));
        Conditional isExpectedList = isList._then()._if(
                ExpressionFactory.invoke("isList").arg(expectedType));
        org.mule.devkit.model.code.Variable newList = isExpectedList._then().decl(ref(List.class), "newList", ExpressionFactory._new(ref(ArrayList.class)));
        org.mule.devkit.model.code.Variable listParameterizedType = isExpectedList._then().decl(ref(java.lang.reflect.Type.class), "valueType",
                ExpressionFactory.cast(ref(ParameterizedType.class), expectedType).
                        invoke("getActualTypeArguments").component(ExpressionFactory.lit(0)));
        org.mule.devkit.model.code.Variable listIterator = isExpectedList._then().decl(ref(ListIterator.class), "iterator",
                ExpressionFactory.cast(ref(List.class), source).
                        invoke("listIterator"));

        Block whileHasNext = isExpectedList._then()._while(listIterator.invoke("hasNext")).body();
        org.mule.devkit.model.code.Variable subTarget = whileHasNext.decl(ref(Object.class), "subTarget", listIterator.invoke("next"));
        whileHasNext.add(newList.invoke("add").arg(
                ExpressionFactory.invoke("evaluateAndTransform").arg(muleContext).arg(muleMessage).arg(listParameterizedType).
                        arg(expectedMimeType).
                        arg(subTarget)
        ));
        isExpectedList._then().assign(target, newList);
        isExpectedList._else().assign(target, source);

        Conditional isMap = isList._elseif(
                ExpressionFactory.invoke("isMap").arg(source.invoke("getClass")));
        Conditional isExpectedMap = isMap._then()._if(
                ExpressionFactory.invoke("isMap").arg(expectedType));

        Block isExpectedMapBlock = isExpectedMap._then();
        org.mule.devkit.model.code.Variable keyType = isExpectedMapBlock.decl(ref(java.lang.reflect.Type.class), "keyType",
                ref(Object.class).dotclass());
        org.mule.devkit.model.code.Variable valueType = isExpectedMapBlock.decl(ref(java.lang.reflect.Type.class), "valueType",
                ref(Object.class).dotclass());

        Block isGenericMap = isExpectedMapBlock._if(Op._instanceof(expectedType, ref(ParameterizedType.class)))._then();

        isGenericMap.assign(keyType, ExpressionFactory.cast(ref(ParameterizedType.class), expectedType).
                invoke("getActualTypeArguments").component(ExpressionFactory.lit(0)));
        isGenericMap.assign(valueType, ExpressionFactory.cast(ref(ParameterizedType.class), expectedType).
                invoke("getActualTypeArguments").component(ExpressionFactory.lit(1)));

        org.mule.devkit.model.code.Variable map = isExpectedMapBlock.decl(ref(Map.class), "map", ExpressionFactory.cast(ref(Map.class), source));

        org.mule.devkit.model.code.Variable newMap = isExpectedMapBlock.decl(ref(Map.class), "newMap", ExpressionFactory._new(ref(HashMap.class)));
        ForEach forEach = isExpectedMapBlock.forEach(ref(Object.class), "entryObj", map.invoke("entrySet"));
        Block forEachBlock = forEach.body().block();
        org.mule.devkit.model.code.Variable entry = forEachBlock.decl(ref(Map.Entry.class), "entry", ExpressionFactory.cast(ref(Map.Entry.class), forEach.var()));
        org.mule.devkit.model.code.Variable newKey = forEachBlock.decl(ref(Object.class), "newKey", ExpressionFactory.invoke("evaluateAndTransform").arg(muleContext).arg(muleMessage).arg(keyType).arg(expectedMimeType).arg(entry.invoke("getKey")));
        org.mule.devkit.model.code.Variable newValue = forEachBlock.decl(ref(Object.class), "newValue", ExpressionFactory.invoke("evaluateAndTransform").arg(muleContext).arg(muleMessage).arg(valueType).arg(expectedMimeType).arg(entry.invoke("getValue")));
        forEachBlock.invoke(newMap, "put").arg(newKey).arg(newValue);

        isExpectedMapBlock.assign(target, newMap);

        isExpectedMap._else().assign(target, source);

        Block otherwise = isMap._else();
        otherwise.assign(target, ExpressionFactory.invoke("evaluate")
                .arg(ref(TemplateParser.class).staticInvoke("createMuleStyleParser").invoke("getStyle"))
                .arg(muleContext.invoke("getExpressionManager"))
                .arg(muleMessage).arg(source));


        Conditional shouldTransform = evaluateAndTransform.body()._if(Op.cand(
                Op.ne(target, ExpressionFactory._null()),
                Op.not(ExpressionFactory.invoke("isAssignableFrom").arg(expectedType).arg(target.invoke("getClass")))
        ));

        org.mule.devkit.model.code.Variable sourceDataType = shouldTransform._then().decl(ref(DataType.class), "sourceDataType",
                ref(DataTypeFactory.class).staticInvoke("create").arg(target.invoke("getClass")));
        org.mule.devkit.model.code.Variable targetDataType = shouldTransform._then().decl(ref(DataType.class), "targetDataType", ExpressionFactory._null());

        Conditional ifParameterizedType = shouldTransform._then()._if(Op._instanceof(expectedType, ref(ParameterizedType.class)));
        ifParameterizedType._then().assign(expectedType, ExpressionFactory.cast(ref(ParameterizedType.class), expectedType).invoke("getRawType"));

        Conditional ifExpectedMimeTypeNotNull = shouldTransform._then()._if(Op.ne(expectedMimeType, ExpressionFactory._null()));
        ifExpectedMimeTypeNotNull._then().assign(targetDataType, ref(DataTypeFactory.class).staticInvoke("create").arg(
                ExpressionFactory.cast(ref(Class.class), expectedType)).arg(expectedMimeType));

        ifExpectedMimeTypeNotNull._else().assign(targetDataType, ref(DataTypeFactory.class).staticInvoke("create").arg(
                ExpressionFactory.cast(ref(Class.class), expectedType)));

        org.mule.devkit.model.code.Variable transformer = shouldTransform._then().decl(ref(Transformer.class), "t",
                muleContext.invoke("getRegistry").invoke("lookupTransformer").arg(sourceDataType).arg(targetDataType));

        shouldTransform._then()._return(transformer.invoke("transform").arg(target));

        shouldTransform._else()._return(target);
    }

    private void generateTransformMethod(DefinedClass abstractExpressionEvaluatorClass) {
        org.mule.devkit.model.code.Method transform = abstractExpressionEvaluatorClass.method(Modifier.PROTECTED, ref(Object.class), "transform");
        transform._throws(ref(TransformerException.class));
        org.mule.devkit.model.code.Variable muleMessage = transform.param(ref(MuleMessage.class), "muleMessage");
        org.mule.devkit.model.code.Variable expectedType = transform.param(ref(java.lang.reflect.Type.class), "expectedType");
        org.mule.devkit.model.code.Variable source = transform.param(ref(Object.class), "source");

        transform.body()._if(Op.eq(source, ExpressionFactory._null()))._then()._return(source);

        org.mule.devkit.model.code.Variable target = transform.body().decl(ref(Object.class), "target", ExpressionFactory._null());
        Conditional isList = transform.body()._if(
                ExpressionFactory.invoke("isList").arg(source.invoke("getClass")));
        Conditional isExpectedList = isList._then()._if(
                ExpressionFactory.invoke("isList").arg(expectedType));
        org.mule.devkit.model.code.Variable newList = isExpectedList._then().decl(ref(List.class), "newList", ExpressionFactory._new(ref(ArrayList.class)));
        org.mule.devkit.model.code.Variable listParameterizedType = isExpectedList._then().decl(ref(java.lang.reflect.Type.class), "valueType",
                ExpressionFactory.cast(ref(ParameterizedType.class), expectedType).
                        invoke("getActualTypeArguments").component(ExpressionFactory.lit(0)));
        org.mule.devkit.model.code.Variable listIterator = isExpectedList._then().decl(ref(ListIterator.class), "iterator",
                ExpressionFactory.cast(ref(List.class), source).
                        invoke("listIterator"));

        Block whileHasNext = isExpectedList._then()._while(listIterator.invoke("hasNext")).body();
        org.mule.devkit.model.code.Variable subTarget = whileHasNext.decl(ref(Object.class), "subTarget", listIterator.invoke("next"));
        whileHasNext.add(newList.invoke("add").arg(
                ExpressionFactory.invoke("transform").arg(muleMessage).arg(listParameterizedType).
                        arg(subTarget)
        ));
        isExpectedList._then().assign(target, newList);
        isExpectedList._else().assign(target, source);

        Conditional isMap = isList._elseif(
                ExpressionFactory.invoke("isMap").arg(source.invoke("getClass")));
        Conditional isExpectedMap = isMap._then()._if(
                ExpressionFactory.invoke("isMap").arg(expectedType));

        Block isExpectedMapBlock = isExpectedMap._then();
        org.mule.devkit.model.code.Variable keyType = isExpectedMapBlock.decl(ref(java.lang.reflect.Type.class), "keyType",
                ref(Object.class).dotclass());
        org.mule.devkit.model.code.Variable valueType = isExpectedMapBlock.decl(ref(java.lang.reflect.Type.class), "valueType",
                ref(Object.class).dotclass());

        Block isGenericMap = isExpectedMapBlock._if(Op._instanceof(expectedType, ref(ParameterizedType.class)))._then();

        isGenericMap.assign(keyType, ExpressionFactory.cast(ref(ParameterizedType.class), expectedType).
                invoke("getActualTypeArguments").component(ExpressionFactory.lit(0)));
        isGenericMap.assign(valueType, ExpressionFactory.cast(ref(ParameterizedType.class), expectedType).
                invoke("getActualTypeArguments").component(ExpressionFactory.lit(1)));

        org.mule.devkit.model.code.Variable map = isExpectedMapBlock.decl(ref(Map.class), "map", ExpressionFactory.cast(ref(Map.class), source));

        org.mule.devkit.model.code.Variable newMap = isExpectedMapBlock.decl(ref(Map.class), "newMap", ExpressionFactory._new(ref(HashMap.class)));
        ForEach forEach = isExpectedMapBlock.forEach(ref(Object.class), "entryObj", map.invoke("entrySet"));
        Block forEachBlock = forEach.body().block();
        org.mule.devkit.model.code.Variable entry = forEachBlock.decl(ref(Map.Entry.class), "entry", ExpressionFactory.cast(ref(Map.Entry.class), forEach.var()));
        org.mule.devkit.model.code.Variable newKey = forEachBlock.decl(ref(Object.class), "newKey", ExpressionFactory.invoke("transform").arg(muleMessage).arg(keyType).arg(entry.invoke("getKey")));
        org.mule.devkit.model.code.Variable newValue = forEachBlock.decl(ref(Object.class), "newValue", ExpressionFactory.invoke("transform").arg(muleMessage).arg(valueType).arg(entry.invoke("getValue")));
        forEachBlock.invoke(newMap, "put").arg(newKey).arg(newValue);

        isExpectedMapBlock.assign(target, newMap);

        isExpectedMap._else().assign(target, source);

        isMap._else().assign(target, source);

        Conditional shouldTransform = transform.body()._if(Op.cand(
                Op.ne(target, ExpressionFactory._null()),
                Op.not(ExpressionFactory.invoke("isAssignableFrom").arg(expectedType).arg(target.invoke("getClass")))
        ));

        org.mule.devkit.model.code.Variable sourceDataType = shouldTransform._then().decl(ref(DataType.class), "sourceDataType",
                ref(DataTypeFactory.class).staticInvoke("create").arg(target.invoke("getClass")));

        Conditional ifParameterizedType = shouldTransform._then()._if(Op._instanceof(expectedType, ref(ParameterizedType.class)));
        ifParameterizedType._then().assign(expectedType, ExpressionFactory.cast(ref(ParameterizedType.class), expectedType).invoke("getRawType"));

        org.mule.devkit.model.code.Variable targetDataType = shouldTransform._then().decl(ref(DataType.class), "targetDataType",
                ref(DataTypeFactory.class).staticInvoke("create").arg(
                        ExpressionFactory.cast(ref(Class.class), expectedType)));

        org.mule.devkit.model.code.Variable transformer = shouldTransform._then().decl(ref(Transformer.class), "t",
                muleMessage.invoke("getMuleContext").invoke("getRegistry").invoke("lookupTransformer").arg(sourceDataType).arg(targetDataType));

        shouldTransform._then()._return(transformer.invoke("transform").arg(target));

        shouldTransform._else()._return(target);
    }

}
