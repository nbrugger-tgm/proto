package com.niton.compile.processor;

import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * Can intercept the annotation processor round. It can modify manipulate the further processing of the chain.
 * This can range from prematurely aborting the round, failing the round to just logging the round.
 * <p>The {@link  BaseProcessor} can easily make use of interceptors.</p>
 * <p>The interceptors are linked up in a chain, each element of the chain is responsible for calling the next element. To implement your behaviour use {@link #process(Set, RoundEnvironment, Processable)}</p>
 *
 * @author Nils Brugger (u0eiuaw)
 */
public abstract class ProcessorInterceptor
{
    protected final ProcessingEnvironment processingEnv;
    protected final ProcessingLogger logger;
    protected final ProcessingVerifier verifier;

    protected ProcessorInterceptor(ProcessingEnvironment processingEnv, ProcessingLogger logger,
        ProcessingVerifier verifier)
    {
        this.processingEnv = processingEnv;
        this.logger = logger;
        this.verifier = verifier;
    }

    /**
     * This method is called for each round of the annotation processor. All rules stated in {@link javax.annotation.processing.Processor#process(Set, RoundEnvironment)} apply to this method
     *
     * @param set The set of annotations that are present in the round
     * @param roundEnv The round environment
     * @param processor the next element of the chain. If you want the processing to continue, call {@link Processable#process(Set, RoundEnvironment)} on this element
     * @return {@link javax.annotation.processing.Processor#process(Set, RoundEnvironment)}
     */
    public abstract boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv, Processable processor);

    /**
     * Creates a processor from that interceptor, that <i>might</i> calls the given {@link Processable}.
     * @param next a processable this interceptor might call
     * @return a callable proessable that will execute the whole chain
     */
    public final Processable processable(Processable next)
    {
        return (annotations, roundEnv) -> process(annotations, roundEnv, next);
    }
}
