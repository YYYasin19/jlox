package com.jlox.lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
    private LoxClass cls;
    private final Map<String, Object> fields = new HashMap<>();

    LoxInstance(LoxClass cls) {
        this.cls = cls;
    }

    @Override
    public String toString() {
        return String.format("<%s instance>", cls.name);
    }

    Object get(Token attributeName) {
        if (fields.containsKey(attributeName.lexeme)) {
            return fields.get(attributeName.lexeme);
        }

        LoxFunction method = this.cls.findMethod(attributeName.lexeme);

        if (method != null)
            return method.bind(this);

        throw new RuntimeError(attributeName, String.format("Unknown property '%s' on %s", attributeName.lexeme, this));
    }

    void set(Token attributeName, Object value) {
        fields.put(attributeName.lexeme, value);
    }
}