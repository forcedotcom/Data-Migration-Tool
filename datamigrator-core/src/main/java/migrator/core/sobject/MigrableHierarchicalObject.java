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
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.sforce.soap.partner.sobject.SObject;
import migrator.core.service.SforceMasterDetail;
import migrator.core.service.SforceObject;
import migrator.core.service.SforceObjectPair;
import migrator.core.service.impl.SfdcApiServiceImpl;
import migrator.core.utils.Utils;

/**
 * MigrableHierarchicalObject : Class to support hierarchical relationships
 *
 * @author anoop.singh
 */
public class MigrableHierarchicalObject extends MigrableMasterDetailObject {

    static Logger log = Logger.getLogger(MigrableHierarchicalObject.class.getName());

    public MigrableHierarchicalObject() {
        super();
    }

    public MigrableHierarchicalObject(ObjectMappingConfig objectMapping, String operation) {
        super(objectMapping, operation);
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

    // Hook for self-references
    public SObject buildMappingUpdate(SforceObject sforceObject, SforceObjectPair sourcePairObj) {
        SObject insertRecord = buildMapping(sforceObject, sourcePairObj);
        insertRecord.setId(sourcePairObj.getTargetId());
        return insertRecord;
    }

    // Using source Org only
    @SuppressWarnings("unchecked")
    private void exportSource() {
        for (SforceObject sforceObject : sForceObjectList) {
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

        for (SforceObject sforceObject : sForceObjectList) {
            String objectName = sforceObject.getsObjectName();

            boolean reIssueQuery = false;

            if (sforceObject.isLookup()) {
                continue;
            }

            if (bReIssueLookupQuery(objectName)) {
                reIssueQuery = true;
            }

            Map<String, SforceObjectPair> sourceRecordsMap = sforceObject.getRecordsMap();
            if (sourceRecordsMap == null) {
                return;
            }

            List<SforceObjectPair> sourceRecords = new ArrayList<SforceObjectPair>();
            for (Map.Entry<String, SforceObjectPair> entry : sourceRecordsMap.entrySet()) {
                // This map contains composite key also, discard those while
                // inserting/updating
                String mapKey = entry.getKey();
                if (!mapKey.startsWith(Utils.PREFIX_COMPOSITE_KEY)) {
                    sourceRecords.add(entry.getValue());
                }
            }

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
            SfdcApiServiceImpl.getSOQLQueryService().upsertWithExternalId(sfdcConnection.getTargetConnection(),
                    sforceObject, sforceObject.getExternalIdField(), sourceRecords, insertRecords, true, false);

            // TODO: This might be needed, just depends on JSON mapping
            // Solving this issue little differently if this part of
            // hierarchical structure. Use refresh flag in JSON

            if (reIssueQuery) {
                // super.reIssueLookupQuery(objectName);
                super.exportLookup(objectName);
                updateTarget(objectName);
            }
        }
    }

    // Update to get the hierarchical relationship right
    public void updateTarget() {

        for (SforceObject sforceObject : sForceObjectList) {
            if (sforceObject.isLookup() || (sforceObject.isRefresh() == false)) {
                continue;
            }

            super.exportLookup();

            Map<String, SforceObjectPair> sourceRecordsMap = sforceObject.getRecordsMap();
            if (sourceRecordsMap == null) {
                return;
            }

            List<SforceObjectPair> sourceRecords = new ArrayList<SforceObjectPair>();
            for (Map.Entry<String, SforceObjectPair> entry : sourceRecordsMap.entrySet()) {
                // This map contains composite key also, discard those while
                // inserting/updating
                String mapKey = entry.getKey();
                if (!mapKey.startsWith(Utils.PREFIX_COMPOSITE_KEY)) {
                    sourceRecords.add(entry.getValue());
                }
            }

            int size = 0;
            if (sourceRecords == null || sourceRecords.size() == 0) {
                continue;
            } else {
                size = sourceRecords.size();
            }
            SObject[] insertRecords = new SObject[size];
            int counter = 0;
            for (SforceObjectPair sObjectPairRecord : sourceRecords) {
                SObject insertRecord = buildMappingUpdate(sforceObject, sObjectPairRecord);
                insertRecords[counter] = insertRecord;
                counter++;
            }
            SfdcApiServiceImpl.getSOQLQueryService().upsertWithExternalId(sfdcConnection.getTargetConnection(),
                    sforceObject, sforceObject.getExternalIdField(), sourceRecords, insertRecords, true, true);
        }
    }

    // Update to get the hierarchical relationship right
    public void updateTarget(String sObjectName) {
        for (SforceObject sforceObject : sForceObjectList) {
            String objectName = sforceObject.getsObjectName();

            if (sforceObject.isLookup() || (sforceObject.isRefresh() == false)) {
                continue;
            }
            if (!sObjectName.equals(objectName)) {
                return;
            }
            super.exportLookup();

            Map<String, SforceObjectPair> sourceRecordsMap = sforceObject.getRecordsMap();
            if (sourceRecordsMap == null) {
                return;
            }

            List<SforceObjectPair> sourceRecords = new ArrayList<SforceObjectPair>();
            for (Map.Entry<String, SforceObjectPair> entry : sourceRecordsMap.entrySet()) {
                // This map contains composite key also, discard those while
                // inserting/updating
                String mapKey = entry.getKey();
                if (!mapKey.startsWith(Utils.PREFIX_COMPOSITE_KEY)) {
                    sourceRecords.add(entry.getValue());
                }
            }

            int size = 0;
            if (sourceRecords == null || sourceRecords.size() == 0) {
                continue;
            } else {
                size = sourceRecords.size();
            }
            SObject[] insertRecords = new SObject[size];

            int counter = 0;
            for (SforceObjectPair sObjectPairRecord : sourceRecords) {
                SObject insertRecord = buildMappingUpdate(sforceObject, sObjectPairRecord);
                insertRecords[counter] = insertRecord;
                counter++;
            }

            SfdcApiServiceImpl.getSOQLQueryService().upsertWithExternalId(sfdcConnection.getTargetConnection(),
                    sforceObject, sforceObject.getExternalIdField(), sourceRecords, insertRecords, true, true);
        }
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
