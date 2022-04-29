package com.niton.compile.processor;

import java.util.Set;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * Functional interface for processing a set of elements.
 */
@FunctionalInterface
public interface Processable
{
    /**
     * {@link Processor#process(Set, RoundEnvironment)}
     */
    boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment);
}
