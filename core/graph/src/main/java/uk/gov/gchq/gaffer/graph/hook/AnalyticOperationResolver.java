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

package uk.gov.gchq.gaffer.graph.hook;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import uk.gov.gchq.gaffer.named.operation.NamedOperation;
import uk.gov.gchq.gaffer.named.operation.cache.exception.CacheOperationFailedException;
import uk.gov.gchq.gaffer.operation.Operation;
import uk.gov.gchq.gaffer.operation.OperationChain;
import uk.gov.gchq.gaffer.operation.Operations;
import uk.gov.gchq.gaffer.operation.analytic.AnalyticOperation;
import uk.gov.gchq.gaffer.operation.analytic.AnalyticOperationDetail;
import uk.gov.gchq.gaffer.operation.io.Input;
import uk.gov.gchq.gaffer.store.Context;
import uk.gov.gchq.gaffer.store.operation.handler.analytic.cache.AnalyticOperationCache;
import uk.gov.gchq.gaffer.user.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link GraphHook} to resolve analytic operations.
 */
@JsonPropertyOrder(alphabetic = true)
public class AnalyticOperationResolver implements GraphHook {
    private final AnalyticOperationCache cache;
    private AnalyticOperation anaOp;

    public AnalyticOperationResolver() {
        this(new AnalyticOperationCache());
    }

    public AnalyticOperationResolver(final AnalyticOperationCache cache) {
        this.cache = cache;
    }

    @Override
    public void preExecute(final OperationChain<?> opChain, final Context context) {
        resolveAnalyticOperations(opChain, context.getUser());
    }

    @Override
    public <T> T postExecute(final T result, final OperationChain<?> opChain, final Context context) {
        return result;
    }

    @Override
    public <T> T onFailure(final T result, final OperationChain<?> opChain, final Context context, final Exception e) {
        return result;
    }

    private void resolveAnalyticOperations(final Operations<?> operations, final User user) {
        final List<Operation> updatedOperations = new ArrayList<>(operations.getOperations().size());
        for (final Operation operation : operations.getOperations()) {
            if (operation instanceof AnalyticOperation) {
                anaOp = (AnalyticOperation) operation;
                updatedOperations.addAll(resolveAnalyticOperation((AnalyticOperation) operation, user));
            } else {
                if (operation instanceof Operations) {
                    resolveAnalyticOperations(((Operations<?>) operation), user);
                    if (null != anaOp && operation instanceof NamedOperation) {
                        ((NamedOperation) operation).setParameters(anaOp.getParameters());
                    }
                }
                updatedOperations.add(operation);
            }
        }
        operations.updateOperations((List) updatedOperations);
    }

    private List<Operation> resolveAnalyticOperation(final AnalyticOperation analyticOp, final User user) {
        final AnalyticOperationDetail analyticOpDetail;
        try {
            analyticOpDetail = cache.getAnalyticOperation(analyticOp.getOperationName(), user);
        } catch (final CacheOperationFailedException e) {
            // Unable to find analytic operation - just return the original analytic operation
            return Collections.singletonList(analyticOp);
        }

        final OperationChain<?> analyticOperation = OperationChain.wrap(analyticOpDetail.getOperation(analyticOp.getParameters()));
        updateOperationInput(analyticOperation, analyticOp.getInput());

        // Call resolveAnalyticOperations again to check there are no nested analytic operations
        resolveAnalyticOperations(analyticOperation, user);
        return analyticOperation.getOperations();
    }

    /**
     * Injects the input of the AnalyticOperation into the first operation in the OperationChain. This is used when
     * chaining AnalyticOperations together.
     *
     * @param opChain the resolved operation chain
     * @param input   the input of the AnalyticOperation
     */
    private void updateOperationInput(final OperationChain<?> opChain, final Object input) {
        final Operation firstOp = opChain.getOperations().get(0);
        if (null != input && (firstOp instanceof Input) && null == ((Input) firstOp).getInput()) {
            ((Input) firstOp).setInput(input);
        }
    }
}
