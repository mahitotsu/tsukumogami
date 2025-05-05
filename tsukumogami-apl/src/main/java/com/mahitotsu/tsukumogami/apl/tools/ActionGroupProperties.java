package com.mahitotsu.tsukumogami.apl.tools;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

@Data
@AllArgsConstructor
@Setter(AccessLevel.NONE)
public class ActionGroupProperties {

    protected ActionGroupProperties(final Class<?> toolApiType) {

        this.toolApiType = toolApiType;

        final ActionGroupAttributes aga = toolApiType.getAnnotation(ActionGroupAttributes.class);
        this.name = aga.name();
        this.description = aga.description();

        this.functions = new ArrayList<>();
        final Method[] methods = toolApiType.getMethods();
        for (final Method m : methods) {
            final FunctionAttributes fa = AnnotationUtils.getAnnotation(m, FunctionAttributes.class);
            if (fa == null) {
                continue;
            }
            final String name = fa.name();
            final String description = fa.description();
            final int numOfArgs = m.getParameterTypes().length;
            final List<ParameterProperties> pps = new ArrayList<>(numOfArgs);
            for (int i = 0; i < numOfArgs; i++) {
                final MethodParameter mp = new MethodParameter(m, i);
                final ParameterAttributes pa = mp.getParameterAnnotation(ParameterAttributes.class);
                if (pa == null) {
                    continue;
                }
                final ParameterProperties pp = new ParameterProperties(pa.name(), pa.type(), pa.required(),
                        pa.description());
                pps.add(pp);
            }
            this.functions.add(new FunctionProperties(name, description, pps));
        }
    }

    @Data
    @AllArgsConstructor
    @Setter(AccessLevel.NONE)
    public static class FunctionProperties {
        private final String name;
        private final String description;
        private final List<ParameterProperties> parameters;
    }

    @Data
    @AllArgsConstructor
    @Setter(AccessLevel.NONE)
    public static class ParameterProperties {
        private final String name;
        private final String type;
        private final boolean required;
        private final String description;
    }

    private final String name;
    private final String description;
    private final Collection<FunctionProperties> functions;
    private final Class<?> toolApiType;

    public int getParameterIndex(final String functionName, final String parameterName) {

        final FunctionProperties fp = this.functions.stream().filter(f -> f.getName().equals(functionName)).findFirst()
                .orElse(null);
        if (fp != null) {
            for (int i = 0; i < fp.getParameters().size(); i++) {
                if (fp.getParameters().get(i).getName().equals(parameterName)) {
                    return i;
                }
            }
        }
        throw new NoSuchElementException("The specified parameter is not found. functionName: " + functionName
                + ", parameterName: " + parameterName);
    }
}
