/*
 * Copyright (c) 2018, Salesforce.com, Inc. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 * 
 * 3. Neither the name of Salesforce.com nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package migrator.core.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class ForceFileUtils {

    static Logger log = Logger.getLogger(ForceFileUtils.class.getName());
    private static final String FILE_PREFIX = "file:";

    public String getFileAsString(String fileName) {
        String data = null;
        try {
            data = FileUtils.readFileToString(FileUtils.toFile(ForceFileUtils.class.getResource(fileName)), "UTF-8");
        } catch (FileNotFoundException e) {
            log.error("Exception, couldn't find " + fileName, e);
        } catch (IOException e) {
            log.error("Exception, couldn't find " + fileName, e);
        } finally {
        }
        return data;
    }

    public String readFile(String fileName) {
        StringBuffer data = new StringBuffer();
        BufferedReader reader = null;

        try {
            fileName = getAbsoluteFileName(fileName);

            // Are we looking in the jar?
            if (fileName.startsWith(FILE_PREFIX)) {

                String[] parts = fileName.split("!");

                if (parts.length == 2) {
                    reader =
                            new BufferedReader(
                                    new InputStreamReader(ForceFileUtils.class.getResourceAsStream(parts[1])));
                }
                // No? So we're looking in the file system
            } else {
                try {
                    reader = new BufferedReader(new FileReader(fileName));
                } catch (FileNotFoundException e) {
                    log.error("Couldn't find " + fileName, e);
                }
            }
            String line;
            while ((line = reader.readLine()) != null) {
                data.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
            }
        }
        return data.toString();
    }

    public String writeToFile(String fileName, String data, boolean append) {
        // fileName = getAbsoluteFileName(fileName);
        BufferedWriter wr = null;
        try {
            // Open the file for writing, without removing its current content.
            wr = new BufferedWriter(new FileWriter(new File(fileName).getCanonicalFile(), append));

            // Write a sample string to the end of the file.
            wr.write(data);
        } catch (IOException ex) {
            System.err.println("An IOException was caught!");
            ex.printStackTrace();
        } finally {
            // Close the file.
            try {
                wr.close();
            } catch (IOException ex) {
                System.err.println("An IOException was caught!");
                ex.printStackTrace();
            }
        }
        return data;
    }

    public static void closeFile(BufferedReader br) {
        if (br != null) {
            try {
                br.close();
            } catch (IOException e) {
                log.error("BufferedReader exception", e);
            }
        }
    }

    public List<String> getFileNamesInDirectory(String inputDirectory, boolean includeDirectories) {
        List<String> fileNames = new ArrayList<String>();

        inputDirectory = getAbsoluteFileName(inputDirectory);


        File dir = new File(inputDirectory);

        if (dir != null && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (includeDirectories || (!includeDirectories && file.isFile())) {
                    fileNames.add(file.getAbsolutePath());
                }
            }
        } else {
            log.warn("\"" + inputDirectory + "\" does not exist in directory.");
        }

        return fileNames;
    }

    public String getAbsoluteFileName(String inputFileName) {

        if (inputFileName.startsWith("/")) {
            return inputFileName;
        }

        if (ClassLoader.getSystemResource(inputFileName) != null) {
            inputFileName = ClassLoader.getSystemResource(inputFileName).getFile();
            inputFileName = inputFileName.replaceAll("%20", "\\ ");
        }

        return inputFileName;
    }

}
