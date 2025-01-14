package com.niton.compile.processor;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.niton.compile.verify.ProcessingVerification;
import com.niton.compile.verify.Verifiable;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;

class ProcessingVerifierTest
{
    static Elements elementUtil = mock(Elements.class);
    static Types typeUtil = mock(Types.class);
    static ProcessingEnvironment env = mock(ProcessingEnvironment.class);
    static ProcessingLogger logger = mock(ProcessingLogger.class);
    ;

    @BeforeAll
    static void prepare()
    {
        lenient().when(env.getElementUtils()).thenReturn(elementUtil);
        lenient().when(env.getTypeUtils()).thenReturn(typeUtil);
    }

    @Test
    void isClassValid()
    {
        isValidOfType(ElementKind.CLASS, "class", ProcessingVerifier::isClass);
    }

    @Test
    void isClassInvalid()
    {
        isInvalidOfType(ElementKind.ENUM, "class", ProcessingVerifier::isClass);
    }

    @Test
    void doesExtend()
    {
        var verifier = new ProcessingVerifier(env, logger);
        var elem = mock(TypeElement.class);
        var elemType = mock(TypeMirror.class);
        var stringTypeElem = mock(TypeElement.class);
        var stringType = mock(DeclaredType.class);

        when(elem.asType()).thenReturn(elemType);
        when(typeUtil.isSubtype(elemType, stringType)).thenReturn(true);
        when(elementUtil.getTypeElement("java.lang.String")).thenReturn(stringTypeElem);
        when(stringTypeElem.asType()).thenReturn(stringType);
        when(stringType.toString()).thenReturn("class java.lang.String");

        assertThat(verifier.doesExtend(elem, String.class).isValid()).isTrue();
        expectLog(verifier.doesExtend(elem, String.class).not(), elem, "should not extend class java.lang.String");
    }

    @Test
    void doesNotExtend()
    {
        var verifier = new ProcessingVerifier(env, logger);
        var elem = mock(TypeElement.class);
        var elemType = mock(TypeMirror.class);
        var stringTypeElem = mock(TypeElement.class);
        var stringType = mock(DeclaredType.class);

        when(elem.asType()).thenReturn(elemType);
        when(typeUtil.isSubtype(elemType, stringType)).thenReturn(false);
        when(elementUtil.getTypeElement("java.lang.String")).thenReturn(stringTypeElem);
        when(stringTypeElem.asType()).thenReturn(stringType);
        when(stringType.toString()).thenReturn("class java.lang.String");

        assertThat(verifier.doesExtend(elem, String.class).isValid()).isFalse();
        expectLog(verifier.doesExtend(elem, String.class), elem, "should extend class java.lang.String");
    }

    @Test
    void isInterfaceValid()
    {
        isValidOfType(ElementKind.INTERFACE, "interface", ProcessingVerifier::isInterface);
    }

    @Test
    void isInterfaceInvalid()
    {
        isInvalidOfType(ElementKind.CLASS, "interface", ProcessingVerifier::isInterface);
    }

    @Test
    void isAnnotationValid()
    {
        isValidOfType(ElementKind.ANNOTATION_TYPE, "annotation", ProcessingVerifier::isAnnotation);
    }

    @Test
    void isAnnotationInvalid()
    {
        isInvalidOfType(ElementKind.INTERFACE, "annotation", ProcessingVerifier::isAnnotation);
    }

    @Test
    void isEnumValid()
    {
        isValidOfType(ElementKind.ENUM, "enum", ProcessingVerifier::isEnum);
    }

    @Test
    void isEnumInvalid()
    {
        isInvalidOfType(ElementKind.CLASS, "enum", ProcessingVerifier::isEnum);
    }

    @Test
    void isFieldValid()
    {
        isValidOfType(ElementKind.FIELD, "field", ProcessingVerifier::isField);
    }

    @Test
    void isFieldInvalid()
    {
        isInvalidOfType(ElementKind.METHOD, "field", ProcessingVerifier::isField);
    }

    @Test
    void isAnnotatedWith()
    {
        var verifier = new ProcessingVerifier(env, logger);
        var clazz = mock(Element.class);
        when(clazz.getAnnotation(Override.class)).thenReturn(mock(Override.class));

        assertThat(verifier.isAnnotatedWith(clazz, Override.class).isValid()).isTrue();

        expectLog(verifier.isAnnotatedWith(clazz, Override.class).not(), clazz,
            "should not be annotated with @Override");
    }

    @Test
    void isNotAnnotatedWith()
    {
        var verifier = new ProcessingVerifier(env, logger);
        var clazz = mock(Element.class);
        when(clazz.getAnnotation(Override.class)).thenReturn(null);

        assertThat(verifier.isAnnotatedWith(clazz, Override.class).isValid()).isFalse();

        expectLog(verifier.isAnnotatedWith(clazz, Override.class), clazz,
            "should be annotated with @Override");
    }

    @Test
    void doesImplement()
    {
        var verifier = new ProcessingVerifier(env, logger);
        var elem = mock(TypeElement.class);
        when(typeUtil.isAssignable(any(), any())).thenReturn(true);
        when(elementUtil.getTypeElement(any())).thenReturn(mock(TypeElement.class));

        assertThat(verifier.doesImplement(elem, Serializable.class).isValid()).isTrue();

        expectLog(verifier.doesImplement(elem, Serializable.class).not(), elem,
            "should not implement interface java.io.Serializable");
    }

    @Test
    void doesNotImplement()
    {
        var verifier = new ProcessingVerifier(env, logger);
        var elem = mock(TypeElement.class);
        when(typeUtil.isAssignable(any(), any())).thenReturn(false);
        when(elementUtil.getTypeElement(any())).thenReturn(mock(TypeElement.class));

        assertThat(verifier.doesImplement(elem, Serializable.class).isValid()).isFalse();

        expectLog(verifier.doesImplement(elem, Serializable.class), elem,
            "should implement interface java.io.Serializable");
    }

    private void isValidOfType(ElementKind kind, String name,
        BiFunction<ProcessingVerifier, Element, ProcessingVerification> function)
    {
        var verifier = new ProcessingVerifier(env, logger);
        var clazz = mock(Element.class);
        when(clazz.getKind()).thenReturn(kind);

        assertThat(function.apply(verifier, clazz).isValid()).isTrue();

        var prefix = "a";
        if (Arrays.binarySearch("aeiou".toCharArray(), name.charAt(0)) > -1)
            prefix = "an";
        expectLog(function.apply(verifier, clazz).not(), clazz, format("should not be %s %s",prefix, name));
    }

    private void isInvalidOfType(ElementKind kind, String name,
        BiFunction<ProcessingVerifier, Element, ProcessingVerification> function)
    {
        var verifier = new ProcessingVerifier(env, logger);
        var clazz = mock(Element.class);
        when(clazz.getKind()).thenReturn(kind);

        assertThat(function.apply(verifier,clazz).isValid()).isFalse();
        var prefix = "a";
        if (Arrays.binarySearch("aeiou".toCharArray(), name.charAt(0)) > -1)
            prefix = "an";
        expectLog(function.apply(verifier, clazz), clazz, format("should be %s %s", prefix, name));
    }

    private void expectLog(Verifiable verify, Element clazz, String expectedLogMessage)
    {
        assertThat(verify.infoOnViolation()).isFalse();
        var logCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).info(eq(clazz), logCaptor.capture(), any());
        assertThat(logCaptor.getValue()).endsWith(expectedLogMessage);
    }
}