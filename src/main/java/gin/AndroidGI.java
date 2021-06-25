package gin;

import com.opencsv.CSVWriter;
import gin.edit.Edit;
import gin.edit.line.*;
import gin.test.AndroidTestResult;
import gin.test.AndroidTestRunner;
import gin.test.AndroidUnitTestResultSet;
import gin.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.rng.simple.JDKRandomBridge;
import org.apache.commons.rng.simple.RandomSource;
import org.pmw.tinylog.Logger;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class AndroidGI {
    AndroidTestRunner testRunner;
    SourceFileLine sourceFile;
    List<Class<? extends Edit>> editTypes;
    JDKRandomBridge rng;
    int indNumber = 20;
    int genNumber = 10;
    private double tournamentPercentage = 0.2;
    private double mutateProbability = 0.5;
    private File outputFile;
    AndroidProject project;

    public AndroidGI(AndroidTestRunner testRunner, SourceFileLine sourceFile, List<Class<? extends Edit>> editTypes, AndroidProject project) {
        long seed = Instant.now().getEpochSecond();
        this.testRunner = testRunner;
        this.sourceFile = sourceFile;
        this.editTypes = editTypes;
        this.rng = new JDKRandomBridge(RandomSource.MT, seed);
        outputFile = new File("log.txt");
        this.project = project;

    }

    public static void main(String[] args) {
        AndroidConfigReader configReader = new AndroidConfigReader("config.properties");
        AndroidConfig config = configReader.readConfig();
        String fileName = config.getFilePath();
        AndroidProject androidProject = new AndroidProject(config);
        AndroidTestRunner testRunner = new AndroidTestRunner(androidProject, config);
        androidProject.buildTestRunner();
        testRunner.installTestAPK();
        List<Class<? extends Edit>> editTypes = Edit.parseEditClassesFromString(Edit.EditType.LINE.toString());
        List<String> targetMethod = new ArrayList<>();
        targetMethod.add("");
        SourceFileLine sourceFile =  (SourceFileLine)SourceFile.makeSourceFileForEditTypes(editTypes, fileName, targetMethod);
        AndroidGI androidGI = new AndroidGI(testRunner, sourceFile, editTypes, androidProject);
        androidGI.localSearch();
        androidGI.testRunner.stop();

    }


    public AndroidUnitTestResultSet getOriginalJank() {
        try {
            Patch patch = new Patch(sourceFile);
            AndroidUnitTestResultSet results = testRunner.runTests(patch, 200,true);
            if (!results.isPatchValid()) {
                System.out.println("Tests Failed on original app");
                System.exit(1);
            }
            for (AndroidTestResult result : results.getResults()) {
                System.out.println(result);
            }
            return results;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    //Method to run local search based GI on Android
    public void localSearch() {
        Map<String, AndroidUnitTestResultSet> tested = new HashMap();
        writeHeader();
        AndroidUnitTestResultSet origRes = getOriginalJank();
        AndroidUnitTestResultSet bestresults = null;
        double originalJank = origRes.getJankiness();
        Patch bestPatch = new Patch(this.sourceFile);
        writePatch(bestPatch, origRes, -1);
        tested.put(bestPatch.toString(), origRes);

        double bestTime = originalJank;
        for (int step = 0; step < 400; step++) {

            try {
                if (step % 5 == 0) {
                    project.killDaemon();
                }
                AndroidUnitTestResultSet results;

                Patch neighbour = neighbour(bestPatch);
                if (!tested.containsKey(neighbour.toString())) {
                    results = testRunner.runTests(neighbour, 200, true);

                } else {
                    results = tested.get(neighbour.toString());
                }
                writePatch(neighbour, results, step);
                if (!results.isPatchValid()) {

                } else if (results.getJankiness() < bestTime) {
                    bestPatch = neighbour;
                    bestTime = results.getJankiness();
                    bestresults = results;
                }
            } catch (Exception e) {
                bestPatch.undoWrite(sourceFile.getFilename());
                e.printStackTrace();
                System.exit(1);
            }

        }
        System.out.println(bestPatch);
        System.out.println(originalJank);
        System.out.println(bestTime);
        if(bestresults == null){
            bestresults = origRes;
        }
        writePatch(bestPatch, bestresults, -2);

    }

    //get mutant
    Patch neighbour(Patch patch) {

        Patch neighbour = patch.clone();
        neighbour.addRandomEditOfClasses(rng, editTypes);
        return neighbour;

    }

    //Genetic Programming Android GI
    public void GP() {

        writeHeader();
        AndroidUnitTestResultSet results;
        // Calculate fitness and record result, including fitness improvement (currently 0)
        AndroidUnitTestResultSet origRes = getOriginalJank();
        double orig = origRes.getJankiness();
        Patch origPatch = new Patch(sourceFile);
        writePatch(origPatch, origRes, -1);
        // Keep best
        double best = orig;

        // Generation 1
        Map<Patch, Double> population = new HashMap<>();
        population.put(origPatch, orig);

        for (int i = 1; i < indNumber; i++) {

            // Add a mutation
            Patch patch = mutate(origPatch);
            // If fitnessThreshold met, add it
            results = testPatch(patch);
            if (fitnessThreshold(results, orig)) {
                population.put(patch, fitness(results));
            }
            writePatch(patch, results, 0);

        }
        int count = 0;
        for (int g = 0; g < genNumber; g++) {

            // Previous generation
            List<Patch> patches = new ArrayList(population.keySet());

            Logger.info("Creating generation: " + (g + 1));

            // Current generation
            Map<Patch, Double> newPopulation = new HashMap<>();

            // Select individuals for crossover
            List<Patch> selectedPatches = select(population, origPatch, orig);

            // Keep a list of patches after crossover
            List<Patch> crossoverPatches = crossover(selectedPatches, origPatch);

            // If less than indNumber variants produced, add random patches from the previous generation
            while (crossoverPatches.size() < indNumber) {
                crossoverPatches.add(patches.get(rng.nextInt(patches.size())).clone());
            }

            // Mutate the newly created population and check fitness
            for (Patch patch : crossoverPatches) {

                // Add a mutation
                patch = mutate(patch);

                Logger.debug("Testing patch: " + patch);

                // Test the patched source file
                results = testPatch(patch);
                count += 1;
                double newFitness = fitness(results);
                if (count >= 10) {
                    count = 0;
                    project.killDaemon();
                }

                // If fitness threshold met, add patch to the mating population
                if (fitnessThreshold(results, orig)) {
                    newPopulation.put(patch, newFitness);
                }
                writePatch(patch, results, g);
            }

            population = new HashMap<Patch, Double>(newPopulation);
            if (population.isEmpty()) {
                population.put(origPatch, orig);
            }


        }

    }


    protected double fitness(AndroidUnitTestResultSet results) {

        double fitness = results.getJankiness();
        return fitness;
    }

    // Calculate fitness threshold, for selection to the next generation
    protected boolean fitnessThreshold(AndroidUnitTestResultSet results, double orig) {

        return results.isPatchValid();
    }

    // Compare two fitness values, newFitness better if result > 0
    protected double compareFitness(double newFitness, double oldFitness) {

        return oldFitness - newFitness;
    }


    // Adds a random edit of the given type with equal probability among allowed types
    protected Patch mutate(Patch oldPatch) {
        Patch patch = oldPatch.clone();
        patch.addRandomEditOfClasses(rng, editTypes);
        return patch;
    }

    // Tournament selection for patches
    protected List<Patch> select(Map<Patch, Double> population, Patch origPatch, double origFitness) {

        List<Patch> patches = new ArrayList(population.keySet());
        if (patches.size() < indNumber) {
            population.put(origPatch, origFitness);
            while (patches.size() < indNumber) {
                patches.add(origPatch);
            }
        }
        List<Patch> selectedPatches = new ArrayList<>();

        // Pick half of the population size
        for (int i = 0; i < indNumber / 2; i++) {

            Collections.shuffle(patches, rng);

            // Best patch from x% randomly selected patches picked each time
            Patch bestPatch = patches.get(0);
            double best = population.get(bestPatch);
            for (int j = 1; j < (indNumber * tournamentPercentage); j++) {
                Patch patch = patches.get(j);
                double fitness = population.get(patch);

                if (compareFitness(fitness, best) > 0) {
                    bestPatch = patch;
                    best = fitness;
                }
            }

            selectedPatches.add(bestPatch.clone());

        }
        return selectedPatches;
    }

    // Uniform crossover: patch1patch2 and patch2patch1 created, each edit added with x% probability
    protected List<Patch> crossover(List<Patch> patches, Patch origPatch) {

        List<Patch> crossedPatches = new ArrayList<>();

        Collections.shuffle(patches, rng);
        int half = patches.size() / 2;
        for (int i = 0; i < half; i++) {

            Patch parent1 = patches.get(i);
            Patch parent2 = patches.get(i + half);
            List<Edit> list1 = parent1.getEdits();
            List<Edit> list2 = parent2.getEdits();

            Patch child1 = origPatch.clone();
            Patch child2 = origPatch.clone();

            for (int j = 0; j < list1.size(); j++) {
                if (rng.nextFloat() > mutateProbability) {
                    child1.add(list1.get(j));
                }
            }
            for (int j = 0; j < list2.size(); j++) {
                if (rng.nextFloat() > mutateProbability) {
                    child1.add(list2.get(j));
                }
                if (rng.nextFloat() > mutateProbability) {
                    child2.add(list2.get(j));
                }
            }
            for (int j = 0; j < list1.size(); j++) {
                if (rng.nextFloat() > mutateProbability) {
                    child2.add(list1.get(j));
                }
            }

            crossedPatches.add(parent1);
            crossedPatches.add(parent2);
            crossedPatches.add(child1);
            crossedPatches.add(child2);
        }

        return crossedPatches;
    }

    private AndroidUnitTestResultSet testPatch(Patch patch, int frames, boolean breakOnFail) {
        AndroidUnitTestResultSet result = new AndroidUnitTestResultSet(patch, false, new ArrayList<>());
        try {
            result = testRunner.runTests(patch, frames, breakOnFail);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return result;
    }

    private AndroidUnitTestResultSet testPatch(Patch patch) {
        return testPatch(patch, 200, true);
    }

    //write output to log
    public void writeHeader() {
        String entry = "Gen, Ind, Patch, Valid, Fitness, Time\n";
        try {
            FileWriter writer = new FileWriter(outputFile, true);
            writer.write(entry);
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }


    public void writePatch(Patch patch, AndroidUnitTestResultSet resultSet, int gen) {
        ZonedDateTime now = ZonedDateTime.now();
        now = now.withZoneSameInstant(ZoneId.of("UTC"));
        String entry =
                gen + ", " + patch.toString() + ", " +
                        Boolean.toString(resultSet.isPatchValid()) + ", " +
                        Double.toString(resultSet.getJankiness()) + ", " + now.toString() + "\n";
        try {
            FileWriter writer = new FileWriter(outputFile, true);
            writer.write(entry);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    private Patch getPatch(String patch) {
        Patch origPatch = new Patch(sourceFile);
        if(patch.replaceAll("\\s+","").equals("|")){
            return origPatch;
        }
        String[] edits = patch.split("\\|");
        for (String edit : edits) {
            if (edit.equals("")) {
                continue;
            }
            String[] editTokens = edit.split(" ");
            String cls = editTokens[1];
            String[] sourceTokens;
            String[] destTokens;
            switch (cls) {
                case "gin.edit.line.CopyLine":
                    sourceTokens = editTokens[2].split(":");
                    destTokens = editTokens[4].split(":");
                    origPatch.add(new CopyLine(sourceTokens[0], Integer.parseInt(sourceTokens[1]), destTokens[0], Integer.parseInt(destTokens[1])));
                    break;
                case "gin.edit.line.DeleteLine":
                    sourceTokens = editTokens[2].split(":");
                    origPatch.add(new DeleteLine(sourceTokens[0], Integer.parseInt(sourceTokens[1])));
                    break;
                case "gin.edit.line.ReplaceLine":
                    sourceTokens = editTokens[2].split(":");
                    destTokens = editTokens[4].split(":");
                    origPatch.add(new ReplaceLine(sourceTokens[0], Integer.parseInt(sourceTokens[1]), destTokens[0], Integer.parseInt(destTokens[1])));
                    break;
                case "gin.edit.line.SwapLine":
                    sourceTokens = editTokens[2].split(":");
                    destTokens = editTokens[4].split(":");
                    origPatch.add(new SwapLine(sourceTokens[0], Integer.parseInt(sourceTokens[1]), destTokens[0], Integer.parseInt(destTokens[1])));
                    break;
            }

        }
        return origPatch;
    }

}
