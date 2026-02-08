package com.niton.compile.processor;

import static java.lang.String.format;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import org.jetbrains.annotations.NotNull;

/**
 * Logger for compile time processing.
 */
public class ProcessingLogger
{
    private final Messager messager;

    public ProcessingLogger(Messager messager)
    {
        this.messager = messager;
    }

    /**
     * This will fail the compilation process with the given exception message.
     */
    public void fail(@NotNull Exception exception)
    {
        fail(exception.getMessage());
    }

    /**
     * This will fail the compilation process with the given message
     * @param exception the exception that causes the fail
     * @param printStackTrace if true the full stack trace will be printed
     */
    public void fail(@NotNull Exception exception,boolean printStackTrace)
    {
        fail(exception.getMessage());
        if(printStackTrace)
        {
            var sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            sw.flush();
            fail("Trace: %s", sw.getBuffer());
        }
    }

    /**
     * throws a compilation warning with a given message, will not fail compilation
     * @param element the element that caused the warning or the warning is related to. For example the field that violates a constraint
     * @param msg the message to add to the warning, formatted according to the {@link String#format(String, Object...)} rules
     * @param args the arguments to pass to  {@link String#format(String, Object...)}
     */
    public void warn(@NotNull Element element, @NotNull String msg, Object... args)
    {
        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, format(msg, args), element);
    }

    /**
     * Adds info to the compilation log, is disabled by maven by default. Similar to the <i>DEBUG</i> log level
     * @param msg the message to add to the log, formatted according to the {@link String#format(String, Object...)} rules
     * @param args the arguments to pass to  {@link String#format(String, Object...)}
     */
    public void info(@NotNull String msg, Object... args)
    {
        messager.printMessage(Diagnostic.Kind.NOTE, format(msg, args));
    }
    /**
     * Adds info to the compilation log, is disabled by maven by default. Similar to the <i>DEBUG</i> log level
     * @param element the element that the message is related to
     * @param msg the message to add to the log, formatted according to the {@link String#format(String, Object...)} rules
     * @param args the arguments to pass to  {@link String#format(String, Object...)}
     */
    public void info(@NotNull Element element,@NotNull String msg, Object... args)
    {
        messager.printMessage(Diagnostic.Kind.NOTE, format(msg, args), element);
    }

    /**
     * Fails the compilation process with the given error message
     * @param element the element that caused the failure or the error is related to. For example the field that violates a constraint
     * @param msg the message to add to the error, formatted according to the {@link String#format(String, Object...)} rules
     * @param args the arguments to pass to  {@link String#format(String, Object...)}
     */
    public void fail(@NotNull Element element, @NotNull String msg, Object... args)
    {
        messager.printMessage(Diagnostic.Kind.ERROR, format(msg, args), element);
    }

    /**
     * <b>Use {@link #fail(Element, String, Object...)} whenever possible</b><br/>
     * Fails the compilation process with the given error message
     * @param msg the message to add to the error, formatted according to the {@link String#format(String, Object...)} rules
     * @param args the arguments to pass to  {@link String#format(String, Object...)}
     */
    public void fail(@NotNull String msg, Object... args)
    {
        messager.printMessage(Diagnostic.Kind.ERROR, format(msg, args));
    }

    /**
     * Logs the information about the current annotation processing round using {@link #info(String, Object...)}
     * @param set the set of annotations that are being processed
     * @param roundEnv the current round environment
     */
    public void logRoundInfo(@NotNull Set<? extends TypeElement> set, @NotNull RoundEnvironment roundEnv)
    {
        info("[%s] Process :%n\tis last: %s%n\thas error:%s%n\tAnnotations : %s%n\tInputs : %s",
            getClass().getSimpleName(),
            roundEnv.processingOver(),
            roundEnv.errorRaised(),
            set,
            roundEnv.getRootElements());
    }

}
