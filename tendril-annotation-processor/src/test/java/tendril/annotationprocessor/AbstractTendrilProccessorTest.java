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
package tendril.annotationprocessor;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import tendril.codegen.classes.JClass;
import tendril.codegen.classes.method.JMethod;
import tendril.codegen.field.type.ClassType;
import tendril.test.AbstractUnitTest;

/**
 * Test case for {@link AbstractTendrilProccessor}
 */
public class AbstractTendrilProccessorTest extends AbstractUnitTest {

    /**
     * Concrete implementation to use for testing
     */
    private class TestTendrilProcessor extends AbstractTendrilProccessor {
        // Flags for tracking the calls to the processing methods
        private int timesTypeCalled = 0;
        private int timesMethodCalled = 0;
        private ClassType lastClassTypeClassData;
        private ClassType lastMethodTypeClassData;
        private JMethod<?> lastMethodTypeMethodData;

        /**
         * CTOR - prepare for test
         */
        private TestTendrilProcessor() {
            this.processingEnv = mockProcessingEnv;
        }

        /**
         * @see tendril.annotationprocessor.AbstractTendrilProccessor#processType(tendril.codegen.field.type.ClassType)
         */
        protected ClassDefinition processType() {
            timesTypeCalled++;
            lastClassTypeClassData = currentClassType;
            return mockGeneratedDef;
        }

        /**
         * Verify that the call to {@code processType} was as expected
         * 
         * @param expectedTimesCalled int the number of times it was expected to be called
         * @param expectedData        {@link ClassType} which should have been provided with the last call
         */
        private void verifyClassType(int expectedTimesCalled, ClassType expectedData) {
            Assertions.assertEquals(expectedTimesCalled, timesTypeCalled);
            Assertions.assertEquals(expectedData, lastClassTypeClassData);
        }

        /**
         * @see tendril.annotationprocessor.AbstractTendrilProccessor#processMethod(tendril.codegen.field.type.ClassType, tendril.codegen.classes.method.JMethod)
         */
        protected ClassDefinition processMethod(ClassType classData, JMethod<?> methodData) {
            timesMethodCalled++;
            lastMethodTypeClassData = classData;
            lastMethodTypeMethodData = methodData;
            return mockGeneratedDef;
        }

        /**
         * Verify that the call to {@code processMethod} was as expected
         * 
         * @param expectedTimesCalled int the number of times it was expected to be called
         * @param expectedClassData   {@link ClassType} which should have been provided with the last call
         * @param expectedMethodData  {@link JMethod} which should have been provided with the last call
         */
        private void verifyMethodType(int expectedTimesCalled, ClassType expectedClassData, JMethod<?> expectedMethodData) {
            Assertions.assertEquals(expectedTimesCalled, timesMethodCalled);
            Assertions.assertEquals(expectedClassData, lastMethodTypeClassData);
            Assertions.assertEquals(expectedMethodData, lastMethodTypeMethodData);
        }
    }

    // Mocks to use for testing
    @Mock
    private TypeElement mockAnnotation;
    @Mock
    private RoundEnvironment mockEnvironment;
    @Mock
    private TypeElement mockTypeElement;
    @Mock
    private Name mockSimpleName;
    @Mock
    private Name mockFullyQualifiedName;
    @Mock
    private ExecutableElement mockMethodElement;
    @Mock
    private TypeMirror mockTypeMirror;
    @Mock
    private ExecutableType mockMethodType;
    @Mock
    private TypeMirror mockParam1TypeMirror;
    @Mock
    private VariableElement mockParam1Var;
    @Mock
    private TypeMirror mockParam2TypeMirror;
    @Mock
    private VariableElement mockParam2Var;
    @Mock
    private ModuleElement mockModuleElement;
    @Mock
    private PackageElement mockPackageElement;
    @Mock
    private Parameterizable mockParameterizableElement;
    @Mock
    private QualifiedNameable mockQualifiedNameableElement;
    @Mock
    private RecordComponentElement mockRecordComponentElement;
    @Mock
    private TypeParameterElement mockParameterElement;
    @Mock
    private VariableElement mockVariableElement;
    @Mock
    private Elements mockElementUtils;
    @Mock
    private Types mockTypeUtils;
    @Mock
    private ProcessingEnvironment mockProcessingEnv;
    @Mock
    private ClassDefinition mockGeneratedDef;
    @Mock
    private ClassType mockGeneratedType;
    @Mock
    private Filer mockFiler;
    @Mock
    private JavaFileObject mockFileObject;
    @Mock
    private Writer mockFileWriter;
    @Mock
    private JClass mockJClass;
    @Mock
    private ClassType mockClassType;
    @Mock
    private JMethod<?> mockJMethod;

    // Instance to test
    private TestTendrilProcessor processor;

    /**
     * @see tendril.test.AbstractUnitTest#prepareTest()
     */
    @Override
    protected void prepareTest() {
        processor = new TestTendrilProcessor();

        lenient().when(mockEnvironment.errorRaised()).thenReturn(false);
        lenient().when(mockEnvironment.processingOver()).thenReturn(false);
    }

    /**
     * Verify that processing doesn't happen is there is an error or processing is complete
     */
    @Test
    public void testEnvShouldNotBeProcessed() {
        // If error raised, do nothing
        when(mockEnvironment.errorRaised()).thenReturn(true);
        Assertions.assertFalse(processor.process(Set.of(mockAnnotation), mockEnvironment));
        verify(mockEnvironment).errorRaised();

        // If no error raised, but processing is complete, still do nothing
        when(mockEnvironment.errorRaised()).thenReturn(false);
        when(mockEnvironment.processingOver()).thenReturn(true);
        Assertions.assertFalse(processor.process(Set.of(mockAnnotation), mockEnvironment));
        verify(mockEnvironment, times(2)).errorRaised();
        verify(mockEnvironment).processingOver();
    }

    /**
     * Verify that everything works properly if no element is detected
     */
    @Test
    public void testNothingToProcess() {
        when(mockEnvironment.getElementsAnnotatedWith(mockAnnotation)).thenReturn(Collections.emptySet());
        Assertions.assertFalse(processor.process(Set.of(mockAnnotation), mockEnvironment));
        verify(mockEnvironment).errorRaised();
        verify(mockEnvironment).processingOver();
        verify(mockEnvironment).getElementsAnnotatedWith(mockAnnotation);

        processor.verifyClassType(0, null);
        processor.verifyMethodType(0, null, null);
    }

    /**
     * Verify that processing doesn't happen if the element is not one of the desired types
     */
    @Test
    public void testDoNotProcessUndesiredTypes() {
        doReturn(Set.of(mockModuleElement, mockPackageElement, mockParameterizableElement, mockQualifiedNameableElement, mockRecordComponentElement, mockParameterElement, mockVariableElement))
                .when(mockEnvironment).getElementsAnnotatedWith(mockAnnotation);
        processor.process(Set.of(mockAnnotation), mockEnvironment);
        verify(mockEnvironment).errorRaised();
        verify(mockEnvironment).processingOver();
        verify(mockEnvironment).getElementsAnnotatedWith(mockAnnotation);

        processor.verifyClassType(0, null);
        processor.verifyMethodType(0, null, null);
    }

    /**
     * Verify that a class element is properly processed
     * @throws IOException 
     */
    @Test
    public void testProcessClass() throws IOException {
        try (MockedStatic<ElementLoader> mockLoader = Mockito.mockStatic(ElementLoader.class)) {
            mockLoader.when(() -> ElementLoader.loadClassDetails(mockTypeElement)).thenReturn(mockJClass);
            when(mockJClass.getType()).thenReturn(mockClassType);
            when(mockGeneratedType.getFullyQualifiedName()).thenReturn("z.x.c.V");
            when(mockGeneratedDef.getType()).thenReturn(mockGeneratedType);
            when(mockGeneratedDef.getCode()).thenReturn("classCode");
            when(mockProcessingEnv.getFiler()).thenReturn(mockFiler);
            when(mockFiler.createSourceFile("z.x.c.V")).thenReturn(mockFileObject);
            when(mockFileObject.openWriter()).thenReturn(mockFileWriter);
    
            // This mock format is required due to compilation error with "normal" method
            doReturn(Set.of(mockTypeElement)).when(mockEnvironment).getElementsAnnotatedWith(mockAnnotation);
            Assertions.assertFalse(processor.process(Set.of(mockAnnotation), mockEnvironment));
            verify(mockEnvironment).errorRaised();
            verify(mockEnvironment).processingOver();
            verify(mockEnvironment).getElementsAnnotatedWith(mockAnnotation);
            mockLoader.verify(() -> ElementLoader.loadClassDetails(mockTypeElement));
            verify(mockGeneratedType).getFullyQualifiedName();
            verify(mockFileWriter).write("classCode", 0, "classCode".length());
            verify(mockFileWriter).close();
    
            processor.verifyClassType(1, mockClassType);
            processor.verifyMethodType(0, null, null);
        }
    }

    /**
     * Verify that a method is properly processed
     * @throws IOException 
     */
    @Test
    public void testProcessMethod() throws IOException {
        try (MockedStatic<ElementLoader> mockLoader = Mockito.mockStatic(ElementLoader.class)) {
            mockLoader.when(() -> ElementLoader.loadMethodDetails(mockMethodElement)).thenReturn(Pair.of(mockClassType, mockJMethod));
            when(mockGeneratedType.getFullyQualifiedName()).thenReturn("z.x.c.V");
            when(mockProcessingEnv.getFiler()).thenReturn(mockFiler);
            when(mockGeneratedDef.getType()).thenReturn(mockGeneratedType);
            when(mockGeneratedDef.getCode()).thenReturn("methodCode");
            when(mockFiler.createSourceFile("z.x.c.V")).thenReturn(mockFileObject);
            when(mockFileObject.openWriter()).thenReturn(mockFileWriter);

            // This mock format is required due to compilation error with "normal" method
            doReturn(Set.of(mockMethodElement)).when(mockEnvironment).getElementsAnnotatedWith(mockAnnotation);
            Assertions.assertFalse(processor.process(Set.of(mockAnnotation), mockEnvironment));
            verify(mockEnvironment).errorRaised();
            verify(mockEnvironment).processingOver();
            verify(mockEnvironment).getElementsAnnotatedWith(mockAnnotation);
            mockLoader.verify(() -> ElementLoader.loadMethodDetails(mockMethodElement));
            verify(mockGeneratedType).getFullyQualifiedName();
            verify(mockFileWriter).write("methodCode", 0, "methodCode".length());
            verify(mockFileWriter).close();
    
            processor.verifyClassType(0, null);
            processor.verifyMethodType(1, mockClassType, mockJMethod);
        }
    }

    /**
     * Verify that parameter mismatch fails processing
     */
    @Test
    public void testMethodMoreParamsThanTypes() {
        when(mockTypeElement.getSimpleName()).thenReturn(mockSimpleName);
        when(mockSimpleName.toString()).thenReturn("Qwerty");
        when(mockTypeElement.getQualifiedName()).thenReturn(mockFullyQualifiedName);
        when(mockFullyQualifiedName.toString()).thenReturn("a.b.c.d.Qwerty");

        doReturn(Set.of(mockMethodElement)).when(mockEnvironment).getElementsAnnotatedWith(mockAnnotation);
        when(mockMethodElement.getEnclosingElement()).thenReturn(mockTypeElement);
        when(mockMethodElement.asType()).thenReturn(mockMethodType);
        doReturn(Arrays.asList(mockParam1TypeMirror)).when(mockMethodType).getParameterTypes();
        doReturn(Arrays.asList(mockParam1Var, mockParam2Var)).when(mockMethodElement).getParameters();

        Assertions.assertThrows(ProcessingException.class, () -> processor.process(Set.of(mockAnnotation), mockEnvironment));
        verify(mockEnvironment).errorRaised();
        verify(mockEnvironment).processingOver();
    }

    /**
     * Verify that parameter mismatch fails processing
     */
    @Test
    public void testMethodMoreTypesThanParams() {
        when(mockTypeElement.getSimpleName()).thenReturn(mockSimpleName);
        when(mockSimpleName.toString()).thenReturn("Qwerty");
        when(mockTypeElement.getQualifiedName()).thenReturn(mockFullyQualifiedName);
        when(mockFullyQualifiedName.toString()).thenReturn("a.b.c.d.Qwerty");

        doReturn(Set.of(mockMethodElement)).when(mockEnvironment).getElementsAnnotatedWith(mockAnnotation);
        when(mockMethodElement.getEnclosingElement()).thenReturn(mockTypeElement);
        when(mockMethodElement.asType()).thenReturn(mockMethodType);
        doReturn(Arrays.asList(mockParam1TypeMirror, mockParam2TypeMirror)).when(mockMethodType).getParameterTypes();
        doReturn(Arrays.asList(mockParam1Var)).when(mockMethodElement).getParameters();

        Assertions.assertThrows(ProcessingException.class, () -> processor.process(Set.of(mockAnnotation), mockEnvironment));
        verify(mockEnvironment).errorRaised();
        verify(mockEnvironment).processingOver();
    }

    /**
     * Verify that the assignability is properly determined
     */
    @Test
    public void testAssignable() {
        when(mockProcessingEnv.getElementUtils()).thenReturn(mockElementUtils);
        when(mockElementUtils.getTypeElement(anyString())).thenReturn(mockAnnotation);
        when(mockAnnotation.asType()).thenReturn(mockParam1TypeMirror);
        when(mockProcessingEnv.getTypeUtils()).thenReturn(mockTypeUtils);
        when(mockTypeElement.asType()).thenReturn(mockTypeMirror);

        when(mockTypeUtils.isAssignable(mockTypeMirror, mockParam1TypeMirror)).thenReturn(false);
        Assertions.assertFalse(processor.isTypeOf(mockTypeElement, getClass()));
        verify(mockProcessingEnv).getElementUtils();
        verify(mockElementUtils).getTypeElement(anyString());
        verify(mockProcessingEnv).getTypeUtils();
        verify(mockTypeUtils).isAssignable(mockTypeMirror, mockParam1TypeMirror);

        when(mockTypeUtils.isAssignable(mockTypeMirror, mockParam1TypeMirror)).thenReturn(true);
        Assertions.assertTrue(processor.isTypeOf(mockTypeElement, getClass()));
        verify(mockProcessingEnv, times(2)).getElementUtils();
        verify(mockElementUtils, times(2)).getTypeElement(anyString());
        verify(mockProcessingEnv, times(2)).getTypeUtils();
        verify(mockTypeUtils, times(2)).isAssignable(mockTypeMirror, mockParam1TypeMirror);
    }
    
    /**
     * Verify that a generated null class definition is not written out.
     */
    @Test
    public void testNullClassDefinition() {
        processor.writeCode(null);
        verifyNoInteractions(mockFileWriter);
    }
}
