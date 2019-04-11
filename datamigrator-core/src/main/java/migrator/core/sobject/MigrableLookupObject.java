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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import com.sforce.soap.partner.FieldType;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.bind.CalendarCodec;
import com.sforce.ws.bind.DateCodec;
import com.sforce.ws.types.Time;
import com.sforce.ws.util.Base64;
import migrator.core.connect.SfdcConnection;
import migrator.core.mask.Masker;
import migrator.core.service.MetadataObjectHolder;
import migrator.core.service.PropertiesReader;
import migrator.core.service.SforceLookupProperties;
import migrator.core.service.SforceMasterDetail;
import migrator.core.service.SforceObject;
import migrator.core.service.SforceObjectPair;
import migrator.core.service.impl.MetadataCompareService;
import migrator.core.service.impl.SfdcApiServiceImpl;
import migrator.core.utils.Utils;

/**
 * MigrableLookupObject : Abstract class to support lookup relationships
 *
 * @author anoop.singh
 */
public class MigrableLookupObject extends MigrableObject {

    protected Map<String, List<SforceLookupProperties>> lookupPropertiesMap =
            new HashMap<String, List<SforceLookupProperties>>();
    protected CalendarCodec calendarCodec = new CalendarCodec();
    protected DateCodec dateCodec = new DateCodec();
    Masker masker = new Masker();
    static Logger log = Logger.getLogger(MigrableLookupObject.class.getName());

    public MigrableLookupObject() {
        super();
    }

    public MigrableLookupObject(ObjectMappingConfig objectMapping, String operation) {
        super(objectMapping, operation);
    }

    @Override
    public void process() {
        if (!connect()) {
            return;
        }
        readMapping();
        setup();
        if (handleDelete()) {
            return;
        }
        query();
        insert();
        cleanup();
        disconnect();
    }

    /**
     * Setup the migrable objects: 1. Gets the Describe details for each SObject 2. Creates a
     * SforceObject (holder of sobjectName, describe details and records) 3. Add SforceObject into a
     * map
     *
     */
    @Override
    public void setup() {
        boolean sourceLogin = false;
        String sourceType =
                PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "source.type");
        if (sourceType == null || sourceType.equals("org")) {
            sourceLogin = true;
        }

        MetadataCompareService compare = new MetadataCompareService(sourceLogin, true);

        // Standalone or parent objects
        for (SforceObject sForceObj : sForceObjectList) {
            MetadataObjectHolder.getInstance().init(SfdcConnection.ORG_TYPE.TARGET, sfdcConnection,
                    sForceObj.getsObjectName());
            Set<String> commonFields = compare.findCommonFields(sForceObj.getsObjectName());
            sForceObj.setDescRefObject(MetadataObjectHolder.getInstance().get(sForceObj.getsObjectName()));
            sForceObj.setCommonFields(commonFields);
        }

        for (Map.Entry<String, List<SforceLookupProperties>> entry : lookupPropertiesMap.entrySet()) {
            List<SforceLookupProperties> lookupList = entry.getValue();

            for (SforceLookupProperties loopProperty : lookupList) {
                String lookObjectName = loopProperty.getsLookupSObjectName();

                if (!doesLookupExists(lookObjectName)) {

                    MetadataObjectHolder.getInstance().init(SfdcConnection.ORG_TYPE.TARGET, sfdcConnection,
                            lookObjectName);
                    Set<String> commonFields = compare.findCommonFields(lookObjectName);

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

            MetadataObjectHolder.getInstance().init(SfdcConnection.ORG_TYPE.TARGET, sfdcConnection, detailObjectName);
            Set<String> commonFields = compare.findCommonFields(detailObjectName);

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
            sForceObj.setFieldsMapping(masterObjectDetail.getFieldsMapping());
            sForceObj.setDefaultValues(masterObjectDetail.getDefaultValues());
            sForceObj.setDescRefObject(MetadataObjectHolder.getInstance().get(detailObjectName));
            sForceObj.setCommonFields(commonFields);

            sForceObjectList.add(sForceObj);
        }

    }

    public void cleanup() {
        if (sForceObjectList != null) {
            for (SforceObject obj : sForceObjectList)
                obj.cleanup();
            sForceObjectList.clear();
        }
        if (lookupPropertiesMap != null)
            lookupPropertiesMap.clear();
        sForceObjectList = null;
        lookupPropertiesMap = null;
        calendarCodec = null;
        dateCodec = null;
    }

    // Using source Org only
    @SuppressWarnings("unchecked")
    private void exportSource() {

        for (SforceObject sforceObject : sForceObjectList) {
            // Its parent or standalone
            if (!sforceObject.isLookup()) {
                Map<String, SforceObjectPair> sforceObjPairMap = new HashMap<String, SforceObjectPair>();
                sforceObjPairMap =
                        SfdcApiServiceImpl.getSOQLQueryService().query(sfdcConnection.getSourceConnection(),
                                sforceObject, sforceObjPairMap, true);
                sforceObject.setRecordsMap(sforceObjPairMap);
            }
        }
    }

    // Fetches lookups from source and target
    public void exportLookup() {
        Map<String, SforceObject> lookupObjects = getUniqueLookupObjects();

        for (SforceObject sforceObject : lookupObjects.values()) {
            if (sforceObject.isLookup()) {
                SfdcApiServiceImpl.getSOQLQueryService().queryLookup(sfdcConnection.getSourceConnection(),
                        sforceObject, lookupPropertiesMap, true);
                SfdcApiServiceImpl.getSOQLQueryService().queryLookup(sfdcConnection.getTargetConnection(),
                        sforceObject, lookupPropertiesMap, false);
            }
        }
    }

    // Fetches lookups from source and target
    public void exportLookup(String sObjectName) {
        Map<String, SforceObject> lookupObjects = getUniqueLookupObjects();

        for (SforceObject sforceObject : lookupObjects.values()) {
            if (sforceObject.isLookup() && sforceObject.getsObjectName().equals(sObjectName)) {
                for (Map.Entry<String, List<SforceLookupProperties>> entry1 : lookupPropertiesMap.entrySet()) {
                    List<SforceLookupProperties> lookupList = entry1.getValue();
                    for (SforceLookupProperties loopProperty : lookupList) {
                        if (loopProperty.getsLookupSObjectName().equalsIgnoreCase(sObjectName)) {
                            SfdcApiServiceImpl.getSOQLQueryService().queryLookup(sfdcConnection.getSourceConnection(),
                                    sforceObject, lookupPropertiesMap, true);
                            SfdcApiServiceImpl.getSOQLQueryService().queryLookup(sfdcConnection.getTargetConnection(),
                                    sforceObject, lookupPropertiesMap, false);
                        }
                    }
                }
            }

        }
    }

    // This is now a parent object but also a lookup. It's not yet in target, so
    // re-issue
    // lookup query, so we have target ids set.
    public void reIssueLookupQuery(String lookupObjectName) {

        for (SforceObject sforceObject : sForceObjectList) {
            String objectName = sforceObject.getsObjectName();

            if (sforceObject.isLookup() && objectName.equals(lookupObjectName)) {

                // Setup lookups - target
                for (Map.Entry<String, List<SforceLookupProperties>> entry2 : lookupPropertiesMap.entrySet()) {
                    List<SforceLookupProperties> lookupList = entry2.getValue();
                    for (SforceLookupProperties loopProperty : lookupList) {

                        if (loopProperty.getsLookupSObjectName().equals(lookupObjectName)) {

                            Map<String, SforceObjectPair> sforceObjPairMap = sforceObject.getLookupRecordsMap();
                            if (sforceObjPairMap == null) {
                                sforceObjPairMap = new HashMap<String, SforceObjectPair>();
                            }

                            String compositeKey =
                                    SfdcApiServiceImpl.getSOQLQueryService().query(
                                            sfdcConnection.getTargetConnection(), sforceObject, sforceObjPairMap,
                                            loopProperty.getCompositeKeyFields(), false);

                            sforceObject.setLookupRecordsMap(sforceObjPairMap);
                        }
                    }
                }
            }

        }

    }

    // Source environment
    @Override
    public void query() {
        exportSource();
        exportLookup();
    }

    // Target environment
    @Override
    public void insert() {
        createTarget();
    }

    // Inserts records
    public void createTarget() {
        for (SforceObject sforceObject : sForceObjectList) {
            if (sforceObject.isLookup()) {
                continue;
            }

            Map<String, SforceObjectPair> sourceRecordsMap = sforceObject.getRecordsMap();
            if (sourceRecordsMap == null) {
                return;
            }
            List<SforceObjectPair> sourceRecords = new ArrayList<SforceObjectPair>(sourceRecordsMap.values());

            int size = 0;
            if (sourceRecords == null || sourceRecords.size() == 0) {
                continue;
            } else {
                size = sourceRecords.size();
            }
            SObject[] insertRecords = new SObject[size];

            int counter = 0;
            for (SforceObjectPair sObjectPairRecord : sourceRecords) {
                SObject insertRecord = buildMapping(sforceObject, sObjectPairRecord);

                insertRecords[counter] = insertRecord;
                counter++;
            }
            if (sforceObject.getExternalIdField() != null && !sforceObject.getExternalIdField().equals("")) {
                SfdcApiServiceImpl.getSOQLQueryService().upsertWithExternalId(sfdcConnection.getTargetConnection(),
                        sforceObject, sforceObject.getExternalIdField(), sourceRecords, insertRecords, true, false);
            } else {
                SfdcApiServiceImpl.getSOQLQueryService().create(sfdcConnection.getTargetConnection(), sforceObject,
                        sourceRecords, insertRecords, true);
            }
        }

    }

    public String getLookupTargetId(SforceLookupProperties loopProperty, SforceObject sforceObject, SObject sourceRecord) {

        String lookObjectName = loopProperty.getsLookupSObjectName();
        String lookupField = loopProperty.getsLookupField();

        Object sourceLookupFieldId = sourceRecord.getField(lookupField);

        // Lookup can be null
        if (sourceLookupFieldId != null) {

            // This is lookup
            SforceObject lookupSforceObj = getLookupSforceObj(lookObjectName);
            Map<String, SforceObjectPair> lookupRecordMap = lookupSforceObj.getLookupRecordsMap();
            SforceObjectPair pair = lookupRecordMap.get(sourceLookupFieldId);

            if (pair == null) {
                log.warn("Warning for Self references: Lookup record doesn't exists in Target Org. LookupId in Source Org: "
                        + sourceLookupFieldId + " ,lookObjectName:" + lookObjectName + " ,lookupField:" + lookupField);
                return null;
            }

            String compositeKey =
                    Utils.getKey(lookObjectName, pair.getSourceSObject(), loopProperty.getCompositeKeyFields());
            // With compositeKey, find the pair and then target org record:
            if (lookupRecordMap != null) {
                pair = lookupRecordMap.get(compositeKey);
                return pair.getTargetId();
            }
        }
        return null;
    }

    // Finds the lookup (Note: lookup!) SForceObject from the list
    public SforceObject getLookupSforceObj(String lookObjectName) {
        for (SforceObject sforceObject : sForceObjectList) {
            String objectName = sforceObject.getsObjectName();

            if (sforceObject.isLookup()) {
                if (lookObjectName != null && objectName != null) {
                    if (lookObjectName.equalsIgnoreCase(objectName)) {
                        return sforceObject;
                    }
                }

            }
        }
        return null;
    }

    @Override
    public SObject buildMapping(SforceObject sforceObject, SforceObjectPair sourcePairObj) {
        SObject sourceRecord = (SObject) sourcePairObj.getSourceSObject();
        SObject insertRecord = new SObject();

        insertRecord.setType(sforceObject.getsObjectName());

        MetadataObjectHolder.MetadataRefObject descRefObj = sforceObject.getDescRefObject();
        Map<String, FieldType> fieldToTypeMap = descRefObj.getFieldToTypeMap();

        Set<String> unmappedFields = sforceObject.getUnmappedFieldsSet();
        Set<String> maskedFieldsSet = sforceObject.getMaskedFieldsSet();
        Set<String> commonFields = sforceObject.getCommonFields();

        List<String> fields = descRefObj.getPrimitiveFieldList();
        Map<String, String> fieldMapping = sforceObject.getFieldsMapping();
        Set<String> createableFieldSet  = new HashSet<String>();
        
        if (sforceObject.getExternalIdField() == null || sforceObject.getExternalIdField().equals("")) {
        		createableFieldSet = descRefObj.getCreateableFieldSet();
        }
        if (fieldMapping != null && fieldMapping.size() > 0) {
            for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
                String sourceField = entry.getKey();
                String targetField = entry.getValue();
                // Id should only be included in the mapping if it needs to be mapped to a different
                // field ("Id": "Id_From_Na10__c")
                Object value = null;
                Object fielValue = sourcePairObj.getSourceSObject().getField(sourceField);
                if (fielValue != null) {
                    if(fielValue instanceof com.sforce.ws.bind.XmlObjectWrapper) {
                    		continue;
                    }
                    value = deserialize((String)fielValue, fieldToTypeMap.get(sourceField));
                    insertRecord.setField(targetField, value);
                } else {
                	// If externalIdField is on mapping but null in source, then use id from source as externalId value
                		if (sforceObject.getExternalIdField() != null && sforceObject.getExternalIdField().equalsIgnoreCase(sourceField)) {
                			insertRecord.setField(targetField, (String)sourcePairObj.getSourceSObject().getField("Id"));
                		}
                }
            }
        } else {
            for (String field : fields) {
                if (field.equalsIgnoreCase("id")) {
                    continue;
                }

                if (unmappedFields != null && unmappedFields.contains(field)) {
                    continue;
                }
                if (commonFields != null && !commonFields.contains(field)) {
                    log.warn(sforceObject.getsObjectName() + "." + field
                            + " field definition not matching in source/target orgs!");
                    continue;
                }
                if(!createableFieldSet.isEmpty() && !createableFieldSet.contains(field)) {
                		continue;
                }
                
                Object value = null;
                Object fielValue = sourcePairObj.getSourceSObject().getField(field);
                if (fielValue != null && !fielValue.equals("")) {
                    if(fielValue instanceof com.sforce.ws.bind.XmlObjectWrapper) {
                    	continue;
                    }
                    if (maskedFieldsSet != null && maskedFieldsSet.contains(field)) {
                        fielValue = masker.mask((String)fielValue);
                    }
                    value = deserialize((String)fielValue, fieldToTypeMap.get(field));
                    insertRecord.setField(field, value);
                } else {
                	// If externalIdField is on mapping but null in source, then use id from source as externalId value
                	if (sforceObject.getExternalIdField() != null && sforceObject.getExternalIdField().equalsIgnoreCase(field)) {
                		insertRecord.setField(field, (String)sourcePairObj.getSourceSObject().getField("Id"));
                	}
                }
            }
        }
        // Handle lookups
        handleLookups(insertRecord, sforceObject, sourceRecord);
        handleNullableFields(insertRecord, sforceObject, sourcePairObj);
        handleRecordTypes(insertRecord, sforceObject, sourceRecord);
        handleDefaultValues(insertRecord, sforceObject, sourceRecord, descRefObj.getFieldToTypeMap());
        return insertRecord;
    }

    public void handleLookups(SObject insertRecord, SforceObject sforceObject, SObject sourceRecord) {
        List<SforceLookupProperties> lookupList = lookupPropertiesMap.get(sforceObject.getsObjectName());
        if (lookupList != null) {
            for (SforceLookupProperties loopProperty : lookupList) {
                String lookObjectName = loopProperty.getsLookupSObjectName();
                String lookupField = loopProperty.getsLookupField();
                String lookupTargetId = getLookupTargetId(loopProperty, sforceObject, sourceRecord);
                if (lookupTargetId != null) {
                    insertRecord.setField(lookupField, lookupTargetId);
                }
            }
        }
    }

    public void handleNullableFields(SObject insertRecord, SforceObject sforceObject, SforceObjectPair sourcePairObj) {
        // Check if nullable fields are having null value

        List<String> realNullableFields = new ArrayList<String>();
        if (sforceObject.getNullableFields() == null || sforceObject.getNullableFields().size() == 0) {
            return;
        }

        for (String nullableField : sforceObject.getNullableFields()) {
            String fielValue = (String) sourcePairObj.getSourceSObject().getField(nullableField);
            if (fielValue == null || fielValue.equals("")) {
                insertRecord.removeField(nullableField);
                realNullableFields.add(nullableField);
            }
        }

        if (realNullableFields.size() > 0) {
            String[] nullableFields = new String[realNullableFields.size()];
            if (realNullableFields.size() > 0) {
                for (int i = 0; i < realNullableFields.size(); i++) {
                    nullableFields[i] = realNullableFields.get(i);
                }
                insertRecord.setFieldsToNull(nullableFields);
            }
        }
    }

    public void handleRecordTypes(SObject insertRecord, SforceObject sforceObject, SObject sourceRecord) {
        insertRecord.removeField("RecordTypeId");
        insertRecord.setField("RecordTypeId",
                sforceObject.getTargetRecordTypeId((String) sourceRecord.getField("RecordTypeId")));
    }

    public void handleDefaultValues(SObject insertRecord, SforceObject sforceObject, SObject sourceRecord,
            Map<String, FieldType> fieldToTypeMap) {
        Map<String, String> defaultValues = sforceObject.getDefaultValues();
        if (defaultValues != null && defaultValues.size() > 0) {
            for (Map.Entry<String, String> entry : defaultValues.entrySet()) {
                String field = entry.getKey();
                Object value = entry.getValue();
                value = deserialize((String) value, fieldToTypeMap.get(field));
                insertRecord.setField(field, value);
            }
        }
    }

    public Object deserialize(String value, FieldType localType) {
        if (value != null && value != "" && value.length() > 0) {
            if (FieldType.string.equals(localType)) {
                return value;
            } else if (FieldType._int.equals(localType)) {
                return Integer.parseInt(value);
            } else if (FieldType._double.equals(localType)) {
                return parseDouble(value);
            } else if (FieldType.currency.equals(localType)) {
                return parseBigDecimal(value);
            } else if (FieldType._double.equals(localType)) {
                return parseBigDecimal(value);
            } else if (FieldType._int.equals(localType)) {
                return Long.parseLong(value);
            } else if (FieldType.time.equals(localType)) {
                return new Time(value);
            } else if (FieldType.date.equals(localType)) {
                return dateCodec.deserialize(value).getTime();
            } else if (FieldType.datetime.equals(localType)) {
                return calendarCodec.deserialize(value);
            } else if (FieldType._boolean.equals(localType)) {
                return Boolean.parseBoolean(value);
            } else if (FieldType.base64.equals(localType)) {
                return Base64.decode(value.getBytes());
            } else {
                return value;
            }
        }
        return value;
    }

    public double parseDouble(String strValue) {
        double value;
        if ("NaN".equals(strValue)) {
            value = Double.NaN;
        } else if ("INF".equals(strValue)) {
            value = Double.POSITIVE_INFINITY;
        } else if ("-INF".equals(strValue)) {
            value = Double.NEGATIVE_INFINITY;
        } else {
            if (strValue != null && strValue.length() > 0)
                return Double.parseDouble(strValue);
            else
                return 0;
        }
        return value;
    }

    private BigDecimal parseBigDecimal(String value) {
        return new BigDecimal(value);
    }

    @SuppressWarnings("unused")
    public String getParentId(String mappedField, SforceObject sforceObject, SforceObjectPair sourcePairObj) {
        SforceMasterDetail masterDetail = sforceObject.getMasterDetail();
        String sParentObjectName = masterDetail.getParentFieldObjectMap().get(mappedField);

        Map<String, SObject> sourceSObjectParentMap = sourcePairObj.getSourceSObjectParentMap();

        // Based on field (as parent referred in child)
        SObject sObjectSource = sourceSObjectParentMap.get(mappedField);
        if (sObjectSource == null) {
            log.warn("Parent might be missing: sParentObjectName:" + sParentObjectName + " mappedField:" + mappedField);
            return null;
        }
        String sObjectSourceId = sObjectSource.getId();
        if (sObjectSourceId == null) {
            log.warn("Parent might be missing: sObjectSourceId is null, sParentObjectName:" + sParentObjectName
                    + " mappedField:" + mappedField);
            return null;
        }
        SforceObject parentSforceObj = getParentSforceObject(sParentObjectName);
        if (parentSforceObj == null) {
            log.warn("Parent might be missing: parentSforceObj is null, sParentObjectName:" + sParentObjectName
                    + " mappedField:" + mappedField);
            return null;
        }

        SforceObjectPair parentPair = parentSforceObj.getRecordsMap().get(sObjectSourceId);
        if (parentPair != null) {
            return parentPair.getTargetId();
        }
        return null;
    }

    public SforceObject getParentSforceObject(String sParentObjectName) {
        for (SforceObject sforceObj : sForceObjectList) {
            if (!sforceObj.isLookup() && sParentObjectName.equalsIgnoreCase(sforceObj.getsObjectName())) {
                return sforceObj;
            }
        }
        return null;
    }

    protected void readMapping() {
        sForceObjectList = objectMapping.getsForceObjectList();
        lookupPropertiesMap = objectMapping.getLookupPropertiesMap();
    }

    private Map<String, SforceObject> getUniqueLookupObjects() {

        Map<String, SforceObject> lookupMap = new HashMap<String, SforceObject>();

        for (SforceObject sforceObject : sForceObjectList) {
            if (sforceObject.isLookup()) {
                for (Map.Entry<String, List<SforceLookupProperties>> entry : lookupPropertiesMap.entrySet()) {
                    List<SforceLookupProperties> lookupList = entry.getValue();
                    for (SforceLookupProperties loopProperty : lookupList) {
                        String lookObjectName = loopProperty.getsLookupSObjectName();
                        if (sforceObject.getsObjectName().equals(lookObjectName)) {
                            lookupMap.put(sforceObject.getsObjectName(), sforceObject);
                        }
                    }
                }
            }
        }
        return lookupMap;
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

}
