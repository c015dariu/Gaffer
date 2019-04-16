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
package uk.gov.gchq.gaffer.traffic;

import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;

import uk.gov.gchq.gaffer.commonutil.StreamUtil;
import uk.gov.gchq.gaffer.commonutil.iterable.CloseableIterable;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.data.element.comparison.ElementPropertyComparator;
import uk.gov.gchq.gaffer.data.element.function.ElementFilter;
import uk.gov.gchq.gaffer.data.element.function.ElementTransformer;
import uk.gov.gchq.gaffer.data.element.id.EntityId;
import uk.gov.gchq.gaffer.data.elementdefinition.view.GlobalViewElementDefinition;
import uk.gov.gchq.gaffer.data.elementdefinition.view.View;
import uk.gov.gchq.gaffer.data.elementdefinition.view.ViewElementDefinition;
import uk.gov.gchq.gaffer.data.generator.CsvGenerator;
import uk.gov.gchq.gaffer.exception.SerialisationException;
import uk.gov.gchq.gaffer.graph.Graph;
import uk.gov.gchq.gaffer.graph.Graph.Builder;
import uk.gov.gchq.gaffer.graph.GraphConfig;
import uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.gaffer.named.operation.AddNamedOperation;
import uk.gov.gchq.gaffer.named.operation.NamedOperation;
import uk.gov.gchq.gaffer.named.operation.ParameterDetail;
import uk.gov.gchq.gaffer.operation.OperationChain;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.operation.analytic.AddAnalyticOperation;
import uk.gov.gchq.gaffer.operation.analytic.AnalyticOperation;
import uk.gov.gchq.gaffer.operation.analytic.GetAllAnalyticOperations;
import uk.gov.gchq.gaffer.operation.data.EntitySeed;
import uk.gov.gchq.gaffer.operation.graph.SeededGraphFilters;
import uk.gov.gchq.gaffer.operation.impl.add.AddElements;
import uk.gov.gchq.gaffer.operation.impl.compare.Sort;
import uk.gov.gchq.gaffer.operation.impl.generate.GenerateElements;
import uk.gov.gchq.gaffer.operation.impl.get.GetAdjacentIds;
import uk.gov.gchq.gaffer.operation.impl.get.GetAllElements;
import uk.gov.gchq.gaffer.operation.impl.get.GetElements;
import uk.gov.gchq.gaffer.operation.impl.output.ToCsv;
import uk.gov.gchq.gaffer.operation.impl.output.ToSet;
import uk.gov.gchq.gaffer.traffic.generator.RoadTrafficStringElementGenerator;
import uk.gov.gchq.gaffer.types.function.FreqMapExtractor;
import uk.gov.gchq.gaffer.user.User;
import uk.gov.gchq.koryphe.impl.predicate.IsMoreThan;
import uk.gov.gchq.koryphe.impl.predicate.range.InDateRangeDual;
import uk.gov.gchq.koryphe.predicate.PredicateMap;

import java.io.IOException;
import java.util.Map;

/**
 * This class runs simple java queries against the road traffic graph.
 */
public class Queries {

    public static void main(final String[] args) throws OperationException, IOException {
        new Queries().run();
    }

    private void run() throws OperationException, IOException {
        final User user = new User("user01");
        final Graph graph = createGraph(user);

        // Get the schema
        //System.out.println(graph.getSchema().toString());

        // Full example
        //runFullExample(graph, user);
        runAnalyticExample(graph, user);
    }

    private void runFullExample(final Graph graph, final User user) throws OperationException {
        final OperationChain<Iterable<? extends String>> opChain = new OperationChain.Builder()
                .first(new GetAdjacentIds.Builder()
                        .input(new EntitySeed("South West"))
                        .view(new View.Builder()
                                .edge("RegionContainsLocation")
                                .build())
                        .build())
                .then(new GetAdjacentIds.Builder()
                        .view(new View.Builder()
                                .edge("LocationContainsRoad")
                                .build())
                        .build())
                .then(new ToSet<>())
                .then(new GetAdjacentIds.Builder()
                        .view(new View.Builder()
                                .edge("RoadHasJunction")
                                .build())
                        .build())
                .then(new GetElements.Builder()
                        .view(new View.Builder()
                                .globalElements(new GlobalViewElementDefinition.Builder()
                                        .groupBy()
                                        .build())
                                .entity("JunctionUse", new ViewElementDefinition.Builder()
                                        .preAggregationFilter(new ElementFilter.Builder()
                                                .select("startDate", "endDate")
                                                .execute(new InDateRangeDual.Builder()
                                                        .start("2000/01/01")
                                                        .end("2001/01/01")
                                                        .build())
                                                .build())
                                        .postAggregationFilter(new ElementFilter.Builder()
                                                .select("countByVehicleType")
                                                .execute(new PredicateMap<>("BUS", new IsMoreThan(1000L)))
                                                .build())

                                        // Extract the bus count out of the frequency map and store in transient property "busCount"
                                        .transientProperty("busCount", Long.class)
                                        .transformer(new ElementTransformer.Builder()
                                                .select("countByVehicleType")
                                                .execute(new FreqMapExtractor("BUS"))
                                                .project("busCount")
                                                .build())
                                        .build())
                                .build())
                        .inOutType(SeededGraphFilters.IncludeIncomingOutgoingType.OUTGOING)
                        .build())
                .then(new Sort.Builder()
                        .comparators(new ElementPropertyComparator.Builder()
                                .groups("JunctionUse")
                                .property("busCount")
                                .reverse(true)
                                .build())
                        .resultLimit(2)
                        .deduplicate(true)
                        .build())
                // Convert the result entities to a simple CSV in format: Junction,busCount.
                .then(new ToCsv.Builder()
                        .generator(new CsvGenerator.Builder()
                                .vertex("Junction")
                                .property("busCount", "Bus Count")
                                .build())
                        .build())
                .build();

        final Iterable<? extends String> results = graph.execute(opChain, user);

        System.out.println("Full example results:");
        for (final String result : results) {
            System.out.println(result);
        }
    }

    private void runAnalyticExample(final Graph graph, final User user) throws OperationException {
        final String fullExampleOpChain = "{\n" +
                "  \"operations\" : [ {\n" +
                "    \"class\" : \"uk.gov.gchq.gaffer.operation.impl.get.GetAdjacentIds\",\n" +
                "    \"view\" : {\n" +
                "      \"edges\" : {\n" +
                "        \"RegionContainsLocation\" : { }\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"class\" : \"uk.gov.gchq.gaffer.operation.impl.get.GetAdjacentIds\",\n" +
                "    \"view\" : {\n" +
                "      \"edges\" : {\n" +
                "        \"LocationContainsRoad\" : { }\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"class\" : \"uk.gov.gchq.gaffer.operation.impl.output.ToSet\"\n" +
                "  }, {\n" +
                "    \"class\" : \"uk.gov.gchq.gaffer.operation.impl.get.GetAdjacentIds\",\n" +
                "    \"view\" : {\n" +
                "      \"edges\" : {\n" +
                "        \"RoadHasJunction\" : { }\n" +
                "      }\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"class\" : \"uk.gov.gchq.gaffer.operation.impl.get.GetElements\",\n" +
                "    \"view\" : {\n" +
                "      \"entities\" : {\n" +
                "        \"JunctionUse\" : {\n" +
                "          \"properties\" : [\"${vehicle}\"],\n" +
                "          \"preAggregationFilterFunctions\" : [ {\n" +
                "            \"selection\" : [ \"startDate\", \"endDate\" ],\n" +
                "            \"predicate\" : {\n" +
                "              \"class\" : \"uk.gov.gchq.koryphe.impl.predicate.range.InDateRangeDual\",\n" +
                "              \"start\" : \"2000/01/01\",\n" +
                "              \"end\" : \"2001/01/01\"\n" +
                "            }\n" +
                "          } ],\n" +
                "          \"transientProperties\" : {\n" +
                "            \"${vehicle}\" : \"Long\"\n" +
                "          },\n" +
                "          \"transformFunctions\" : [ {\n" +
                "            \"selection\" : [ \"countByVehicleType\" ],\n" +
                "            \"function\" : {\n" +
                "              \"class\" : \"uk.gov.gchq.gaffer.types.function.FreqMapExtractor\",\n" +
                "              \"key\" : \"${vehicle}\"\n" +
                "            },\n" +
                "            \"projection\" : [ \"${vehicle}\" ]\n" +
                "          } ]\n" +
                "        }\n" +
                "      },\n" +
                "      \"globalElements\" : [ {\n" +
                "        \"groupBy\" : [ ]\n" +
                "      } ]\n" +
                "    },\n" +
                "    \"includeIncomingOutGoing\" : \"OUTGOING\"\n" +
                "  }, {\n" +
                "    \"class\" : \"uk.gov.gchq.gaffer.operation.impl.compare.Sort\",\n" +
                "    \"comparators\" : [ {\n" +
                "      \"class\" : \"uk.gov.gchq.gaffer.data.element.comparison.ElementPropertyComparator\",\n" +
                "      \"property\" : \"${vehicle}\",\n" +
                "      \"groups\" : [ \"JunctionUse\" ],\n" +
                "      \"reversed\" : true\n" +
                "    } ],\n" +
                "    \"deduplicate\" : true,\n" +
                "    \"resultLimit\" : \"${result-limit}\"\n" +
                "  }, {\n" +
                "    \"class\" : \"uk.gov.gchq.gaffer.operation.impl.If\",\n" +
                "    \"condition\" : \"${to-csv}\",\n" +
                "    \"then\" : {\n" +
                "        \"class\" : \"uk.gov.gchq.gaffer.operation.impl.output.ToCsv\",\n" +
                "        \"elementGenerator\" : {\n" +
                "          \"class\" : \"uk.gov.gchq.gaffer.data.generator.CsvGenerator\",\n" +
                "          \"fields\" : {\n" +
                "            \"VERTEX\" : \"Junction\",\n" +
                "            \"${vehicle}\" : \"${vehicle}\"\n" +
                "          },\n" +
                "          \"constants\" : { },\n" +
                "          \"quoted\" : false,\n" +
                "          \"commaReplacement\" : \" \"\n" +
                "        },\n" +
                "        \"includeHeader\" : true\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
        final Map<String, ParameterDetail> fullExampleParams = Maps.newHashMap();
        fullExampleParams.put("vehicle", new ParameterDetail.Builder()
                .defaultValue("BUS")
                .description("The type of vehicle: HGVR3, BUS, HGVR4, AMV, HGVR2, HGVA3, PC, HGVA3, PC, HGCA5, HGVA6, CAR, HGV, WM2, LGV")
                .valueClass(String.class)
                .required(false)
                .build());
        fullExampleParams.put("result-limit", new ParameterDetail.Builder()
                .defaultValue(2)
                .description("The maximum number of junctions to return")
                .valueClass(Integer.class)
                .required(false)
                .build());
        fullExampleParams.put("to-csv", new ParameterDetail.Builder()
                .defaultValue(false)
                .description("Enable this parameter to convert the results to a simple CSV in the format: Junction, Count")
                .valueClass(Boolean.class)
                .required(false)
                .build());
        final AddNamedOperation addFullExampleNamedOperation = new AddNamedOperation.Builder()
                .name("frequent-vehicles-in-region")
                .description("Finds the junctions in a region with the most of an individual vehicle (e.g BUS, CAR) in the year 2000. The input is the region.")
                .overwrite(true)
                .parameters(fullExampleParams)
                .operationChain(fullExampleOpChain)
                .build();

        graph.execute(addFullExampleNamedOperation, user);
        final Map<String, Object> paramMap = Maps.newHashMap();
        paramMap.put("result-limit", 5);
        final Map<String, Object> paramMap2 = Maps.newHashMap();
        paramMap2.put("result-limit", 2);
        final Map<String, String> metaData = Maps.newHashMap();
        metaData.put("iconURL", "pic.jpg");
        final Map<String, String> outputMap = Maps.newHashMap();
        outputMap.put("output", "table");

        final NamedOperation runExampleNamedOperation = new NamedOperation.Builder<EntityId, CloseableIterable<? extends Element>>()
                .name("frequent-vehicles-in-region")
                .parameters(paramMap)
                .build();

        final GetAllElements getElements = new GetAllElements.Builder()
                .build();

        final GetAllAnalyticOperations getAna = new GetAllAnalyticOperations.Builder()
                .build();

        final AddAnalyticOperation addAnalyticOperation = new AddAnalyticOperation.Builder()
                .name("analyticTest")
                .operation("{\n" +
                        "   \"class\": \"uk.gov.gchq.gaffer.named.operation.NamedOperation\",\n" +
                        "   \"operationName\": \"frequent-vehicles-in-region\",\n" +
                        "   \"parameters\": { \"result-limit\": 5 }\n" +
                        "}")
                .overwrite()
                .metaData(metaData)
                .outputType(outputMap)
                .build();

        graph.execute(addAnalyticOperation, user);
        graph.execute(getAna, user);

        final AnalyticOperation runAnalyticOperation = new AnalyticOperation.Builder<EntityId, CloseableIterable<? extends Element>>()
                .name("analyticTest")
                .parameters(paramMap2)
                .build();

        final OperationChain operationChain = new OperationChain.Builder()
                .first(getElements)
                .then(runAnalyticOperation)
                .build();

        Iterable<? extends String> results = (Iterable<? extends String>) graph.execute(operationChain, user);

        try {
            System.out.println(new String(JSONSerialiser.serialise(results, true)));
        } catch (SerialisationException e) {
            e.printStackTrace();
        }


    }

    private Graph createGraph(final User user) throws IOException, OperationException {
        final Graph graph = new Builder()
                .config(new GraphConfig.Builder()
                        .graphId("roadTraffic")
                        .build())
                .addSchemas(StreamUtil.openStreams(ElementGroup.class, "schema"))
                .storeProperties(StreamUtil.openStream(getClass(), "accumulo/store.properties"))
                .build();

        final OperationChain<Void> populateChain = new OperationChain.Builder()
                .first(new GenerateElements.Builder<String>()
                        .input(IOUtils.readLines(StreamUtil.openStream(getClass(), "roadTrafficSampleData.csv")))
                        .generator(new RoadTrafficStringElementGenerator())
                        .build())
                .then(new AddElements.Builder()
                        .skipInvalidElements(false)
                        .build())
                .build();
        graph.execute(populateChain, user);

        return graph;
    }
}
