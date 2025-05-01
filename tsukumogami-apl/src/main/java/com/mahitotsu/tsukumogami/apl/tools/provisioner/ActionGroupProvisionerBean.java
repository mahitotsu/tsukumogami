package com.mahitotsu.tsukumogami.apl.tools.provisioner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
                        指定された名称の中で有効化できないツールがあった場合、エラーが返ります。
                        有効化に成功した場合は有効化されたActionGroupの名称の配列をJSON文字列で返します。
                        """,
                        Arrays.asList(new ParameterProperties("actionGroupNames", "ARRAY", true, """
                                有効化するActionGroupの名前。複数指定可能です。
                                """))),
                new FunctionProperties("listAvailableActionGroups", """
                        利用可能なツールの名称を含む配列を返します。返されたツールは有効化してエージェントから利用可能です。
                        返される配列の要素はActionGroupの名称と説明です。
                        利用可能なActionGroupがない場合は空の配列が返ります。
                        """, null)),
                ActionGroupProvisioner.class);

        this.actionGroupRegistry.put(this.getName(), this);
        if (actionGroupRegistry != null) {
            actionGroupRegistry.stream().forEach(ag -> this.actionGroupRegistry.put(ag.getName(), ag));
        }

        this.activeActionGroupNames.add(this.getName());
    }

    private Map<String, ActionGroupProperties> actionGroupRegistry = new HashMap<>();

    private Set<String> activeActionGroupNames = new HashSet<>();

    public String[] activateActionGroups(final String... actionGroupNames) {

        final Collection<String> agNames = Arrays.asList(actionGroupNames);
        if (this.actionGroupRegistry.keySet().containsAll(agNames) == false) {
            throw new IllegalStateException(
                    "Failed to activate the one of the specified actionGroup. names: " + agNames);
        }

        this.activeActionGroupNames.clear();
        this.activeActionGroupNames.add(this.getName());
        this.activeActionGroupNames.addAll(agNames);
        return this.activeActionGroupNames.toArray(new String[this.activeActionGroupNames.size()]);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String>[] listAvailableActionGroups() {
        return this.actionGroupRegistry.values().stream().map(ag -> Map.of(ag.getName(), ag.getDescription()))
                .toArray(i -> new Map[i]);
    }

    public Collection<AgentActionGroup> getActiveActionGroups() {

        final Collection<AgentActionGroup> actionGroups = new ArrayList<>();

        for (final String actionGroupName : this.activeActionGroupNames) {
            final ActionGroupProperties agProps = this.actionGroupRegistry.get(actionGroupName);
            if (agProps == null) {
                throw new IllegalStateException(
                        "The specified ActionGroup is activated, but the tool is not found. actionGroup: "
                                + actionGroupName);
            }
            if (actionGroupName.equals(this.getName())) {
                actionGroups.add(this.toAgentActionGroup(this));
            } else {
                actionGroups.add(this.toAgentActionGroup(agProps));
            }
        }
        return actionGroups;
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
                        .functions(agProps.getFunctions() == null ? Collections.emptyList()
                                : agProps.getFunctions().stream().map(f -> this.toFunctionDefinition(f)).toList())
                        .build())
                .build();
    }

    private FunctionDefinition toFunctionDefinition(final FunctionProperties fnProps) {

        return FunctionDefinition.builder()
                .name(fnProps.getName())
                .description(fnProps.getDescription())
                .parameters(fnProps.getParameters() == null ? Collections.emptyMap()
                        : fnProps.getParameters().stream()
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
