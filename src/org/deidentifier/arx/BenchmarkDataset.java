package org.deidentifier.arx;
    
    public class BenchmarkDataset {
        private BenchmarkDatafile datafile = null;
        private Integer customQiCount = null;
        
        /**
         * @param datafile
         * @param customQiCount
         */
        public BenchmarkDataset(BenchmarkDatafile datafile, Integer customQiCount) {
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
            ADULT {
                @Override
                public String toString() {
                    return "Adult";
                }
            },
            CUP {
                @Override
                public String toString() {
                    return "Cup";
                }
            },
            FARS {
                @Override
                public String toString() {
                    return "Fars";
                }
            },
            ATUS {
                @Override
                public String toString() {
                    return "Atus";
                }
            },
            IHIS {
                @Override
                public String toString() {
                    return "Ihis";
                }
            },
            ACS13{
                @Override
                public String toString() {
                    return "ACS13";
                }
            };
        }

}
