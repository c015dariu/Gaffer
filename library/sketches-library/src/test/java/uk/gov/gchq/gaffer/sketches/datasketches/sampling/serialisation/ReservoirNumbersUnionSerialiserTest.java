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
package uk.gov.gchq.gaffer.sketches.datasketches.sampling.serialisation;

import com.yahoo.sketches.sampling.ReservoirItemsSketch;
import com.yahoo.sketches.sampling.ReservoirItemsUnion;
import org.junit.Test;
import uk.gov.gchq.gaffer.exception.SerialisationException;

import static org.junit.Assert.*;

public class ReservoirNumbersUnionSerialiserTest {
    private static final ReservoirNumbersUnionSerialiser SERIALISER = new ReservoirNumbersUnionSerialiser();

    @Test
    public void testSerialiseAndDeserialise() {
        final ReservoirItemsUnion<Number> union = ReservoirItemsUnion.getInstance(20);
        union.update(1L);
        union.update(2L);
        union.update(3L);
        testSerialiser(union);

        final ReservoirItemsUnion<Number> emptyUnion = ReservoirItemsUnion.getInstance(20);
        testSerialiser(emptyUnion);
    }

    private void testSerialiser(final ReservoirItemsUnion<Number> union) {
        final ReservoirItemsSketch<Number> result = union.getResult();
        final boolean resultIsNull = result == null;
        Number[] sample = new Number[]{};
        if (!resultIsNull) {
            sample = union.getResult().getSamples();
        }
        final byte[] unionSerialised;
        try {
            unionSerialised = SERIALISER.serialise(union);
        } catch (final SerialisationException exception) {
            fail("A SerialisationException occurred");
            return;
        }

        final ReservoirItemsUnion<Number> unionDeserialised;
        try {
            unionDeserialised = SERIALISER.deserialise(unionSerialised);
        } catch (final SerialisationException exception) {
            fail("A SerialisationException occurred");
            return;
        }
        final ReservoirItemsSketch<Number> deserialisedResult = unionDeserialised.getResult();
        if (deserialisedResult == null) {
            assertTrue(resultIsNull);
        } else {
            assertArrayEquals(sample, unionDeserialised.getResult().getSamples());
        }
    }

    @Test
    public void testCanHandleReservoirItemsUnion() {
        assertTrue(SERIALISER.canHandle(ReservoirItemsUnion.class));
        assertFalse(SERIALISER.canHandle(String.class));
    }
}
