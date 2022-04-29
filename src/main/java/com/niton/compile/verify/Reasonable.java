package com.niton.compile.verify;

import java.lang.annotation.Annotation;

/**
 * Used to enrich the verification process with a reasoning for the verification.
 */
public interface Reasonable
{
    /**
     * Completes the message should the verification fail with a reasoniung
     *
     * @param message the message to be used ({@link String#format(String, Object...)})
     * @param args    the arguments to be used in the message ({@link String#format(String, Object...)})
     * @return the {@link Verifiable} instance
     */
    Verifiable because(String message, Object... args);

    /**
     * A shorthand to {@link #because(String, Object...)}, where the message is that the reason is that the element is annotated with the given annotation
     *
     * @param message the annotation that requires this assertion
     * @return the {@link Verifiable} instance
     */
    default Verifiable because(Class<? extends Annotation> message)
    {
        return because("it is annotated with %s", message.getName());
    }
}
