package com.niton.compile;

import com.niton.compile.processor.BaseProcessor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

public final class ProcessingUtils {
    private ProcessingUtils(){}
    public static TypeMirror getSuperclass(Types types, TypeMirror mirror) {
        return types.directSupertypes(mirror).get(0);
    }
    public static TypeMirror getSuperclass(BaseProcessor processor, TypeMirror mirror) {
        return getSuperclass(processor.getProcessingEnvironment(), mirror);
    }
    public static TypeMirror getSuperclass(ProcessingEnvironment env, TypeMirror mirror) {
        return getSuperclass(env.getTypeUtils(), mirror);
    }
}
