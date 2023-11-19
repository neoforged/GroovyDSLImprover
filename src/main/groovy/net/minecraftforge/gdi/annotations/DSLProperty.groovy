/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.gdi.annotations

import net.minecraftforge.gdi.transformer.DSLPropertyTransformer
import org.codehaus.groovy.transform.GroovyASTTransformationClass
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.*
import org.gradle.api.provider.*

import java.lang.annotation.*

/**
 * Annotate an <strong>abstract method of a groovy interface</strong> in order to generate DSL methods for the property. <br>
 * Methods with {@link Action} or {@link Closure} parameters will be generated only if the property
 * {@link DSLProperty#isConfigurable() is configurable} (which needs to be explicitly declared) or if its type implements {@link org.gradle.util.Configurable}. <br>
 * This annotation will generate the following methods based on the return type of the method:
 * <table>
 *     <tr>
 *          <th>Property Type</th>
 *          <th>Generated Methods</th>
 *     </tr>
 *     <tr>
 *          <th>{@link Property}</th>
 *          <th>
 *              <ul>
 *                  <li>{@code $propertyName(T)}</li>
 *                  <li>{@code $propertyName(Action<T>)} - the action will be executed on the provider's value, and if it doesn't have a value, one will be created using the {@linkplain DSLProperty#factory() factory} </li>
 *                  <li>{@code $propertyName(@DelegatesTo(T.class) Closure<T>)} - same behaviour as the action</li>
 *                  <li>{@code $propertyName(T, Action<T>)} - the action will be executed on the given value, which will be then {@linkplain org.gradle.api.provider.Property#set(java.lang.Object) set} as the provider's value</li>
 *                  <li>{@code $propertyName(T, @DelegatesTo(T.class) Closure<T>)} - same behaviour as the action</li>
 *              </ul>
 *          </th>
 *     </tr>
 *     <tr>
 *          <th>any {@link HasMultipleValues} ({@link ListProperty}, {@link SetProperty})</th>
 *          <th>
 *              <ul>
 *                  <li>{@code $singularName(T)} - the given value will be added to the list</li>
 *                  <li>{@code $propertyName(T...)} - the given values will be added to the list</li>
 *                  <li>{@code $propertyName(Iterable<? extends T>)} - the given values will be added to the list</li>
 *                  <li>{@code $singularName(T, Action<T>)} - the action will be executed on the given value, which will be then added to the list</li>
 *                  <li>{@code $singularName(T, @DelegatesTo(T.class) Closure<T>)} - same behaviour as the action</li>
 *              </ul>
 *              The methods below are generated <i>only</i> if a {@linkplain DSLProperty#factory() factory} is supplied:
 *              <ul>
 *                  <li>{@code $singularName(Action<T>)} - the action will be executed on an object created with the factory, which will be then added to the list</li>
 *                  <li>{@code $singularName(@DelegatesTo(T.class) Closure<T>)} - same behaviour as the action</li>
 *              </ul>
 *          </th>
 *     </tr>
 *     <tr>
 *          <th>{@link MapProperty}</th>
 *          <th>
 *              <ul>
 *                  <li>{@code $singularName(K, V)} - the given value will be added to the map, at the given key</li>
 *                  <li>{@code $singularName(K, V, Action<V>)} - the action will be executed on the given value, which will be then added to the map at the given key</li>
 *                  <li>{@code $singularName(K, V, @DelegatesTo(V.class) Closure<V>)} - same behaviour as the action</li>
 *                  <li>{@code $propertyName(Map<K, V>)} - calls {@link MapProperty#putAll(java.util.Map)}</li>
 *                  <li>{@code $propertyName(CommonAncestor...)} - computes a key-value mapping from the vararg array (expecting an array such as [k1, v1, k2, v2, ...], with an even number of elements, and of the correct key and value types), and calls {@link MapProperty#put(java.lang.Object, java.lang.Object)} with each pair; the array type is determined by the first common type in the inheritance tree of both the key and the value, or {@link Object} otherwise</li>
 *              </ul>
 *              The methods below are generated <i>only</i> if a {@linkplain DSLProperty#factory() factory} is supplied:
 *              <ul>
 *                  <li>{@code $singularName(K, Action<V>)} - the action will be executed on an object created with the factory, which will be then added to the map at the given key</li>
 *                  <li>{@code $singularName(K, @DelegatesTo(V.class) Closure<V>)} - same behaviour as the action</li>
 *              </ul>
 *          </th>
 *     </tr>
 *     <tr>
 *          <th>{@link NamedDomainObjectContainer}</th>
 *          <th>
 *              <ul>
 *                  <li>{@code $singularName(String, Action<T>)} - calls {@link NamedDomainObjectContainer#register(java.lang.String, Action)}</li>
 *                  <li>{@code $singularName(K, @DelegatesTo(V.class) Closure<V>)} - calls {@link NamedDomainObjectContainer#register(java.lang.String, Action)}</li>
 *                  <li>{@code $propertyName(@DelegatesTo(NamedDomainObjectContainer.class) Closure<V>)} - calls {@link NamedDomainObjectContainer#register(java.lang.String, Action)}</li>
 *              </ul>
 *          </th>
 *     </tr>
 *     <br>
 *     <tr>
 *          <th>{@link DirectoryProperty} - requires a {@link ProjectGetter}</th>
 *          <th>
 *              <ul>
 *                  <li>{@code $propertyName(Directory)} - calls {@link DirectoryProperty#value(Directory)}</li>
 *                  <li>{@code $propertyName(File)} - calls {@link DirectoryProperty#set(java.io.File)}</li>
 *                  <li>{@code $propertyName(Object)} - calls {@link DirectoryProperty#set(java.io.File)} with a file got using {@link Project#file(java.lang.Object)}</li>
 *              </ul>
 *          </th>
 *     </tr>
 *     <tr>
 *          <th>{@link RegularFileProperty} - requires a {@link ProjectGetter}</th>
 *          <th>
 *              <ul>
 *                  <li>{@code $propertyName(RegularFile)} - calls {@link RegularFileProperty#value(RegularFile)}</li>
 *                  <li>{@code $propertyName(File)} - calls {@link RegularFileProperty#set(java.io.File)}</li>
 *                  <li>{@code $propertyName(Object)} - calls {@link RegularFileProperty#set(java.io.File)} with a file got using {@link Project#file(java.lang.Object)}</li>
 *              </ul>
 *          </th>
 *     </tr>
 *     <tr>
 *          <th>{@link ConfigurableFileCollection}</th>
 *          <th>
 *              <ul>
 *                  <li>{@code $singularName(Object)} - calls {@link ConfigurableFileCollection#from(java.lang.Object ...)}</li>
 *                  <li>{@code $propertyName(Object...)} - calls {@link ConfigurableFileCollection#from(java.lang.Object ...)}</li>
 *              </ul>
 *          </th>
 *     </tr>
 * </table>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@GroovyASTTransformationClass(classes = DSLPropertyTransformer)
@interface DSLProperty {
    /**
     * The name of the property. If not specified this will be the method name without
     * {@code get} and {@link org.codehaus.groovy.runtime.StringGroovyMethods#uncapitalize(java.lang.CharSequence) uncapitalized}.
     */
    String propertyName() default ''

    /**
     * The singular name of the property, used by map or collection methods adding a single element. <br>
     * If not specified it will be computed from the {@link #propertyName()} using {@link net.minecraftforge.gdi.transformer.Unpluralizer#unpluralize(java.lang.String)}.
     */
    String singularName() default ''

    /**
     * The factory used to create instances of the property type, when applicable.
     */
    Class<Closure> factory() default Closure.class

    /**
     * If this property is configurable. <br>
     * <strong>While the default value is {@code true}, it will only be considered when it is explicitly defined.</strong>
     */
    boolean isConfigurable() default true
}
