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

import java.util.Map;
import java.util.HashMap;
import com.sforce.soap.partner.sobject.SObject;

/**
 * Wrapper class - Relate the source and target environment records
 *
 * @author anoop.singh
 */
public class SforceObjectPair implements ISforceObjectPair {

    // record id from source org
    protected String sourceId;
    // record id from target org
    protected String targetId;
    // This is self SObject (from source org)
    protected SObject sourceSObject;
    // This is self SObject (from target org)
    protected SObject targetSObject;
    // This is from source org (field, sobject)
    protected Map<String, SObject> sourceSObjectParentMap;

    public SforceObjectPair() {
        sourceSObjectParentMap = new HashMap<String, SObject>();
    }

    public String getSourceId() {
        return this.sourceId;
    }

    public String getTargetId() {
        return this.targetId;
    }

    public void setSourceId(String id) {
        this.sourceId = id;
    }

    public void setTargetId(String id) {
        this.targetId = id;
    }

    public SObject getSourceSObject() {
        return this.sourceSObject;
    }

    public void setSourceSObject(SObject sObjectSelf) {
        this.sourceSObject = sObjectSelf;
    }

    public SObject getTargetSObject() {
        return targetSObject;
    }

    public void setTargetSObject(SObject targetSObjectSelf) {
        this.targetSObject = targetSObjectSelf;
    }

    public Map<String, SObject> getSourceSObjectParentMap() {
        return sourceSObjectParentMap;
    }

    public void setSourceSObjectParentMap(Map<String, SObject> sourceSObjectParentMap) {
        this.sourceSObjectParentMap = sourceSObjectParentMap;
    }

    public String toString() {
        return "SforceObjectPair[" + "sourceId:" + sourceId + ", targetId:" + targetId + "]";
    }

}
