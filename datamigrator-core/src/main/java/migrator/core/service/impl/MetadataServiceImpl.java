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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.FieldType;
import com.sforce.soap.partner.PicklistEntry;
import com.sforce.ws.ConnectionException;
import migrator.core.connect.SfdcConnection;
import migrator.core.service.MetadataObjectHolder;
import migrator.core.service.MetadataService;
import migrator.core.service.MetadataObjectHolder.MetadataRefObject.ChildRelationship;
import migrator.core.utils.Utils;

/**
 * MetadataServiceImpl
 *
 * @author anoop.singh
 */
public class MetadataServiceImpl implements MetadataService {
    protected SfdcConnection sfdcConnection;
    private List<String> fieldList;
    private List<String> primitiveFieldList;
    private String fieldAsString;
    private String primitiveFieldAsString;
    private List<String> referecensTo;
    private Map<String, String> referecensToMap;
    private Map<String, FieldType> fieldToTypeMap;
    private Map<String, FieldType> fieldToTypeMapAll;
    private Set<Field> fieldListAsSet;
    private Set<String> createableFieldSet;
    private Set<String> updateableFieldSet;

    public MetadataServiceImpl() {}

    public MetadataServiceImpl(SfdcConnection connection) {
        this.sfdcConnection = connection;
        fieldList = new ArrayList<String>();
        fieldAsString = "";
        primitiveFieldAsString = "";
        referecensTo = new ArrayList<String>();
        referecensToMap = new HashMap<String, String>();
        primitiveFieldList = new ArrayList<String>();
        fieldToTypeMap = new HashMap<String, FieldType>();
        fieldListAsSet = new HashSet<Field>();
        fieldToTypeMapAll = new HashMap<String, FieldType>();
        createableFieldSet = new HashSet<String>();
        updateableFieldSet = new HashSet<String>();
    }

    public MetadataObjectHolder.MetadataRefObject process(SfdcConnection.ORG_TYPE orgType, String sObjectName) {
        return process(orgType, sObjectName, false, null);
    }

    public MetadataObjectHolder.MetadataRefObject process(SfdcConnection.ORG_TYPE orgType, String sObjectName,
            boolean processChilds, List<String> migrableObjects) {
        List<Field> childFields = new ArrayList<Field>();

        Map<String, String> sourceIdToNameRecordTypeMap = new HashMap<String, String>();
        MetadataObjectHolder.MetadataRefObject refObject = null;
        try {
            // Make the describe call
            DescribeSObjectResult describeSObjectResult = null;
            if (orgType == SfdcConnection.ORG_TYPE.SOURCE) {
                describeSObjectResult = sfdcConnection.getSourceConnection().describeSObject(sObjectName);;
            } else if (orgType == SfdcConnection.ORG_TYPE.TARGET) {
                describeSObjectResult = sfdcConnection.getTargetConnection().describeSObject(sObjectName);;
            }

            com.sforce.soap.partner.RecordTypeInfo[] recordTypes = describeSObjectResult.getRecordTypeInfos();
            for (com.sforce.soap.partner.RecordTypeInfo recordType : recordTypes) {
                sourceIdToNameRecordTypeMap.put(recordType.getRecordTypeId(), recordType.getName());
            }

            // Get sObject metadata
            if (describeSObjectResult != null) {
                if (describeSObjectResult.isCreateable()) {
                    // System.out.println("Createable");
                }

                // Get the fields
                Field[] fields = describeSObjectResult.getFields();

                // Iterate through each field and gets its properties
                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];

                    if (field.isCalculated() || field.isAutoNumber()) {
                        continue;
                    }
                    // if (field.isCreateable() && (!field.isNillable()) &&
                    // (!field.isDefaultedOnCreate())) {
                    if (Utils.EXCLUDE_QUERY_FIELDS.contains(field.getName())) {
                        continue;
                    } else {
                        fieldList.add(field.getName());
                        fieldAsString += field.getName() + ",";
                        fieldToTypeMap.put(field.getName(), field.getType());
                        fieldToTypeMapAll.put(field.getName(), field.getType());
                        fieldListAsSet.add(field);
                    }
                    if (field.isCreateable()) {
                        createableFieldSet.add(field.getName());
                    }
                    if (field.isUpdateable()) {
                        updateableFieldSet.add(field.getName());
                    }

                    // If this is a picklist field, show the picklist values
                    if (field.getType().equals(FieldType.picklist)) {
                        PicklistEntry[] picklistValues = field.getPicklistValues();
                        if (picklistValues != null) {
                            for (int j = 0; j < picklistValues.length; j++) {
                                if (picklistValues[j].getLabel() != null) {
                                }
                            }
                        }
                    }

                    // If a reference field, show what it references
                    if (field.getType().equals(FieldType.reference)) {

                        String[] referenceTos = field.getReferenceTo();
                        for (int j = 0; j < referenceTos.length; j++) {
                            referecensTo.add(referenceTos[j]);
                            referecensToMap.put(field.getName(), referenceTos[j]);
                        }
                        fieldListAsSet.add(field);
                        fieldToTypeMapAll.put(field.getName(), field.getType());
                    } else {
                        primitiveFieldList.add(field.getName());
                        primitiveFieldAsString += field.getName() + ",";
                    }

                    // This is detail in master-detail
                    if (field.getRelationshipOrder() > 0) {
                        childFields.add(field);
                    }
                }
            }

            if (fieldAsString.length() > 1) {
                fieldAsString = fieldAsString.substring(0, fieldAsString.length() - 1);
            }
            if (primitiveFieldAsString.length() > 1) {
                primitiveFieldAsString = primitiveFieldAsString.substring(0, primitiveFieldAsString.length() - 1);
            }

            if (orgType == SfdcConnection.ORG_TYPE.SOURCE) {
                refObject = MetadataObjectHolder.getInstance().getDescribeRefObjectMap().get(sObjectName);
                if (refObject == null) {
                    refObject = new MetadataObjectHolder.MetadataRefObject(sObjectName);
                    MetadataObjectHolder.getInstance().getDescribeRefObjectMap().put(sObjectName, refObject);
                }
            } else if (orgType == SfdcConnection.ORG_TYPE.TARGET) {
                refObject = MetadataObjectHolder.getInstance().getTargetDescribeRefObjectMap().get(sObjectName);
                if (refObject == null) {
                    refObject = new MetadataObjectHolder.MetadataRefObject(sObjectName);
                    MetadataObjectHolder.getInstance().getTargetDescribeRefObjectMap().put(sObjectName, refObject);
                    MetadataObjectHolder.getInstance().getDescribeRefObjectMap().put(sObjectName, refObject);
                }
            }

            refObject.setFieldsAsString(fieldAsString);
            refObject.setFieldList(fieldList);
            refObject.setPrimitiveFieldList(primitiveFieldList);
            refObject.setPrimitiveFieldAsString(primitiveFieldAsString);
            refObject.setReferecensTo(referecensTo);
            refObject.setReferecensToMap(referecensToMap);
            refObject.setSourceIdToNameRecordTypeMap(sourceIdToNameRecordTypeMap);
            refObject.setFieldToTypeMap(fieldToTypeMap);
            refObject.setFieldListAsSet(fieldListAsSet);
            refObject.setFieldToTypeMapAll(fieldToTypeMapAll);
            refObject.setCreateableFieldSet(createableFieldSet);
            refObject.setUpdateableFieldSet(updateableFieldSet);

            if (processChilds) {
                List<ChildRelationship> childRelationships = refObject.getChildRelationships();
                for (com.sforce.soap.partner.ChildRelationship cr : describeSObjectResult.getChildRelationships()) {

                    if (cr.getRelationshipName() != null
                            && !Utils.EXCLUDE_CHILD_RELATIONSHIPS.contains(cr.getRelationshipName())) {

                        if (doesMigrableContainsChildObject(cr.getChildSObject(), migrableObjects)) {

                            String sChildObjName = cr.getChildSObject();
                            ChildRelationship childRelationship =
                                    new ChildRelationship(sChildObjName, cr.getField(), cr.getRelationshipName());
                            childRelationships.add(childRelationship);

                        }
                    }
                }
            }

            // Get target record types for this object
            if (sfdcConnection.getTargetConnection() != null) {
                DescribeSObjectResult describeSObjectResultTarget =
                        sfdcConnection.getTargetConnection().describeSObject(sObjectName);

                Map<String, String> targetNameToIdRecordTypeMap = new HashMap<String, String>();
                com.sforce.soap.partner.RecordTypeInfo[] recordTypesTarget =
                        describeSObjectResultTarget.getRecordTypeInfos();
                for (com.sforce.soap.partner.RecordTypeInfo recordType : recordTypesTarget) {
                    targetNameToIdRecordTypeMap.put(recordType.getName(), recordType.getRecordTypeId());
                }
                refObject.setTargetNameToIdRecordTypeMap(targetNameToIdRecordTypeMap);
            }

        } catch (ConnectionException ce) {
            ce.printStackTrace();
        }

        return refObject;
    }

    private boolean doesMigrableContainsChildObject(String sChildObjectName, List<String> migrableObjects) {
        for (String sObjectName : migrableObjects) {
            if (sChildObjectName.equalsIgnoreCase(sObjectName)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getFields() {
        return fieldList;
    }

    public String getFieldsAsString() {
        return fieldAsString;
    }

    public List<String> getReferenceTo() {
        return referecensTo;
    }

}
