package io.github.cjustinn.specialisedworkforce2.services;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import javax.annotation.Nullable;
import javax.script.ScriptEngine;
import java.util.Map;
import java.util.Set;

public class EvaluationService {
    public static @Nullable Context context = null;
    public static double evaluate(String expression) {
        if (context == null) {
            context = Context.create();
        }

        Value result = context.eval("js", expression);
        return result.asDouble();
    }

    public static String populateEquation(String rawEquation, Map<String, Object> values) {
        String equation = rawEquation;

        for (Map.Entry<String, Object> value : values.entrySet()) {
            equation = equation.replaceAll("\\{" + value.getKey() + "\\}", String.valueOf(value.getValue()));
        }

        return equation;
    }
}
