package com.mahitotsu.tsukumogami.app.tools;

import java.util.List;

import lombok.Data;

@Data
public class ToolProperties {

    @Data
    public static class Function {
        private String name;
        private String description;
        private List<Parameter> parameters;
    }

    @Data
    public static class Parameter {
        private String name;
        private String description;
        private String type;
        private boolean required = false;
    }

    private String description;
    private List<Function> functions;
}
