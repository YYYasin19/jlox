package com.jlox.lox;

/*
 * Is thrown inside a function that then unwinds up to the call statement
 */
class Return extends RuntimeException {
  final Object value;

  Return(Object value) {
    super(null, null, false, false);
    this.value = value;
  }
}
