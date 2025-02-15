/*
 * Copyright 2024 Jaroslav Bosak
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/license/MIT
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tendril.codegen.classes;

import tendril.codegen.field.JVisibleType;
import tendril.codegen.field.VisibileTypeBuilder;
import tendril.codegen.field.type.Type;

/**
 * Builder which adds the created element as a nested element to the provided {@link ClassBuilder}. If a {@link ClassBuilder} is provided, then {@code build()} is disabled as {@code finish()} must be
 * used instead. Vice versa if no {@link ClassBuilder} is provided, {@code finish()} is disabled and {@code build} must be used.
 * 
 * @param <DATA_TYPE> extends {@link Type} indicating the type of data that is represented
 * @param <ELEMENT>   extends {@link JVisibleType} indicating what specific element the builder creates
 * @param <BUILDER>   extends {@link NestedClassElementBuilder} indicating what type of builder is employed
 */
public abstract class NestedClassElementBuilder<DATA_TYPE extends Type, ELEMENT extends JVisibleType<DATA_TYPE>, BUILDER extends NestedClassElementBuilder<DATA_TYPE, ELEMENT, BUILDER>>
        extends VisibileTypeBuilder<DATA_TYPE, ELEMENT, BUILDER> {
    /** The class containing the method */
    protected final ClassBuilder classBuilder;

    /**
     * CTOR
     * 
     * @param classBuilder {@link ClassBuilder} building the class to which the nested element belongs
     * @param name         {@link String} the name of the element
     */
    public NestedClassElementBuilder(ClassBuilder classBuilder, String name) {
        super(name);
        this.classBuilder = classBuilder;
    }

    /**
     * @see tendril.codegen.BaseBuilder#build()
     */
    @Override
    public ELEMENT build() {
        if (classBuilder != null)
            throw new IllegalStateException("Cannot be built directly - ClassBuilder for encompassing class provided. Use finish() instead to build the element and add it to the encompassing class");

        return super.build();
    }

    /**
     * Finish specifying the details of the method, build it, and apply it to the target class
     * 
     * @return {@link ClassBuilder} to which the method is applied
     */
    public ClassBuilder finish() {
        if (classBuilder == null)
            throw new IllegalStateException("No ClassBuilder for encompassing class provided. Use build() to build the element without adding it to an encompassing class");

        addToClass(classBuilder, super.build());
        return classBuilder;
    }

    /**
     * Add the created element to the {@link ClassBuilder} in whatever manner is appropriate
     * 
     * @param classBuilder {@link ClassBuilder} to which to add the element
     * @param toAdd        ELEMENT
     */
    protected abstract void addToClass(ClassBuilder classBuilder, ELEMENT toAdd);
}
