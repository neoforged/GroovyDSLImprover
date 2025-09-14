/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

//file:noinspection UnnecessaryQualifiedReference
package net.neoforged.gdi.transformer

import groovy.transform.*
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import groovy.transform.stc.SimpleType
import groovyjarjarasm.asm.Opcodes
import net.neoforged.gdi.annotations.DSLProperty
import net.neoforged.gdi.markers.IsConfigurable
import net.neoforged.gdi.transformer.property.CollectionPropertyHandler
import net.neoforged.gdi.transformer.property.DefaultPropertyHandler
import net.neoforged.gdi.transformer.property.MapPropertyHandler
import net.neoforged.gdi.transformer.property.NamedDomainObjectContainerHandler
import net.neoforged.gdi.transformer.property.PropertyHandler
import net.neoforged.gdi.transformer.property.files.DirectoryPropertyHandler
import net.neoforged.gdi.transformer.property.files.FileCollectionPropertyHandler
import net.neoforged.gdi.transformer.property.files.FilePropertyHandler
import org.apache.groovy.util.Maps
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.ast.tools.GenericsUtils
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.TransformWithPriority
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty
import org.gradle.util.Configurable

import javax.annotation.Nullable
import java.util.stream.Collectors
import java.util.stream.Stream

import static org.codehaus.groovy.ast.tools.GeneralUtils.*

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class DSLPropertyTransformer extends AbstractASTTransformation implements TransformWithPriority {
    private static final ClassNode DELEGATES_TO_TYPE = ClassHelper.make(DelegatesTo)
    private static final ClassNode CLOSURE_PARAMS_TYPE = ClassHelper.make(ClosureParams)
    public static final ClassNode CONFIGURABLE_TYPE = ClassHelper.make(Configurable)
    public static final ClassNode IS_CONFIGURABLE_TYPE = ClassHelper.make(IsConfigurable)

    public static final ClassNode RAW_GENERIC_CLOSURE = GenericsUtils.makeClassSafe(Closure)

    /**
     * A list of property handlers which will be called in the order they're defined here.
     */
    private static final List<PropertyHandler> HANDLERS = [
            new MapPropertyHandler(),
            new CollectionPropertyHandler(ListProperty, SetProperty),
            new FileCollectionPropertyHandler(),
            new FilePropertyHandler(),
            new DirectoryPropertyHandler(),
            new NamedDomainObjectContainerHandler(),
            new DefaultPropertyHandler(), // This needs to be last, as fallback
    ] as List<PropertyHandler>

    private static final Set<ClassNode> NON_CONFIGURABLE_TYPES = new HashSet<>([
            ClassHelper.STRING_TYPE,
            ClassHelper.int_TYPE, ClassHelper.Integer_TYPE,
            ClassHelper.byte_TYPE, ClassHelper.Byte_TYPE,
            ClassHelper.short_TYPE, ClassHelper.Short_TYPE,
            ClassHelper.long_TYPE, ClassHelper.Long_TYPE,
            ClassHelper.char_TYPE, ClassHelper.Character_TYPE,
            ClassHelper.boolean_TYPE, ClassHelper.Boolean_TYPE,
            ClassHelper.float_TYPE, ClassHelper.Float_TYPE,
            ClassHelper.double_TYPE, ClassHelper.Double_TYPE,
    ])

    public static final Map<ClassNode, ClassNode> WRAPPER_TO_PRIMITIVE = Maps.of(
            ClassHelper.Integer_TYPE, ClassHelper.int_TYPE,
            ClassHelper.Byte_TYPE, ClassHelper.byte_TYPE,
            ClassHelper.Short_TYPE, ClassHelper.short_TYPE,
            ClassHelper.Long_TYPE, ClassHelper.long_TYPE,
            ClassHelper.Character_TYPE, ClassHelper.char_TYPE,
            ClassHelper.Boolean_TYPE, ClassHelper.boolean_TYPE,
            ClassHelper.Float_TYPE, ClassHelper.float_TYPE,
            ClassHelper.Double_TYPE, ClassHelper.double_TYPE
    )

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        this.init(nodes, source)
        final methodNodeAST = nodes[1]
        if (methodNodeAST !instanceof MethodNode) throw new IllegalArgumentException('Unexpected non-method node!')
        final methodNode = (MethodNode) methodNodeAST
        if (!methodNode.public) throw new IllegalArgumentException('Methods annotated with DSLProperty can only be abstract and public!')
        visitMethod(methodNode, nodes[0] as AnnotationNode)
    }

    private void visitMethod(MethodNode method, AnnotationNode annotation) {
        final propertyName = getPropertyName(method, annotation)

        final List<MethodNode> methods = []
        final Utils utils = new Utils() {
            @Override
            List<MethodNode> getMethods() {
                return methods
            }

            @Override
            boolean getBoolean(AnnotationNode an, String name, boolean defaultValue = true) {
                final value = getMemberValue(an, name)
                if (value === null) return defaultValue
                return (boolean)value
            }

            @Override
            void addError(String message, ASTNode node) {
                DSLPropertyTransformer.this.addError(message, node)
            }
        }
        HANDLERS.find { it.handle(method, annotation, propertyName, utils) }
        methods.each(method.declaringClass.&addMethod)
    }

    private static String getPropertyName(MethodNode methodNode, AnnotationNode annotation) {
        return getMemberStringValue(annotation, 'propertyName', methodNode.name.substring('get'.size()).uncapitalize())
    }

    static final AnnotationNode GENERATED_ANNOTATION = new AnnotationNode(ClassHelper.make(Generated))

    @Override
    int priority() {
        return 1
    }

    @CompileStatic
    abstract class Utils {
        abstract List<MethodNode> getMethods()

        @NamedVariant
        MethodNode createAndAddMethod(@NamedParam(required = true) final String methodName,
                                      @NamedParam final int modifiers = Opcodes.ACC_PUBLIC,
                                      @NamedParam final ClassNode returnType = ClassHelper.VOID_TYPE,
                                      @NamedParam final List<Parameter> parameters = new ArrayList<>(),
                                      @NamedParam final ClassNode[] exceptions = ClassNode.EMPTY_ARRAY,
                                      @NamedParam final List<AnnotationNode> annotations = [GENERATED_ANNOTATION],
                                      @NamedParam Statement code = new BlockStatement(),
                                      @NamedParam final List<? extends Expression> codeExpr = null,
                                      @NamedParam final Closure<List<OverloadDelegationStrategy>> delegationStrategies = { [] as List<OverloadDelegationStrategy> }) {
            if (codeExpr !== null) code = block(new VariableScope(), codeExpr.stream().map { stmt(it) }.toArray { new Statement[it] })

            final MethodNode method = new MethodNode(methodName, modifiers, returnType, parameters.stream().toArray { new Parameter[it] }, exceptions, code)
            method.addAnnotations(annotations)
            method.addAnnotation(new AnnotationNode(ClassHelper.make(CompileStatic)))

            getMethods().add(method)

            delegationStrategies.call().each { strategy ->
                final otherParamName = method.parameters[strategy.paramIndex].name

                this.createAndAddMethod(
                        methodName: method.name,
                        returnType: method.returnType,
                        parameters: Stream.of(method.parameters).filter { it.name !== otherParamName }.collect(Collectors.<Parameter>toList()),
                        code: GeneralUtils.stmt(GeneralUtils.callThisX(method.name, GeneralUtils.args(
                                Stream.of(method.parameters).map {
                                    if (it.name == otherParamName) return strategy.overload
                                    return GeneralUtils.varX(it)
                                }.collect(Collectors.<Expression>toList())
                        )))
                )
            }

            return method
        }

        MethodNode factory(ClassNode expectedType, AnnotationNode annotation, String propertyName) {
            final fac = annotation.members.get('factory')
            if (fac !== null) return this.createAndAddMethod(
                    methodName: "_default${propertyName.capitalize()}",
                    modifiers: Opcodes.ACC_PUBLIC,
                    code: GeneralUtils.returnS(GeneralUtils.callX(annotation.members.get('factory'), 'call')),
                    returnType: expectedType
            )
            return null
        }

        static Parameter closureParam(ClassNode type, String name = 'closure') {
            new Parameter(
                    RAW_GENERIC_CLOSURE,
                    name
            ).tap {
                if (type.isGenericsPlaceHolder()) {
                    it.addAnnotation(new AnnotationNode(CLOSURE_PARAMS_TYPE).tap {
                        it.addMember('value', GeneralUtils.classX(FromString))
                        it.addMember('options', GeneralUtils.constX(type.unresolvedName))
                    })
                } else {
                    final String asString = typeToString(type)
                    if (type.usingGenerics) {
                        it.addAnnotation(new AnnotationNode(DELEGATES_TO_TYPE).tap {
                            it.addMember('type', GeneralUtils.constX(asString))
                            it.addMember('strategy', GeneralUtils.constX(Closure.DELEGATE_FIRST))
                        })
                        it.addAnnotation(new AnnotationNode(CLOSURE_PARAMS_TYPE).tap {
                            it.addMember('value', GeneralUtils.classX(FromString))
                            it.addMember('options', GeneralUtils.constX(asString))
                        })
                    } else {
                        it.addAnnotation(new AnnotationNode(DELEGATES_TO_TYPE).tap {
                            it.addMember('value', GeneralUtils.classX(type))
                            it.addMember('strategy', GeneralUtils.constX(Closure.DELEGATE_FIRST))
                        })
                        it.addAnnotation(new AnnotationNode(CLOSURE_PARAMS_TYPE).tap {
                            it.addMember('value', GeneralUtils.classX(SimpleType))
                            it.addMember('options', GeneralUtils.constX(asString))
                        })
                    }
                }
            }
        }

        List<? extends Expression> delegateAndCall(VariableExpression closure, Expression delegate) {
            return [
                    GeneralUtils.callX(closure, 'setDelegate', GeneralUtils.args(delegate)),
                    GeneralUtils.callX(closure, 'setResolveStrategy', GeneralUtils.constX(Closure.DELEGATE_FIRST)),
                    GeneralUtils.callX(closure, 'call', GeneralUtils.args(delegate))
            ]
        }

        void visitPropertyType(ClassNode type, AnnotationNode annotation) {
            if (annotation.members.containsKey('isConfigurable')) return
            if (
                    type in NON_CONFIGURABLE_TYPES ||
                            (
                                    !GeneralUtils.isOrImplements(type, CONFIGURABLE_TYPE) &&
                                            !GeneralUtils.isOrImplements(type, IS_CONFIGURABLE_TYPE)
                            )
            ) {
                annotation.addMember('isConfigurable', GeneralUtils.constX(false, true))
            }
        }

        String getSingularPropertyName(String plural, AnnotationNode annotation) {
            if (annotation.members.containsKey('singularName')) return getMemberStringValue(annotation, 'singularName')
            return Unpluralizer.unpluralize(plural)
        }

        abstract boolean getBoolean(AnnotationNode annotation, String name, boolean defaultValue = true)
        abstract void addError(String message, ASTNode node)
    }

    @CompileStatic
    @TupleConstructor
    static final class OverloadDelegationStrategy {
        final int paramIndex
        final Expression overload
    }

    @CompileDynamic
    private static String typeToString(ClassNode type) {
        if (type.usingGenerics) {
            return type.name.replace('$', '.') + '<' + Arrays.stream(type.genericsTypes)
                .map { typeToString(it.type) }.collect(Collectors.joining(',')) + '>'
        } else {
            return type.name.replace('$', '.')
        }
    }
}

@CompileStatic
interface PropertyQuery {
    PropertyQuery PROPERTY = new PropertyQuery() {
        @Override
        Expression getter(MethodNode getterMethod) {
            return callX(callThisX(getterMethod.name), 'getOrNull')
        }

        @Override
        Expression setter(MethodNode getterMethod, Expression args) {
            return callX(callThisX(getterMethod.name), 'set', args)
        }

        @Override
        Expression getOrElse(MethodNode node, Expression orElse) {
            return callX(callThisX(node.name), 'getOrElse', orElse)
        }
    }

    PropertyQuery GETTER = new PropertyQuery() {
        @Override
        Expression getter(MethodNode getterMethod) {
            return callThisX(getterMethod.name)
        }

        @Override
        Expression setter(MethodNode node, Expression args) {
            return null
        }

        @Override
        Expression getOrElse(MethodNode node, Expression orElse) {
            final getter = getter(node)
            return ternaryX(isNullX(getter), orElse, getter)
        }
    }

    Expression getter(MethodNode getterMethod)

    @Nullable
    Expression setter(MethodNode node, Expression args)

    @Nullable
    Expression getOrElse(MethodNode node, Expression orElse)
}