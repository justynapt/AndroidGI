package gin.util;

import gin.Patch;
import org.checkerframework.checker.units.qual.A;

import java.util.*;
import java.util.stream.Collector;

public class NSGAIIPop {
    private ArrayList<NSGAIIInd> population;
    private int noObj;
    private Map<Integer, ArrayList<NSGAIIInd>> fronts;
    private  ArrayList<Integer> fitnessDirs;

    public NSGAIIPop(int noObj){
        this.population = new ArrayList<>();
        this.noObj = noObj;
        for (int i = 0; i < noObj; i++){
            fitnessDirs.add(1);
        }
    }

    public NSGAIIPop(int noObj, ArrayList<Integer> fitnessDirs){
        this.population = new ArrayList<>();
        this.noObj = noObj;
        this.fitnessDirs = fitnessDirs;
    }

    public NSGAIIPop(NSGAIIPop p, NSGAIIPop q){
        if (p.noObj != q.noObj){
            throw new IllegalArgumentException("No objectives dont match");
        }
        noObj = p.noObj;
        if (p.fitnessDirs.equals(q.fitnessDirs)){
            fitnessDirs = p.fitnessDirs;

        }
        else{
            throw new IllegalArgumentException("Fitness Directions do not match");
        }
        this.population = new ArrayList<>();
        for (NSGAIIInd ind: p.getPopulation()){
            addInd(ind);
        }
        for (NSGAIIInd ind: q.getPopulation()){
            addInd(ind);
        }
    }

    public void addInd(Patch patch, ArrayList<Double> fitnesses){
        if (fitnesses.size() != noObj){
            throw new IllegalArgumentException("Incorrect number of fitnesses");
        }
        population.add(new NSGAIIInd(patch, fitnesses));
    }

    public void addInd(NSGAIIInd ind){
        population.add(ind);
    }

    protected void nonDominatedSort(){
        Map<NSGAIIInd, ArrayList<NSGAIIInd>> dominationSets = new HashMap<NSGAIIInd, ArrayList<NSGAIIInd>>();
        Map<NSGAIIInd, Integer> dominationScores = new HashMap<NSGAIIInd, Integer>();
        fronts = new HashMap<>();
        fronts.put(1, new ArrayList<>());
        for  (NSGAIIInd p: population){
            ArrayList<NSGAIIInd> dominationSet = new ArrayList<>();
            int dominationScore = 0;
            for  (NSGAIIInd q: population){
                if (dominates(p,q)){
                    dominationSet.add(q);
                }
                else if (dominates(q,p)){
                    dominationScore += 1;
                }
            }
            dominationScores.put(p, dominationScore);
            dominationSets.put(p, dominationSet);
            if (dominationScore == 0){
                fronts.get(1).add(p);
            }
        }
        int i = 1;
        while (true) {
            ArrayList<NSGAIIInd> currentFront = fronts.get(i);
            ArrayList<NSGAIIInd> nextFront = new ArrayList<>();
            for (NSGAIIInd p : currentFront) {
                for (NSGAIIInd q : dominationSets.get(p)) {
                    Integer score = dominationScores.get(q);
                    score -= 1;
                    dominationScores.put(q, score);
                    if (score == 0) {
                        q.setRank(i+1);
                        nextFront.add(q);
                    }
                }
            }
            i +=1;
            if (nextFront.size() == 0){
                break;
            }
            else {
                fronts.put(i, nextFront);
            }
        }
    }


    public void setCrowding(){
        for(int m = 0; m < noObj; m++){
            sortByObj(m);
            population.get(0).setCrowding(Double.MAX_VALUE);
            population.get(population.size()-1).setCrowding(Double.MAX_VALUE);
            double max = population.get(population.size()-1).getFitnesses().get(m);
            double min = population.get(0).getFitnesses().get(m);
            double denom = max - min;
            for(int i = 1; i< population.size() -1; i++){
                double currentDist = population.get(i).getCrowding();
                double num = population.get(i+1).getFitnesses().get(m) - population.get(i-1).getFitnesses().get(m);
                population.get(i).setCrowding(currentDist + (num/denom));
            }
        }

    }

    public boolean dominates(NSGAIIInd p, NSGAIIInd q){
        boolean better = false;
        for (int i= 0; i < noObj; i++){
            float dir = fitnessDirs.get(i);
            if(p.getFitnesses().get(i) * dir < q.getFitnesses().get(i) * dir){
                return false;
            }
            if(p.getFitnesses().get(i) * dir > q.getFitnesses().get(i) * dir){
                better = true;
            }
        }
        return better;
    }
    public void sortByObj(int index){
        Collections.sort(population, Comparator.comparing((NSGAIIInd ind) -> ind.getFitnesses().get(index)));
    }

    public ArrayList<NSGAIIInd> getPopulation(){
        return population;
    }

    public ArrayList<Patch> getNextGen(int popSize){
        ArrayList<Patch> out = new ArrayList<>();
        nonDominatedSort();
        setCrowding();
        for (int front = 1; front <= fronts.size(); front++ ){
            if (fronts.get(front).size() < popSize - out.size()){
                for (NSGAIIInd ind : fronts.get(front)){
                    out.add(ind.getPatch().clone());
                }
            }
            else {
                Collections.sort(fronts.get(front), Comparator.comparing((NSGAIIInd ind) -> ind.getCrowding()));
                int frontInd = 0;
                while (out.size() < popSize){
                    out.add(fronts.get(front).get(frontInd).getPatch().clone());
                    frontInd++;
                }

            }
        }
        return out;
    }
}
