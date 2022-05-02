package com.niton.compile.verify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import javax.lang.model.element.TypeElement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.niton.compile.processor.ProcessingLogger;

class ProcessingVerificationTest
{

    @Test
    void invertValid()
    {
        ProcessingVerification pv = new ProcessingVerification(
            mock(ProcessingLogger.class),
            e -> true,
            "",
            null
        ).not();
        assertThat(pv.isValid()).isFalse();
    }

    @Test
    void invertInvalid()
    {
        ProcessingVerification pv = new ProcessingVerification(
            mock(ProcessingLogger.class),
            e -> false,
            "",
            null
        ).not();
        assertThat(pv.isValid()).isTrue();
    }

    @Test
    void because()
    {
        var elem = mock(TypeElement.class);
        var logger = mock(ProcessingLogger.class);
        ProcessingVerification pv = new ProcessingVerification(
            logger,
            e -> false,
            "This should be false",
            elem
        );
        pv.because("it is %s", "impossible").infoOnViolation();
        verify(logger).info(elem, "This should be false, because it is impossible");
    }
    @Test
    void becauseAnnotation()
    {
        var elem = mock(TypeElement.class);
        var logger = mock(ProcessingLogger.class);
        ProcessingVerification pv = new ProcessingVerification(
            logger,
            e -> false,
            "This should be false",
            elem
        );
        pv.because(Override.class).infoOnViolation();
        verify(logger).info(elem, "This should be false, because it is annotated with @Override");
    }

    @ParameterizedTest
    @CsvSource({
        "true,true,true",
        "true,false,false",
        "false,true,false",
        "false,false,true",
    })
    void testFailOnViolation(boolean verifySuccess, boolean invert, boolean shouldWarn)
    {
        var elem = mock(TypeElement.class);
        var logger = mock(ProcessingLogger.class);
        ProcessingVerification pv = new ProcessingVerification(
            logger,
            e -> verifySuccess,
            "This should be true",
            elem
        );
        if (invert)
            pv = pv.not();
        pv.failOnViolation();
        if (shouldWarn)
            verify(logger).fail(elem, "This should be true");
        else
            verifyNoInteractions(logger);
    }

    @ParameterizedTest
    @CsvSource({
        "true,true,true",
        "true,false,false",
        "false,true,false",
        "false,false,true",
    })
    void testInfoOnViolation(boolean verifySuccess, boolean invert, boolean shouldWarn)
    {
        var elem = mock(TypeElement.class);
        var logger = mock(ProcessingLogger.class);
        ProcessingVerification pv = new ProcessingVerification(
            logger,
            e -> verifySuccess,
            "This should be true",
            elem
        );
        if (invert)
            pv = pv.not();
        pv.infoOnViolation();
        if (shouldWarn)
            verify(logger).info(elem, "This should be true");
        else
            verifyNoInteractions(logger);
    }

    @ParameterizedTest
    @CsvSource({
        "true,true,true",
        "true,false,false",
        "false,true,false",
        "false,false,true",
    })
    void testWarnOnViolation(boolean verifySuccess, boolean invert, boolean shouldWarn)
    {
        var elem = mock(TypeElement.class);
        var logger = mock(ProcessingLogger.class);
        ProcessingVerification pv = new ProcessingVerification(
            logger,
            e -> verifySuccess,
            "This should be true",
            elem
        );
        if (invert)
            pv = pv.not();
        pv.warnOnViolation();
        if (shouldWarn)
            verify(logger).warn(elem, "This should be true");
        else
            verifyNoInteractions(logger);
    }

    @Test
    void notMessageReplacementApplied()
    {
        var elem = mock(TypeElement.class);
        var logger = mock(ProcessingLogger.class);
        ProcessingVerification pv = new ProcessingVerification(
            logger,
            e -> true,
            "This should [not ]be true",
            elem
        ).not();
        pv.infoOnViolation();
        verify(logger).info(elem, "This should not be true");
    }
    @Test
    void multipleNotMessageReplacementApplied()
    {
        var elem = mock(TypeElement.class);
        var logger = mock(ProcessingLogger.class);
        ProcessingVerification pv = new ProcessingVerification(
            logger,
            e -> true,
            "This should [not ]be [so very ]true",
            elem
        ).not();
        pv.infoOnViolation();
        verify(logger).info(elem, "This should not be so very true");
    }

    @Test
    void multipleNotMessageReplacementEscaped()
    {
        var elem = mock(TypeElement.class);
        var logger = mock(ProcessingLogger.class);
        ProcessingVerification pv = new ProcessingVerification(
            logger,
            e -> true,
            "This should not be \\[very\\] true",
            elem
        ).not();
        pv.infoOnViolation();
        verify(logger).info(elem, "This should not be [very] true");
    }
    @Test
    void multipleNotMessageReplacementEscapedWithoutViolation()
    {
        var elem = mock(TypeElement.class);
        var logger = mock(ProcessingLogger.class);
        ProcessingVerification pv = new ProcessingVerification(
            logger,
            e -> true,
            "This should [not ]be \\[very\\] true",
            elem
        ).not();
        pv.infoOnViolation();
        verify(logger).info(elem, "This should not be [very] true");
    }

    @Test
    void multipleNotMessageReplacementEscapedWithoutViolation2()
    {
        var elem = mock(TypeElement.class);
        var logger = mock(ProcessingLogger.class);
        ProcessingVerification pv = new ProcessingVerification(
            logger,
            e -> true,
            "This should [not\\]\\]\\[] be \\[very\\] true",
            elem
        ).not();
        pv.infoOnViolation();
        verify(logger).info(elem, "This should not]][ be [very] true");
    }
    @Test
    void notMessageReplacementNotApplied()
    {
        var elem = mock(TypeElement.class);
        var logger = mock(ProcessingLogger.class);
        ProcessingVerification pv = new ProcessingVerification(
            logger,
            e -> false,
            "This should [not ]be true",
            elem
        );
        pv.infoOnViolation();
        verify(logger).info(elem, "This should be true");
    }
    @Test
    void isValid()
    {
        ProcessingVerification pv = new ProcessingVerification(
            mock(ProcessingLogger.class),
            e -> true,
            "",
            null
        );
        assertThat(pv.isValid()).isTrue();
    }

    @Test
    void isInValid()
    {
        ProcessingVerification pv = new ProcessingVerification(
            mock(ProcessingLogger.class),
            e -> false,
            "",
            null
        );
        assertThat(pv.isValid()).isFalse();
    }
}