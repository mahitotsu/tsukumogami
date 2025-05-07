package com.mahitotsu.tsukumogami.apl.tools.jsexec;

import org.graalvm.polyglot.Context;
import org.springframework.stereotype.Component;

import com.mahitotsu.tsukumogami.apl.tools.ActionGroupProperties;

@Component
public class JavaScriptExecutorBean extends ActionGroupProperties implements JavaScriptExecutor {

    public JavaScriptExecutorBean() {
        super(JavaScriptExecutor.class);
        this.engine = Context.create();
    }

    private Context engine;

    @Override
    public Object execute(final String script) {
        try {
            return this.engine.eval("js", script).asString();
        } catch (Exception e) {
            throw new IllegalStateException("An error occurred while running the script.", e);
        }
    }
}
