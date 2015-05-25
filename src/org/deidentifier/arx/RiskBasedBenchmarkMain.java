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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.deidentifier.arx.QiConfiguredDataset.BenchmarkDatafile;
import org.deidentifier.arx.RiskBasedBenchmarkSetup.BenchmarkMetric;
import org.deidentifier.arx.RiskBasedBenchmarkSetup.BenchmarkPrivacyCriterium;

import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.buffered.BufferedArithmeticMeanAnalyzer;
import de.linearbits.subframe.io.CSVLine;

/**
 * Main benchmark class.
 * 
 * @author Fabian Prasser
 */
public class RiskBasedBenchmarkMain {
    
    /** Repetitions */
    private static final int       REPETITIONS       = 3;
    
    /** The variables, over which the benchmark iterates */
    private static final String[] BENCHMARK_VARIABLES = new String[] { "Criterium", "Dataset", "CustomQIs", "Metric", "Suppression", "Algorithm" };
    
    /** The benchmark instance */
    private static final Benchmark BENCHMARK         = new Benchmark(BENCHMARK_VARIABLES);
    /** Label for execution times */
    public static final int        EXECUTION_TIME    = BENCHMARK.addMeasure("Execution time");

    static {
        BENCHMARK.addAnalyzer(EXECUTION_TIME, new BufferedArithmeticMeanAnalyzer(REPETITIONS));
    }    
    // TODO include information loss etc.

    

    /**
     * Main entry point
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
    	
    	System.out.println("Starting Flash comparison");
        performHeuraklesFlashComparison();
        
        System.out.println("\nStarting self comparison");
        performHeuraklesSelfComparison();
        
        System.out.println("\ndone.");
    }

    private static void performHeuraklesFlashComparison() throws IOException {
    	
        // repeat for each privacy criterium
        for (BenchmarkPrivacyCriterium privCriterium : RiskBasedBenchmarkSetup.getPrivacyCriteria()) {
        	
            // repeat for each data set
            for (BenchmarkDatafile datafile : RiskBasedBenchmarkSetup.getFlashComparisonDatafiles()) {
                QiConfiguredDataset dataset = new QiConfiguredDataset(datafile, datafile.equals(BenchmarkDatafile.ACS13) ? 9 : null);
                
                // repeat for each metric
                for (BenchmarkMetric metric : RiskBasedBenchmarkSetup.getMetrics()) {
                	
                    // repeat for each suppression factor
                    for (double suppression : RiskBasedBenchmarkSetup.getSuppressionValues()) {                    	
                    	String resultFileName = "resultFlashCompare.csv";
                    	
                    	// perform the Flash run
                        long avgExecutionTimeMillis = runAndRecordBenchmark(privCriterium, dataset, metric, suppression, "Flash", null, resultFileName);
                        
                        // perform a Heurakles run with the same configuration and the execution time
                        // of the previous Flash run as Heurakles' runtime limit
                        runAndRecordBenchmark(privCriterium, dataset, metric, suppression, "Heurakles", avgExecutionTimeMillis, resultFileName);
                    }
                }
            }
        }
    }

    private static void performHeuraklesSelfComparison() throws IOException {
    	
        // repeat for each privacy criterium
        for (BenchmarkPrivacyCriterium criterium : RiskBasedBenchmarkSetup.getPrivacyCriteria()) {
        	
            // repeat for each data set, each dataset represents 
            for (BenchmarkDatafile datafile : RiskBasedBenchmarkSetup.getSelfComparisonDatafiles()) {
            	
                // repeat for each metric
                for (BenchmarkMetric metric : RiskBasedBenchmarkSetup.getMetrics()) {
                	
                    // repeat for each suppression factor
                    for (double suppression : RiskBasedBenchmarkSetup.getSuppressionValues()) {
                    	
                        // repeat for different QI counts
                        for (int qiCount : RiskBasedBenchmarkSetup.getSelfComparisonQiCounts()) {
                            QiConfiguredDataset dataset = new QiConfiguredDataset(datafile, qiCount);
                            runAndRecordBenchmark(criterium, dataset, metric, suppression, "Heurakles", Long.valueOf(600000), "resultSelfCompare.csv");
                        }
                    }
                }
            }
        }
    }

	/**
	 * @param privCriterium
	 * @param dataset
	 * @param metric
	 * @param suppression
	 * @param algo
	 * @param runtimeLimitMillis
	 * @param resultFileName
	 * @return the execution time of the algorithm
	 * @throws IOException
	 */
	private static long runAndRecordBenchmark(
			BenchmarkPrivacyCriterium privCriterium,
			QiConfiguredDataset dataset,
			BenchmarkMetric metric,
			double suppression,
			String algo,
			Long runtimeLimitMillis,
			String resultFileName) throws IOException {
        
		// tell the user what's happening
		System.out.println("Benchmarking (" + algo + " / " + privCriterium + " / " + dataset.getDatafile().toString() + " / " + 
    			dataset.getCustomQiCount() + " / " +  metric.toString() + " / " + suppression + ")");
    	
    	// create the anonymizer
    	ARXAnonymizer anonymizer = new ARXAnonymizer();
		
        // build a algorithm configuration based on the benchmark parameters
        ARXConfiguration anonConfig = RiskBasedBenchmarkSetup.prepareConfiguration(privCriterium, metric, suppression, runtimeLimitMillis);
        
		// start benchmarking
		BENCHMARK.addRun(privCriterium.toString(), dataset.getDatafile().toString(), dataset.getCustomQiCount(), metric.toString(), suppression, algo);
		for (int i = 0; i < REPETITIONS; i++) {
			BENCHMARK.startTimer(EXECUTION_TIME);
		    ARXResult result = anonymizer.anonymize(dataset.toArxData(), anonConfig);
		    BENCHMARK.addStopTimer(EXECUTION_TIME);
		}
		
		// Write results to file
		BENCHMARK.getResults().write(new File(resultFileName));
		
		return getLastExecutionTimeMillis(BENCHMARK);
	}
	
	/**
	 * @param benchmark
	 * @return the execution time of the last benchmark as reported in the benchmark log
	 */
	private static long getLastExecutionTimeMillis(Benchmark benchmark) {
		Iterator<CSVLine> resultIterator = benchmark.getResults().iterator();
		CSVLine lastResultLine = resultIterator.next();
		while (resultIterator.hasNext()) {
			lastResultLine = resultIterator.next();
		}
		long executionTimeOfLastResultMillis = Double.valueOf(lastResultLine.getData()[BENCHMARK_VARIABLES.length + EXECUTION_TIME]).longValue() / 1000000;
		return executionTimeOfLastResultMillis;
	}
}
