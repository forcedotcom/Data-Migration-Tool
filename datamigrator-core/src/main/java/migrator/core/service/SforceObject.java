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
package migrator.core.service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * Holds SObject fields and records
 *
 * @author anoop.singh
 */

public class SforceObject {

    // Object API name
    private String sObjectName;

    // External Id field
    private String externalIdField;

    // Unmapped fields
    private Set<String> unmappedFieldsSet;

    // Nullbale fields
    private List<String> nullableFields;

    // Masked fields
    private Set<String> maskedFieldsSet;

    // fields mapping
    private Map<String, String> fieldsMapping;

    // default values
    private Map<String, String> defaultValues;

    // Where clause to use as-as
    private String where;

    // lookup keys
    private List<String> compositeKeyFields;

    // Object API name
    private Class clazz;

    //
    private MetadataObjectHolder.MetadataRefObject descRefObject;

    // Map of SObject records (Paired records: from source and target orgs)
    // Key is source org id
    Map<String, SforceObjectPair> recordsMap = null;

    // Map of SObject records (Paired records: from source and target orgs)
    // based on composite key
    Map<String, SforceObjectPair> lookupRecordsMap = null;

    // Object API name
    private boolean isLookup = false;

    // Does this needs to be refreshed(this is mainly for hierarchical
    // relationships. e.g. Product2)
    private boolean refresh = false;

    // The lookup may have more than one lookup properties
    List<SforceLookupProperties> lookupProperties;

    // e.g. {AccFunct__c, AccountFunction__c}
    Map<String, String> parentFieldObjectMap;

    SforceMasterDetail masterDetail;

    private int sequence;

    private Set<String> commonFields;

    // batch size, default is 200
    private int batchSize;

    public SforceObject() {
        recordsMap = new HashMap<String, SforceObjectPair>();
        lookupRecordsMap = new HashMap<String, SforceObjectPair>();
        parentFieldObjectMap = new HashMap<String, String>();

        isLookup = false;
        batchSize = 200;
    }

    public String getsObjectName() {
        return sObjectName;
    }

    public void setsObjectName(String sObjectName) {
        this.sObjectName = sObjectName;
    }

    public String getWhere() {
        return where;
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public Class getsObjectClass() {
        return this.clazz;
    }

    public void setsObjectClass(Class cls) {
        clazz = cls;
    }

    public MetadataObjectHolder.MetadataRefObject getDescRefObject() {
        return descRefObject;
    }

    public void setDescRefObject(MetadataObjectHolder.MetadataRefObject descRefObject) {
        this.descRefObject = descRefObject;
    }

    public Map<String, SforceObjectPair> getRecordsMap() {
        return recordsMap;
    }

    public void setRecordsMap(Map<String, SforceObjectPair> records) {
        this.recordsMap = records;
    }

    public Map<String, SforceObjectPair> getLookupRecordsMap() {
        return lookupRecordsMap;
    }

    public void setLookupRecordsMap(Map<String, SforceObjectPair> lookupRecordsMap) {
        this.lookupRecordsMap = lookupRecordsMap;
    }

    public boolean isLookup() {
        return isLookup;
    }

    public void setLookup(boolean isLookup) {
        this.isLookup = isLookup;
    }

    public Map<String, String> getParentFieldObjectMap() {
        return parentFieldObjectMap;
    }

    public void setParentFieldObjectMap(Map<String, String> parentFieldObjectMap) {
        this.parentFieldObjectMap = parentFieldObjectMap;
    }

    public SforceMasterDetail getMasterDetail() {
        return masterDetail;
    }

    public void setMasterDetail(SforceMasterDetail masterDetail) {
        this.masterDetail = masterDetail;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public String getExternalIdField() {
        return externalIdField;
    }

    public void setExternalIdField(String externalIdField) {
        this.externalIdField = externalIdField;
    }

    public List<SforceLookupProperties> getLookupProperties() {
        return lookupProperties;
    }

    public void setLookupProperties(List<SforceLookupProperties> lookupProperties) {
        this.lookupProperties = lookupProperties;
    }

    public boolean isRefresh() {
        return refresh;
    }

    public void setRefresh(boolean refresh) {
        this.refresh = refresh;
    }

    /**
     * @return the unmappedFieldsSet
     */
    public Set<String> getUnmappedFieldsSet() {
        return unmappedFieldsSet;
    }

    /**
     * @param unmappedFieldsSet the unmappedFieldsSet to set
     */
    public void setUnmappedFieldsSet(Set<String> unmappedFieldsSet) {
        this.unmappedFieldsSet = unmappedFieldsSet;
    }

    public Set<String> getMaskedFieldsSet() {
        return maskedFieldsSet;
    }

    public void setMaskedFieldsSet(Set<String> maskedFieldsSet) {
        this.maskedFieldsSet = maskedFieldsSet;
    }

    public Map<String, String> getFieldsMapping() {
        return fieldsMapping;
    }

    public void setFieldsMapping(Map<String, String> fieldsMapping) {
        this.fieldsMapping = fieldsMapping;
    }

    public Map<String, String> getDefaultValues() {
        return defaultValues;
    }

    public void setDefaultValues(Map<String, String> defaultValues) {
        this.defaultValues = defaultValues;
    }

    /**
     * @return the nullableFields
     */
    public List<String> getNullableFields() {
        return nullableFields;
    }

    /**
     * @param nullableFields the nullableFields to set
     */
    public void setNullableFields(List<String> nullableFields) {
        this.nullableFields = nullableFields;
    }

    public List<String> getCompositeKeyFields() {
        return compositeKeyFields;
    }

    public void setCompositeKeyFields(List<String> compositeKeyFields) {
        this.compositeKeyFields = compositeKeyFields;
    }

    public String getTargetRecordTypeId(String sourceRecordTypeId) {
        String recordTypeName = descRefObject.getSourceIdToNameRecordTypeMap().get(sourceRecordTypeId);
        if (recordTypeName == null) {
            return null;
        }
        return descRefObject.getTargetNameToIdRecordTypeMap().get(recordTypeName);
    }

    /**
     * @return the commonFields
     */
    public Set<String> getCommonFields() {
        return commonFields;
    }

    /**
     * @param commonFields the commonFields to set
     */
    public void setCommonFields(Set<String> commonFields) {
        this.commonFields = commonFields;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void cleanup() {
        if (recordsMap != null)
            recordsMap.clear();
        if (lookupRecordsMap != null)
            lookupRecordsMap.clear();
        if (lookupProperties != null)
            lookupProperties.clear();
        if (parentFieldObjectMap != null)
            parentFieldObjectMap.clear();
        if (masterDetail != null)
            masterDetail = null;
        if (commonFields != null)
            commonFields.clear();
    }

    public String toString() {
        return "SforceObject[" + "sObjectName:" + sObjectName + ", isLookup:" + isLookup + ", sequence:" + sequence
                + ", refresh:" + refresh + ", externalIdField:" + externalIdField
                // + ", compositeKeyFields:" + compositeKeyFields
                + "]";
    }
}
