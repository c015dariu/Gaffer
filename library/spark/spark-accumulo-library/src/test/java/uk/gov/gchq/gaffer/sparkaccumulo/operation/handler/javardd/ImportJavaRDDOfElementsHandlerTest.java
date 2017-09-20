/*
 * Copyright 2017 Crown Copyright
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
package uk.gov.gchq.gaffer.sparkaccumulo.operation.handler.javardd;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import uk.gov.gchq.gaffer.commonutil.CommonConstants;
import uk.gov.gchq.gaffer.commonutil.CommonTestConstants;
import uk.gov.gchq.gaffer.commonutil.TestGroups;
import uk.gov.gchq.gaffer.commonutil.TestPropertyNames;
import uk.gov.gchq.gaffer.data.element.Edge;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.data.element.Entity;
import uk.gov.gchq.gaffer.graph.Graph;
import uk.gov.gchq.gaffer.graph.GraphConfig;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.spark.operation.javardd.GetJavaRDDOfAllElements;
import uk.gov.gchq.gaffer.spark.operation.javardd.ImportJavaRDDOfElements;
import uk.gov.gchq.gaffer.sparkaccumulo.operation.handler.AbstractGetRDDHandler;
import uk.gov.gchq.gaffer.sparkaccumulo.operation.handler.SparkSessionProvider;
import uk.gov.gchq.gaffer.user.User;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ImportJavaRDDOfElementsHandlerTest {
    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder(CommonTestConstants.TMP_DIRECTORY);


    @Test
    public void checkImportJavaRDDOfElements() throws OperationException, IOException, InterruptedException {
        final Graph graph1 = new Graph.Builder()
                .config(new GraphConfig.Builder()
                        .graphId("graphId")
                        .build())
                .addSchema(getClass().getResourceAsStream("/schema/elements.json"))
                .addSchema(getClass().getResourceAsStream("/schema/types.json"))
                .addSchema(getClass().getResourceAsStream("/schema/serialisation.json"))
                .storeProperties(getClass().getResourceAsStream("/store.properties"))
                .build();

        final List<Element> elements = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final Entity entity = new Entity.Builder()
                    .group(TestGroups.ENTITY)
                    .vertex("" + i)
                    .build();

            final Edge edge1 = new Edge.Builder()
                    .group(TestGroups.EDGE)
                    .source("" + i)
                    .dest("B")
                    .directed(false)
                    .property(TestPropertyNames.COUNT, 2)
                    .build();

            final Edge edge2 = new Edge.Builder()
                    .group(TestGroups.EDGE)
                    .source("" + i)
                    .dest("C")
                    .directed(false)
                    .property(TestPropertyNames.COUNT, 4)
                    .build();

            elements.add(edge1);
            elements.add(edge2);
            elements.add(entity);
        }
        final User user = new User();

        final JavaSparkContext sparkContext = SparkSessionProvider.getJavaSparkContext();

        // Create Hadoop configuration and serialise to a string
        final Configuration configuration = new Configuration();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        configuration.write(new DataOutputStream(baos));
        final String configurationString = new String(baos.toByteArray(), CommonConstants.UTF_8);
        final String outputPath = testFolder.getRoot().getAbsolutePath() + "/output";
        final String failurePath = testFolder.getRoot().getAbsolutePath() + "/failure";

        final JavaRDD<Element> elementJavaRDD = sparkContext.parallelize(elements);
        final ImportJavaRDDOfElements addRdd = new ImportJavaRDDOfElements.Builder()
                .javaSparkContext(sparkContext)
                .input(elementJavaRDD)
                .option("outputPath", outputPath)
                .option("failurePath", failurePath)
                .build();
        graph1.execute(addRdd, user);

        // Check all elements were added
        final GetJavaRDDOfAllElements rddQuery = new GetJavaRDDOfAllElements.Builder()
                .javaSparkContext(sparkContext)
                .option(AbstractGetRDDHandler.HADOOP_CONFIGURATION_KEY, configurationString)
                .build();

        final JavaRDD<Element> rdd = graph1.execute(rddQuery, user);
        if (rdd == null) {
            fail("No rdd returned");
        }
        final Set<Element> results = new HashSet<>(rdd.collect());
        assertEquals(elements.size(), results.size());
    }
}
