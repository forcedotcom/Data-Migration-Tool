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
package migrator.core.sobject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import migrator.core.service.SforceLookupProperties;
import migrator.core.service.SforceMasterDetail;
import migrator.core.service.SforceObject;

/**
 * ObjectMappingConfig : Immutable ObjectMappingConfig object, reads the JSON objects mapping
 *
 * @author anoop.singh
 */

public class ObjectMappingConfig {
    static Logger log = Logger.getLogger(ObjectMappingConfig.class.getName());

    private Migrable.RelationType relationType = null;

    private Map<String, SforceMasterDetail> masterDetailsMap = new HashMap<String, SforceMasterDetail>();
    private Map<String, List<SforceLookupProperties>> lookupPropertiesMap =
            new HashMap<String, List<SforceLookupProperties>>();
    private List<SforceObject> sForceObjectList = new ArrayList<SforceObject>();

    private ObjectMappingConfig(Migrable.RelationType relationType, List<SforceObject> sForceObjectList,
            Map<String, List<SforceLookupProperties>> lookupPropertiesMap,
            Map<String, SforceMasterDetail> masterDetailsMap) {
        this.relationType = relationType;
        this.sForceObjectList = sForceObjectList;
        this.lookupPropertiesMap = lookupPropertiesMap;
        this.masterDetailsMap = masterDetailsMap;
    }

    /**
     * Returns a new ObjectMappingConfig
     *
     * @return a mapping
     */
    public static ObjectMappingConfig getMapping(String mapping) {
        return ObjectMappingConfig.reader(mapping).read();
    }

    /**
     * Returns a new ObjectMappingConfigReader
     *
     * @return a reader
     */
    public static ObjectMappingConfigReader reader(String mapping) {
        return new ObjectMappingConfigReader(mapping);
    }

    public Migrable.RelationType getRelationType() {
        return relationType;
    }

    public Map<String, SforceMasterDetail> getMasterDetailsMap() {
        return masterDetailsMap;
    }

    public Map<String, List<SforceLookupProperties>> getLookupPropertiesMap() {
        return lookupPropertiesMap;
    }

    public List<SforceObject> getsForceObjectList() {
        return sForceObjectList;
    }

    /**
     * ObjectMappingConfig reader
     */
    public static class ObjectMappingConfigReader {
        private String mapping;
        private Migrable.RelationType relationType = null;
        private Map<String, SforceMasterDetail> masterDetailsMapInt = new HashMap<String, SforceMasterDetail>();
        private Map<String, List<SforceLookupProperties>> lookupPropertiesMapInt =
                new HashMap<String, List<SforceLookupProperties>>();
        private List<SforceObject> sForceObjectListInt = new ArrayList<SforceObject>();

        public ObjectMappingConfigReader(String mapping) {
            this.mapping = mapping;
        }

        public ObjectMappingConfig read() {
            readMapping();
            if (relationType == null) {
                relationType = Migrable.RelationType.LOOKUP;
            }
            return new ObjectMappingConfig(relationType, sForceObjectListInt, lookupPropertiesMapInt,
                    masterDetailsMapInt);
        }

        // Migrable.RelationType.LOOKUP.toString()
        protected void readMapping() {
            try {
                JSONArray jsonArray = new JSONArray(mapping);

                for (int index = 0; index < jsonArray.length(); index++) {
                    JSONObject jsonObj = jsonArray.getJSONObject(index);

                    String jParent = (String) jsonObj.get("parent");

                    SforceObject sForceObj = new SforceObject();
                    sForceObj.setsObjectName(jParent);

                    Boolean hasUnmappedFields = jsonObj.has("unmappedFields");
                    if (hasUnmappedFields) {
                        JSONArray unmappedFields = (JSONArray) jsonObj.getJSONArray("unmappedFields");
                        Set<String> unmappedFieldsSet = new HashSet<String>();
                        for (int index4 = 0; index4 < unmappedFields.length(); index4++) {
                            unmappedFieldsSet.add(unmappedFields.getString(index4));
                        }
                        sForceObj.setUnmappedFieldsSet(unmappedFieldsSet);
                    }
                    Boolean hasMaskedFields = jsonObj.has("maskedFields");
                    if (hasMaskedFields) {
                        JSONArray maskedFields = (JSONArray) jsonObj.getJSONArray("maskedFields");
                        Set<String> maskedFieldSet = new HashSet<String>();
                        for (int index4 = 0; index4 < maskedFields.length(); index4++) {
                            maskedFieldSet.add(maskedFields.getString(index4));
                        }
                        sForceObj.setMaskedFieldsSet(maskedFieldSet);
                    }
                    Boolean hasFieldMapping = jsonObj.has("fieldMapping");
                    if (hasFieldMapping) {
                        JSONArray fieldMapping = (JSONArray) jsonObj.getJSONArray("fieldMapping");
                        Map<String, String> fieldMappingMap = new HashMap<String, String>();
                        for (int index4 = 0; index4 < fieldMapping.length(); index4++) {
                            JSONObject entry = fieldMapping.getJSONObject(index4);
                            Iterator<?> keys = entry.keys();
                            while (keys.hasNext()) {
                                String key = (String) keys.next();
                                fieldMappingMap.put(key, entry.get(key).toString());
                            }
                        }
                        sForceObj.setFieldsMapping(fieldMappingMap);
                    }
                    Boolean hasDefaultValues = jsonObj.has("defaultValues");
                    if (hasDefaultValues) {
                        JSONArray defaultValues = (JSONArray) jsonObj.getJSONArray("defaultValues");
                        Map<String, String> defaultValuesMap = new HashMap<String, String>();
                        for (int index4 = 0; index4 < defaultValues.length(); index4++) {
                            JSONObject entry = defaultValues.getJSONObject(index4);
                            Iterator<?> keys = entry.keys();
                            while (keys.hasNext()) {
                                String key = (String) keys.next();
                                defaultValuesMap.put(key, entry.get(key).toString());
                            }
                        }
                        sForceObj.setDefaultValues(defaultValuesMap);
                    }
                    Boolean hasNullable = jsonObj.has("nullable");
                    if (hasNullable) {
                        JSONArray nullableFields = (JSONArray) jsonObj.getJSONArray("nullable");
                        List<String> nullbaleFields = new ArrayList<String>();
                        for (int index4 = 0; index4 < nullableFields.length(); index4++) {
                            nullbaleFields.add(nullableFields.getString(index4));
                        }
                        sForceObj.setNullableFields(nullbaleFields);
                    }

                    Boolean hasExternalIdField = jsonObj.has("externalIdField");
                    if (hasExternalIdField) {
                        sForceObj.setExternalIdField((String) jsonObj.get("externalIdField"));
                    }
                    Boolean hasWhere = jsonObj.has("where");
                    if (hasWhere) {
                        sForceObj.setWhere((String) jsonObj.get("where"));
                    }
                    Boolean hasRefresh = jsonObj.has("refresh");
                    if (hasRefresh) {
                        sForceObj.setRefresh((Boolean) jsonObj.get("refresh"));
                        setRelationType(Migrable.RelationType.HIERARCHICAL);
                    }
                    Boolean batchSize = jsonObj.has("batchSize");
                    if (batchSize) {
                        sForceObj.setBatchSize((Integer) jsonObj.get("batchSize"));
                    }
                    sForceObjectListInt.add(sForceObj);

                    // handle children (might be nested)
                    readChilds(jsonObj, jParent);

                    // handle lookups (parent level)
                    readLookups(jsonObj, jParent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Read children in a map structure
        private void readChilds(JSONObject jsonObj, String sObjectName) throws Exception {
            Boolean hasChilds = jsonObj.has("children");
            if (hasChilds) {
                setRelationType(Migrable.RelationType.MASTERDETAIL);

                JSONArray jChildArray = (JSONArray) jsonObj.get("children");
                for (int index2 = 0; index2 < jChildArray.length(); index2++) {
                    JSONObject jsonChildObj = jChildArray.getJSONObject(index2);

                    String childObject = (String) jsonChildObj.get("childObject");
                    String childFieldMappedField = (String) jsonChildObj.get("parentMappedField");

                    Integer sequence = 0;
                    boolean hasSequence = jsonChildObj.has("sequence");
                    if (hasSequence) {
                        sequence = (Integer) jsonChildObj.get("sequence");
                    }
                    Boolean hasExternalIdField = jsonChildObj.has("externalIdField");

                    Map<String, String> parentFieldObjectMap = null;
                    SforceMasterDetail masterDetail = masterDetailsMapInt.get(childObject);
                    if (masterDetail == null) {
                        parentFieldObjectMap = new HashMap<String, String>();
                        masterDetail = new SforceMasterDetail(childObject, parentFieldObjectMap);
                    } else {
                        parentFieldObjectMap = masterDetail.getParentFieldObjectMap();
                    }
                    if (hasExternalIdField) {
                        masterDetail.setExternalIdField((String) jsonChildObj.get("externalIdField"));
                    }
                    Boolean hasWhere = jsonChildObj.has("where");
                    if (hasWhere) {
                        masterDetail.setWhere((String) jsonChildObj.get("where"));
                    }

                    Boolean hasRefresh = jsonChildObj.has("refresh");
                    if (hasRefresh) {
                        masterDetail.setRefresh((Boolean) jsonChildObj.get("refresh"));
                        setRelationType(Migrable.RelationType.HIERARCHICAL);
                    }
                    Boolean batchSize = jsonChildObj.has("batchSize");
                    if (batchSize) {
                        masterDetail.setBatchSize((Integer) jsonChildObj.get("batchSize"));
                    }

                    Boolean hasUnmappedFields = jsonChildObj.has("unmappedFields");
                    if (hasUnmappedFields) {
                        JSONArray unmappedFields = (JSONArray) jsonChildObj.getJSONArray("unmappedFields");
                        Set<String> unmappedFieldsSet = new HashSet<String>();
                        for (int index4 = 0; index4 < unmappedFields.length(); index4++) {
                            unmappedFieldsSet.add(unmappedFields.getString(index4));
                        }
                        masterDetail.setUnmappedFieldsSet(unmappedFieldsSet);
                    }
                    Boolean hasMaskedFields = jsonChildObj.has("maskedFields");
                    if (hasMaskedFields) {
                        JSONArray maskedFields = (JSONArray) jsonChildObj.getJSONArray("maskedFields");
                        Set<String> maskedFieldSet = new HashSet<String>();
                        for (int index4 = 0; index4 < maskedFields.length(); index4++) {
                            maskedFieldSet.add(maskedFields.getString(index4));
                        }
                        masterDetail.setMaskedFieldsSet(maskedFieldSet);
                    }
                    Boolean hasFieldMapping = jsonChildObj.has("fieldMapping");
                    if (hasFieldMapping) {
                        JSONArray fieldMapping = (JSONArray) jsonChildObj.getJSONArray("fieldMapping");
                        Map<String, String> fieldMappingMap = new HashMap<String, String>();
                        for (int index4 = 0; index4 < fieldMapping.length(); index4++) {
                            JSONObject entry = fieldMapping.getJSONObject(index4);
                            Iterator<?> keys = entry.keys();
                            while (keys.hasNext()) {
                                String key = (String) keys.next();
                                fieldMappingMap.put(key, entry.get(key).toString());
                            }
                        }
                        masterDetail.setFieldsMapping(fieldMappingMap);
                    }
                    Boolean hasDefaultValues = jsonChildObj.has("defaultValues");
                    if (hasDefaultValues) {
                        JSONArray defaultValues = (JSONArray) jsonChildObj.getJSONArray("defaultValues");
                        Map<String, String> defaultValuesMap = new HashMap<String, String>();
                        for (int index4 = 0; index4 < defaultValues.length(); index4++) {
                            JSONObject entry = defaultValues.getJSONObject(index4);
                            Iterator<?> keys = entry.keys();
                            while (keys.hasNext()) {
                                String key = (String) keys.next();
                                defaultValuesMap.put(key, entry.get(key).toString());
                            }
                        }
                        masterDetail.setDefaultValues(defaultValuesMap);
                    }
                    Boolean hasNullable = jsonChildObj.has("nullable");
                    if (hasNullable) {
                        JSONArray nullableFields = (JSONArray) jsonChildObj.getJSONArray("nullable");
                        List<String> nullbaleFields = new ArrayList<String>();
                        for (int index4 = 0; index4 < nullableFields.length(); index4++) {
                            nullbaleFields.add(nullableFields.getString(index4));
                        }
                        masterDetail.setNullableFields(nullbaleFields);
                    }

                    // There can be multiple parents, add them to map
                    parentFieldObjectMap.put(childFieldMappedField, sObjectName);
                    masterDetail.setSequence(sequence);
                    masterDetailsMapInt.put(childObject, masterDetail);

                    // The child might have lookups as well, handle it
                    readLookups(jsonChildObj, childObject);

                    hasChilds = jsonChildObj.has("children");
                    if (hasChilds) {
                        readChilds(jsonChildObj, childObject);
                    }
                }
            }
        }

        // Read lookups in a map structure
        private void readLookups(JSONObject jsonObj, String sObjectName) throws Exception {

            Boolean hasLookups = jsonObj.has("lookups");
            if (hasLookups) {

                JSONArray jLookupArray = (JSONArray) jsonObj.get("lookups");
                for (int index3 = 0; index3 < jLookupArray.length(); index3++) {
                    JSONObject jsonLookupObj = jLookupArray.getJSONObject(index3);

                    String lookupFieldMappedField = (String) jsonLookupObj.get("lookupMappedField");
                    String lookupObject = (String) jsonLookupObj.get("lookupObject");
                    JSONArray keys = (JSONArray) jsonLookupObj.getJSONArray("keys");

                    List<String> keysList = new ArrayList<String>();
                    for (int index4 = 0; index4 < keys.length(); index4++) {
                        keysList.add(keys.getString(index4));
                    }
                    Boolean hasExternalIdField = jsonLookupObj.has("externalIdField");

                    List<SforceLookupProperties> lookupListProps = lookupPropertiesMapInt.get(sObjectName);
                    if (lookupListProps == null) {
                        lookupListProps = new ArrayList<SforceLookupProperties>();
                    }
                    SforceLookupProperties lookProps =
                            new SforceLookupProperties(sObjectName, lookupFieldMappedField, lookupObject, keysList);
                    if (hasExternalIdField) {
                        lookProps.setExternalIdField((String) jsonLookupObj.get("externalIdField"));
                    }
                    Boolean hasWhere = jsonLookupObj.has("where");
                    if (hasWhere) {
                        lookProps.setWhere((String) jsonLookupObj.get("where"));
                    }
                    Boolean hasUnmappedFields = jsonLookupObj.has("unmappedFields");
                    if (hasUnmappedFields) {
                        JSONArray unmappedFields = (JSONArray) jsonLookupObj.getJSONArray("unmappedFields");
                        Set<String> unmappedFieldsSet = new HashSet<String>();
                        for (int index4 = 0; index4 < unmappedFields.length(); index4++) {
                            unmappedFieldsSet.add(unmappedFields.getString(index4));
                        }
                        lookProps.setUnmappedFieldsSet(unmappedFieldsSet);
                    }

                    lookupListProps.add(lookProps);

                    lookupPropertiesMapInt.put(sObjectName, lookupListProps);

                    hasLookups = jsonLookupObj.has("lookups");
                    if (hasLookups) {
                        readLookups(jsonLookupObj, lookupObject);
                    }
                }
            }
        }

        private void setRelationType(Migrable.RelationType type) {
            if (relationType == Migrable.RelationType.HIERARCHICAL) {
                return;
            }
            if (relationType != Migrable.RelationType.HIERARCHICAL && type == Migrable.RelationType.HIERARCHICAL) {
                relationType = type;
                return;
            }
            if (relationType == Migrable.RelationType.MASTERDETAIL || type == Migrable.RelationType.MASTERDETAIL) {
                relationType = type;
            }
        }
    }

}
