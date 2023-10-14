package io.github.cjustinn.specialisedworkforce2.services;

import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;
import jdk.jshell.execution.LocalExecutionControlProvider;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvaluationService {
    public static @Nullable JShell context = null;
    public static double evaluate(String expression) {
        if (context == null) {
            context = JShell.builder().executionEngine(new LocalExecutionControlProvider(), new HashMap<>()).build();
        }

        double result = -1.0;
        List<SnippetEvent> snippets = context.eval(expression);

        for (SnippetEvent event : snippets) {
            if (event.status() == Snippet.Status.VALID) {
                result = Double.parseDouble(event.value());
                break;
            }
        }

        return result;
    }

    public static String populateEquation(String rawEquation, Map<String, Object> values) {
        String equation = rawEquation;

        for (Map.Entry<String, Object> value : values.entrySet()) {
            equation = equation.replaceAll("\\{" + value.getKey() + "\\}", String.valueOf(value.getValue()));
        }

        return equation;
    }
}
