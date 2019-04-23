/*
 * Copyright 2016-2019 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.gchq.gaffer.operation.analytic;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

import uk.gov.gchq.gaffer.commonutil.CommonConstants;
import uk.gov.gchq.gaffer.commonutil.Required;
import uk.gov.gchq.gaffer.exception.SerialisationException;
import uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.gaffer.named.operation.NamedOperation;
import uk.gov.gchq.gaffer.named.operation.ParameterDetail;
import uk.gov.gchq.gaffer.operation.Operation;
import uk.gov.gchq.gaffer.operation.OperationChain;
import uk.gov.gchq.gaffer.operation.OperationChainDAO;
import uk.gov.gchq.gaffer.operation.Operations;
import uk.gov.gchq.koryphe.Since;
import uk.gov.gchq.koryphe.Summary;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

@JsonPropertyOrder(value = {"class", "operationName", "description", "score", "operations"}, alphabetic = true)
@Since("1.0.0")
@Summary("Adds a new analytic")
public class AddAnalyticOperation implements Operation, Operations<Operation> {
    @Required
    private String operations;
    private String operationName;
    private String description;
    private List<String> readAccessRoles = new ArrayList<>();
    private List<String> writeAccessRoles = new ArrayList<>();
    private boolean overwriteFlag = false;
    private Map<String, ParameterDetail> parameters;
    private Map<String, String> options;
    private Integer score;
    private Map<String, String> metaData;
    private Map<String, String> outputType;

    private static final String CHARSET_NAME = CommonConstants.UTF_8;

    public boolean isOverwriteFlag() {
        return overwriteFlag;
    }

    public void setOverwriteFlag(final boolean overwriteFlag) {
        this.overwriteFlag = overwriteFlag;
    }

    @JsonIgnore
    public void setOperation(final String operation) {
        this.operations = operation;
    }

    @JsonSetter("operation")
    public void setOperation(final JsonNode opNode) {
        this.operations = opNode.toString();
    }

    @JsonIgnore
    public String getOperationAsString() {
        return operations;
    }

    @JsonGetter("operation")
    public JsonNode getOperationAsJsonNode() {
        try {
            return JSONSerialiser.getJsonNodeFromString(operations);
        } catch (final SerialisationException se) {
            throw new IllegalArgumentException(se.getMessage());
        }
    }

    @JsonIgnore
    public void setOperation(final Operation operation) {
        try {
            this.operations = new String(JSONSerialiser.serialise(operation), Charset.forName(CHARSET_NAME));
        } catch (final SerialisationException se) {
            throw new IllegalArgumentException(se.getMessage());
        }
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(final String operationName) {
        this.operationName = operationName;
    }

    public List<String> getReadAccessRoles() {
        return readAccessRoles;
    }

    public void setReadAccessRoles(final List<String> readAccessRoles) {
        this.readAccessRoles = readAccessRoles;
    }

    public List<String> getWriteAccessRoles() {
        return writeAccessRoles;
    }

    public void setWriteAccessRoles(final List<String> writeAccessRoles) {
        this.writeAccessRoles = writeAccessRoles;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setParameters(final Map<String, ParameterDetail> parameters) {
        this.parameters = parameters;
    }

    public Map<String, ParameterDetail> getParameters() {
        return parameters;
    }

    @JsonSetter("metaData")
    public void setMetaData(final Map<String, String> metaData) {
        this.metaData = metaData;
    }

    public Map<String, String> getMetaData() {
        return metaData;
    }

    @JsonSetter("outputType")
    public void setOutputType(final Map<String, String> outputType) {
        this.outputType = outputType;
    }

    public Map<String, String> getOutputType() {
        return outputType;
    }

    @Override
    public AddAnalyticOperation shallowClone() {
        return new AddAnalyticOperation.Builder()
                .operation(operations)
                .name(operationName)
                .description(description)
                .readAccessRoles(readAccessRoles.toArray(new String[readAccessRoles.size()]))
                .writeAccessRoles(writeAccessRoles.toArray(new String[writeAccessRoles.size()]))
                .overwrite(overwriteFlag)
                .parameters(parameters)
                .metaData(metaData)
                .outputType(outputType)
                .options(options)
                .score(score)
                .build();
    }

    @Override
    public Map<String, String> getOptions() {
        return options;
    }

    @Override
    public void setOptions(final Map<String, String> options) {
        this.options = options;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(final Integer score) {
        this.score = score;
    }

    /**
     * @return a list of the operations in the operation chain resolved using the default parameters.
     */
    @Override
    @JsonIgnore
    public Collection<Operation> getOperations() {
        return getOperationsWithDefaultParams();
    }

    @Override
    public void updateOperations(final Collection<Operation> operations) {
        // ignore - Analytic operations will be updated when run instead
    }

    private Collection<Operation> getOperationsWithDefaultParams() {
        String opStringWithDefaults = operations;

        if (null != parameters) {
            for (final Map.Entry<String, ParameterDetail> parameterDetailPair : parameters.entrySet()) {
                String paramKey = parameterDetailPair.getKey();

                try {
                    opStringWithDefaults = opStringWithDefaults.replace(buildParamNameString(paramKey),
                            new String(JSONSerialiser.serialise(parameterDetailPair.getValue().getDefaultValue(), CHARSET_NAME), CHARSET_NAME));
                } catch (final SerialisationException | UnsupportedEncodingException e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            }
        }

        Operation op;
        if (StringUtils.isEmpty(opStringWithDefaults)) {
            op = null;
        } else {
            try {
                op = JSONSerialiser.deserialise(opStringWithDefaults.getBytes(CHARSET_NAME), OperationChainDAO.class);
            } catch (final Exception e) {
                try {
                    op = JSONSerialiser.deserialise(opStringWithDefaults.getBytes(CHARSET_NAME), NamedOperation.class);
                } catch (final Exception f) {
                    op = null;
                }
            }
        }
        final Collection<Operation> operations;
        if (op instanceof Operations && !(op instanceof NamedOperation)) {
            operations = ((OperationChain) op).getOperations();
            return operations;
        } else if (op instanceof NamedOperation) {
            operations = ((NamedOperation) op).getOperations();
            operations.add(op);
            return operations;
        } else {
            return Arrays.asList(op);
        }
    }

    private String buildParamNameString(final String paramKey) {
        return "\"${" + paramKey + "}\"";
    }

    public static class Builder extends BaseBuilder<AddAnalyticOperation, AddAnalyticOperation.Builder> {
        public Builder() {
            super(new AddAnalyticOperation());
        }

        public AddAnalyticOperation.Builder operation(final String opString) {
            _getOp().setOperation(opString);
            return _self();
        }

        public AddAnalyticOperation.Builder operation(final Operation op) {
            _getOp().setOperation(op);
            return _self();
        }

        public AddAnalyticOperation.Builder name(final String name) {
            _getOp().setOperationName(name);
            return _self();
        }

        public AddAnalyticOperation.Builder description(final String description) {
            _getOp().setDescription(description);
            return _self();
        }

        public AddAnalyticOperation.Builder readAccessRoles(final String... roles) {
            Collections.addAll(_getOp().getReadAccessRoles(), roles);
            return _self();
        }

        public AddAnalyticOperation.Builder writeAccessRoles(final String... roles) {
            Collections.addAll(_getOp().getWriteAccessRoles(), roles);
            return _self();
        }

        public AddAnalyticOperation.Builder parameters(final Map<String, ParameterDetail> parameters) {
            _getOp().setParameters(parameters);
            return _self();
        }

        public AddAnalyticOperation.Builder parameter(final String name, final ParameterDetail detail) {
            Map<String, ParameterDetail> parameters = _getOp().getParameters();
            if (isNull(parameters)) {
                parameters = new HashMap<>();
                _getOp().setParameters(parameters);
            }
            parameters.put(name, detail);
            return _self();
        }

        public AddAnalyticOperation.Builder metaData(final Map<String, String> metaData) {
            _getOp().setMetaData(metaData);
            return _self();
        }

        public AddAnalyticOperation.Builder outputType(final Map<String, String> outputType) {
            _getOp().setOutputType(outputType);
            return _self();
        }

        public AddAnalyticOperation.Builder overwrite(final boolean overwriteFlag) {
            _getOp().setOverwriteFlag(overwriteFlag);
            return _self();
        }

        public AddAnalyticOperation.Builder overwrite() {
            return overwrite(true);
        }

        public AddAnalyticOperation.Builder score(final Integer score) {
            _getOp().setScore(score);
            return _self();
        }
    }

}
