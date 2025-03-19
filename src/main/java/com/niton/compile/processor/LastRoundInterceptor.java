package com.niton.compile.processor;

import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Set;
/**
 * This processor interceptor should be used as base of a processor to bypass
 * the bug <a href="https://bugs.openjdk.java.net/browse/JDK-8256826">JDK-8256826</a> in OpenJDK javac.
 * <p>
 * It archives this by creating a fake last round. As a byproduct "strange classes" will be generated.
 * There is not much that one can do to prevent this. Residual class example <b>MyProcessor$jdk_8256826_bug$round5</b>
 * </p>
 *
 * @author Nils Brugger (u0eiuaw)
 */
class LastRoundInterceptor extends ProcessorInterceptor
{
    private static final String LAST_ROUND_BUG = "jdk_8256826_bug";
    private int round;
    private boolean processingOver;
    private static int instances = 0;
    private final int instance = instances++;

    protected LastRoundInterceptor(ProcessingEnvironment processingEnv, ProcessingLogger logger,
        ProcessingVerifier verifier)
    {
        super(processingEnv, logger, verifier);
    }

    @Override
    public final boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv, Processable processor)
    {
        if (roundEnv.processingOver() || processingOver)
        {
            logger.info("Skip processing of javac last round");
            return false;//the javac last round is not allowed to be used
        }
        var bugfileName = String.format("%s%d$%s$round%d", getClass().getSimpleName(),instance, LAST_ROUND_BUG, round);
        var fakeLastRound = isFakeLastRound(roundEnv);

        if (!fakeLastRound)
        {
            writeDummyClass(bugfileName,processingEnv);
        }
        round++;
        processingOver = fakeLastRound;
        return processor.process(set, new FakeEndRoundEnv(roundEnv, fakeLastRound));
    }

    private void writeDummyClass(String bugfileName, ProcessingEnvironment processingEnv)
    {
        logger.info("[%s] Write file %s to prevent javac bug JDK-8256826", getClass().getSimpleName(), bugfileName);
        try
        {
            //Creating this file will prevent javac from creating the errornous "lastRound".
            JavaFile.builder(getClass().getPackageName(), TypeSpec.classBuilder(bugfileName)
                    .addModifiers(Modifier.FINAL).build())
                .build()
                .writeTo(processingEnv.getFiler());
        }
        catch (IOException e)
        {
            logger.fail(e);
        }
    }

    private boolean isFakeLastRound(RoundEnvironment roundEnv)
    {
        return roundEnv.getRootElements()
            .stream()
            .map(Element::getSimpleName)
            .allMatch(n -> n.toString().contains(LAST_ROUND_BUG));
    }


    private static class FakeEndRoundEnv implements RoundEnvironment
    {
        private final RoundEnvironment roundEnv;
        private final boolean fakeLastRound;

        public FakeEndRoundEnv(RoundEnvironment roundEnv, boolean fakeLastRound)
        {
            this.roundEnv = roundEnv;
            this.fakeLastRound = fakeLastRound;
        }

        @Override
        public boolean processingOver()
        {
            return fakeLastRound;
        }

        @Override
        public boolean errorRaised()
        {
            return roundEnv.errorRaised();
        }

        @Override
        public Set<? extends Element> getRootElements()
        {
            return roundEnv.getRootElements();
        }

        @Override
        public Set<? extends Element> getElementsAnnotatedWith(TypeElement typeElement)
        {
            return roundEnv.getElementsAnnotatedWith(typeElement);
        }

        @Override
        public Set<? extends Element> getElementsAnnotatedWith(Class<? extends Annotation> aClass)
        {
            return roundEnv.getElementsAnnotatedWith(aClass);
        }
    }
}
