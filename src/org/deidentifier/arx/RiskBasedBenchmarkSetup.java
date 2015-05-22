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
import java.util.ArrayList;
import java.util.Arrays;

import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.aggregates.HierarchyBuilder;
import org.deidentifier.arx.aggregates.HierarchyBuilderIntervalBased;
import org.deidentifier.arx.aggregates.HierarchyBuilder.Type;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.metric.Metric;

/**
 * This class encapsulates most of the parameters of a benchmark run
 * @author Fabian Prasser
 */
public class RiskBasedBenchmarkSetup {
    
    private static final String acs13DatafilePath="data/ss13acs_68726Recs_Massachusetts_edited.csv";
    /**
     * Returns all datasets for the benchmark
     * @return
     */
    public static BenchmarkDataset[] getFlashComparisonDatasets() {
        return new BenchmarkDataset[] { 
         BenchmarkDataset.ADULT,
//         BenchmarkDataset.CUP,
//         BenchmarkDataset.FARS,
//         BenchmarkDataset.ATUS,
//         BenchmarkDataset.IHIS,
         BenchmarkDataset.ACS13_09,
        };
    }
    
    /**
     * Returns all metrics
     * @return
     */
    public static BenchmarkMetric[] getMetrics() {
        return new BenchmarkMetric[] { 
//         BenchmarkMetric.AECS,
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
//                             1.0
        };
    }

    public static enum BenchmarkDataset {
        ADULT (null){
            @Override
            public String toString() {
                return "Adult";
            }
        },
        CUP (null){
            @Override
            public String toString() {
                return "Cup";
            }
        },
        FARS (null){
            @Override
            public String toString() {
                return "Fars";
            }
        },
        ATUS (null){
            @Override
            public String toString() {
                return "Atus";
            }
        },
        IHIS (null){
            @Override
            public String toString() {
                return "Ihis";
            }
        },
        ACS13_09 (9){
            @Override
            public String toString() {
                return "ACS13_with_09_QIs";
            }
        },
        ACS13_15 (15){
            @Override
            public String toString() {
                return "ACS13_with_15_QIs";
            }
        },
        ACS13_20 (20){
            @Override
            public String toString() {
                return "ACS13_with_20_QIs";
            }
        },
        ACS13_25 (25){
            @Override
            public String toString() {
                return "ACS13_with_25_QIs";
            }
        },
        ACS13_30 (30){
            @Override
            public String toString() {
                return "ACS13_with_30_QIs";
            }
        };
        
        // the dataset can be configured to use only a subset
        // of the QIs contained in the dataset
        private final Integer customQiCount;
        
        /**
         * @param customQiCount if not null, the dataset will be configured to use only a subset 
         * of the first "customQiCount" QIs contained in the dataset. If null, all QIs in the
         * dataset will be used
         */
        private BenchmarkDataset(Integer customQiCount) {
            this.customQiCount = customQiCount;
        }
        
        public Integer getCustomQiCount() {
            return this.customQiCount;
        }
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
     * Returns a configuration for the ARX framework
     * @param dataset
     * @param criteria
     * @return
     * @throws IOException
     */
    public static ARXConfiguration getConfiguration(BenchmarkMetric metric,
                                                    double suppression,
                                                    Integer runtimeLimitMs) throws IOException {
        ARXConfiguration config = ARXConfiguration.create();
        switch (metric) {
        case AECS:config.setMetric(Metric.createAECSMetric());
            break;
        case LOSS:
            config.setMetric(Metric.createLossMetric());
            break;
        default:
            break;
        
        }
        config.addCriterion(new KAnonymity(5));
        config.setMaxOutliers(suppression);
        return config;
    }

    /**
     * Configures and returns the dataset 
     * @param dataset
     * @param criteria
     * @return
     * @throws IOException
     */

    public static Data getData(BenchmarkDataset dataset
                               ) throws IOException {
        Data data = null;
        switch (dataset) {
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
        case ACS13_09:
        case ACS13_15:
        case ACS13_20:
        case ACS13_25:
        case ACS13_30:
            data = Data.create(acs13DatafilePath, ';');
            break;
        default:
            throw new RuntimeException("Invalid dataset");
        }

            for (String qi : getQuasiIdentifyingAttributes(dataset)) {
                data.getDefinition().setAttributeType(qi, getHierarchy(dataset, qi));
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
    public static Hierarchy getHierarchy(BenchmarkDataset dataset, String attribute) throws IOException {
        switch (dataset) {
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
        case ACS13_09:
        case ACS13_15:
        case ACS13_20:
        case ACS13_25:
        case ACS13_30:
            String filePath = "hierarchies/ss13acs_hierarchy_" + SS13PMA_SEMANTIC_QI.valueOf(attribute).fileBaseName();
            switch (SS13PMA_SEMANTIC_QI.valueOf(attribute).getType()) {
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
                    throw new RuntimeException("Inconsistent Hierarchy types: expected: interval-based, found: " + loaded.getType());
                }
            case ORDER:
                filePath += ".csv";
                return Hierarchy.create(filePath, ';');
            default:
                break;
            }
        default:
            throw new RuntimeException("Invalid dataset");
        }
    }

    /**
     * Returns the quasi-identifiers for the dataset
     * @param dataset
     * @return
     */
    public static String[] getQuasiIdentifyingAttributes(BenchmarkDataset dataset) {
        switch (dataset) {
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
        case ACS13_09:
        case ACS13_15:
        case ACS13_20:
        case ACS13_25:
        case ACS13_30:
            ArrayList<String> al = new ArrayList<>();
            for (SS13PMA_SEMANTIC_QI qi : Arrays.copyOf(SS13PMA_SEMANTIC_QI.values(), dataset.getCustomQiCount())) {
                al.add(qi.toString());
            }
            String[] qiArr = new String[al.size()];
            qiArr = al.toArray(qiArr);
            return qiArr;
        default:
            throw new RuntimeException("Invalid dataset");
        }
    }
    


    private enum SS13PMA_SEMANTIC_QI {
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

        private final String        distinctionLetter;
        private final HierarchyType ht;

        // constructor
        SS13PMA_SEMANTIC_QI(HierarchyType ht) {
            this.ht = ht;

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
        }

        // needed for file name generation
        public String fileBaseName() {
            return (distinctionLetter + "_" + this.name());
        }

        public HierarchyType getType() {
            return ht;
        }
    }
}
