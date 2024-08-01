package com.jlox.lox;

import java.util.List;

class LoxClass implements LoxCallable {
    final String name;

    LoxClass(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("<%s cls>", name);
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        LoxInstance instance = new LoxInstance(this);
        return instance;
    }

    @Override
    public int arity() {
        return 0; // TODO
    }
}