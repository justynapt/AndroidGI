package gin;

import gin.edit.Edit;
import gin.edit.line.DeleteLine;
import gin.test.AndroidTestRunner;
import gin.util.AndroidConfig;
import gin.util.AndroidConfigReader;
import gin.util.AndroidProject;
import gin.util.AndroidTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.rng.simple.JDKRandomBridge;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.Test;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.CookieHandler;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.*;

public class AndroidCoverageReporter {
    ArrayList<String> files;
    Map<String, Patch> patches;
    AndroidTestRunner testRunner;
    Map<AndroidTest,TestDeletePatch> editLists;
    SourceFile sourceFile;
    List<Class<? extends Edit>> editTypes;
    JDKRandomBridge rng;
    private AndroidProject project;

    public AndroidCoverageReporter(AndroidTestRunner testRunner, AndroidProject project) {
        long seed = Instant.now().getEpochSecond();
        this.testRunner = testRunner;
        this.rng = new JDKRandomBridge(RandomSource.MT, seed);
        this.project = project;
        editLists = new HashMap<>();
        patches = new HashMap<>();
        files = new ArrayList<>();
        for( AndroidTest androidTest: project.getAllInstrumentedTests()){
            if(!files.contains(androidTest.fileName)){
                files.add(androidTest.fileName);
            }
            editLists.put(androidTest, new TestDeletePatch(androidTest));
        }
        project.unitTests = project.getAllLocalTests();
        for( AndroidTest androidTest: project.getAllLocalTests()){
            if(!files.contains(androidTest.fileName)){
                files.add(androidTest.fileName);
            }
            editLists.put(androidTest, new TestDeletePatch(androidTest));
        }
        for (String file: files) {
            List<Class<? extends Edit>> editTypes = Edit.parseEditClassesFromString(Edit.EditType.LINE.toString());
            List<String> targetMethod = new ArrayList<>();
            targetMethod.add("");
            SourceFile sourceFile = SourceFile.makeSourceFileForEditTypes(editTypes, file, targetMethod);
            patches.put(file, new Patch(sourceFile));
        }
    }

    public static void main(String[] args) {

        AndroidConfigReader configReader = new AndroidConfigReader("config.properties");
        AndroidConfig config = configReader.readConfig();
        AndroidProject androidProject = new AndroidProject(config);
        AndroidTestRunner testRunner = new AndroidTestRunner(androidProject, config);
        //androidProject.buildTestRunner();
        //testRunner.installTestAPK();
        AndroidCoverageReporter coverageReporter = new AndroidCoverageReporter(testRunner, androidProject);
        coverageReporter.project.instrumentedTests = coverageReporter.project.getAllInstrumentedTests();

        try {
            for(AndroidTest androidTest: coverageReporter.project.instrumentedTests) {
                int out = coverageReporter.generateCoverageReport(androidTest);
                if (out == 0) {
                    coverageReporter.moveReport(androidTest);
                }
            }
            for(AndroidTest androidTest: coverageReporter.project.getAllLocalTests()){
                int out = coverageReporter.generateLocalCoverageReport(androidTest);
                if (out ==0){
                    coverageReporter.moveReport(androidTest);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
            //coverageReporter.restoreAllTests();
        }finally {
            //coverageReporter.restoreAllTests();
        }
        coverageReporter.restoreAllTests();
        testRunner.stop();
    }

    public void restoreAllTests(){
        for(String file: patches.keySet()){
            patches.get(file).undoWrite(file);
        }
    }
    
    public void isolateTest(AndroidTest test){
        HashMap<String, ArrayList<Integer>> toRemove = new HashMap<>();
        for (AndroidTest otherTest: editLists.keySet()){
            String filename = otherTest.fileName;
            if(!toRemove.containsKey(filename)){
                toRemove.put(filename, new ArrayList<>());
            }
            if (! otherTest.equals(test)){
                TestDeletePatch patchSpec = editLists.get(otherTest);
                ArrayList<Integer> toRemoveArray = toRemove.get(filename);

                for (int i : patchSpec.getEdits()){
                    if (! toRemoveArray.contains(i)){
                        toRemoveArray.add(i);
                    }
                }
            }
        }
        for (String file : files){
            Patch patch = patches.get(file);
            ArrayList<Integer> toRemoveArray = toRemove.get(file);
            //System.out.println(file + " - removing lines: " + toRemoveArray);
            Collections.sort(toRemoveArray, Collections.reverseOrder());
            for (int line : toRemoveArray){
                patch.add(new DeleteLine(file, line));
            }
        }
        for(String toApplyfile: patches.keySet()){
            Patch toApply = patches.get(toApplyfile);
            toApply.writePatchedSourceToFile(toApplyfile);
            toApply.edits = new LinkedList<>();
        }
        String keepFile = test.fileName;
        for (String file : files){
            if(! file.equals(keepFile)){
                try {
                    FileUtils.forceDelete(new File(file));
                } catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
    }

    public void isolateTestIgnore(AndroidTest test){
        HashMap<String, ArrayList<Integer>> toRemove = new HashMap<>();
        for (AndroidTest otherTest: editLists.keySet()){
            String filename = otherTest.fileName;
            if(!toRemove.containsKey(filename)){
                toRemove.put(filename, new ArrayList<>());
            }

            if (! otherTest.equals(test)){
                TestDeletePatch patchSpec = editLists.get(otherTest);
                ArrayList<Integer> toRemoveArray = toRemove.get(filename);

                int i = patchSpec.getEdits().get(0);
                if (! toRemoveArray.contains(i)){
                        toRemoveArray.add(i);
                }
            }
        }

        for (String file : files){
            ArrayList<Integer> toRemoveArray = toRemove.get(file);

            Patch patch = patches.get(file);
            //System.out.println(file + " - removing lines: " + toRemoveArray);
            Collections.sort(toRemoveArray, Collections.reverseOrder());
            SourceFileLine ignored = (SourceFileLine) patch.getSourceFile().copyOf();
            for (int line : toRemoveArray){
                ignored = ignored.insertLine(line-1, "@Ignore");
            }
            try {
                Scanner lines = new Scanner(new File(file));
                int lineNo = 0;
                while (lines.hasNext()){
                    String line = lines.nextLine();
                    lineNo++;
                    if(line.trim().startsWith("package")){
                        ignored = ignored.insertLine(lineNo+1, "import org.junit.Ignore;");
                    }
                }
            }
            catch (FileNotFoundException e){
                e.printStackTrace();
            }
            try {
                FileUtils.writeStringToFile(new File(file), ignored.toString(), Charset.defaultCharset());
            } catch (IOException e) {
                Logger.error("Exception writing source code of patched program to: " + file);
                Logger.trace(e);
                System.exit(-1);
            }
        }
    }

    public int generateCoverageReport(AndroidTest test){
        System.out.println("Coverage for: " + test);
        isolateTestIgnore(test);
        int out = project.coverageTask();
        restoreAllTests();
        return out;

    }

    public int generateLocalCoverageReport(AndroidTest test){
        System.out.println("Coverage for: " + test);
        isolateTestIgnore(test);
        int out = project.localCoverageTask();
        restoreAllTests();
        return out;

    }


    public void moveReport(AndroidTest test){
        String sourceDir;
        String flavourString = Character.toLowerCase(project.flavour.charAt(0)) + project.flavour.substring(1);
        if (test.isInstrumented()) {
            sourceDir = StringUtils.substringBefore(project.apkPath, "/build/") + "/build/reports/coverage/" + flavourString + "/debug/";
        }
        else{
            sourceDir = StringUtils.substringBefore(project.apkPath, "/build/") + "/build/reports/jacoco/" + flavourString + "Debug/";
        }
        String destDir = "reports/" + test.getFullClassName() + "." + test.getMethodName();
        try {
            File destDirFile = new File(destDir);
            if(!destDirFile.exists()) {
                FileUtils.forceMkdir(new File(destDir));
            }
            FileUtils.copyDirectoryToDirectory(new File(sourceDir), new File(destDir));
        } catch (IOException e){
            System.out.println("Failed to move " + sourceDir + "to " + destDir);
            e.printStackTrace();
        }
    }
}
