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
import java.util.Set;

/**
 * SforceLookupProperties
 *
 * @author anoop.singh
 */
public class SforceLookupProperties {
    String sObjectName;

    // e.g. {AccFunct__c, AccountFunction__c}
    String sLookupField;

    String sLookupSObjectName;

    private String where;

    List<String> compositeKeyFields = null;

    private Set<String> unmappedFieldsSet;

    // External Id field
    private String externalIdField;

    public String getExternalIdField() {
        return externalIdField;
    }

    public void setExternalIdField(String externalIdField) {
        this.externalIdField = externalIdField;
    }

    public Set<String> getUnmappedFieldsSet() {
        return unmappedFieldsSet;
    }

    public void setUnmappedFieldsSet(Set<String> unmappedFieldsSet) {
        this.unmappedFieldsSet = unmappedFieldsSet;
    }

    public SforceLookupProperties() {}

    public SforceLookupProperties(String sObjectName, String sLookupField, String sLookupSObjectName,
            List<String> compositeKeyFields) {
        this.sObjectName = sObjectName;
        this.sLookupField = sLookupField;
        this.sLookupSObjectName = sLookupSObjectName;
        this.compositeKeyFields = compositeKeyFields;
    }

    public String getsObjectName() {
        return sObjectName;
    }

    public void setsObjectName(String sObjectName) {
        this.sObjectName = sObjectName;
    }

    public String getsLookupField() {
        return sLookupField;
    }

    public void setsLookupField(String sLookupField) {
        this.sLookupField = sLookupField;
    }

    public String getsLookupSObjectName() {
        return sLookupSObjectName;
    }

    public void setsLookupSObjectName(String sLookupSObjectName) {
        this.sLookupSObjectName = sLookupSObjectName;
    }

    public List<String> getCompositeKeyFields() {
        return compositeKeyFields;
    }

    public void setCompositeKeyFields(List<String> compositeKeyFields) {
        this.compositeKeyFields = compositeKeyFields;
    }

    public String getWhere() {
        return where;
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public String toString() {
        return "SforceLookupProperties[" + "sObjectName:" + sObjectName + ", sLookupField:" + sLookupField
                + ", sLookupSObjectName:" + sLookupSObjectName + ", compositeKeyFields:" + compositeKeyFields + "]";
    }
}
