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
import org.deidentifier.arx.BenchmarkDataset.BenchmarkDatafile;
import org.deidentifier.arx.RiskBasedBenchmarkSetup.BenchmarkMetric;
import org.deidentifier.arx.RiskBasedBenchmarkSetup.BenchmarkPrivacyCriterium;

//import de.linearbits.subframe.Benchmark;
//import de.linearbits.subframe.analyzer.buffered.BufferedArithmeticMeanAnalyzer;
//import de.linearbits.subframe.analyzer.buffered.BufferedStandardDeviationAnalyzer;
/**
 * Main benchmark class.
 * 
 * @author Fabian Prasser
 */
public class RiskBasedBenchmarkMain {
    
    /** Repetitions */
    private static final int       REPETITIONS       = 3;
//    /** The benchmark instance */
//    private static final Benchmark BENCHMARK         = new Benchmark(new String[] { "Algorithm", "Dataset", "Criteria" });
//    /** Label for execution times */
//    public static final int        EXECUTION_TIME    = BENCHMARK.addMeasure("Execution time");
//    /** Label for number of checks */
//    public static final int        NUMBER_OF_CHECKS  = BENCHMARK.addMeasure("Number of checks");
//    /** Label for number of roll-ups */
//    public static final int        NUMBER_OF_ROLLUPS = BENCHMARK.addMeasure("Number of rollups");
//    
//    // TODO include information loss
//
//    static {
//        BENCHMARK.addAnalyzer(EXECUTION_TIME, new BufferedArithmeticMeanAnalyzer(REPETITIONS));
//        BENCHMARK.addAnalyzer(EXECUTION_TIME, new BufferedStandardDeviationAnalyzer(REPETITIONS));
//        BENCHMARK.addAnalyzer(NUMBER_OF_CHECKS, new BufferedArithmeticMeanAnalyzer(REPETITIONS));
//        BENCHMARK.addAnalyzer(NUMBER_OF_ROLLUPS, new BufferedArithmeticMeanAnalyzer(REPETITIONS));
//    }
    

    /**
     * Main entry point
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

//        performHeuraklesFlashComparison();
        performHeuraklesSelfComparison();
    }

    private static void performHeuraklesFlashComparison() throws IOException {
        // repeat for each privacy criterium
        for (BenchmarkPrivacyCriterium criterium : RiskBasedBenchmarkSetup.getPrivacyCriteria()) {
            // Repeat for each data set
            for (BenchmarkDatafile datafile : RiskBasedBenchmarkSetup.getFlashComparisonDatafiles()) {
                BenchmarkDataset dataset = new BenchmarkDataset(datafile, datafile.equals(BenchmarkDatafile.ACS13) ? 10 : null);
                // repeat for each metric
                for (BenchmarkMetric metric : RiskBasedBenchmarkSetup.getMetrics()) {
                    // repeat for each suppression factor
                    for (double suppression : RiskBasedBenchmarkSetup.getSuppressionValues()) {
                        // build a Flash config with those parameters; a runtime limit is not needed by flash
                        ARXConfiguration flashConfig = RiskBasedBenchmarkSetup.getConfiguration(criterium, metric, suppression, null);
                        // we perform repetitive anonymization runs ...
                        int []flashRuntimes = new int[REPETITIONS];
                        for (int i = 0; i < REPETITIONS; i++) {
                            flashRuntimes[i] = anonymize(dataset, flashConfig);
                        }
                        // ... and calculate the arithmetic mean of those runs
                        int meanFlashRuntime = (int) Math.round(computeArithmeticMean(flashRuntimes));
                        // build a identical Heurakles configuration using the runtime limit, that we just calculated
                        ARXConfiguration heuraklesConfig = RiskBasedBenchmarkSetup.getConfiguration(criterium, metric, suppression, meanFlashRuntime);
                        anonymize(dataset, heuraklesConfig);
                    }
                }
            }
        }
    }

    private static void performHeuraklesSelfComparison() throws IOException {
        // repeat for each privacy criterium
        for (BenchmarkPrivacyCriterium criterium : RiskBasedBenchmarkSetup.getPrivacyCriteria()) {
            // Repeat for each data set, each dataset represents 
            for (BenchmarkDatafile datafile : RiskBasedBenchmarkSetup.getSelfComparisonDatafiles()) {
                // repeat for each metric
                for (BenchmarkMetric metric : RiskBasedBenchmarkSetup.getMetrics()) {
                    // repeat for each suppression factor
                    for (double suppression : RiskBasedBenchmarkSetup.getSuppressionValues()) {
                        // repeat for different QI counts
                        for (int qiCount : RiskBasedBenchmarkSetup.getSelfComparisonQiCounts()) {
                            BenchmarkDataset dataset = new BenchmarkDataset(datafile, qiCount);
                            // build a Heurakles configuration
                            ARXConfiguration heuraklesConfig = RiskBasedBenchmarkSetup.getConfiguration(criterium, metric, suppression, 600000);
                            anonymize(dataset, heuraklesConfig);
                        }
                    }
                }
            }
        }
    }

    private static double computeArithmeticMean(int[] valueArray) {
        double sum = 0.0;
        
        for (int i = 0; i < valueArray.length; i++) {
            sum += valueArray[i];
        }
        return sum / valueArray.length;
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
    private static int anonymize(BenchmarkDataset dataset, ARXConfiguration config) throws IOException {
        Data data = RiskBasedBenchmarkSetup.getData(dataset.getDatafile(), dataset.getCustomQiCount());
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
        System.out.println(dataset.getDatafile());
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
