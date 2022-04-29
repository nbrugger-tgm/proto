module niton.proto {
  exports com.niton.compile.processor;
  exports com.niton.compile.verify;
  requires com.squareup.javapoet;
  requires java.compiler;
  requires org.apache.commons.lang3;
}