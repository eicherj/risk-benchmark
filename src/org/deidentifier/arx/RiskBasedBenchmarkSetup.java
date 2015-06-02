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

import org.deidentifier.arx.ARXPopulationModel.Region;
import org.deidentifier.arx.BenchmarkDataset.BenchmarkDatafile;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.criteria.PopulationUniqueness;
import org.deidentifier.arx.metric.Metric;
import org.deidentifier.arx.metric.Metric.AggregateFunction;

/**
 * This class encapsulates most of the parameters of a benchmark run
 * @author Fabian Prasser
 */
public class RiskBasedBenchmarkSetup {

    /**
     * Returns the datafiles for the Heurakles-Flash-Comparison
     * @return
     */
    public static BenchmarkDataset[] getFlashComparisonDatasets() {
        return new BenchmarkDataset[] {
         new BenchmarkDataset(BenchmarkDatafile.ADULT, null),
         new BenchmarkDataset(BenchmarkDatafile.CUP, null),
         new BenchmarkDataset(BenchmarkDatafile.FARS, null),
         new BenchmarkDataset(BenchmarkDatafile.ATUS, null),
         new BenchmarkDataset(BenchmarkDatafile.IHIS, null),
         new BenchmarkDataset(BenchmarkDatafile.ACS13, 10),
        };
    }
    
    
    /**
     * Returns the datafiles for the Heurakles-Self-Comparison
     * @return
     */
    public static BenchmarkDatafile[] getSelfComparisonDatafiles() {
        return new BenchmarkDatafile[] {
          BenchmarkDatafile.ACS13,
        };
    }
    
    
    /**
     * Returns the QI counts for the Heurakles-Self-Comparison
     * @return
     */
    public static int[] getSelfComparisonQiCounts() {
        return new int[] {
              5,
              6,
              7,
              8
//          15,
//          20,
//          25,
//          30
        };
    }
    
    /**
     * Returns all metrics
     * @return
     */
    public static BenchmarkMetric[] getMetrics() {
        return new BenchmarkMetric[] {
         BenchmarkMetric.LOSS,
         BenchmarkMetric.AECS,
        };
    }
    
    /**
     * Returns all suppression values
     * @return
     */
    public static double[] getSuppressionValues() {
        return new double[] { 
                 0.0,
                 1.0
        };
    }
    
    /**
     * Returns all privacy criteria
     * @return
     */
    public static BenchmarkPrivacyCriterium[] getPrivacyCriteria() {
        return new BenchmarkPrivacyCriterium[] { 
                             BenchmarkPrivacyCriterium.FIVE_ANONYMITY,
                             BenchmarkPrivacyCriterium.ZERO_DOT_01_UNIQUENESS
        };
    }
    
    
    // definition section
    
    public static enum BenchmarkPrivacyCriterium {
        FIVE_ANONYMITY {
            @Override
            public String toString() {
                return "(5)-Anonymity";
            }
        },
        ZERO_DOT_01_UNIQUENESS {
            @Override
            public String toString() {
                return "(0.01)-Uniqueness";
            }
        };
    }
    

    
    public static enum BenchmarkMetric {
        AECS {
            @Override
            public String toString() {
                return "AECS";
            }
        },
        LOSS {
            @Override
            public String toString() {
                return "Loss";
            }
        }
    }
    

    
    public static enum Algorithm {
        FLASH {
            @Override
            public String toString() {
                return "Flash";
            }
        },
        HEURAKLES {
            @Override
            public String toString() {
                return "Heurakles";
            }
        }
    }

    /**
     * @param criterium
     * @param metric
     * @param suppression
     * @param runTimeLimitMillis
     * @return
     * @throws IOException
     */
    public static ARXConfiguration prepareConfiguration(Algorithm algo,
                                                    BenchmarkPrivacyCriterium criterium,
                                                    BenchmarkMetric metric,
                                                    double suppression,
                                                    Long runTimeLimitMillis) throws IOException {
        
    	// create empty ARX configuration
        ARXConfiguration config = ARXConfiguration.create();
        

        // TODO implement configuration of Algorithm
        
        // configure privacy criterium
        switch (criterium) {
        case FIVE_ANONYMITY:
            config.addCriterion(new KAnonymity(5));
            break;
        case ZERO_DOT_01_UNIQUENESS:
            config.addCriterion(new PopulationUniqueness(0.01, ARXPopulationModel.create(Region.USA)));
            break;
        default:
            throw new RuntimeException("Invalid criterium");        
        }
        
        // configure metric
        switch (metric) {
        case AECS:config.setMetric(Metric.createAECSMetric());
            break;
        case LOSS:
            config.setMetric(Metric.createLossMetric(AggregateFunction.GEOMETRIC_MEAN));
            break;
        default:
            throw new RuntimeException("Invalid metric");        
        }
        
        // configure suppression factor
        config.setMaxOutliers(suppression);
        
        // TODO implement configuration of runtime limit
        
        return config;
    }
}
