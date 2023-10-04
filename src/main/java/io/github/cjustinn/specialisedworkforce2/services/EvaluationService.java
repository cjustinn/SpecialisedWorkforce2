package io.github.cjustinn.specialisedworkforce2.services;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.Map;
import java.util.Set;

public class EvaluationService {
    public static double evaluate(String expression) {
        try (Context context = Context.create()){
            Value result = context.eval("js", expression);
            return result.asDouble();
        }
    }

    public static String populateEquation(String rawEquation, Map<String, Object> values) {
        String equation = rawEquation;

        for (Map.Entry<String, Object> value : values.entrySet()) {
            equation = equation.replaceAll("\\{" + value.getKey() + "\\}", String.valueOf(value.getValue()));
        }

        return equation;
    }
}
