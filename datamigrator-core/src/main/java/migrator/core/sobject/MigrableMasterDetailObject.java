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
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import org.apache.log4j.Logger;

import com.sforce.soap.partner.FieldType;
import com.sforce.soap.partner.sobject.SObject;

import migrator.core.service.MetadataObjectHolder;
import migrator.core.service.SforceLookupProperties;
import migrator.core.service.SforceMasterDetail;
import migrator.core.service.SforceObject;
import migrator.core.service.SforceObjectPair;
import migrator.core.service.impl.SfdcApiServiceImpl;

/**
 * MigrableMasterDetailObject : Class to support master detail relationships
 *
 * @author anoop.singh
 */
public class MigrableMasterDetailObject extends MigrableLookupObject {

    static Logger log = Logger.getLogger(MigrableMasterDetailObject.class.getName());

    public MigrableMasterDetailObject() {
        super();
    }

    public MigrableMasterDetailObject(ObjectMappingConfig objectMapping, String operation) {
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
        super.setup();

        Collections.sort(sForceObjectList, new Comparator<SforceObject>() {
            @Override
            public int compare(SforceObject p1, SforceObject p2) {
                return p1.getSequence() - p2.getSequence();
            }
        });
    }

    public void cleanup() {
        super.cleanup();
        if (objectMapping.getMasterDetailsMap() != null)
            objectMapping.getMasterDetailsMap().clear();
    }

    // Source environment
    @Override
    public void query() {
        exportLookup();
        exportSource();
    }

    // Target environment
    @Override
    public void insert() {
        createTarget();
        updateTarget();
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

        // Handle masterdetail
        handleMasterDetail(insertRecord, sforceObject, sourcePairObj);
        handleLookups(insertRecord, sforceObject, sourceRecord);
        handleNullableFields(insertRecord, sforceObject, sourcePairObj);
        handleRecordTypes(insertRecord, sforceObject, sourceRecord);
        handleDefaultValues(insertRecord, sforceObject, sourceRecord, descRefObj.getFieldToTypeMap());
        return insertRecord;
    }

    private void handleMasterDetail(SObject insertRecord, SforceObject sforceObject, SforceObjectPair sourcePairObj) {
        SforceMasterDetail masterDetail = sforceObject.getMasterDetail();
        if (masterDetail == null || masterDetail.getsObjectName() == null) {
            // No parent
        } else {
            Map<String, String> parentFields = sforceObject.getMasterDetail().getParentFieldObjectMap();

            for (Map.Entry<String, String> entry : parentFields.entrySet()) {
                String mappedField = entry.getKey();
                String parentId = getParentId(mappedField, sforceObject, sourcePairObj);
                insertRecord.setField(mappedField, parentId);
            }
        }

    }

    // no impl
    public void updateTarget() {}

    // Using source Org only
    @SuppressWarnings("unchecked")
    private void exportSource() {

        for (SforceObject sforceObject : sForceObjectList) {

            // Its parent (child in master is handled in below else part. Lookup handled in parent
            // class)
            if (sforceObject.getMasterDetail() == null && !sforceObject.isLookup()) {
                Map<String, SforceObjectPair> sforceObjPairMap = new HashMap<String, SforceObjectPair>();
                sforceObjPairMap =
                        SfdcApiServiceImpl.getSOQLQueryService().query(sfdcConnection.getSourceConnection(),
                                sforceObject, sforceObjPairMap, true);
                sforceObject.setRecordsMap(sforceObjPairMap);
            }
            // We have a master-detail relationship:
            else if (sforceObject.getMasterDetail() != null) {
                SforceMasterDetail masterDetail = sforceObject.getMasterDetail();
                // Find parent sforceObject?
                Map<String, SforceObjectPair> sforceObjPairMap = sforceObject.getRecordsMap();
                if (sforceObjPairMap == null) {
                    sforceObjPairMap = new HashMap<String, SforceObjectPair>();
                }
                sforceObjPairMap =
                        SfdcApiServiceImpl.getSOQLQueryService().queryChildren(sfdcConnection.getSourceConnection(),
                                sforceObject, sforceObjPairMap, masterDetail, sForceObjectList, true);
            }
        }
    }

    // Inserts records
    @Override
    public void createTarget() {
        boolean reIssueQuery = false;
        for (SforceObject sforceObject : sForceObjectList) {
            String objectName = sforceObject.getsObjectName();
            reIssueQuery = false;

            if (sforceObject.isLookup()) {
                continue;
            }

            // Add example mapping here
            if (bReIssueLookupQuery(objectName)) {
                reIssueQuery = true;
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

            if (reIssueQuery) {
                super.reIssueLookupQuery(objectName);
            }
        }
    }

    // This is now a parent object but also lookup. It's not yet in target, so re-issue
    // lookup query, so we have target ids set.
    public boolean bReIssueLookupQuery(String sCurrentObjectName) {

        for (Map.Entry<String, List<SforceLookupProperties>> entry : lookupPropertiesMap.entrySet()) {
            List<SforceLookupProperties> lookupList = entry.getValue();
            for (SforceLookupProperties loopProperty : lookupList) {
                String lookObjectName = loopProperty.getsLookupSObjectName();

                if (sCurrentObjectName.equals(lookObjectName)) {
                    return true;
                }
            }
        }
        return false;
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

}
