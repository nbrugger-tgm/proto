module niton.proto {
  requires com.palantir.javapoet;
  requires java.compiler;
  requires org.apache.commons.lang3;
  requires static org.jetbrains.annotations;

  exports com.niton.compile.processor;
  exports com.niton.compile.verify;
}