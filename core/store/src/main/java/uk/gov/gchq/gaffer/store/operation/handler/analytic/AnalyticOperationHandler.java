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

package uk.gov.gchq.gaffer.store.operation.handler.analytic;

import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.operation.analytic.AnalyticOperation;
import uk.gov.gchq.gaffer.store.Context;
import uk.gov.gchq.gaffer.store.Store;
import uk.gov.gchq.gaffer.store.operation.handler.OutputOperationHandler;

/**
 * Operation handler for {@link AnalyticOperation}. Analytic operations are resolved by
 * the {@code AnalyticOperationResolver} {@code GraphHook}.
 * If this handler is invoked then it means the analytic operation could not be resolved.
 */
public class AnalyticOperationHandler implements OutputOperationHandler<AnalyticOperation<?, Object>, Object> {
    @Override
    public Object doOperation(final AnalyticOperation<?, Object> operation, final Context context, final Store store) throws OperationException {
        throw new UnsupportedOperationException("The analytic operation: " + operation.getOperationName() + " was not found.");
    }
}
