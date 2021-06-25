package gin.util;

import gin.Patch;

import java.util.ArrayList;

public class NSGAIIInd {
    private Patch patch;
    private double crowding;



    private int rank;
    private ArrayList<Double> fitnesses;

    public NSGAIIInd(Patch patch, ArrayList<Double> fitnesses){
        this.patch = patch;
        this.fitnesses = fitnesses;
        this.crowding = 0;
    }
    public NSGAIIInd(ArrayList<Double> fitnesses){
        this.fitnesses = fitnesses;
        this.crowding = 0;
    }

    public void setCrowding(double crowding) {
        this.crowding = crowding;
    }

    public double getCrowding() {
        return crowding;
    }

    public ArrayList<Double> getFitnesses() {
        return fitnesses;
    }

    public Patch getPatch() {
        return patch;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }
}
