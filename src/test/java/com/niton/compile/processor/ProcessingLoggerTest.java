package com.niton.compile.processor;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.MANDATORY_WARNING;
import static javax.tools.Diagnostic.Kind.NOTE;
import static javax.tools.Diagnostic.Kind.WARNING;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Set;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;

import org.junit.jupiter.api.Test;

class ProcessingLoggerTest
{

    @Test
    void failException()
    {
        var plog = mock(Messager.class);
        var logger = new ProcessingLogger(plog);
        logger.fail(new Exception("Some error"));
        verify(plog).printMessage(eq(ERROR), contains("Some error"));
    }

    @Test
    void failExceptionWithTrace()
    {
        var plog = mock(Messager.class);
        var logger = new ProcessingLogger(plog);
        logger.fail(new Exception("Some error"),true);
        verify(plog,times(2)).printMessage(eq(ERROR), contains("Some error"));
    }
    @Test
    void failExceptionWithoutTrace()
    {
        var plog = mock(Messager.class);
        var logger = new ProcessingLogger(plog);
        logger.fail(new Exception("Some error"),false);
        verify(plog,times(1)).printMessage(eq(ERROR), contains("Some error"));
    }

    @Test
    void warn() {
        var plog = mock(Messager.class);
        var logger = new ProcessingLogger(plog);
        logger.warn(null,"warning");
        verify(plog).printMessage(eq(MANDATORY_WARNING), contains("warning"),isNull());
    }

    @Test
    void info()
    {
        var plog = mock(Messager.class);
        var logger = new ProcessingLogger(plog);
        logger.info((Element) null,"info");
        verify(plog).printMessage(eq(NOTE), contains("info"),isNull());
    }

    @Test
    void infoWithoutElement()
    {
        var plog = mock(Messager.class);
        var logger = new ProcessingLogger(plog);
        logger.info("some %s","info");
        verify(plog).printMessage(eq(NOTE), contains("some info"));
    }

    @Test
    void logRoundInfo()
    {
        var plog = mock(Messager.class);
        var logger = new ProcessingLogger(plog);
        logger.logRoundInfo(Set.of(),mock(RoundEnvironment.class));
        verify(plog).printMessage(eq(NOTE), contains("false"));
    }
}