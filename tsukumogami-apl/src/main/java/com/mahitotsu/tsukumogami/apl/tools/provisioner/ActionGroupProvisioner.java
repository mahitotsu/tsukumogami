package com.mahitotsu.tsukumogami.apl.tools.provisioner;

import java.util.Map;

public interface ActionGroupProvisioner {

    String[] activateActionGroups(String... actionGroupNames);

    Map<String,String>[] listAvailableActionGroups();
}
