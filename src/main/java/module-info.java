module niton.proto {
  requires com.squareup.javapoet;
  requires java.compiler;
  requires org.apache.commons.lang3;
  requires static org.jetbrains.annotations;

  exports com.niton.compile.processor;
  exports com.niton.compile.verify;
}