package org.deidentifier.arx;

import java.util.ArrayList;
import java.util.List;

import org.deidentifier.arx.metric.InformationLoss;

public class ProgressListener {
    public class Datapoint {
        private long timestamp;
        private InformationLoss<?> loss;
        
        Datapoint (long timestamp, InformationLoss<?> loss) {
            this.timestamp = timestamp;
            this.loss = loss;
        }
        public long getTime() {
            return timestamp;
        }

        public InformationLoss<?> getLoss() {
            return loss;
        }
    }
    
    private List<Datapoint> datapoints = new ArrayList<>();
    
    ProgressListener() {
        datapoints.add(new Datapoint(System.currentTimeMillis(), null));
    }
    
    public void transformationFound(long time, InformationLoss<?> loss) {
        datapoints.add(new Datapoint(time, loss));
    }
    
    Datapoint[] getDatapoints () {
        return (Datapoint[])datapoints.toArray();
    }
    
    boolean solutionFound() {
        return datapoints.size() > 1;
    }
    
    
}
