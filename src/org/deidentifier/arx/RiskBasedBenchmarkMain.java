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

import org.deidentifier.arx.ARXLattice.ARXNode;
import org.deidentifier.arx.RiskBasedBenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.RiskBasedBenchmarkSetup.BenchmarkMetric;
/**
 * Main benchmark class.
 * 
 * @author Fabian Prasser
 */
public class RiskBasedBenchmarkMain {

    /** Repetitions */
    private static final int REPETITIONS = 3;

    /**
     * Main entry point
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        performHeuraklesFlashComparison();
        performHeuraklesSelfComparison();
    }

    private static void performHeuraklesFlashComparison() throws IOException {
        // Repeat for each data set
        for (BenchmarkDataset dataset : RiskBasedBenchmarkSetup.getFlashComparisonDatasets()) {
            for (BenchmarkMetric metric : RiskBasedBenchmarkSetup.getMetrics()) {
                for (double suppression : RiskBasedBenchmarkSetup.getSuppressionValues()) {
                    ARXConfiguration flashConfig = RiskBasedBenchmarkSetup.getConfiguration(metric, suppression, null);
                    int []flashRuntimes = new int[REPETITIONS];
                    for (int i = 0; i < REPETITIONS; i++) {
                        flashRuntimes[i] = anonymize(flashConfig, dataset);
                    }
                    int heuraklesRuntime = (int) getGeometricMean(flashRuntimes);
                    ARXConfiguration heuraklesConfig = RiskBasedBenchmarkSetup.getConfiguration(metric, suppression, heuraklesRuntime);
                    anonymize(heuraklesConfig, dataset);
                }
            }
        }
    }

    private static double getGeometricMean(int[] runtimeArray) {
        double value = 1.0;
        
        for (int i = 0; i < runtimeArray.length; i++) {
            value *= runtimeArray[i];
        }
        value = Math.pow(value, 1.0 / ((double) runtimeArray.length));
        return Math.round(value);
    }

    private static void performHeuraklesSelfComparison() throws IOException {
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
     * Performs the experiments
     * 
     * @param dataset
     * @throws IOException
     */
    private static int anonymize(ARXConfiguration config, BenchmarkDataset dataset) throws IOException {
        Data data = RiskBasedBenchmarkSetup.getData(dataset);
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        long startTime = System.currentTimeMillis();
        ARXResult result = anonymizer.anonymize(data, config);
        long finishTime = System.currentTimeMillis();
        int runtime = (int)(finishTime - startTime);
        startTime = System.currentTimeMillis() - startTime;
        int searchSpaceSize = 1;
        for (String qi : data.getDefinition().getQuasiIdentifyingAttributes()) {
            searchSpaceSize *= data.getDefinition().getHierarchy(qi)[0].length;
        }
        Iterator<String[]> iter = result.getOutput().iterator();
        System.out.println(dataset);
        System.out.println(" - Time          : " + startTime + " [ms]");
        System.out.println(" - QIs           : " + data.getDefinition().getQuasiIdentifyingAttributes().size());
        System.out.println(" - Search space  : " + searchSpaceSize);
        System.out.println(" - Checked       : " + getCheckedTransformations(result));
        System.out.println(" - Header        : " + Arrays.toString(iter.next()));
        System.out.println(" - Tuple         : " + Arrays.toString(getTuple(iter)));
        System.out.println(" - Suppressed    : " + getSuppressed(result.getOutput()));
        System.out.println(" - Transformation: " + Arrays.toString(result.getGlobalOptimum().getTransformation()));
        System.out.println(" - Heights       : " + Arrays.toString(result.getLattice().getTop().getTransformation()));
        System.out.println(" - Total         : " + data.getHandle().getNumRows());
        System.out.println(" - Infoloss      : " + result.getGlobalOptimum().getMinimumInformationLoss().toString());
        
        return runtime;
    }

    /**
     * Returns the number of checked transformations
     * @param result
     * @return
     */
    private static int getCheckedTransformations(ARXResult result) {
        int count = 0;
        for (ARXNode[] level : result.getLattice().getLevels()) {
            for (ARXNode node : level) {
                if (node.isChecked()) {
                    count++;
                }
            }
        }
        return count;
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
}
