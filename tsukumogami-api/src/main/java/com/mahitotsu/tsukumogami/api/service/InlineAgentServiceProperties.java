package com.mahitotsu.tsukumogami.api.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Data;
import software.amazon.awssdk.services.bedrockagentruntime.model.ActionGroupExecutor;
import software.amazon.awssdk.services.bedrockagentruntime.model.AgentActionGroup;
import software.amazon.awssdk.services.bedrockagentruntime.model.CustomControlMethod;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionDefinition;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionSchema;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeInlineAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.ParameterDetail;

@Data
public class InlineAgentServiceProperties {

    @Data
    public static class ParameterBean {
        private String name;
        private String type;
        private String description;
    }

    @Data
    public static class FunctionBean {
        private String name;
        private String description;
        private List<ParameterBean> parameters;
    }

    @Data
    public static class ActionGroupBean {
        private String name;
        private String description;
        private List<FunctionBean> functions;
    }

    private String foundationModel;
    private String instruction;
    private List<ActionGroupBean> actionGroups;

    protected InvokeInlineAgentRequest.Builder initInvokeInlineAgentRequestBuilder() {
        return InvokeInlineAgentRequest.builder()
                .foundationModel(this.foundationModel)
                .instruction(this.instruction)
                .actionGroups(this.toActionGroups(this.actionGroups));
    }

    private Collection<AgentActionGroup> toActionGroups(final Collection<ActionGroupBean> actionGroupBean) {
        return this.actionGroups.stream().map(ag -> AgentActionGroup.builder()
                .actionGroupExecutor(ActionGroupExecutor.fromCustomControl(CustomControlMethod.RETURN_CONTROL))
                .actionGroupName(ag.getName())
                .description(ag.getDescription())
                .functionSchema(this.toFunctionSchema(ag.getFunctions()))
                .build()).toList();
    }

    private FunctionSchema toFunctionSchema(final Collection<FunctionBean> functions) {
        return FunctionSchema.builder().functions(functions.stream().map(f -> FunctionDefinition.builder()
                .name(f.getName())
                .description(f.getDescription())
                .parameters(this.toParameters(f.getParameters()))
                .build()).toList())
                .build();
    }

    private Map<String, ParameterDetail> toParameters(final Collection<ParameterBean> parameters) {
        return parameters.stream().collect(Collectors.toMap(p -> p.getName(), p -> ParameterDetail.builder()
                .type(p.getType())
                .description(p.getDescription())
                .build()));
    }
}
