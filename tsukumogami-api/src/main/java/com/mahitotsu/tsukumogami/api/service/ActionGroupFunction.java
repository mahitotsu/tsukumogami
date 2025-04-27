package com.mahitotsu.tsukumogami.api.service;

import java.util.List;

import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionParameter;

public interface ActionGroupFunction {

    String invoke(List<FunctionParameter> parameters) throws Exception;
}
