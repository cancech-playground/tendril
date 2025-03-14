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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.Generated;

import tendril.codegen.DefinitionException;
import tendril.codegen.Utilities;
import tendril.codegen.annotation.JAnnotationFactory;
import tendril.codegen.classes.method.JConstructor;
import tendril.codegen.classes.method.JMethod;
import tendril.codegen.field.JField;
import tendril.codegen.field.VisibileTypeBuilder;
import tendril.codegen.field.type.ClassType;
import tendril.codegen.field.type.Type;
import tendril.codegen.field.type.VoidType;
import tendril.codegen.field.value.JValueFactory;
import tendril.codegen.generics.GenericType;

/**
 * The common logic/capability to power builders which create class definitions of different types.
 */
public abstract class ClassBuilder extends VisibileTypeBuilder<ClassType, JClass, ClassBuilder> {

    /** The methods that are to be applied to the class */
    protected final List<JMethod<?>> methods = new ArrayList<>();
    /** The fields that are to be applied to the class */
    protected final List<JField<?>> fields = new ArrayList<>();
    /** The constructors for initializing the class */
    protected final List<JConstructor> ctors = new ArrayList<>();
    /** The representation of the explicit parent class */
    protected JClass parent = null;
    /** The representation of the interfaces the class implements */
    protected List<JClass> interfaces = new ArrayList<>();

    /**
     * Get the class builder for creating concrete classes
     * 
     * @param type {@link Class} of the class to build
     * @return {@link ClassBuilder}
     */
    public static ClassBuilder forConcreteClass(Class<?> type) {
        return forConcreteClass(new ClassType(type));
    }

    /**
     * Get the class builder for creating concrete classes
     * 
     * @param type {@link ClassType} of the class to build
     * @return {@link ClassBuilder}
     */
    public static ClassBuilder forConcreteClass(ClassType type) {
        return new ConcreteClassBuilder(type);
    }

    /**
     * Get the class builder for creating abstract classes
     * 
     * @param type {@link Class} of the class to build
     * @return {@link ClassBuilder}
     */
    public static ClassBuilder forAbstractClass(Class<?> type) {
        return forAbstractClass(new ClassType(type));
    }

    /**
     * Get the class builder for creating abstract classes
     * 
     * @param type {@link ClassType} of the class to build
     * @return {@link ClassBuilder}
     */
    public static ClassBuilder forAbstractClass(ClassType type) {
        return new AbstractClassBuilder(type);
    }

    /**
     * Get the class builder for creating interfaces
     * 
     * @param type {@link ClassType} of the interface to build
     * @return {@link ClassBuilder}
     */
    public static ClassBuilder forInterface(ClassType type) {
        return new InterfaceBuilder(type);
    }

    /**
     * Get the class builder for creating interfaces
     * 
     * @param type {@link Class} of the interface to build
     * @return {@link ClassBuilder}
     */
    public static ClassBuilder forInterface(Class<?> type) {
        return forInterface(new ClassType(type));
    }

    /**
     * Get the class builder for creating annotations
     * 
     * @param type {@link ClassType} of the annotation to build
     * @return {@link ClassBuilder}
     */
    public static ClassBuilder forAnnotation(ClassType type) {
        return new AnnotationBuilder(type);
    }

    /**
     * Get the class builder for creating annotations
     * 
     * @param type {@link Class} of the annotation to build
     * @return {@link ClassBuilder}
     */
    public static ClassBuilder forAnnotation(Class<?> type) {
        return forAnnotation(new ClassType(type));
    }

    /**
     * Get the class builder for creating enums
     * 
     * @param type {@link ClassType} of the enum to build
     * @return {@link ClassBuilder}
     */
    public static ClassBuilder forEnum(ClassType type) {
        return new EnumClassBuilder(type);
    }

    /**
     * Get the class builder for creating enums
     * 
     * @param type {@link Class} of the enum to build
     * @return {@link ClassBuilder}
     */
    public static ClassBuilder forEnum(Class<?> type) {
        return forEnum(new ClassType(type));
    }

    /**
     * CTOR
     * 
     * @param type {@link ClassType} which has the basic class description included
     */
    ClassBuilder(ClassType type) {
        super(type.getSimpleName());
        setType(type);
        addAnnotation(JAnnotationFactory.create(Generated.class, Map.of("value", JValueFactory.create("tendril"), "date", JValueFactory.create(Utilities.iso8061TimeStamp()))));

        // Add any generics that may have been applied to the type
        for (GenericType g : type.getGenerics())
            addGeneric(g);
    }

    /**
     * @see tendril.codegen.field.VisibileTypeBuilder#applyDetails(tendril.codegen.field.JVisibleType)
     */
    @Override
    protected JClass applyDetails(JClass element) {
        element.setParentClass(parent);
        element.setParentInterfaces(interfaces);
        fields.forEach(f -> element.addField(f));
        ctors.forEach(c -> element.addConstructor(c));
        methods.forEach(m -> element.addMethod(m));
        return super.applyDetails(element);
    }

    /**
     * Specify the parent class that the defined class is to extend
     * 
     * @param parent {@link JClass} representing the desired parent class
     * @return {@link ClassBuilder}
     */
    public ClassBuilder extendsClass(JClass parent) {
        this.parent = parent;
        return this;
    }

    /**
     * Add an interface that the defined class is to implement
     * 
     * @param iface {@link JClass} representing the desired interface
     * @return {@link ClassBuilder}
     */
    public ClassBuilder implementsInterface(JClass iface) {
        this.interfaces.add(iface);
        return this;
    }

    /**
     * Create a method builder through which to add a new void method to the class.
     * 
     * @param name {@link String} the name of the method
     * @return {@link MethodBuilder} for creating a the method
     */
    public MethodBuilder<VoidType> buildMethod(String name) {
        return buildMethod(VoidType.INSTANCE, name);
    }

    /**
     * Create a method builder through which to add a new method which returns a Class object.
     * 
     * @param returnType {@link Class} of the object that is to be returned
     * @param name       {@link String} the name of the method
     * @return {@link MethodBuilder} for creating the method
     */
    public MethodBuilder<ClassType> buildMethod(Class<?> returnType, String name) {
        return buildMethod(new ClassType(returnType), name);
    }

    /**
     * Helper which triggers the creation of the {@link MethodBuilder} and immediately applies the specified return type to it.
     * 
     * @param <RETURN_TYPE> extending {@link Type} indicating what the nature of the return of the method is
     * @param returnType    RETURN_TYPE what specifically the method is to return
     * @param name          {@link String} the name of the method
     * @return {@link MethodBuilder} for creating the method
     */
    public <RETURN_TYPE extends Type> MethodBuilder<RETURN_TYPE> buildMethod(RETURN_TYPE returnType, String name) {
        MethodBuilder<RETURN_TYPE> builder = createMethodBuilder(name);
        builder.setType(returnType);
        return builder;
    }

    /**
     * Creates the {@link MethodBuilder} instance that is to be used for the purpose of defining methods
     * 
     * @param <RETURN_TYPE> extends {@link Type} indicating what type is to be returned by the method
     * @param name          {@link String} the name of the method to build
     * @return {@link MethodBuilder} for creating the method
     */
    protected abstract <RETURN_TYPE extends Type> MethodBuilder<RETURN_TYPE> createMethodBuilder(String name);

    /**
     * To be called by the created {@link MethodBuilder} when it {@code finish}es creating the method.
     * 
     * @param method {@link JMethod} that is to be added to the class
     */
    void add(JMethod<?> method) {
        methods.add(method);
    }

    /**
     * Create a field builder through which to add a new class field
     * 
     * @param type {@link Class} of the object that is to be contained in the field
     * @param name {@link String} the name of the field
     * @return {@link FieldBuilder} for creating the field
     */
    public FieldBuilder<ClassType> buildField(Class<?> type, String name) {
        return buildField(new ClassType(type), name);
    }

    /**
     * Helper which triggers the creation of the {@link FieldBuilder} and immediately applies the specified type to it.
     * 
     * @param <TYPE> extending {@link Type} indicating type of data is stored in the field
     * @param type   TYPE what specifically the field contains
     * @param name   {@link String} the name of the field
     * @return {@link FieldBuilder} which will create the field
     */
    public <TYPE extends Type> FieldBuilder<TYPE> buildField(TYPE type, String name) {
        FieldBuilder<TYPE> builder = new FieldBuilder<>(this, name);
        builder.setType(type);
        return builder;
    }

    /**
     * To be called by the created {@link MethodBuilder} when it {@code finish}es creating the method.
     * 
     * @param method {@link JMethod} that is to be added to the class
     */
    void add(JField<?> method) {
        fields.add(method);
    }

    /**
     * Create a constructor builder through which constructors can be added to the class
     * 
     * @return {@link ConstructorBuilder} for the class
     */
    public ConstructorBuilder buildConstructor() {
        return new ConstructorBuilder(this, type);
    }

    /**
     * To be called by the created {@link ConstructorBuilder} when it {@code finish}es creating the constructor.
     * 
     * @param ctor {@link JConstructor} that is to be added to the class
     */
    void add(JConstructor ctor) {
        ctors.add(ctor);
    }

    /**
     * Allows for the creation of an enumerated item within an {@link Enum} class. Note that this will only work when defining an enumeration, with a {@link DefinitionException} thrown for all other
     * class types.
     * 
     * @param name {@link String} the name of the enumeration entry to create
     * @return {@link EnumerationBuilder} through which the entry is to be built
     */
    public EnumerationBuilder buildEnumeration(String name) {
        throw new DefinitionException(type, "Only enums can have enumerations");
    }

    /**
     * To be called by the {@link EnumerationBuilder} when the enumerated entry has been created and it is to be added to the enum class. Note that this will only work when defining an enumeration,
     * with a {@link DefinitionException} thrown for all other class types.
     * 
     * @param enumEntry {@link EnumerationEntry} that is to be added
     */
    void add(EnumerationEntry enumEntry) {
        throw new DefinitionException(type, "Only enums can have enumerations");
    }
}
