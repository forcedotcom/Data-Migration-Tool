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
import java.util.Set;

/**
 * Represents the child (detail) object in master-detail relationship
 *
 * @author anoop.singh
 */
public class SforceMasterDetail {
    String sObjectName;

    // e.g. {AccFunct__c, AccountFunction__c}
    Map<String, String> parentFieldObjectMap;

    private int sequence;

    private String where;

    private Set<String> unmappedFieldsSet;

    private List<String> nullableFields;

    // Masked fields
    private Set<String> maskedFieldsSet;

    // fields mapping
    private Map<String, String> fieldsMapping;

    // default values
    private Map<String, String> defaultValues;

    // Does this needs to be refreshed(this is mainly for hierarchical
    // relationships. e.g. Product2)
    private boolean refresh = false;

    // External Id field
    private String externalIdField;

    // batch size, default is 200
    private int batchSize;

    public SforceMasterDetail() {
        this.batchSize = 200;
    }

    public SforceMasterDetail(String sObjectName, Map<String, String> parentFieldObjectMap) {
        this.sObjectName = sObjectName;
        this.parentFieldObjectMap = parentFieldObjectMap;
        this.batchSize = 200;
    }

    public String getsObjectName() {
        return sObjectName;
    }

    public void setsObjectName(String sObjectName) {
        this.sObjectName = sObjectName;
    }

    public Map<String, String> getParentFieldObjectMap() {
        return parentFieldObjectMap;
    }

    public void setParentFieldObjectMap(Map<String, String> parentFieldObjectMap) {
        this.parentFieldObjectMap = parentFieldObjectMap;
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

    public String getWhere() {
        return where;
    }

    public void setWhere(String where) {
        this.where = where;
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

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public String toString() {
        return "SforceMasterDetail[" + "sObjectName:" + sObjectName + "]";
    }
}
