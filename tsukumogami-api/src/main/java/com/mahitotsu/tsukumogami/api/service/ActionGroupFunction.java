package com.mahitotsu.tsukumogami.api.service;

import java.util.List;
import java.util.function.Function;

import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionParameter;

public interface ActionGroupFunction extends Function<List<FunctionParameter>, String> {
    
}
