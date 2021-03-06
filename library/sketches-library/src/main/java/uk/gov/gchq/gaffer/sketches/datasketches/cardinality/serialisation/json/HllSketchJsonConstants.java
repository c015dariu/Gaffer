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
package uk.gov.gchq.gaffer.sketches.datasketches.cardinality.serialisation.json;

public final class HllSketchJsonConstants {
    public static final String MODULE_NAME = "HllSketchJsonSerialiser";
    public static final int DEFAULT_LOG_K = 10;
    public static final String BYTES = "bytes";
    public static final String CARDINALITY = "cardinality";
    public static final String LOG_K = "logK";
    public static final String VALUES = "values";


    private HllSketchJsonConstants() {
        // private to prevent this class being instantiated. All methods are static and should be called directly.
    }
}
