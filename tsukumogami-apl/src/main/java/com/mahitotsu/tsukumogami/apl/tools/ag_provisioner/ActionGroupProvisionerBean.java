package com.mahitotsu.tsukumogami.apl.tools.ag_provisioner;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.mahitotsu.tsukumogami.apl.tools.ActionGroupProperties;

import software.amazon.awssdk.services.bedrockagentruntime.model.ActionGroupExecutor;
import software.amazon.awssdk.services.bedrockagentruntime.model.AgentActionGroup;
import software.amazon.awssdk.services.bedrockagentruntime.model.CustomControlMethod;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionDefinition;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionSchema;
import software.amazon.awssdk.services.bedrockagentruntime.model.ParameterDetail;
import software.amazon.awssdk.services.bedrockagentruntime.model.ParameterType;

public class ActionGroupProvisionerBean extends ActionGroupProperties implements ActionGroupProvisioner {

    public ActionGroupProvisionerBean(final Collection<ActionGroupProperties> actionGroupRegistry) {
        super("ActionGroupProvisioner", """
                エージェントが利用可能なツールをActionGroupとして利用できるように有効化する操作を提供します。
                """, Arrays.asList(
                new FunctionProperties("activateActionGroups", """
                        指定された名称のActionGroupを利用できるように有効化します。
                        """,
                        Arrays.asList(new ParameterProperties("actionGroupNames", "ARRAY", true, """
                                有効化するActionGroupの名前。複数指定可能です。
                                """)))),
                ActionGroupProvisioner.class);

        if (actionGroupRegistry != null) {
            actionGroupRegistry.stream().forEach(ag -> this.actionGroupRegistry.put(ag.getName(), ag));
        }
    }

    private Map<String, ActionGroupProperties> actionGroupRegistry = new HashMap<>();

    private Set<String> activeActionGroupNames = new HashSet<>();

    public String[] activateActionGroups(final String... actionGroupNames) {

        this.activeActionGroupNames.clear();
        this.activeActionGroupNames.addAll(Arrays.asList(actionGroupNames));
        return this.activeActionGroupNames.toArray(new String[this.activeActionGroupNames.size()]);
    }

    public Collection<AgentActionGroup> getActiveActionGroups() {

        return this.activeActionGroupNames.stream().map(n -> this.actionGroupRegistry.get(n)).filter(n -> n != null)
                .map(ag -> this.toAgentActionGroup(ag)).toList();
    }

    public ActionGroupProperties getActionGroupBean(final String actionGroupName) {

        return this.activeActionGroupNames.contains(actionGroupName) ? this.actionGroupRegistry.get(actionGroupName)
                : null;
    }

    public AgentActionGroup toAgentActionGroup(final ActionGroupProperties agProps) {

        return AgentActionGroup.builder()
                .actionGroupName(agProps.getName())
                .actionGroupExecutor(ActionGroupExecutor.fromCustomControl(CustomControlMethod.RETURN_CONTROL))
                .description(agProps.getDescription())
                .functionSchema(FunctionSchema.builder()
                        .functions(agProps.getFunctions().stream().map(f -> this.toFunctionDefinition(f)).toList())
                        .build())
                .build();
    }

    private FunctionDefinition toFunctionDefinition(final FunctionProperties fnProps) {

        return FunctionDefinition.builder()
                .name(fnProps.getName())
                .description(fnProps.getDescription())
                .parameters(fnProps.getParameters().stream()
                        .collect(Collectors.toMap(p -> p.getName(), p -> this.toParameterDetails(p))))
                .build();
    }

    private ParameterDetail toParameterDetails(final ParameterProperties pmProps) {

        return ParameterDetail.builder()
                .type(ParameterType.fromValue(pmProps.getType().toLowerCase()))
                .required(pmProps.isRequired())
                .description(pmProps.getDescription())
                .build();
    }
}
