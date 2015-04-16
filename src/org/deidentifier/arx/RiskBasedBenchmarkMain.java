/*
 * Benchmark of risk-based anonymization in ARX 3.0.0
 * Copyright 2015 - Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.deidentifier.arx.RiskBasedBenchmarkSetup.BenchmarkDataset;

/**
 * Main benchmark class.
 * 
 * @author Fabian Prasser
 */
public class RiskBasedBenchmarkMain {

    /** Repetitions */
    private static final int REPETITIONS = 5;

    /**
     * Main entry point
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // Repeat for each data set
        for (BenchmarkDataset data : RiskBasedBenchmarkSetup.getDatasets()) {
            for (int i = 0; i < REPETITIONS; i++) {
                anonymize(data);
            }
        }
    }

    /**
     * Performs the experiments
     * 
     * @param dataset
     * @throws IOException
     */
    private static void anonymize(BenchmarkDataset dataset) throws IOException {
        Data data = RiskBasedBenchmarkSetup.getData(dataset);
        ARXConfiguration config = RiskBasedBenchmarkSetup.getConfiguration(dataset);
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        long time = System.currentTimeMillis();
        ARXResult result = anonymizer.anonymize(data, config);
        time = System.currentTimeMillis() - time;
        int searchSpaceSize = 1;
        for (String qi : data.getDefinition().getQuasiIdentifyingAttributes()) {
            searchSpaceSize *= data.getDefinition().getHierarchy(qi)[0].length;
        }
        Iterator<String[]> iter = result.getOutput().iterator();
        System.out.println(dataset);
        System.out.println(" - Time          : " + time + " [ms]");
        System.out.println(" - QIs           : " + data.getDefinition().getQuasiIdentifyingAttributes().size());
        System.out.println(" - Search space  : " + searchSpaceSize);
        System.out.println(" - Header        : " + Arrays.toString(iter.next()));
        System.out.println(" - Tuple         : " + Arrays.toString(getTuple(iter)));
        System.out.println(" - Suppressed    : " + getSuppressed(result.getOutput()));
        System.out.println(" - Transformation: " + Arrays.toString(result.getGlobalOptimum().getTransformation()));
        System.out.println(" - Total         : " + data.getHandle().getNumRows());
        System.out.println(" - Infoloss      : " + result.getGlobalOptimum().getMinimumInformationLoss().toString());
    }

    /**
     * Returns the first tuple that is not suppressed
     * 
     * @param iter
     * @return
     */
    private static String[] getTuple(Iterator<String[]> iter) {
        String[] tuple = iter.next();
        while (allEqual(tuple)) {
            tuple = iter.next();
        }
        return tuple;
    }

    /**
     * Returns whether all entries are equal
     * 
     * @param tuple
     * @return
     */
    private static boolean allEqual(String[] tuple) {
        String value = tuple[0];
        for (int i = 1; i < tuple.length; i++) {
            if (!tuple[i].equals(value)) { return false; }
        }
        return true;
    }

    /**
     * Returns the number of suppressed tuples
     * 
     * @param output
     * @return
     */
    private static int getSuppressed(DataHandle output) {
        int count = 0;
        for (int i = 0; i < output.getNumRows(); i++) {
            if (output.isOutlier(i)) {
                count++;
            }
        }
        return count;
    }
}