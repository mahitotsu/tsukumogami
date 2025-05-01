package com.mahitotsu.tsukumogami.apl.tools;

import java.util.Collection;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

@Data
@AllArgsConstructor
@Setter(AccessLevel.NONE)
public class ActionGroupProperties {

    @Data
    @AllArgsConstructor
    @Setter(AccessLevel.NONE)
    public static class FunctionProperties {
        private final String name;
        private final String description;
        private final Collection<ParameterProperties> parameters;
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

}
