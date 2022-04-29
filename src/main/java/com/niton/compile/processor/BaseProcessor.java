package com.niton.compile.processor;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.uncapitalize;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

/**
 * This should be used as the base for annotation processors, instead of {@link AbstractProcessor} since it provides
 * common functionality as well as a Javac bug fix {@link LastRoundInterceptor} and {@link #applyJavacBugWorkaround()}.
 * <p><i>
 * A difference from the {@link AbstractProcessor} the processing method is not called
 * {@link javax.annotation.processing.Processor#process(Set, RoundEnvironment)}
 * but {@link #performProcessing(Set, RoundEnvironment)}
 * </i></p>
 *
 * <h3>Javac bug</h3>
 * <p>
 * Said workaround is applied by default, if the bug should be fixed (looks like java 19 will be the according version)
 * the workaround can be disabled by overwriting {@link #applyJavacBugWorkaround()} to return false.
 *
 * <h3>Utility</h3>
 * The additional functionality includes logging (see {@link ProcessingLogger}) with {@link #logger} and verifications (see {@link #verifier}).
 *
 * <h3>Interceptors</h3>
 * Should you stumble uppon a bug you can add a custom {@link ProcessorInterceptor}. These interceptors work similar to
 * HttpInterceptors. Read more how to implement them in the {@link ProcessorInterceptor} documentation.
 * To apply an interceptor overwrite {@link #getInterceptors(ProcessingEnvironment, ProcessingLogger, ProcessingVerifier)}
 *
 * @author Nils Brugger (u0eiuaw)
 */
public abstract class BaseProcessor extends AbstractProcessor implements Processable
{
    /**
     * Use this to log & fail the compile process
     */
    protected ProcessingLogger logger;
    /**
     * Use this to verify elements (classes, fields, annotations, etc.)
     */
    protected ProcessingVerifier verifier;
    /**
     * The next processor step. This contains the whole interceptor chain.
     * Processing this will call all interceptors and at the end the processor itself is called.
     */
    private Processable endpoint;
    /**
     * If true a log entry will be created for each generated class.
     */
    protected boolean logClassWriting;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment)
    {
        return endpoint.process(annotations, roundEnvironment);
    }

    /**
     * Same as {@link #process(Set, RoundEnvironment)} with the difference that it runs after all interceptors.
     *
     * @see #process(Set, RoundEnvironment)
     */
    public abstract boolean performProcessing(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment);

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv)
    {
        super.init(processingEnv);
        logger = new ProcessingLogger(processingEnv);
        verifier = new ProcessingVerifier(processingEnv, logger);
        endpoint = this::performProcessing;
        applyInterceptors(processingEnv);
    }

    private void applyInterceptors(ProcessingEnvironment processingEnv)
    {
        if (applyJavacBugWorkaround())
            endpoint = new LastRoundInterceptor(processingEnv, logger, verifier).processable(endpoint);
        var interceptors = getInterceptors(processingEnv, logger, verifier);
        for (var interceptor : interceptors)
        {
            endpoint = interceptor.processable(endpoint);
        }
    }

    /**
     * @return interceptors that will run before {@link #performProcessing(Set, RoundEnvironment)}
     */
    protected List<ProcessorInterceptor> getInterceptors(ProcessingEnvironment processingEnv, ProcessingLogger logger,
        ProcessingVerifier verifier)
    {
        return List.of();
    }

    /**
     * @return if true the interceptor that prevents the bug <a href="https://bugs.openjdk.java.net/browse/JDK-8256826">JDK-8256826</a>  will be applied.
     */
    public boolean applyJavacBugWorkaround()
    {
        return true;
    }

    /**
     * Write the generated class to the output directory to be compiled.
     * errors will be propagated to the compiler.
     * <p>The class will also be annotated with {@link Generated}</p>
     *
     * @param pack the package name to write the class to
     * @param cls  the class name to write
     */
    protected void writeClass(String pack, TypeSpec cls)
    {
        try
        {
            cls = annotateGenerated(cls);
            var javaFile = JavaFile.builder(pack, cls).build();
            javaFile.writeTo(processingEnv.getFiler());
            if (logClassWriting)
                logger.info("Generated class: %s", cls.name);
        }
        catch (IOException e)
        {
            logger.fail("Failed to write class %s", cls.name);
            logger.fail("Exception: %s", e.getMessage());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            sw.flush();
            logger.fail("Trace: %s", sw.getBuffer());
        }
    }

    private TypeSpec annotateGenerated(TypeSpec cls)
    {
        return cls.toBuilder().addAnnotation(AnnotationSpec
            .builder(Generated.class)
            .addMember("value", "$S", this.getClass().getName())
            .addMember("date", "$S", ZonedDateTime.now().toString())
            .build()).build();
    }

    /**
     * Tries to convert a variable name to a valid java identifier.
     * <pre>
     *     foo-bar -> fooBar
     *     foo_bar -> fooBar
     *     a.b.c.foo-bar -> fooBar
     * </pre>
     *
     * @param name the name to derive the identifier from
     * @return the identifier
     */
    protected String getVariableName(String name)
    {
        return uncapitalize(getClassName(name));
    }

    /**
     * Tries to convert a generic name to a valid java class identifier.
     * <pre>
     *     foo-bar -> FooBar
     *     foo_bar -> FooBar
     *     a.b.c.foo-bar -> FooBar
     *  </pre>
     *
     * @param name a name that uses kebab case or snake case
     * @return the class identifier
     */
    protected String getClassName(String name)
    {
        String[] parts = name.split("\\.");
        parts = parts[parts.length - 1].split("[_-]");
        for (int i = 0; i < parts.length; i++)
        {
            parts[i] = capitalize(parts[i]);
        }

        return String.join("", parts);
    }
}
