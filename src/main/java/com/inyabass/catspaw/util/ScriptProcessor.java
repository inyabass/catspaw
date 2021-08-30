package com.inyabass.catspaw.util;

import com.inyabass.catspaw.config.ConfigReader;
import com.inyabass.catspaw.logging.Logger;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScriptProcessor {

    final static Logger logger = new Logger(MethodHandles.lookup().lookupClass());

    private List<String> lines = null;
    private String workingDirectory = null;

    private static String fs = System.getProperty("file.separator");

    public static void main(String[] args) throws Throwable {
        ScriptProcessor scriptProcessor = new ScriptProcessor();
        scriptProcessor.setWorkingDirectory(System.getProperty("java.io.tmpdir") + "cats" + fs + System.currentTimeMillis());
        scriptProcessor.addLine("pwd");
        scriptProcessor.addLine("git clone " + ConfigReader.get("git.repo.url"));
        scriptProcessor.addLine("cd catspaw");
        scriptProcessor.addLine("git checkout develop");
        scriptProcessor.addLine("mvn clean package");
        scriptProcessor.run();
    }

    public ScriptProcessor() {
        this.lines = new ArrayList<>();
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public void addLine(String line) {
       this.lines.add(line);
    }

    public void run() throws Throwable {
        if(this.lines.size()==0) {
            logger.info("No script lines to process");
            return;
        }
        String tempFileName = System.currentTimeMillis() + ".sh";
        String tempFilePath = null;
        Path dirPath = null;
        if(this.workingDirectory!=null&&!this.workingDirectory.equals("")) {
            dirPath = Paths.get(this.workingDirectory);
            if(!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            tempFilePath = this.workingDirectory + fs + tempFileName;
        } else {
            tempFilePath = tempFileName;
        }
        logger.info("Creating temp script " + tempFilePath);
        Path scriptFilePath = Paths.get(tempFilePath);
        if(Files.exists(scriptFilePath)) {
            Files.delete(scriptFilePath);
        }
        Files.createFile(scriptFilePath);
        scriptFilePath.toFile().setExecutable(true);
        FileWriter fileWriter = new FileWriter(tempFilePath);
        fileWriter.write("#!/usr/bin/env bash" + "\n");
        int i = 0;
        for(String string: this.lines) {
            i++;
            logger.info("Command -> " + string);
            if(i < this.lines.size()) {
                fileWriter.write(string + "\n");
            } else {
                fileWriter.write(string);
            }
        }
        fileWriter.close();
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.redirectErrorStream(true);
        Map<String, String> environment = processBuilder.environment();
        try {
            if(dirPath!=null) {
                processBuilder.directory(dirPath.toFile());
            }
            processBuilder.command(this.getBash(environment), tempFileName);
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }
        } catch (Throwable t) {
            Assert.fail("Unable to Run bash");
        }
    }

    private String getBash(Map<String, String> environment) {
        if(!System.getProperty("os.name").toLowerCase().contains("windows")) {
            return "bash";
        }
        return "C:\\Program Files\\Git\\bin\\bash";
    }
}
