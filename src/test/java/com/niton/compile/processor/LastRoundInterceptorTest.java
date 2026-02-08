package com.niton.compile.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LastRoundInterceptorTest
{
    ProcessingEnvironment env;

    @BeforeEach
    void init() throws IOException
    {
        var writer = mock(Writer.class);
        var javaFileObject = mock(JavaFileObject.class);
        var filer = mock(Filer.class);
        env = mock(ProcessingEnvironment.class);
        lenient().when(env.getFiler()).thenReturn(filer);
        lenient().when(filer.createSourceFile(any(), any())).thenReturn(javaFileObject);
        lenient().when(javaFileObject.openWriter()).thenReturn(writer);
    }

    @Test
    void skipOnLastRound()
    {
        LastRoundInterceptor lri = new LastRoundInterceptor(env, mock(ProcessingLogger.class), null, "test");
        var round = mock(RoundEnvironment.class);
        var processor = mock(Processable.class);
        when(round.processingOver()).thenReturn(true);
        assertThat(lri.process(null, round, processor)).isFalse();
        verifyNoInteractions(processor);
    }

    @Test
    void fakeLastRound()
    {
        LastRoundInterceptor lri = new LastRoundInterceptor(env, mock(ProcessingLogger.class), null, "test");

        var processor = mock(Processable.class);
        when(processor.process(any(), any())).thenReturn(true);

        TypeElement fakeElement = mockFakeElement();
        var round = mock(RoundEnvironment.class);
        when(round.processingOver()).thenReturn(false);
        doReturn(Set.of(fakeElement)).when(round).getRootElements();

        assertThat(lri.process(null, round, processor)).isTrue();
        var captor = ArgumentCaptor.forClass(RoundEnvironment.class);
        verify(processor).process(any(), captor.capture());
        assertThat(captor.getValue().processingOver()).isTrue();
    }

    @Test
    void stopAfterFakeRound()
    {
        LastRoundInterceptor lri = new LastRoundInterceptor(env, mock(ProcessingLogger.class), null, "test");

        var processor = mock(Processable.class);
        when(processor.process(any(), any())).thenReturn(true);

        TypeElement fakeElement = mockFakeElement();
        var round = mock(RoundEnvironment.class);
        when(round.processingOver()).thenReturn(false, false);
        doReturn(Set.of(fakeElement), Set.of(fakeElement)).when(round).getRootElements();

        assertThat(lri.process(null, round, processor)).isTrue();//fakeLastRound-counted
        assertThat(lri.process(null, round, processor)).isFalse();//roundAfterFake-not counted
        verify(processor, times(1)).process(any(), any());
    }

    @Test
    void normalRound()
    {
        LastRoundInterceptor lri = new LastRoundInterceptor(env, mock(ProcessingLogger.class), null, "test");

        var processor = mock(Processable.class);
        when(processor.process(any(), any())).thenReturn(true);

        var element = mockRealElement();
        var fakeElement = mockFakeElement();
        var round = mock(RoundEnvironment.class);
        when(round.processingOver()).thenReturn(false);
        doReturn(Set.of(element), Set.of(fakeElement, element)).when(round).getRootElements();

        assertThat(lri.process(null, round, processor)).isTrue();
        assertThat(lri.process(null, round, processor)).isTrue();
        var captor = ArgumentCaptor.forClass(RoundEnvironment.class);
        verify(processor, times(2)).process(any(), captor.capture());
        assertThat(captor.getValue().processingOver()).isFalse();
    }

    @Test
    void proxyEnvironment()
    {
        LastRoundInterceptor lri = new LastRoundInterceptor(env, mock(ProcessingLogger.class), null, "test");

        var processor = mock(Processable.class);
        when(processor.process(any(), any())).thenReturn(true);

        var element = mockRealElement();
        var round = mock(RoundEnvironment.class);
        when(round.processingOver()).thenReturn(false);
        doReturn(Set.of(element)).when(round).getRootElements();

        when(round.errorRaised()).thenReturn(true);
        doReturn(Set.of(mockRealElement(), mockFakeElement()))
            .when(round).getRootElements();

        lri.process(null, round, processor);//generates the proxy internally
        var captor = ArgumentCaptor.forClass(RoundEnvironment.class);
        verify(processor).process(isNull(), captor.capture());
        assertThat(captor.getValue()).as("The environment should be proxied").isNotSameAs(round);
        assertThat(captor.getValue().errorRaised()).isTrue();

        var f1 = mockTypeElement("F1");
        var f2 = mockTypeElement("F2");
        doReturn(Set.of(f1))
            .when(round).getElementsAnnotatedWith(any(TypeElement.class));
        doReturn(Set.of(f1,f2))
            .when(round).getElementsAnnotatedWith(any(Class.class));

        Set<Element> res = new HashSet<>(captor.getValue().getRootElements());
        verify(round, times(2)).getRootElements();//one time internal
        assertThat(res).containsExactlyInAnyOrder(round.getRootElements().toArray(Element[]::new));

        res = new HashSet<>(captor.getValue().getElementsAnnotatedWith(mockTypeElement("Override")));
        verify(round, times(1)).getElementsAnnotatedWith(any(TypeElement.class));
        assertThat(res).containsExactlyInAnyOrder(round.getElementsAnnotatedWith(mockTypeElement("Override")).toArray(Element[]::new));

        res = new HashSet<>(captor.getValue().getElementsAnnotatedWith(Override.class));
        verify(round, times(1)).getElementsAnnotatedWith(Override.class);
        assertThat(res).containsExactlyInAnyOrder(round.getElementsAnnotatedWith(Override.class).toArray(Element[]::new));
    }

    private TypeElement mockRealElement()
    {
        return mockTypeElement("FooService");
    }

    @NotNull
    private TypeElement mockTypeElement(String FooService)
    {
        var fakeName = mock(Name.class);
        when(fakeName.toString()).thenReturn(FooService);

        var fakeElement = mock(TypeElement.class);
        when(fakeElement.getSimpleName()).thenReturn(fakeName);

        return fakeElement;
    }

    @NotNull
    private TypeElement mockFakeElement()
    {
        return mockTypeElement("LastRoundInterceptorTest$jdk_8256826_bug$round4");
    }

}