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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.FieldType;
import migrator.core.connect.SfdcConnection;
import migrator.core.service.impl.MetadataServiceImpl;

/**
 * MetadataObjectHolder
 *
 * @author anoop.singh
 */
public class MetadataObjectHolder {
    private static MetadataObjectHolder holder = new MetadataObjectHolder();
    private Map<String, MetadataRefObject> describeObjectMap = null;
    private Map<String, MetadataRefObject> targetDescribeObjectMap = null;

    private MetadataObjectHolder() {
        describeObjectMap = new HashMap<String, MetadataRefObject>();
        targetDescribeObjectMap = new HashMap<String, MetadataRefObject>();
    }

    public static MetadataObjectHolder getInstance() {
        return holder;
    }

    public Map<String, MetadataRefObject> getDescribeRefObjectMap() {
        return describeObjectMap;
    }

    public MetadataRefObject get(String key) {
        return describeObjectMap.get(key);
    }

    public void put(String key, MetadataRefObject value) {
        describeObjectMap.put(key, value);
    }

    public Map<String, MetadataRefObject> getTargetDescribeRefObjectMap() {
        return targetDescribeObjectMap;
    }

    public MetadataRefObject init(SfdcConnection.ORG_TYPE orgType, SfdcConnection connection, String sObjectAPIName) {
        MetadataRefObject describeRefObject = holder.get(sObjectAPIName);
        if (describeRefObject == null) {
            describeRefObject = new MetadataServiceImpl(connection).process(orgType, sObjectAPIName);
            describeObjectMap.put(sObjectAPIName, describeRefObject);
        }
        return describeRefObject;
    }

    public MetadataRefObject init(SfdcConnection.ORG_TYPE orgType, SfdcConnection connection, String sObjectAPIName,
            List<String> migrableObjects) {
        MetadataRefObject describeRefObject = holder.get(sObjectAPIName);
        describeRefObject =
                new MetadataServiceImpl(connection).process(orgType, sObjectAPIName, true, migrableObjects);
        describeObjectMap.put(sObjectAPIName, describeRefObject);
        return describeRefObject;
    }

    public MetadataRefObject initTarget(SfdcConnection.ORG_TYPE orgType, SfdcConnection connection,
            String sObjectAPIName, List<String> migrableObjects) {
        MetadataRefObject describeRefObject =
                new MetadataServiceImpl(connection).process(orgType, sObjectAPIName, true, migrableObjects);
        targetDescribeObjectMap.put(sObjectAPIName, describeRefObject);
        return describeRefObject;
    }

    public static class MetadataRefObject {

        private String objectName;
        private Map<String, Field> fieldInfoMap;
        private List<String> fieldList;
        private String fieldsAsString;
        private String primitiveFieldAsString;

        private List<String> referecensTo;
        private String spitMapping;
        private List<String> primitiveFieldList;
        private Map<String, String> referecensToMap;
        private Map<String, FieldType> fieldToTypeMap;
        private Map<String, FieldType> fieldToTypeMapAll;

        private Map<String, String> sourceIdToNameRecordTypeMap;
        private Map<String, String> targetNameToIdRecordTypeMap;
        private List<ChildRelationship> childRelationships;

        private List<MetadataRefObject> masters;
        private Map<String, String> lookupsMap;

        private Set<Field> fieldListAsSet;
        private Set<String> createableFieldSet;
        private Set<String> updateableFieldSet;

        public MetadataRefObject(String objectName) {
            this.objectName = objectName;
            this.childRelationships = new ArrayList<ChildRelationship>();
            this.masters = new ArrayList<MetadataRefObject>();
            this.lookupsMap = new HashMap<String, String>();
            this.fieldToTypeMapAll = new HashMap<String, FieldType>();
            this.fieldListAsSet = new HashSet<Field>();
            this.createableFieldSet = new HashSet<String>();
            this.updateableFieldSet = new HashSet<String>();
        }

        public String getObjectName() {
            return objectName;
        }

        public Map<String, Field> getFieldInfoMap() {
            return fieldInfoMap;
        }

        public void setFieldInfoMap(Map<String, Field> map) {
            this.fieldInfoMap = map;
        }

        public List<String> getFieldList() {
            return fieldList;
        }

        public void setFieldList(List<String> list) {
            this.fieldList = list;
        }

        public List<String> getPrimitiveFieldList() {
            return primitiveFieldList;
        }

        public void setPrimitiveFieldList(List<String> primitiveFieldList) {
            this.primitiveFieldList = primitiveFieldList;
        }

        public List<String> getReferecensTo() {
            return referecensTo;
        }

        public void setReferecensTo(List<String> list) {
            this.referecensTo = list;
        }

        public Map<String, String> getReferecensToMap() {
            return referecensToMap;
        }

        public void setReferecensToMap(Map<String, String> referecensToMap) {
            this.referecensToMap = referecensToMap;
        }

        public String getFieldsAsString() {
            return fieldsAsString;
        }

        public void setFieldsAsString(String fieldAsString) {
            this.fieldsAsString = fieldAsString;
        }

        public Map<String, String> getSourceIdToNameRecordTypeMap() {
            return sourceIdToNameRecordTypeMap;
        }

        public void setSourceIdToNameRecordTypeMap(Map<String, String> sourceIdToNameRecordTypeMap) {
            this.sourceIdToNameRecordTypeMap = sourceIdToNameRecordTypeMap;
        }

        public Map<String, String> getTargetNameToIdRecordTypeMap() {
            return targetNameToIdRecordTypeMap;
        }

        public void setTargetNameToIdRecordTypeMap(Map<String, String> targetNameToIdRecordTypeMap) {
            this.targetNameToIdRecordTypeMap = targetNameToIdRecordTypeMap;
        }

        public String getSpitMapping() {
            return spitMapping;
        }

        public void setSpitMapping(String spitMapping) {
            this.spitMapping = spitMapping;
        }

        public List<ChildRelationship> getChildRelationships() {
            return childRelationships;
        }

        public List<MetadataRefObject> getMasters() {
            return masters;
        }

        public Map<String, String> getLookupsMap() {
            return lookupsMap;
        }

        /**
         * @return the primitiveFieldAsString
         */
        public String getPrimitiveFieldAsString() {
            return primitiveFieldAsString;
        }

        /**
         * @param primitiveFieldAsString the primitiveFieldAsString to set
         */
        public void setPrimitiveFieldAsString(String primitiveFieldAsString) {
            this.primitiveFieldAsString = primitiveFieldAsString;
        }

        /**
         * @return the fieldListAsSet
         */
        public Set<Field> getFieldListAsSet() {
            return fieldListAsSet;
        }

        /**
         * @param fieldListAsSet the fieldListAsSet to set
         */
        public void setFieldListAsSet(Set<Field> fieldListAsSet) {
            this.fieldListAsSet = fieldListAsSet;
        }

        /**
         * @return the fieldToTypeMap
         */
        public Map<String, FieldType> getFieldToTypeMap() {
            return fieldToTypeMap;
        }

        /**
         * @param fieldToTypeMap the fieldToTypeMap to set
         */
        public void setFieldToTypeMap(Map<String, FieldType> fieldToTypeMap) {
            this.fieldToTypeMap = fieldToTypeMap;
        }

        /**
         * @return the fieldToTypeMapAll
         */
        public Map<String, FieldType> getFieldToTypeMapAll() {
            return fieldToTypeMapAll;
        }

        public Set<String> getCreateableFieldSet() {
            return createableFieldSet;
        }

        public void setCreateableFieldSet(Set<String> createableFieldSet) {
            this.createableFieldSet = createableFieldSet;
        }

        public Set<String> getUpdateableFieldSet() {
            return updateableFieldSet;
        }

        public void setUpdateableFieldSet(Set<String> updateableFieldSet) {
            this.updateableFieldSet = updateableFieldSet;
        }

        /**
         * @param fieldToTypeMapAll the fieldToTypeMapAll to set
         */
        public void setFieldToTypeMapAll(Map<String, FieldType> fieldToTypeMapAll) {
            this.fieldToTypeMapAll = fieldToTypeMapAll;
        }

        public static class ChildRelationship {
            private String childSObject;
            private String field;
            private String relationshipName;

            public ChildRelationship(String childSObject, String field, String relationshipName) {
                this.childSObject = childSObject;
                this.field = field;
                this.relationshipName = relationshipName;
            }

            public String getChildSObject() {
                return childSObject;
            }

            public String getField() {
                return field;
            }

            public String getRelationshipName() {
                return relationshipName;
            }

            public String toString() {
                return "\n ChildSObject: " + childSObject + ", field: " + field + ", relationshipName:"
                        + relationshipName + "\n";
            }
        }

        public String toString() {
            return "\n" + spitMapping + "\n";
        }
    }
}
