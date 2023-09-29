package io.github.cjustinn.specialisedworkforce2.services;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class EvaluationService {
    public static double evaluate(String expression) {
        try (Context context = Context.create()){
            Value result = context.eval("js", expression);
            return result.asDouble();
        }
    }
}
