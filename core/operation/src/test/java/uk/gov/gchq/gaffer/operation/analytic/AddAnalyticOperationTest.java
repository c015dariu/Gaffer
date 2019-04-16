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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Test;

import uk.gov.gchq.gaffer.commonutil.JsonAssert;
import uk.gov.gchq.gaffer.data.element.id.EntityId;
import uk.gov.gchq.gaffer.exception.SerialisationException;
import uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.gaffer.named.operation.NamedOperation;
import uk.gov.gchq.gaffer.named.operation.ParameterDetail;
import uk.gov.gchq.gaffer.operation.Operation;
import uk.gov.gchq.gaffer.operation.OperationChain;
import uk.gov.gchq.gaffer.operation.OperationChainDAO;
import uk.gov.gchq.gaffer.operation.OperationTest;
import uk.gov.gchq.gaffer.operation.data.EntitySeed;
import uk.gov.gchq.gaffer.operation.impl.get.GetAdjacentIds;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class AddAnalyticOperationTest extends OperationTest<AddAnalyticOperation> {
    public static final String USER = "User";
    private static final OperationChain OPERATION_CHAIN = new OperationChain.Builder().first(new GetAdjacentIds.Builder().input(new EntitySeed("seed")).build()).build();
    private static final Operation NAMED_OP = new NamedOperation.Builder().name("NamTest").build();

    @Override
    public void shouldJsonSerialiseAndDeserialise() {
        final AddAnalyticOperation obj = new AddAnalyticOperation.Builder()
                .operation(OPERATION_CHAIN)
                .description("Test Analytic Operation")
                .name("Test")
                .overwrite()
                .readAccessRoles(USER)
                .writeAccessRoles(USER)
                .score(0)
                .build();

        // When
        final byte[] json = toJson(obj);
        final AddAnalyticOperation deserialisedObj = fromJson(json);

        // Then
        JsonAssert.assertEquals(String.format("{\n" +
                "  \"class\" : \"uk.gov.gchq.gaffer.operation.analytic.AddAnalyticOperation\",\n" +
                "  \"operationName\" : \"Test\",\n" +
                "  \"description\" : \"Test Analytic Operation\",\n" +
                "  \"score\" : 0,\n" +
                "  \"operation\" : {\n" +
                "    \"class\" : \"uk.gov.gchq.gaffer.operation.OperationChain\",\n" +
                "    \"operations\" : [ {\n" +
                "      \"class\" : \"uk.gov.gchq.gaffer.operation.impl.get.GetAdjacentIds\",\n" +
                "      \"input\" : [ {\n" +
                "        \"class\" : \"uk.gov.gchq.gaffer.operation.data.EntitySeed\",\n" +
                "        \"vertex\" : \"seed\"\n" +
                "      } ]\n" +
                "    } ]\n" +
                "  },\n" +
                "  \"overwriteFlag\" : true,\n" +
                "  \"readAccessRoles\" : [ \"User\" ],\n" +
                "  \"writeAccessRoles\" : [ \"User\" ]\n" +
                "}"), new String(json));
        assertNotNull(deserialisedObj);
    }

    @Override
    public void builderShouldCreatePopulatedOperation() {
        AddAnalyticOperation AddAnalyticOperation = new AddAnalyticOperation.Builder()
                .operation(OPERATION_CHAIN)
                .description("Test Analytic Operation")
                .name("Test")
                .overwrite()
                .readAccessRoles(USER)
                .writeAccessRoles(USER)
                .build();
        String opChain = null;
        try {
            opChain = new String(JSONSerialiser.serialise(new OperationChainDAO<>(OPERATION_CHAIN.getOperations())));
        } catch (final SerialisationException e) {
            fail();
        }
        assertEquals("{\"class\":\"uk.gov.gchq.gaffer.operation.OperationChain\",\"operations\":[{\"class\":\"uk.gov.gchq.gaffer.operation.impl.get.GetAdjacentIds\",\"input\":[{\"class\":\"uk.gov.gchq.gaffer.operation.data.EntitySeed\",\"vertex\":\"seed\"}]}]}", AddAnalyticOperation.getOperationAsString());
        assertEquals("Test", AddAnalyticOperation.getOperationName());
        assertEquals("Test Analytic Operation", AddAnalyticOperation.getDescription());
        assertEquals(Collections.singletonList(USER), AddAnalyticOperation.getReadAccessRoles());
        assertEquals(Collections.singletonList(USER), AddAnalyticOperation.getWriteAccessRoles());
    }

    @Override
    public void shouldShallowCloneOperation() {
        // Given
        Map<String, ParameterDetail> parameters = new HashMap<>();
        parameters.put("testParameter", mock(ParameterDetail.class));

        Map<String, String> metaData = new HashMap<>();
        metaData.put("testHeader", "testString");

        Map<String, String> outputType = new HashMap<>();
        outputType.put("testOutputType", "testString");

        AddAnalyticOperation AddAnalyticOperation = new AddAnalyticOperation.Builder()
                .operation(OPERATION_CHAIN)
                .description("Test Analytic Operation")
                .name("Test")
                .overwrite(false)
                .readAccessRoles(USER)
                .writeAccessRoles(USER)
                .parameters(parameters)
                .metaData(metaData)
                .outputType(outputType)
                .score(2)
                .build();

        // When
        AddAnalyticOperation clone = AddAnalyticOperation.shallowClone();

        // Then
        assertNotSame(AddAnalyticOperation, clone);
        assertEquals("{\"class\":\"uk.gov.gchq.gaffer.operation.OperationChain\",\"operations\":[{\"class\":\"uk.gov.gchq.gaffer.operation.impl.get.GetAdjacentIds\",\"input\":[{\"class\":\"uk.gov.gchq.gaffer.operation.data.EntitySeed\",\"vertex\":\"seed\"}]}]}"
                , clone.getOperationAsString());
        assertEquals("Test", clone.getOperationName());
        assertEquals("Test Analytic Operation", clone.getDescription());
        assertEquals(2, (int) clone.getScore());
        assertFalse(clone.isOverwriteFlag());
        assertEquals(Collections.singletonList(USER), clone.getReadAccessRoles());
        assertEquals(Collections.singletonList(USER), clone.getWriteAccessRoles());
        assertEquals(parameters, clone.getParameters());
        assertEquals(metaData, clone.getMetaData());
        assertEquals(outputType, clone.getOutputType());
    }

    @Test
    public void shouldGetOperationsWithDefaultParameters() {
        // Given
        final AddAnalyticOperation AddAnalyticOperation = new AddAnalyticOperation.Builder()
                .operation("{\"operations\":[{\"class\": \"uk.gov.gchq.gaffer.operation.impl.get.GetAdjacentIds\", \"input\": [{\"vertex\": \"${testParameter}\", \"class\": \"uk.gov.gchq.gaffer.operation.data.EntitySeed\"}]}]}")
                .description("Test Analytic Operation")
                .name("Test")
                .overwrite(false)
                .readAccessRoles(USER)
                .writeAccessRoles(USER)
                .parameter("testParameter", new ParameterDetail.Builder()
                        .description("the seed")
                        .defaultValue("seed1")
                        .valueClass(String.class)
                        .required(false)
                        .build())
                .score(2)
                .build();

        // When
        Collection<Operation> operations = AddAnalyticOperation.getOperations();

        // Then
        assertEquals(
                Collections.singletonList(GetAdjacentIds.class),
                operations.stream().map(o -> o.getClass()).collect(Collectors.toList())
        );
        final GetAdjacentIds nestedOp = (GetAdjacentIds) operations.iterator().next();
        final List<? extends EntityId> input = Lists.newArrayList(nestedOp.getInput());
        assertEquals(Collections.singletonList(new EntitySeed("seed1")), input);
    }

    @Test
    public void shouldGetOperationsWhenNoDefaultParameter() {
        // Given
        final AddAnalyticOperation AddAnalyticOperation = new AddAnalyticOperation.Builder()
                .operation("{\"operations\":[{\"class\": \"uk.gov.gchq.gaffer.operation.impl.get.GetAdjacentIds\", \"input\": [{\"vertex\": \"${testParameter}\", \"class\": \"uk.gov.gchq.gaffer.operation.data.EntitySeed\"}]}]}")
                .description("Test Analytic Operation")
                .name("Test")
                .overwrite(false)
                .readAccessRoles(USER)
                .writeAccessRoles(USER)
                .parameter("testParameter", new ParameterDetail.Builder()
                        .description("the seed")
                        .valueClass(String.class)
                        .required(false)
                        .build())
                .score(2)
                .build();

        // When
        Collection<Operation> operations = AddAnalyticOperation.getOperations();

        // Then
        assertEquals(
                Collections.singletonList(GetAdjacentIds.class),
                operations.stream().map(o -> o.getClass()).collect(Collectors.toList())
        );
        final GetAdjacentIds nestedOp = (GetAdjacentIds) operations.iterator().next();
        final List<? extends EntityId> input = Lists.newArrayList(nestedOp.getInput());
        assertEquals(Collections.singletonList(new EntitySeed(null)), input);
    }

    @Test
    public void shouldRunNamedOperation() {
        // Given
        final AddAnalyticOperation AddAnalyticOperation = new AddAnalyticOperation.Builder()
                .operation(NAMED_OP)
                .description("Test Analytic Operation")
                .name("Test")
                .overwrite(false)
                .readAccessRoles(USER)
                .writeAccessRoles(USER)
                .parameter("testParameter", new ParameterDetail.Builder()
                        .description("the seed")
                        .valueClass(String.class)
                        .required(false)
                        .build())
                .score(2)
                .build();

        // When
        Collection<Operation> operations = AddAnalyticOperation.getOperations();

        // Then
        assertEquals(
                Collections.singletonList(NamedOperation.class),
                operations.stream().map(o -> o.getClass()).collect(Collectors.toList())
        );
        final NamedOperation nOp = (NamedOperation) operations.iterator().next();
        assertEquals(nOp.getClass(), NAMED_OP.getClass());
        System.out.println(nOp.toString());
    }

    @Override
    protected AddAnalyticOperation getTestObject() {
        return new AddAnalyticOperation();
    }

    @Override
    protected Set<String> getRequiredFields() {
        return Sets.newHashSet("operations");
    }
}
