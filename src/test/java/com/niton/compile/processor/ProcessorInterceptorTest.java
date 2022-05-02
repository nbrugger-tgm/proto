package com.niton.compile.processor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

import org.junit.jupiter.api.Test;

class ProcessorInterceptorTest
{

    @Test
    void processable()
    {
        var innerProcessable = mock(Processable.class);
        var nextElement = mock(Processable.class);
        var interceptor = new ProcessorInterceptor(null,null,null)
        {
            @Override
            public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv, Processable processor)
            {
                innerProcessable.process(Set.of(),null);
                return processor.process(set, roundEnv);
            }
        };
        var asProcessable = interceptor.processable(nextElement);
        asProcessable.process(null, null);
        verify(nextElement).process(null, null);
        verify(innerProcessable).process(Set.of(),null);
    }
}