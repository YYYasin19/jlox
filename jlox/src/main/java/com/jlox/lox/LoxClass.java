package com.jlox.lox;

import java.util.List;
import java.util.Map;

class LoxClass implements LoxCallable {
    final String name;
    private final Map<String, LoxFunction> methods;

    LoxClass(String name, Map<String, LoxFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    @Override
    public String toString() {
        return String.format("<%s cls>", name);
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        LoxInstance instance = new LoxInstance(this);

        LoxFunction init = findMethod("init");
        if (init != null) {
            // bind (i.e. add 'this') and run now
            init.bind(instance).call(interpreter, args);
        }

        return instance;
    }

    @Override
    public int arity() {
        LoxFunction init = findMethod("init");
        if (init == null)
            return 0; // default init does not take any arguments

        return init.arity();
    }

    LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        return null;
    }
}