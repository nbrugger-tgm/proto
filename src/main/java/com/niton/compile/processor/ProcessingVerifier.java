package com.niton.compile.processor;

import static java.lang.String.format;

import java.lang.annotation.Annotation;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import com.niton.compile.verify.ProcessingVerification;

/**
 * utility class for verifying elements (methods, annotations, fields, etc.)
 */
public class ProcessingVerifier
{
    private final ProcessingEnvironment env;
    private final ProcessingLogger log;

    public ProcessingVerifier(ProcessingEnvironment env, ProcessingLogger log)
    {
        this.env = env;
        this.log = log;
    }

    /**
     * verifies that the element is a class (not interface, enum, annotation, etc.)
     *
     * @param elem the element to verify
     * @return the verification, can be used to fail or warn.
     */
    public ProcessingVerification isClass(Element elem)
    {
        return new ProcessingVerification(
            log,
            e -> e.getKind() == ElementKind.CLASS,
            format("%s should [not] be a class", elem),
            elem
        );
    }

    /**
     * verifies that the element extends the given class
     *
     * @param element the class to verify
     * @param superClass the class that element should extend
     * @return the verification, can be used to fail or warn.
     */
    public ProcessingVerification doesExtend(Element element, Class<?> superClass)
    {
        return new ProcessingVerification(
            log,
            e -> doesElementExtendClass(e, superClass),
            format("%s should [not] extend %s", element, superClass),
            element
        );
    }

    /**
     * verifies that the element is a interface (not class, enum, annotation, etc.)
     * @param elem the element to verify
     * @return the verification, can be used to fail or warn.
     */
    public ProcessingVerification isInterface(Element elem)
    {
        return new ProcessingVerification(
            log,
            e -> e.getKind() == ElementKind.INTERFACE,
            format("%s should [not] be an interface", elem),
            elem
        );
    }

    /**
     * verifies that the element is an annotation
     * @param elem the element to verify
     * @return the verification, can be used to fail or warn.
     */
    public ProcessingVerification isAnnotation(Element elem){
        return new ProcessingVerification(
            log,
            e -> e.getKind() == ElementKind.ANNOTATION_TYPE,
            format("%s should [not] be an annotation", elem),
            elem
        );
    }

    /**
     * verifies that the element is an enum
     * @param elem the element to verify
     * @return the verification, can be used to fail or warn.
     */
    public ProcessingVerification isEnum(Element elem){
        return new ProcessingVerification(
            log,
            e -> e.getKind() == ElementKind.ENUM,
            format("%s should [not] be an enum", elem),
            elem
        );
    }

    /**
     * verifies that the element is a field
     * @param elem the element to verify
     * @return the verification, can be used to fail or warn.
     */
    public ProcessingVerification isField(Element elem){
        return new ProcessingVerification(
            log,
            e -> e.getKind() == ElementKind.FIELD,
            format("%s should [not] be a field", elem),
            elem
        );
    }

    /**
     * verifies that the element is annotated with the given annotation
     * @param elem the element to verify
     * @param annotation the annotation to check the presence of
     * @return the verification, can be used to fail or warn.
     */
    public ProcessingVerification isAnnotatedWith(Element elem, Class<? extends Annotation> annotation){
        return new ProcessingVerification(
            log,
            e -> e.getAnnotation(annotation) != null,
            format("%s should [not] be annotated with %s", elem, annotation),
            elem
        );
    }

    /**
     * verifies that the element does implement the given interface (not necessarily direct, the interface just needs to be in the inheritance hierarchy)
     * @param element the element to verify
     * @param iFace the interface to check the presence of
     * @return the verification, can be used to fail or warn.
     */
    public ProcessingVerification doesImplement(Element element, Class<?> iFace)
    {
        return new ProcessingVerification(
            log,
            e -> doesElementImplementClass(e, iFace),
            format("%s should [not] implement %s", element, iFace),
            element
        );
    }

    private boolean doesElementImplementClass(Element element, Class<?> iFace)
    {
        if (!(element instanceof TypeElement))
            return false;
        var iFaceType = env.getElementUtils().getTypeElement(iFace.getName()).asType();
        return env.getTypeUtils().isAssignable(element.asType(), iFaceType);
    }

    private boolean doesElementExtendClass(Element element, Class<?> superClass)
    {
        if (!(element instanceof TypeElement))
            return false;
        var superType = env.getElementUtils().getTypeElement(superClass.getName()).asType();
        return env.getTypeUtils().isSubtype(element.asType(), superType);
    }
}
