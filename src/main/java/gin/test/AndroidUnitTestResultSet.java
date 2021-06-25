package gin.test;

import com.google.common.math.Stats;
import gin.Patch;
import gin.util.AndroidTest;
import gin.util.JankyFrames;
import gin.util.TestExecutionMemory;
import org.checkerframework.checker.units.qual.A;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class AndroidUnitTestResultSet {

    private List<AndroidTestResult> results;
    private Patch patch;
    private boolean patchValid;


    public AndroidUnitTestResultSet(Patch patch, boolean patchValid, List<AndroidTestResult> results){
        this.patch = patch;
        this.results = results;
        this.patchValid = patchValid;

    }

    public boolean isPatchValid() {
        return patchValid;
    }

    public double getJankiness(){
        HashMap<String, ArrayList<JankyFrames>> testRes =  getJankinessPerTest();
        ArrayList<Double> jankiness = new ArrayList<>();
        for(String test : testRes.keySet()) {
            ArrayList<Double> jankinessRatios = new ArrayList<>();
            for (JankyFrames jankyFrames : testRes.get(test)) {
                jankinessRatios.add((double) jankyFrames.per95th);
            }
            if(jankinessRatios.size() > 0) {
                jankiness.add(median(jankinessRatios));
            }
        }
        if(jankiness.size() > 0) {
            return median(jankiness);
        }
        else {
            return 10000;
        }
    }




    public HashMap<String, ArrayList<JankyFrames>> getJankinessPerTest(){
        HashMap<String, ArrayList<JankyFrames>> out = new HashMap<>();
        for (AndroidTestResult result : results){
            String test = result.getTest().getTestName();

            if (out.containsKey(test)){
                JankyFrames jankyFrames = result.getJankiness();
                if (jankyFrames.totalFrames > 1) {
                    out.get(test).add(result.getJankiness());
                }
            }
            else {
                ArrayList<JankyFrames> forTest = new ArrayList<>();
                JankyFrames jankyFrames = result.getJankiness();
                if (jankyFrames.totalFrames > 1) {
                    forTest.add(result.getJankiness());
                }
                out.put(test, forTest);
            }

        }
        return out;
    }

    public HashMap<String, Long> getAverageJankinessPerTest(){
        HashMap<String, Long> count = new HashMap<>();
        HashMap<String, Long> out = new HashMap<>();
        for (AndroidTestResult result : results){
            String test = result.getTest().getTestName();
            JankyFrames jankyFrames = result.getJankiness();
            if (out.containsKey(test)){
                if (jankyFrames.totalFrames > 1) {
                    out.put(test,out.get(test) + jankyFrames.per95th);
                    count.put(test, count.get(test) + 1);
                }
            }
            else {
                if (jankyFrames.totalFrames > 1) {
                    out.put(test, jankyFrames.per95th);
                    count.put(test, 1L);
                }
            }

        }

        for (String test : out.keySet()){
            if (out.containsKey(test)) {
                out.put(test, out.get(test) / count.get(test));
            }
        }
        return out;
    }

    public Double getMemoryUsage(){
        ArrayList<Float> memMaxes = new ArrayList<>();
        for(AndroidTestResult result : results) {
            memMaxes.add((Float.valueOf(result.getExecutionMemory())));
        }
        return Double.valueOf(TestExecutionMemory.median(memMaxes));
    }

    public Double getExecutionTime(){
        ArrayList<Double> execTimes = new ArrayList<>();
        for(AndroidTestResult result: results){
            execTimes.add(result.getCPUTime());
        }
        return Double.valueOf(median(execTimes));
    }

    public List<AndroidTestResult> getResults() {
        return results;
    }

    public static Double median(ArrayList<Double> values) {
        Collections.sort(values);
        if (values.size() == 0){return 0d;}
        if (values.size() % 2 == 1)
            return values.get((values.size() + 1) / 2 - 1);
        else {
            double lower = values.get(values.size() / 2 - 1);
            double upper = values.get(values.size() / 2);

            return (lower + upper) / 2.0f;
        }
    }
}
