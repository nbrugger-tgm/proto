package com.niton.compile.processor;

import static javax.tools.Diagnostic.Kind.ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import com.palantir.javapoet.TypeSpec;

class BaseProcessorTest
{
    private BaseProcessor processor;
    private ProcessorInterceptor interruptingInterceptor = spy(new ProcessorInterceptor(null, null, null)
    {
        @Override
        public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv, Processable processor)
        {
            return false;
        }
    });
    private ProcessorInterceptor forwardingInterceptor = spy(new ProcessorInterceptor(null, null, null)
    {
        @Override
        public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv, Processable processor)
        {
            return processor.process(set, roundEnv);
        }
    });

    @BeforeEach
    void setUp()
    {
        processor = getProcessor(false);
    }

    ProcessingEnvironment env;

    @BeforeEach
    void mockEnv() throws IOException
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
    void applyJavacFixByDefault()
    {
        assertThat(new BaseProcessor()
        {
            @Override
            public boolean performProcessing(@NotNull Set<? extends TypeElement> annotations,
                @NotNull RoundEnvironment roundEnvironment)
            {
                return false;
            }
        }.applyJavacBugWorkaround()).isTrue();
    }

    @Test
    void writeClass() throws IOException
    {
        var env = mock(ProcessingEnvironment.class);
        processor.init(env);
        TypeSpec spec = TypeSpec.classBuilder("Test").build();

        var filter = mock(Filer.class);
        var writer = new StringWriter();
        var file = mock(JavaFileObject.class);
        when(env.getFiler()).thenReturn(filter);
        when(filter.createSourceFile(any(), any())).thenReturn(file);
        when(file.openWriter()).thenReturn(writer);

        processor.writeClass("com.test", spec);

        var classNameCaptor = ArgumentCaptor.forClass(CharSequence.class);
        verify(filter).createSourceFile(classNameCaptor.capture(), any());
        assertThat(classNameCaptor.getValue().toString())
            .contains("Test")
            .contains("com.test");
        assertThat(writer.toString())
            .contains("class Test")
            .contains("package com.test;")
            .contains("@Generated");
    }

    @Test
    void writeClassException() throws IOException
    {
        var env = mock(ProcessingEnvironment.class);
        var messager = mock(Messager.class);
        when(env.getMessager()).thenReturn(messager);
        processor.init(env);
        TypeSpec spec = TypeSpec.classBuilder("Test").build();


        var filter = mock(Filer.class);
        var writer = new StringWriter();
        var file = mock(JavaFileObject.class);
        when(env.getFiler()).thenReturn(filter);
        when(filter.createSourceFile(any(), any())).thenReturn(file);
        when(file.openWriter()).thenThrow(new IOException("some exception"));

        assertThatCode(() -> processor.writeClass("com.test", spec)).doesNotThrowAnyException();
        var messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(messager, times(2)).printMessage(eq(ERROR), messageCaptor.capture());
        assertThat(messageCaptor.getValue()).contains("some exception");
    }

    @ParameterizedTest
    @CsvSource({
        "foo-bar,fooBar",
        "foo_bar,fooBar",
        "a.b.c.foo-bar,fooBar"
    })
    void getVariableName(String original, String expected)
    {
        assertThat(processor.getVariableName(original)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "foo-bar,FooBar",
        "foo_bar,FooBar",
        "a.b.c.foo-bar,FooBar"
    })
    void getClassName(String original, String expected)
    {
        assertThat(processor.getClassName(original)).isEqualTo(expected);
    }

    @Test
    void interuptingInterceptor()
    {
        processor = getProcessor(false, interruptingInterceptor);
        processor.init(mock(ProcessingEnvironment.class));
        assertThat(processor.process(null, null)).isFalse();
        verify(processor, never()).performProcessing(any(), any());
    }

    @Test
    void forwardingInterceptor()
    {
        processor = getProcessor(false, forwardingInterceptor);
        processor.init(mock(ProcessingEnvironment.class));
        assertThat(processor.process(null, null)).isTrue();
        verify(processor).performProcessing(any(), any());
    }

    @Test
    void applyingJavacFix()
    {
        processor = getProcessor(true);
        processor.init(env);
        var mockRound = mock(RoundEnvironment.class);
        assertThat(processor.process(null, mockRound)).isTrue();
        var captor = ArgumentCaptor.forClass(RoundEnvironment.class);
        verify(processor).performProcessing(any(), captor.capture());
        assertThat(captor.getValue()).isNotSameAs(mockRound);
    }

    @Test
    void multipleForwardingInterceptors()
    {
        processor = getProcessor(false, forwardingInterceptor, forwardingInterceptor);
        processor.init(mock(ProcessingEnvironment.class));
        assertThat(processor.process(null, null)).isTrue();
        verify(processor).performProcessing(any(), any());
        verify(forwardingInterceptor, times(2)).process(any(), any(), any());
    }

    @Test
    void multipleInterruptingInterceptors()
    {
        processor = getProcessor(false, interruptingInterceptor, interruptingInterceptor);
        processor.init(mock(ProcessingEnvironment.class));
        assertThat(processor.process(null, null)).isFalse();
        verify(processor, never()).performProcessing(any(), any());
        verify(interruptingInterceptor, times(1)).process(any(), any(), any());
    }

    @Test
    void interceptorOrder1()
    {
        processor = getProcessor(false, interruptingInterceptor, forwardingInterceptor);
        processor.init(mock(ProcessingEnvironment.class));
        assertThat(processor.process(null, null)).isFalse();
        verify(processor, never()).performProcessing(any(), any());
        verify(forwardingInterceptor, never()).process(any(), any(), any());
        verify(interruptingInterceptor, times(1)).process(any(), any(), any());
    }

    @Test
    void interceptorOrder2()
    {
        processor = getProcessor(false, forwardingInterceptor, interruptingInterceptor);
        processor.init(mock(ProcessingEnvironment.class));
        assertThat(processor.process(null, null)).isFalse();
        verify(processor, never()).performProcessing(any(), any());
        verify(forwardingInterceptor, times(1)).process(any(), any(), any());
        verify(interruptingInterceptor, times(1)).process(any(), any(), any());
    }

    private BaseProcessor getProcessor(boolean javacFix, ProcessorInterceptor... interceptors)
    {
        return spy(new BaseProcessor()
        {
            @Override
            public boolean performProcessing(@NotNull Set<? extends TypeElement> annotations,
                @NotNull RoundEnvironment roundEnvironment)
            {
                return true;
            }

            @Override
            public boolean applyJavacBugWorkaround()
            {
                return javacFix;
            }

            @Override
            protected @NotNull List<ProcessorInterceptor> getInterceptors(@NotNull ProcessingEnvironment processingEnv,
                @NotNull ProcessingLogger logger, @NotNull ProcessingVerifier verifier)
            {
                return List.of(interceptors);
            }
        });
    }
}