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
package migrator.core.service.impl;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import migrator.core.connect.SfdcConnection;
import migrator.core.service.MetadataObjectHolder;
import migrator.core.service.PropertiesReader;
import migrator.core.utils.ForceFileUtils;

/**
 * MetadataCompareService - Compares metadata
 *
 * @author anoop.singh
 */
public class MetadataCompareService {

    SfdcConnection sfdcConnection;
    List<MetadataObjectHolder.MetadataRefObject> allRefObjects = null;

    public MetadataCompareService() {
        this(true, true);
    }

    public MetadataCompareService(boolean loginSource, boolean loginTarget) {
        sfdcConnection = new SfdcConnection();
        if (loginSource && !sfdcConnection.loginSource()) {
            System.out.println("Source Login failed!");
            return;
        }
        if (loginTarget && !sfdcConnection.loginTarget()) {
            System.out.println("Target Login failed!");
            return;
        }
    }

    // This method is not supported for JSON-Org migration.
    // Supported only for org-org migration
    public boolean compare(String objects) {
        String[] objectsArray = objects.replace(" ", "").split(",");
        List<String> migrableObjects = Arrays.asList(objectsArray);

        String sourceType =
                PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "source.type");
        for (String migrableObject : migrableObjects) {
            if (sourceType == null || sourceType.equals("org")) {
                MetadataObjectHolder.getInstance().init(SfdcConnection.ORG_TYPE.SOURCE, sfdcConnection, migrableObject,
                        migrableObjects);
            }
            MetadataObjectHolder.getInstance().initTarget(SfdcConnection.ORG_TYPE.TARGET, sfdcConnection,
                    migrableObject, migrableObjects);
        }
        // disconnect from source and target
        sfdcConnection.disconnect();

        compareMetadata();

        return false;
    }

    public Set<String> findCommonFields(String objectName) {
        return findCommonFields(objectName, false);
    }

    public Set<String> findCommonFields(String objectName, boolean serialize) {
        Set<String> commonFieldSet = new HashSet<String>();

        List<String> migrableObjects = Arrays.asList(objectName);
        Set<String> srcFieldSet = new HashSet<String>();

        String sourceType =
                PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "source.type");
        if (sourceType == null || sourceType.equals("org")) {
            MetadataObjectHolder.getInstance().init(SfdcConnection.ORG_TYPE.SOURCE, sfdcConnection, objectName,
                    migrableObjects);
            Map<String, MetadataObjectHolder.MetadataRefObject> srcRefObjectMap =
                    MetadataObjectHolder.getInstance().getDescribeRefObjectMap();
            MetadataObjectHolder.MetadataRefObject srcRefObj = srcRefObjectMap.get(objectName);
            srcFieldSet = srcRefObj.getFieldToTypeMapAll().keySet();
        } else if (sourceType.equals("json")) {
            String dataMappingDir = "data-mappings/";
            if (PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "data.mapping.dir") != null) {
                dataMappingDir =
                        PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD,
                                "data.mapping.dir");
            }
            String currentPath = "";
            try {
                Path currentRelativePath =
                        Paths.get(MetadataCompareService.class.getResource("/build.properties").toURI());
                currentPath = currentRelativePath.toAbsolutePath().toString();
                currentPath = currentPath.substring(0, currentPath.indexOf("build.properties"));
                if (currentPath != null && !currentPath.equals("")) {
                    if (currentPath.indexOf("/target/classes") != -1) {
                        currentPath = currentPath.substring(0, currentPath.indexOf("/target/classes"));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                String abFileName = currentPath + "/src/main/resources/" + dataMappingDir + objectName + ".json";
                File file = new File(abFileName);
                if (file.exists()) {
                    String content = new ForceFileUtils().readFile(abFileName);
                    JSONArray jsonArray = new JSONArray(content);
                    if (jsonArray.length() > 0) {
                        JSONObject jsonObject = jsonArray.getJSONObject(0);
                        Iterator<?> keys = jsonObject.keys();
                        while (keys.hasNext()) {
                            String key = (String) keys.next();
                            if (!key.equals("Id"))
                                srcFieldSet.add(key);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        SfdcConnection.ORG_TYPE orgType;
        // Use source org only as target for serialization
        if (serialize) {
            orgType = SfdcConnection.ORG_TYPE.SOURCE;
        } else {
            orgType = SfdcConnection.ORG_TYPE.TARGET;
        }

        MetadataObjectHolder.getInstance().initTarget(orgType, sfdcConnection, objectName, migrableObjects);
        Map<String, MetadataObjectHolder.MetadataRefObject> tgtRefObjectMap =
                MetadataObjectHolder.getInstance().getTargetDescribeRefObjectMap();
        MetadataObjectHolder.MetadataRefObject tgtRefObj = tgtRefObjectMap.get(objectName);
        Set<String> tgtFieldSet = tgtRefObj.getFieldToTypeMapAll().keySet();

        // Check if fields are different?
        boolean diffFound = false;
        Set<String> srcExtraFieldSet = new HashSet<String>();
        Set<String> tgtExtraFieldSet = new HashSet<String>();

        if (srcFieldSet.size() != tgtFieldSet.size()) {
            diffFound = true;
        }

        if (!srcFieldSet.containsAll(tgtFieldSet)) {
            diffFound = true;
        }

        for (String srcField : srcFieldSet) {
            if (tgtFieldSet.contains(srcField)) {
                commonFieldSet.add(srcField);
            } else {
                srcExtraFieldSet.add(srcField);
            }
            tgtExtraFieldSet.remove(srcField);
        }
        // Left overs in target are extras
        tgtExtraFieldSet.addAll(tgtExtraFieldSet);

        if (tgtRefObjectMap.containsKey(objectName)) {
            tgtRefObjectMap.remove(objectName);
        }

        return commonFieldSet;
    }

    // This method is not supported for JSON-Org migration.
    // Supported only for org-org migration
    private Boolean compareMetadata() {

        Map<String, MetadataObjectHolder.MetadataRefObject> srcRefObjectMap =
                MetadataObjectHolder.getInstance().getDescribeRefObjectMap();
        Map<String, MetadataObjectHolder.MetadataRefObject> tgtRefObjectMap =
                MetadataObjectHolder.getInstance().getTargetDescribeRefObjectMap();

        // 1. Check if object exists in source/target, some object are missing
        if (srcRefObjectMap.size() != tgtRefObjectMap.size()) {
            // System.out.println("Metadata differences found!!!");
        }

        // 2. Check if count of fields same?

        // 3. Check if fields are different?
        for (String srcKey : srcRefObjectMap.keySet()) {
            boolean diffFound = false;
            Set<String> commonFieldSet = new HashSet<String>();
            Set<String> srcExtraFieldSet = new HashSet<String>();
            Set<String> tgtExtraFieldSet = new HashSet<String>();

            MetadataObjectHolder.MetadataRefObject srcRefObj = srcRefObjectMap.get(srcKey);

            Set<String> srcFieldSet = srcRefObj.getFieldToTypeMapAll().keySet();

            MetadataObjectHolder.MetadataRefObject tgtRefObj = tgtRefObjectMap.get(srcKey);
            Set<String> tgtFieldSet = tgtRefObj.getFieldToTypeMapAll().keySet();

            if (srcFieldSet.size() != tgtFieldSet.size()) {
                diffFound = true;
            }

            if (!srcFieldSet.containsAll(tgtFieldSet)) {
                diffFound = true;
            }

            if (srcFieldSet.size() != tgtFieldSet.size()) {
                System.out.println("\n Number of Fields for object::[" + srcKey + "]" + " Source Size:"
                        + srcFieldSet.size() + " Target Size:" + tgtFieldSet.size());
            }

            for (String srcField : srcFieldSet) {
                if (tgtFieldSet.contains(srcField)) {
                    commonFieldSet.add(srcField);
                } else {
                    srcExtraFieldSet.add(srcField);
                }
                tgtFieldSet.remove(srcField);
            }
            // Left overs in target are extras
            tgtExtraFieldSet.addAll(tgtFieldSet);

            if (tgtRefObjectMap.containsKey(srcKey)) {
                tgtRefObjectMap.remove(srcKey);
            }

            if (srcExtraFieldSet.size() > 0 || tgtExtraFieldSet.size() > 0) {
                System.out.println("Field differences for object::" + srcKey);
            }

            if (srcExtraFieldSet.size() > 0) {
                System.out.println("  Source Org Field differences:");
                for (String sField : srcExtraFieldSet) {
                    System.out.println("	" + sField);
                }
            }
            if (tgtExtraFieldSet.size() > 0) {
                System.out.println("  Target Org Field differences:");
                for (String tField : tgtExtraFieldSet) {
                    System.out.println("	" + tField);
                }
            }
        }

        // Extra objects in target
        if (tgtRefObjectMap.size() > 0) {
            System.out.println("Difference in objects!!!");
        }
        for (String tgtKey : tgtRefObjectMap.keySet()) {
            System.out.println(tgtKey);
        }

        // 4. Check if the field type is different?

        return false;
    }

}
