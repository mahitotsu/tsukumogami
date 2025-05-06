package com.mahitotsu.tsukumogami.apl.tools.provisioner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahitotsu.tsukumogami.apl.tools.ActionGroupProperties;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.services.bedrockagentruntime.model.ActionGroupExecutor;
import software.amazon.awssdk.services.bedrockagentruntime.model.AgentActionGroup;
import software.amazon.awssdk.services.bedrockagentruntime.model.ContentBody;
import software.amazon.awssdk.services.bedrockagentruntime.model.CustomControlMethod;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionDefinition;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionInvocationInput;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionParameter;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionResult;
import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionSchema;
import software.amazon.awssdk.services.bedrockagentruntime.model.InlineAgentReturnControlPayload;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvocationInputMember;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvocationResultMember;
import software.amazon.awssdk.services.bedrockagentruntime.model.ParameterDetail;
import software.amazon.awssdk.services.bedrockagentruntime.model.ParameterType;

@Component
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ActionGroupProvisionerBean extends ActionGroupProperties implements ActionGroupProvisioner {

    public ActionGroupProvisionerBean() {
        super(ActionGroupProvisioner.class);
    }

    @Autowired(required = false)
    private Collection<ActionGroupProperties> actionGroups;

    private Map<String, ActionGroupProperties> actionGroupRegistry = new HashMap<>();

    private final Set<String> activeActionGroupNames = new HashSet<>();

    @Autowired
    private ConversionService converter;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void setup() {

        if (this.actionGroups != null) {
            this.actionGroups.stream().forEach(ag -> this.actionGroupRegistry.put(ag.getName(), ag));
        }
        this.actionGroupRegistry.put(this.getName(), this);
        this.activeActionGroupNames.add(this.getName());
    }

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

    public ActionGroupProperties requireActionGroupBean(final String actionGroupName) {

        return Optional.ofNullable(this.getActionGroupBean(actionGroupName))
                .orElseThrow(() -> new NoSuchElementException(
                        "The specified actionGroup is not found. actionGroup="
                                + actionGroupName));
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

    public Collection<InvocationResultMember> buildInvocationResultMembers(
            final InlineAgentReturnControlPayload payload) {

        final Collection<InvocationResultMember> results = new ArrayList<>();
        for (final InvocationInputMember i : payload.invocationInputs()) {
            FunctionResult result;
            try {
                result = this.processReturnCotrol(i.functionInvocationInput());
            } catch (Exception e) {
                result = this.reportException(i.functionInvocationInput(), e);
                e.printStackTrace();
            }
            results.add(InvocationResultMember.builder().functionResult(result).build());
        }

        return results;
    }

    private FunctionResult reportException(final FunctionInvocationInput input, final Exception e) {

        return FunctionResult.builder()
                .actionGroup(input.actionGroup())
                .function(input.function())
                .responseBody(Collections.singletonMap("TEXT",
                        ContentBody.builder().body(e.getMessage()).build()))
                .build();
    }

    private FunctionResult processReturnCotrol(final FunctionInvocationInput input) throws Exception {

        final String actionGroup = input.actionGroup();
        final String function = input.function();
        final ActionGroupProperties tool = this.requireActionGroupBean(actionGroup);

        final int numOfArgs = input.parameters().size();
        final Method method = tool.getMethod(function);

        final Class<?>[] parameterTypes = method.getParameterTypes();
        final Object[] args = new Object[numOfArgs];
        final FunctionParameter[] params = new FunctionParameter[numOfArgs];
        input.parameters().stream().forEach(p -> params[tool.getParameterIndex(function, p.name())] = p);

        for (int i = 0; i < args.length; i++) {
            final FunctionParameter param = params[i];
            final String pv = param.value();
            Object value;
            switch (param.type()) {
                case "string":
                case "integer":
                case "boolean":
                    value = pv;
                    break;
                case "array":
                    value = Arrays.stream(pv.substring(1, pv.length() - 1).split(","))
                            .map(s -> s.trim())
                            .toArray(j -> new String[j]);
                    break;
                default:
                    throw new IllegalStateException("Unkonwn type is specified: " + param.type());
            }
            args[i] = this.converter.convert(value, parameterTypes[i]);
        }

        final Object result = method.invoke(tool, args);

        return FunctionResult.builder()
                .actionGroup(actionGroup)
                .function(function)
                .responseBody(Collections.singletonMap("TEXT",
                        ContentBody.builder().body(this.objectMapper.writeValueAsString(result))
                                .build()))
                .build();
    }
}
