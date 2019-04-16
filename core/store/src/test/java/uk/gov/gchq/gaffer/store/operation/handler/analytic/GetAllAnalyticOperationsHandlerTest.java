/*
 * Copyright 2018-2019 Crown Copyright
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Iterables;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import uk.gov.gchq.gaffer.cache.CacheServiceLoader;
import uk.gov.gchq.gaffer.commonutil.iterable.CloseableIterable;
import uk.gov.gchq.gaffer.operation.analytic.AddAnalyticOperation;
import uk.gov.gchq.gaffer.operation.analytic.AnalyticOperationDetail;
import uk.gov.gchq.gaffer.operation.analytic.GetAllAnalyticOperations;
import uk.gov.gchq.gaffer.store.Context;
import uk.gov.gchq.gaffer.store.Store;
import uk.gov.gchq.gaffer.store.StoreProperties;
import uk.gov.gchq.gaffer.store.operation.handler.analytic.cache.AnalyticOperationCache;
import uk.gov.gchq.gaffer.user.User;
import java.util.ArrayList;

public class GetAllAnalyticOperationsHandlerTest {
    private final AnalyticOperationCache cache = new AnalyticOperationCache();
    private final AddAnalyticOperationHandler addAnalyticOperationHandler = new AddAnalyticOperationHandler(cache);
    private final GetAllAnalyticOperationHandler getAllAnalyticOperationsHandler = new GetAllAnalyticOperationHandler(cache);
    private Context context = new Context(new User.Builder()
            .userId(User.UNKNOWN_USER_ID)
            .build());

    private final AnalyticOperationDetail expectedOperationDetailWithInputType = new AnalyticOperationDetail.Builder()
            .operationName("exampleOp")
            .inputType("uk.gov.gchq.gaffer.data.element.Element[]")
            .creatorId(User.UNKNOWN_USER_ID)
            .operation("{\"operations\":[{\"class\":\"uk.gov.gchq.gaffer.operation.impl.add.AddElements\",\"skipInvalidElements\":false,\"validate\":true}]}")
            .readers(new ArrayList<>())
            .writers(new ArrayList<>())
            .build();

    private final AnalyticOperationDetail expectedOperationDetailWithoutInputType = new AnalyticOperationDetail.Builder()
            .operationName("exampleOp")
            .inputType(null)
            .creatorId(User.UNKNOWN_USER_ID)
            .operation("{\"operations\":[{\"class\":\"uk.gov.gchq.gaffer.store.operation.GetSchema\",\"compact\":false}]}")
            .readers(new ArrayList<>())
            .writers(new ArrayList<>())
            .build();

    private Store store = mock(Store.class);

    @AfterClass
    public static void tearDown() {
        CacheServiceLoader.shutdown();
    }

    @Before
    public void before() {
        given(store.getProperties()).willReturn(new StoreProperties());
        StoreProperties properties = new StoreProperties();
        properties.set("gaffer.cache.service.class", "uk.gov.gchq.gaffer.cache.impl.HashMapCacheService");
        CacheServiceLoader.initialise(properties.getProperties());
    }

    @Test
    public void shouldReturnAnalyticOperationWithInputType() throws Exception {
        // Given
        AddAnalyticOperation addAnalyticOperation = new AddAnalyticOperation.Builder()
                .name(expectedOperationDetailWithInputType.getOperationName())
                .description(expectedOperationDetailWithInputType.getDescription())
                .operation(expectedOperationDetailWithInputType.getOperationWithDefaultParams())
                .build();

        addAnalyticOperationHandler.doOperation(addAnalyticOperation, context, store);

        // When
        CloseableIterable<AnalyticOperationDetail> allAnalyticOperationsList = getAllAnalyticOperationsHandler.doOperation(new GetAllAnalyticOperations(), context, store);

        // Then
        assertEquals(1, Iterables.size(allAnalyticOperationsList));
        assertTrue(Iterables.contains(allAnalyticOperationsList, expectedOperationDetailWithInputType));
    }

    @Test
    public void shouldReturnAnalyticOperationWithNoInputType() throws Exception {
        // Given
        AddAnalyticOperation addAnalyticOperation = new AddAnalyticOperation.Builder()
                .name(expectedOperationDetailWithoutInputType.getOperationName())
                .description(expectedOperationDetailWithoutInputType.getDescription())
                .operation(expectedOperationDetailWithoutInputType.getOperationWithDefaultParams())
                .build();

        addAnalyticOperationHandler.doOperation(addAnalyticOperation, context, store);

        // When
        CloseableIterable<AnalyticOperationDetail> allAnalyticOperationsList = getAllAnalyticOperationsHandler.doOperation(new GetAllAnalyticOperations(), context, store);

        // Then
        assertEquals(1, Iterables.size(allAnalyticOperationsList));
        assertTrue(Iterables.contains(allAnalyticOperationsList, expectedOperationDetailWithoutInputType));
    }
}
