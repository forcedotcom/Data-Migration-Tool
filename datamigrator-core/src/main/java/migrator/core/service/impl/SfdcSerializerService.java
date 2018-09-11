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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;
import migrator.core.connect.SfdcConnection;
import migrator.core.connect.SfdcConnectionFactory;
import migrator.core.service.MetadataObjectHolder;
import migrator.core.service.PropertiesReader;
import migrator.core.service.SforceLookupProperties;
import migrator.core.service.SforceMasterDetail;
import migrator.core.service.SforceObject;
import migrator.core.service.SforceObjectPair;
import migrator.core.sobject.MigrableMasterDetailObject;
import migrator.core.sobject.ObjectMappingConfig;
import migrator.core.utils.ForceFileUtils;

/**
 * SfdcSerializer : Serialize the data from source org into a JSON based on object mapping JSON file
 *
 * @author anoop.singh
 */
public class SfdcSerializerService extends MigrableMasterDetailObject {

    static Logger log = Logger.getLogger(SfdcSerializerService.class.getName());
    private MetadataCompareService compare = null;

    public SfdcSerializerService() {
        super();
    }

    public SfdcSerializerService(String mapping) {
        super(ObjectMappingConfig.getMapping(new ForceFileUtils().getFileAsString(PropertiesReader.getInstance()
                .getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "object.mapping.dir") + mapping)), null);
    }

    @Override
    public void process() {
        if (!connect()) {
            return;
        }
        readMapping();
        setup();
        query();
        disconnect();
    }

    @Override
    public void setup() {

        // Standalone or parent objects
        for (SforceObject sForceObj : sForceObjectList) {
            MetadataObjectHolder.getInstance().init(SfdcConnection.ORG_TYPE.SOURCE, sfdcConnection,
                    sForceObj.getsObjectName());
            Set<String> commonFields = compare.findCommonFields(sForceObj.getsObjectName(), true);
            sForceObj.setDescRefObject(MetadataObjectHolder.getInstance().get(sForceObj.getsObjectName()));
            sForceObj.setCommonFields(commonFields);
        }

        for (Map.Entry<String, List<SforceLookupProperties>> entry : lookupPropertiesMap.entrySet()) {
            List<SforceLookupProperties> lookupList = entry.getValue();

            for (SforceLookupProperties loopProperty : lookupList) {
                String lookObjectName = loopProperty.getsLookupSObjectName();

                if (!doesLookupExists(lookObjectName)) {

                    MetadataObjectHolder.getInstance().init(SfdcConnection.ORG_TYPE.SOURCE, sfdcConnection,
                            lookObjectName);
                    Set<String> commonFields = compare.findCommonFields(lookObjectName, true);

                    SforceObject sForceObj = new SforceObject();
                    sForceObj.setLookup(true);
                    sForceObj.setsObjectName(lookObjectName);
                    sForceObj.setExternalIdField(loopProperty.getExternalIdField());
                    sForceObj.setWhere(loopProperty.getWhere());
                    sForceObj.setCompositeKeyFields(loopProperty.getCompositeKeyFields());
                    sForceObj.setDescRefObject(MetadataObjectHolder.getInstance().get(lookObjectName));
                    sForceObj.setUnmappedFieldsSet(loopProperty.getUnmappedFieldsSet());
                    sForceObj.setLookupProperties(lookupList);
                    sForceObj.setCommonFields(commonFields);
                    sForceObjectList.add(sForceObj);
                }
            }
        }
        // Setup for master-detail objects here
        for (Map.Entry<String, SforceMasterDetail> entry : objectMapping.getMasterDetailsMap().entrySet()) {
            String detailObjectName = entry.getKey();
            SforceMasterDetail masterObjectDetail = entry.getValue();

            MetadataObjectHolder.getInstance().init(SfdcConnection.ORG_TYPE.SOURCE, sfdcConnection, detailObjectName);
            Set<String> commonFields = compare.findCommonFields(detailObjectName, true);

            SforceObject sForceObj = new SforceObject();
            sForceObj.setMasterDetail(masterObjectDetail);
            sForceObj.setsObjectName(detailObjectName);
            sForceObj.setSequence(masterObjectDetail.getSequence());
            sForceObj.setExternalIdField(masterObjectDetail.getExternalIdField());
            sForceObj.setWhere(masterObjectDetail.getWhere());
            sForceObj.setBatchSize(masterObjectDetail.getBatchSize());
            sForceObj.setRefresh(masterObjectDetail.isRefresh());
            sForceObj.setUnmappedFieldsSet(masterObjectDetail.getUnmappedFieldsSet());
            sForceObj.setNullableFields(masterObjectDetail.getNullableFields());
            sForceObj.setMaskedFieldsSet(masterObjectDetail.getMaskedFieldsSet());

            sForceObj.setDescRefObject(MetadataObjectHolder.getInstance().get(detailObjectName));
            sForceObj.setCommonFields(commonFields);

            sForceObjectList.add(sForceObj);
        }

        Collections.sort(sForceObjectList, new Comparator<SforceObject>() {
            @Override
            public int compare(SforceObject p1, SforceObject p2) {
                return p1.getSequence() - p2.getSequence(); // Ascending
            }
        });
    }

    @Override
    public boolean connect() {
        PropertiesReader.getInstance();
        this.sfdcConnection = SfdcConnectionFactory.getConnection();
        if (!sfdcConnection.loginSource()) {
            System.out.println("Login failed!");
            return false;
        }
        return true;
    }

    private boolean doesLookupExists(String lookObjectName) {
        for (SforceObject sforceObject : sForceObjectList) {
            if (sforceObject.isLookup()) {
                if (sforceObject.getsObjectName().equals(lookObjectName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void serialize() {
        compare = new MetadataCompareService(true, false);
        process();
    }

    public void serialize(SforceObject sforceObject, List<QueryResult> queryResults) {
        JSONArray jsonArray = buildJSONSObject(sforceObject, queryResults);
        writeToFile(sforceObject.getsObjectName(), jsonArray);
    }

    public ArrayList<QueryResult> deSerialize(SforceObject sforceObject) {
        compare = new MetadataCompareService(false, true);

        String dataMappingDir = "/data-mappings/";
        if (PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "data.mapping.dir") != null) {
            dataMappingDir =
                    PropertiesReader.getInstance()
                            .getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "data.mapping.dir");
        }
        String dataJson =
                new ForceFileUtils().getFileAsString(dataMappingDir + sforceObject.getsObjectName() + ".json");
        ArrayList<QueryResult> queryResults = new ArrayList<QueryResult>();

        try {
            JSONArray jsonArray = new JSONArray(dataJson);
            SObject[] sObjects = new SObject[jsonArray.length()];

            Set<String> fieldList = sforceObject.getCommonFields();

            if (sforceObject.getUnmappedFieldsSet() != null) {
                Set<String> unmappedFieldList = sforceObject.getUnmappedFieldsSet();
                fieldList.removeAll(unmappedFieldList);
            }
            fieldList.add("Id");

            for (int index = 0; index < jsonArray.length(); index++) {
                JSONObject jsonObj = jsonArray.getJSONObject(index);

                SObject sObject = new SObject();
                sObject.setField("Type", jsonObj.get("Type"));

                for (String field : fieldList) {
                    if (!field.equals("Type")) {
                        sObject.setField(field, jsonObj.get(field));
                        sObjects[index] = sObject;
                    }
                }
            }
            QueryResult queryResult = new QueryResult();
            queryResult.setDone(true);
            queryResult.setSize(jsonArray.length());
            queryResult.setRecords(sObjects);
            queryResults.add(queryResult);

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return queryResults;
    }

    @Override
    public void query() {
        exportSource();
    }

    @SuppressWarnings("unchecked")
    private void exportSource() {
        Set<String> objectSet = new HashSet<String>();
        for (SforceObject sforceObject : sForceObjectList) {
            if (objectSet.contains(sforceObject.getsObjectName())) {
                continue;
            }
            sforceObject.setLookup(false); // TODO: Anoop: This is hack for JSON store
            objectSet.add(sforceObject.getsObjectName());
            Map<String, SforceObjectPair> sforceObjPairMap = new HashMap<String, SforceObjectPair>();
            sforceObjPairMap =
                    SfdcApiServiceImpl.getSOQLQueryService().persistJson(sfdcConnection.getSourceConnection(),
                            sforceObject, sforceObjPairMap, true);
        }
    }

    @SuppressWarnings("unchecked")
    private JSONArray buildJSONSObject(SforceObject sforceObject, List<QueryResult> queryResults) {
        JSONArray list = new JSONArray();
        Set<String> unmappedFields = sforceObject.getUnmappedFieldsSet();
        Set<String> fieldList = sforceObject.getCommonFields();

        try {
            for (int index = 0; index < queryResults.size(); index++) {
                QueryResult queryResult = queryResults.get(index);
                for (SObject sobject : queryResult.getRecords()) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("Type", sforceObject.getsObjectName());
                    for (String field : fieldList) {
                        if (unmappedFields != null && unmappedFields.contains(field)) {
                            continue;
                        }
                        String val = "";
                        if (sobject.getField(field) != null) {
                            val = (String) sobject.getField(field);
                        }
                        jsonObject.put(field, val);
                    }
                    list.put(jsonObject);
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return list;
    }

    private void writeToFile(String fileName, JSONArray jsonArray) {
        String dataMappingDir = "data-mappings/";
        if (PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "data.mapping.dir") != null) {
            dataMappingDir =
                    PropertiesReader.getInstance()
                            .getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "data.mapping.dir");
        }
        try {
            log.debug(fileName + " JSON=\n" + jsonArray.toString(4));
            log.debug("Filename=" + dataMappingDir + fileName + ".json");

            Path currentRelativePath = Paths.get(SfdcSerializerService.class.getResource("/build.properties").toURI());
            String currentPath = currentRelativePath.toAbsolutePath().toString();
            currentPath = currentPath.substring(0, currentPath.indexOf("build.properties"));
            if (currentPath != null && !currentPath.equals("")) {
                if (currentPath.indexOf("/target/classes") != -1) {
                    currentPath = currentPath.substring(0, currentPath.indexOf("/target/classes"));
                }
            }
            String abFileName = currentPath + "/src/main/resources/" + dataMappingDir + fileName + ".json";
            log.debug("Absolute File Name===" + abFileName);
            JSONArray newJsonArray = consolidateJsonObject(abFileName, jsonArray);
            new ForceFileUtils().writeToFile(abFileName, newJsonArray.toString(4), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // If data .json existing exists, then merge the data from new JSONArray into existing .json
    private JSONArray consolidateJsonObject(String fileName, JSONArray jsonArray) throws Exception {
        JSONArray newList = jsonArray;
        String oid;
        List<String> newListIds = new ArrayList<String>();
        for (int a = 0; a < newList.length(); a++) {
            newListIds.add(newList.getJSONObject(a).getString("Id"));
        }

        File file = new File(fileName);
        if (file.exists() && !newListIds.isEmpty()) {
            String content = new ForceFileUtils().readFile(fileName);
            JSONArray oldlist = new JSONArray(content);

            if (oldlist.length() > 0) {
                JSONObject oldJsonObject;
                for (int i = 0; i < oldlist.length(); i++) {
                    oid = oldlist.getJSONObject(i).getString("Id");

                    if (!newListIds.contains(oid)) {
                        oldJsonObject = oldlist.getJSONObject(i);
                        newList.put(oldJsonObject);
                    }
                }
            }
        }
        return newList;
    }
}
