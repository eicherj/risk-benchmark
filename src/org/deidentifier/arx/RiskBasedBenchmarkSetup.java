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

import org.deidentifier.arx.ARXPopulationModel.Region;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.BenchmarkDataset.BenchmarkDatafile;
import org.deidentifier.arx.aggregates.HierarchyBuilder;
import org.deidentifier.arx.aggregates.HierarchyBuilderIntervalBased;
import org.deidentifier.arx.aggregates.HierarchyBuilder.Type;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.criteria.PopulationUniqueness;
import org.deidentifier.arx.metric.Metric;

/**
 * This class encapsulates most of the parameters of a benchmark run
 * @author Fabian Prasser
 */
public class RiskBasedBenchmarkSetup {
	
	
	// configuration section
    
	// file path for acs13 data
    private static final String acs13DatafilePath="data/ss13acs_68726Recs_Massachusetts_edited.csv";
    
    /**
     * Returns the datafiles for the Heurakles-Flash-Comparison
     * @return
     */
    public static BenchmarkDatafile[] getFlashComparisonDatafiles() {
        return new BenchmarkDatafile[] { 
         BenchmarkDatafile.ADULT,
         BenchmarkDatafile.CUP,
         BenchmarkDatafile.FARS,
         BenchmarkDatafile.ATUS,
         BenchmarkDatafile.IHIS,
         BenchmarkDatafile.ACS13,
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
     * Returns the datasets for the Heurakles-Self-Comparison
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
         BenchmarkMetric.AECS,
         BenchmarkMetric.LOSS
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
                             BenchmarkPrivacyCriterium.NULL_DOT_01_UNIQUENESS
        };
    }
    
    
    // defintion section
    
    public static enum BenchmarkPrivacyCriterium {
        FIVE_ANONYMITY {
            @Override
            public String toString() {
                return "(5)-Anonymity";
            }
        },
        NULL_DOT_01_UNIQUENESS {
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

    /**
     * @param criterium
     * @param metric
     * @param suppression
     * @param runTimeLimitMillis
     * @return
     * @throws IOException
     */
    public static ARXConfiguration getConfiguration(BenchmarkPrivacyCriterium criterium,
                                                    BenchmarkMetric metric,
                                                    double suppression,
                                                    Long runTimeLimitMillis) throws IOException {
        
    	// create empty ARX configuration
        ARXConfiguration config = ARXConfiguration.create();
        
        // configure privacy criterium
        switch (criterium) {
        case FIVE_ANONYMITY:
            config.addCriterion(new KAnonymity(5));
            break;
        case NULL_DOT_01_UNIQUENESS:
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
            config.setMetric(Metric.createLossMetric());
            break;
        default:
            throw new RuntimeException("Invalid metric");        
        }
        
        // configure suppression factor
        config.setMaxOutliers(suppression);
        
        // TODO implement configuration of runtimeLimit
        
        return config;
    }

    /**
     * Configures and returns the dataset 
     * @param dataset
     * @param criteria
     * @return
     * @throws IOException
     */

    public static Data getData(BenchmarkDataset dataset) throws IOException {
        BenchmarkDatafile datafile = dataset.getDatafile();

        Data data;
        switch (datafile) {
        case ADULT:
            data = Data.create("data/adult.csv", ';');
            break;
        case ATUS:
            data = Data.create("data/atus.csv", ';');
            break;
        case CUP:
            data = Data.create("data/cup.csv", ';');
            break;
        case FARS:
            data = Data.create("data/fars.csv", ';');
            break;
        case IHIS:
            data = Data.create("data/ihis.csv", ';');
            break;
        case ACS13:
            data = Data.create(acs13DatafilePath, ';');
            break;
        default:
            throw new RuntimeException("Invalid dataset");
        }
        for (String qi : getQuasiIdentifyingAttributes(datafile, dataset.getCustomQiCount())) {
        	data.getDefinition().setAttributeType(qi, getHierarchy(datafile, qi));
        }

        return data;
    }

    /**
     * Returns the generalization hierarchy for the dataset and attribute
     * @param dataset
     * @param attribute
     * @return
     * @throws IOException
     */
    public static Hierarchy getHierarchy(BenchmarkDatafile datafile, String attribute) throws IOException {
        switch (datafile) {
        case ADULT:
            return Hierarchy.create("hierarchies/adult_hierarchy_" + attribute + ".csv", ';');
        case ATUS:
            return Hierarchy.create("hierarchies/atus_hierarchy_" + attribute + ".csv", ';');
        case CUP:
            return Hierarchy.create("hierarchies/cup_hierarchy_" + attribute + ".csv", ';');
        case FARS:
            return Hierarchy.create("hierarchies/fars_hierarchy_" + attribute + ".csv", ';');
        case IHIS:
            return Hierarchy.create("hierarchies/ihis_hierarchy_" + attribute + ".csv", ';');
        case ACS13:
            return createACS13Hierarchy("hierarchies/ss13acs_hierarchy_", attribute);
        default:
            throw new RuntimeException("Invalid dataset");
        }
    }

    private static Hierarchy createACS13Hierarchy(String fileBaseName, String attribute) throws IOException {
        String filePath = fileBaseName + ACS13_SEMANTIC_QI.valueOf(attribute).fileBaseName();
        switch (ACS13_SEMANTIC_QI.valueOf(attribute).getType()) {
        case INTERVAL:
            filePath += ".ahs";
            HierarchyBuilder<?> loaded = HierarchyBuilder.create(filePath);
            if (loaded.getType() == Type.INTERVAL_BASED) {
                HierarchyBuilderIntervalBased<?> builder = (HierarchyBuilderIntervalBased<?>) loaded;
                Data data = Data.create(acs13DatafilePath, ';');
                int index = data
                                .getHandle()
                                .getColumnIndexOf(attribute);
                String[] dataArray = data
                                         .getHandle()
                                         .getStatistics()
                                         .getDistinctValues(index);
                builder.prepare(dataArray);
                return builder.build();
            } else {
                throw new RuntimeException("Inconsistent hierarchy types. Expected: interval-based, found: " + loaded.getType());
            }
        case ORDER:
            filePath += ".csv";
            return Hierarchy.create(filePath, ';');
        default:
            throw new RuntimeException("Invalid hierarchy Type");
        }
    }

    /**
     * Returns the quasi-identifiers for the dataset
     * @param dataset
     * @return
     */
    public static String[] getQuasiIdentifyingAttributes(BenchmarkDatafile datafile, Integer customQiCount) {
        switch (datafile) {
        case ADULT:
            return new String[] {   "age",
                                    "education",
                                    "marital-status",
                                    "native-country",
                                    "race",
                                    "salary-class",
                                    "sex",
                                    "workclass",
                                    "occupation"};
        case ATUS:
            return new String[] {   "Age",
                                    "Birthplace",
                                    "Citizenship status",
                                    "Labor force status",
                                    "Marital status",
                                    "Race",
                                    "Region",
                                    "Sex",
                                    "Highest level of school completed"};
        case CUP:
            return new String[] {   "AGE",
                                    "GENDER",
                                    "INCOME",
                                    "MINRAMNT",
                                    "NGIFTALL",
                                    "STATE",
                                    "ZIP",
                                    "RAMNTALL"};
        case FARS:
            return new String[] {   "iage",
                                    "ideathday",
                                    "ideathmon",
                                    "ihispanic",
                                    "iinjury",
                                    "irace",
                                    "isex",
                                    "istatenum"};
        case IHIS:
            return new String[] {   "AGE",
                                    "MARSTAT",
                                    "PERNUM",
                                    "QUARTER",
                                    "RACEA",
                                    "REGION",
                                    "SEX",
                                    "YEAR",
                                    "EDUC"};
        case ACS13:
            return Arrays.copyOf(ACS13_SEMANTIC_QI.getNames(), customQiCount != null ? customQiCount : ACS13_SEMANTIC_QI.values().length);
        default:
            throw new RuntimeException("Invalid dataset");
        }
    }
    


    private enum ACS13_SEMANTIC_QI {
        AGEP(HierarchyType.INTERVAL), // height 10
        CIT(HierarchyType.ORDER), // height 06
        COW(HierarchyType.ORDER), // height 06
        DDRS(HierarchyType.ORDER), // height 05
        DEAR(HierarchyType.ORDER), // height 05
        DEYE(HierarchyType.ORDER), // height 05
        DOUT(HierarchyType.ORDER), // height 04
        DPHY(HierarchyType.ORDER), // height 04
        DREM(HierarchyType.ORDER), // height 03
        FER(HierarchyType.ORDER), // height 02
        GCL(HierarchyType.ORDER), // height 02
        HINS1(HierarchyType.ORDER), // height 02
        HINS2(HierarchyType.ORDER), // height 02
        HINS3(HierarchyType.ORDER), // height 02
        HINS4(HierarchyType.ORDER), // height 02
        HINS5(HierarchyType.ORDER), // height 02
        HINS6(HierarchyType.ORDER), // height 02
        HINS7(HierarchyType.ORDER), // height 02
        INTP(HierarchyType.INTERVAL), // height 02
        MAR(HierarchyType.ORDER), // height 02
        MARHD(HierarchyType.ORDER), // height 02
        MARHM(HierarchyType.ORDER), // height 02
        MARHW(HierarchyType.ORDER), // height 02
        MIG(HierarchyType.ORDER), // height 02
        MIL(HierarchyType.ORDER), // height 02
        PWGTP(HierarchyType.INTERVAL), // height 03
        RELP(HierarchyType.ORDER), // height 04
        SCHG(HierarchyType.ORDER), // height 02
        SCHL(HierarchyType.ORDER), // height 02
        SEX(HierarchyType.ORDER), // height 02
        ;

        private enum HierarchyType {
            INTERVAL, // interval based
            ORDER // order based
        }

        private final HierarchyType ht;

        // constructor
        ACS13_SEMANTIC_QI(HierarchyType ht) {
            this.ht = ht;
        }

        // needed for file name generation
        public String fileBaseName() {
            final String        distinctionLetter;

            switch (ht) {
            case INTERVAL:
                distinctionLetter = "i";
                break;
            case ORDER:
                distinctionLetter = "o";
                break;
            default:
                distinctionLetter = "x";
            }
            return (distinctionLetter + "_" + this.name());
        }

        public HierarchyType getType() {
            return ht;
        }
        
        public static String[] getNames() {
            ACS13_SEMANTIC_QI[] qis = values();
            String[] names = new String[qis.length];

            for (int i = 0; i < qis.length; i++) {
                names[i] = qis[i].name();
            }

            return names;
        }
    }
}
