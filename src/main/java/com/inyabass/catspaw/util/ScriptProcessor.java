package com.inyabass.catspaw.util;

import com.inyabass.catspaw.config.ConfigReader;
import com.inyabass.catspaw.logging.Logger;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.File;
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
    private File stdoutFile = null;
    private int exitValue = 0;

    public static String fs = System.getProperty("file.separator");

    public static void main(String[] args) throws Throwable {
        System.out.println("x");
    }

    public ScriptProcessor() {
        this.lines = new ArrayList<>();
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public File getStdoutFile() {
        return this.stdoutFile;
    }

    public int getExitValue() {
        return this.exitValue;
    }

    public void addLine(String line) {
       this.lines.add(line);
    }

    public void run() throws Throwable {
        if (this.lines.size() == 0) {
            logger.error("No script lines to process");
            return;
        }
        String scriptFileName = null;
        scriptFileName = Util.getTemp() + "catspaw-" + System.currentTimeMillis() + Util.getGuid() + ".sh";
        logger.debug("Creating temp script " + scriptFileName);
        Path scriptFilePath = Paths.get(scriptFileName);
        if(Files.exists(scriptFilePath)) {
            Files.delete(scriptFilePath);
        }
        Files.createFile(scriptFilePath);
        scriptFilePath.toFile().setExecutable(true);
        FileWriter fileWriter = new FileWriter(scriptFileName);
        fileWriter.write("#!/usr/bin/bash" + System.lineSeparator());
        int i = 0;
        for(String string: this.lines) {
            i++;
            logger.debug("Command -> " + string);
            if(i < this.lines.size()) {
                fileWriter.write(string + System.lineSeparator());
            } else {
                fileWriter.write(string);
            }
        }
        fileWriter.close();
        String workingDirectoryName = Util.getTemp();
        Path dirPath = Paths.get(workingDirectoryName);;
        if(this.workingDirectory!=null&&!this.workingDirectory.equals("")) {
            workingDirectoryName += this.workingDirectory;
            dirPath = Paths.get(workingDirectoryName);
            if(!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
        }
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.redirectErrorStream(true);
        Process process = null;
        Map<String, String> environment = processBuilder.environment();
        try {
            processBuilder.directory(dirPath.toFile());
            processBuilder.command(this.getBash(environment), scriptFileName);
            process = processBuilder.start();
            Thread.sleep(250);
            if(Util.isUnix()) {
                process.waitFor();
            }
            logger.info("Dropped through waitFor()");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String outputFileName = Util.getTemp() + "catsrunlog-" + System.currentTimeMillis() + ".log";
            fileWriter = new FileWriter(outputFileName);
            String line = null;
            while ((line = reader.readLine()) != null) {
                logger.debug(line);
                fileWriter.write(line + System.lineSeparator());
            }
            fileWriter.close();
            this.stdoutFile = new File(outputFileName);
        } catch (Throwable t) {
            logger.error("Unable to run bash: " + t.getMessage());
            Assert.fail("Unable to Run bash");
        }
        this.exitValue = process.exitValue();
    }

    private String getBash(Map<String, String> environment) {
        if(Util.isUnix()) {
            if(System.getenv("SHELL")!=null) {
                return System.getenv("SHELL");
            } else {
                return "/bin/bash";
            }
        }
        return "C:\\Program Files\\Git\\bin\\bash";
    }
}