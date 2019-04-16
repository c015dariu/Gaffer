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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import uk.gov.gchq.gaffer.commonutil.CommonConstants;
import uk.gov.gchq.gaffer.commonutil.ToStringBuilder;
import uk.gov.gchq.gaffer.exception.SerialisationException;
import uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.gaffer.named.operation.NamedOperation;
import uk.gov.gchq.gaffer.named.operation.ParameterDetail;
import uk.gov.gchq.gaffer.operation.Operation;
import uk.gov.gchq.gaffer.operation.OperationChain;
import uk.gov.gchq.gaffer.operation.OperationChainDAO;
import uk.gov.gchq.gaffer.user.User;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnalyticOperationDetail implements Serializable {

    private static final String CHARSET_NAME = CommonConstants.UTF_8;
    private String operationName;
    private String inputType;
    private String description;
    private String creatorId;
    private String operations;
    private List<String> readAccessRoles;
    private List<String> writeAccessRoles;
    private Map<String, ParameterDetail> parameters = Maps.newHashMap();
    private Map<String, String> options = Maps.newHashMap();
    private Map<String, String> metaData = Maps.newHashMap();
    private Map<String, String> outputType = Maps.newHashMap();
    private Integer score;

    public AnalyticOperationDetail() {
    }

    public AnalyticOperationDetail(final String operationName, final String description, final String userId,
                                   final String operations, final List<String> readers,
                                   final List<String> writers, final Map<String, ParameterDetail> parameters,
                                   final Integer score, final Map<String, String> options, final Map<String, String> metaData,
                                   final Map<String, String> outputType) {
        this(operationName, null, description, userId, operations, readers, writers, parameters, metaData, outputType, score, options);
    }

    public AnalyticOperationDetail(final String operationName, final String inputType, final String description, final String userId,
                                   final String operations, final List<String> readers,
                                   final List<String> writers, final Map<String, ParameterDetail> parameters,
                                   final Map<String, String> metaData, final Map<String, String> outputType, final Integer score, final Map<String, String> options) {
        if (null == operations) {
            throw new IllegalArgumentException("Operation Chain must not be empty");
        }
        if (null == operationName || operationName.isEmpty()) {
            throw new IllegalArgumentException("Operation Name must not be empty");
        }

        this.operationName = operationName;
        this.inputType = inputType;
        this.description = description;
        this.creatorId = userId;
        this.operations = operations;

        this.readAccessRoles = readers;
        this.writeAccessRoles = writers;
        this.parameters = parameters;
        this.metaData = metaData;
        this.outputType = outputType;
        this.score = score;
        this.options = options;
    }

    public String getOperationName() {
        return operationName;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(final String inputType) {
        this.inputType = inputType;
    }

    public String getDescription() {
        return description;
    }

    public String getOperations() {
        return operations;
    }

    public List<String> getReadAccessRoles() {
        return readAccessRoles;
    }

    public List<String> getWriteAccessRoles() {
        return writeAccessRoles;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public Map<String, ParameterDetail> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, ParameterDetail> parameters) {
        this.parameters = parameters;
    }

    public Integer getScore() {
        return score;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public Map<String, String> getMetaData() {
        return metaData;
    }

    public Map<String, String> getOutputType() {
        return outputType;
    }

    private String buildParamNameString(final String paramKey) {
        return "\"${" + paramKey + "}\"";
    }

    /**
     * Gets the OperationChain after adding in default values for any parameters. If a parameter
     * does not have a default, null is inserted.
     *
     * @return The {@link OperationChain}
     * @throws IllegalArgumentException if substituting the parameters fails
     */
    @JsonIgnore
    public Operation getOperationWithDefaultParams() {
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

        Operation opChain;
        try {
            opChain = JSONSerialiser.deserialise(opStringWithDefaults.getBytes(CHARSET_NAME), OperationChainDAO.class);
        } catch (final Exception e) {
            try {
                opChain = JSONSerialiser.deserialise(opStringWithDefaults.getBytes(CHARSET_NAME), NamedOperation.class);
            } catch (final Exception f) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        return opChain;
    }

    /**
     * Gets the OperationChain after adding in any provided parameters.
     *
     * @param executionParams the parameters for the {@link uk.gov.gchq.gaffer.operation.Operation} to be executed
     * @return The {@link OperationChain}
     * @throws IllegalArgumentException if substituting the parameters fails
     */
    public Operation getOperation(final Map<String, Object> executionParams) {
        String opStringWithParams = operations;

        // First check all the parameters supplied are expected parameter names
        if (null != parameters) {
            if (null != executionParams) {
                Set<String> paramDetailKeys = parameters.keySet();
                Set<String> paramKeys = executionParams.keySet();

                if (!paramDetailKeys.containsAll(paramKeys)) {
                    throw new IllegalArgumentException("Unexpected parameter name in AnalyticOperation");
                }
            }

            for (final Map.Entry<String, ParameterDetail> parameterDetailPair : parameters.entrySet()) {
                String paramKey = parameterDetailPair.getKey();
                try {
                    if (null != executionParams && executionParams.containsKey(paramKey)) {
                        Object paramObj = JSONSerialiser.deserialise(JSONSerialiser.serialise(executionParams.get(paramKey)), parameterDetailPair.getValue().getValueClass());

                        opStringWithParams = opStringWithParams.replace(buildParamNameString(paramKey),
                                new String(JSONSerialiser.serialise(paramObj, CHARSET_NAME), CHARSET_NAME));
                    } else if (!parameterDetailPair.getValue().isRequired()) {
                        opStringWithParams = opStringWithParams.replace(buildParamNameString(paramKey),
                                new String(JSONSerialiser.serialise(parameterDetailPair.getValue().getDefaultValue(), CHARSET_NAME), CHARSET_NAME));
                    } else {
                        throw new IllegalArgumentException("Missing parameter " + paramKey + " with no default");
                    }
                } catch (final SerialisationException | UnsupportedEncodingException e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            }
        }

        Operation opChain;

        try {
            opChain = JSONSerialiser.deserialise(opStringWithParams.getBytes(CHARSET_NAME), OperationChainDAO.class);
        } catch (final Exception e) {
            try {
                opChain = JSONSerialiser.deserialise(opStringWithParams.getBytes(CHARSET_NAME), NamedOperation.class);
            } catch (final Exception f) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        return opChain;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (null == obj || getClass() != obj.getClass()) {
            return false;
        }

        final AnalyticOperationDetail op = (AnalyticOperationDetail) obj;

        return new EqualsBuilder()
                .append(operationName, op.operationName)
                .append(inputType, op.inputType)
                .append(creatorId, op.creatorId)
                .append(operations, op.operations)
                .append(readAccessRoles, op.readAccessRoles)
                .append(writeAccessRoles, op.writeAccessRoles)
                .append(parameters, op.parameters)
                .append(metaData, op.metaData)
                .append(outputType, op.outputType)
                .append(score, op.score)
                .append(options, op.options)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(71, 3)
                .append(operationName)
                .append(inputType)
                .append(creatorId)
                .append(operations)
                .append(readAccessRoles)
                .append(writeAccessRoles)
                .append(parameters)
                .append(metaData)
                .append(outputType)
                .append(score)
                .append(options)
                .hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .appendSuper(super.toString())
                .append("inputType", inputType)
                .append("creatorId", creatorId)
                .append("operation", operations)
                .append("readAccessRoles", readAccessRoles)
                .append("writeAccessRoles", writeAccessRoles)
                .append("parameters", parameters)
                .append("metaData", metaData)
                .append("outputType", outputType)
                .append("score", score)
                .append("options", options)
                .toString();
    }

    public boolean hasReadAccess(final User user) {
        return hasAccess(user, readAccessRoles, null);
    }

    public boolean hasReadAccess(final User user, final String adminAuth) {
        return hasAccess(user, readAccessRoles, adminAuth);
    }

    public boolean hasWriteAccess(final User user) {
        return hasAccess(user, writeAccessRoles, null);
    }

    public boolean hasWriteAccess(final User user, final String adminAuth) {
        return hasAccess(user, writeAccessRoles, adminAuth);
    }

    private boolean hasAccess(final User user, final List<String> roles, final String adminAuth) {
        if (null != roles) {
            for (final String role : roles) {
                if (user.getOpAuths().contains(role)) {
                    return true;
                }
            }
        }
        if (StringUtils.isNotBlank(adminAuth)) {
            if (user.getOpAuths().contains(adminAuth)) {
                return true;
            }
        }
        return user.getUserId().equals(creatorId);
    }

    public static final class Builder {
        private String operationName;
        private String inputType;
        private String description;
        private String creatorId;
        private String op;
        private List<String> readers;
        private List<String> writers;
        private Map<String, ParameterDetail> parameters;
        private Map<String, String> metaData;
        private Map<String, String> outputType;
        private Integer score;
        private Map<String, String> options;

        public AnalyticOperationDetail.Builder creatorId(final String creatorId) {
            this.creatorId = creatorId;
            return this;
        }

        public AnalyticOperationDetail.Builder operationName(final String operationName) {
            this.operationName = operationName;
            return this;
        }

        public AnalyticOperationDetail.Builder inputType(final String inputType) {
            this.inputType = inputType;
            return this;
        }

        public AnalyticOperationDetail.Builder description(final String description) {
            this.description = description;
            return this;
        }

        public AnalyticOperationDetail.Builder operation(final String op) {

            this.op = op;
            return this;
        }

        public AnalyticOperationDetail.Builder operation(final OperationChain op) {
            try {
                this.op = new String(JSONSerialiser.serialise(op), Charset.forName(CHARSET_NAME));
            } catch (final SerialisationException se) {
                throw new IllegalArgumentException(se.getMessage());
            }

            return this;
        }


        public AnalyticOperationDetail.Builder parameters(final Map<String, ParameterDetail> parameters) {
            this.parameters = parameters;
            return this;
        }

        public AnalyticOperationDetail.Builder readers(final List<String> readers) {
            this.readers = readers;
            return this;
        }

        public AnalyticOperationDetail.Builder writers(final List<String> writers) {
            this.writers = writers;
            return this;
        }

        public AnalyticOperationDetail.Builder score(final Integer score) {
            this.score = score;
            return this;
        }

        public AnalyticOperationDetail.Builder options(final Map<String, String> options) {
            this.options = options;
            return this;
        }

        public AnalyticOperationDetail.Builder metaData(final Map<String, String> metaData) {
            this.metaData = metaData;
            return this;
        }

        public AnalyticOperationDetail.Builder outputType(final Map<String, String> outputType) {
            this.outputType = outputType;
            return this;
        }

        public AnalyticOperationDetail build() {
            return new AnalyticOperationDetail(operationName, inputType, description, creatorId, op, readers, writers, parameters, metaData, outputType, score, options);
        }
    }
}
