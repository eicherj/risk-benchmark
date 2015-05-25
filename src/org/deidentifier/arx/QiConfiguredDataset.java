package org.deidentifier.arx;

import java.io.IOException;
import java.util.Arrays;

import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.aggregates.HierarchyBuilder;
import org.deidentifier.arx.aggregates.HierarchyBuilderIntervalBased;
import org.deidentifier.arx.aggregates.HierarchyBuilder.Type;
    

    /**
     * this class encapsulates the configuration of a dataset the location 
     * and loading of its
     * its data- and hierarchy-files on the filesystem and the number of QIs
     * actually used for the dataest
     * @author helmut spengler
     *
     */
    public class QiConfiguredDataset {
        private BenchmarkDatafile datafile = null;
        private Integer customQiCount = null;
                
        /**
         * @param datafile
         * @param customQiCount
         */
        public QiConfiguredDataset(BenchmarkDatafile datafile, Integer customQiCount) {
            this.datafile = datafile;
            this.customQiCount = customQiCount;
        }

        public BenchmarkDatafile getDatafile() {
            return datafile;
        }

        /**
         * @return the number of QIs used in the dataset
         */
        public Integer getCustomQiCount() {
            return customQiCount;
        }        


        public static enum BenchmarkDatafile {
            ADULT ("adult"){
                @Override
                public String toString() {
                    return "Adult";
                }
            },
            CUP ("cup") {
                @Override
                public String toString() {
                    return "Cup";
                }
            },
            FARS ("fars"){
                @Override
                public String toString() {
                    return "Fars";
                }
            },
            ATUS ("atus"){
                @Override
                public String toString() {
                    return "Atus";
                }
            },
            IHIS ("ihis"){
                @Override
                public String toString() {
                    return "Ihis";
                }
            },
            ACS13 ("ss13acs"){
                @Override
                public String toString() {
                    return "ACS13";
                }
            };
            
        	private String baseStringForFilename = null;
        	
        	BenchmarkDatafile (String baseStringForFilename) {
        		this.baseStringForFilename = baseStringForFilename;
        	}
        	
        	/**
        	 * @return the string, that will be used for finding and loading the
        	 * datafile with its hierarchies from the filesystem
        	 */
        	private String getBaseStringForFilename() {
        		return baseStringForFilename;
        	}
        }

        
        /**
         * Configures and returns the dataset as <code>org.deidentifier.arx.Data</code>
         * @param dataset
         * @param criteria
         * @return
         * @throws IOException
         */

        public Data toArxData() throws IOException {
            Data data = Data.create("data/" + datafile.getBaseStringForFilename() + ".csv", ';');
            for (String qi : getQuasiIdentifyingAttributes()) {
            	data.getDefinition().setAttributeType(qi, loadHierarchy(qi));
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
        private Hierarchy loadHierarchy(String attribute) throws IOException {
        	if (!datafile.equals(BenchmarkDatafile.ACS13)) {
        		return Hierarchy.create("hierarchies/" + datafile.getBaseStringForFilename() + "_hierarchy_" + attribute + ".csv", ';');
        	} else {
        		return loadACS13Hierarchy("hierarchies/" + datafile.getBaseStringForFilename() + "_hierarchy_", attribute);
        	}
        }

        private static Hierarchy loadACS13Hierarchy(String fileBaseName, String attribute) throws IOException {
            String filePath = fileBaseName + ACS13_SEMANTIC_QI.valueOf(attribute).fileBaseName();
            switch (ACS13_SEMANTIC_QI.valueOf(attribute).getType()) {
            case INTERVAL:
                filePath += ".ahs";
                HierarchyBuilder<?> loaded = HierarchyBuilder.create(filePath);
                if (loaded.getType() == Type.INTERVAL_BASED) {
                    HierarchyBuilderIntervalBased<?> builder = (HierarchyBuilderIntervalBased<?>) loaded;
                    Data data = Data.create("data/" + BenchmarkDatafile.ACS13.getBaseStringForFilename() + ".csv", ';');
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
        private String[] getQuasiIdentifyingAttributes() {
            switch (datafile) {
            case ADULT:
                return customizeQis ((new String[] {	"age",
                                        				"education",
                                        				"marital-status",
                                        				"native-country",
                                        				"race",
                                        				"salary-class",
                                        				"sex",
                                        				"workclass",
                										"occupation"}),
                						customQiCount);
            case ATUS:
                return customizeQis ((new String[] {   "Age",
                                        				"Birthplace",
                                        				"Citizenship status",
				                                        "Labor force status",
				                                        "Marital status",
				                                        "Race",
				                                        "Region",
				                                        "Sex",
				                                        "Highest level of school completed"}),
				                         customQiCount);
            case CUP:
                return customizeQis ((new String[] {   "AGE",
				                                        "GENDER",
				                                        "INCOME",
				                                        "MINRAMNT",
				                                        "NGIFTALL",
				                                        "STATE",
				                                        "ZIP",
				                                        "RAMNTALL"}),
                                        customQiCount);
            case FARS:
                return customizeQis ((new String[] {   "iage",
				                                        "ideathday",
				                                        "ideathmon",
				                                        "ihispanic",
				                                        "iinjury",
				                                        "irace",
				                                        "isex",
				                                        "istatenum"}),
                                        customQiCount);
            case IHIS:
                return customizeQis ((new String[] {   "AGE",
				                                        "MARSTAT",
				                                        "PERNUM",
				                                        "QUARTER",
				                                        "RACEA",
				                                        "REGION",
				                                        "SEX",
				                                        "YEAR",
				                                        "EDUC"}),
                                        customQiCount);
            case ACS13:
                return customizeQis (ACS13_SEMANTIC_QI.getNames(), customQiCount);
            default:
                throw new RuntimeException("Invalid dataset");
            }
        }
        
        private static String[] customizeQis(String[] qis, Integer customQiCount) {
        	return customQiCount == null ? qis : Arrays.copyOf(qis, customQiCount);
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
