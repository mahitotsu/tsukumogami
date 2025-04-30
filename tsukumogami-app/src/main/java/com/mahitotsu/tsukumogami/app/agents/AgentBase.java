package com.mahitotsu.tsukumogami.app.agents;

import java.util.HashMap;
import java.util.Map;

import com.mahitotsu.tsukumogami.app.tools.ToolBase;

public class AgentBase extends AgentProperties {

    private final Map<String, ToolBase> actionGroupToolMap = new HashMap<>();

    protected void registerToolAsActionGroup(final String actionGroupName, final ToolBase toolBase) {

        if (actionGroupName == null || toolBase == null) {
            throw new IllegalArgumentException("The actionGroupName and toolBase must not be null.");
        }
        if (this.actionGroupToolMap.containsKey(actionGroupName)) {
            throw new IllegalStateException("The specified actionGroupName is already used.");
        }

        this.actionGroupToolMap.put(actionGroupName, toolBase);
    }
}
