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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import migrator.core.connect.SfdcConnection;
import migrator.core.service.MetadataObjectHolder;
import migrator.core.service.SforceObject;
import migrator.core.service.SforceObjectPair;
import migrator.core.utils.Utils;

/**
 * ValidatorService : Counts the number of records in source / target orgs
 *
 * @author anoop.singh
 */

public class ValidatorService {

    SfdcConnection sfdcConnection;
    List<ResultCompare> resultCompareList = new ArrayList<ResultCompare>();

    // Constructor
    public ValidatorService() {
        sfdcConnection = new SfdcConnection();
        if (!sfdcConnection.login()) {
            System.out.println("Login failed!");
            return;
        }
    }

    private void validate(String sObjectName, List<String> uniquefieldNames, String where) {

        SforceObject sForceObjSrc = new SforceObject();
        sForceObjSrc.setsObjectName(sObjectName);
        sForceObjSrc.setWhere(where);


        SforceObject sForceObjTgt = new SforceObject();
        sForceObjTgt.setsObjectName(sObjectName);
        sForceObjTgt.setWhere(where);


        MetadataObjectHolder.getInstance().init(SfdcConnection.ORG_TYPE.SOURCE, sfdcConnection, sObjectName);

        Map<String, SforceObjectPair> sourceSforceObjPairMap = new HashMap<String, SforceObjectPair>();
        SfdcApiServiceImpl.getSOQLQueryService().queryValidate(sfdcConnection.getSourceConnection(), sForceObjSrc,
                sourceSforceObjPairMap, uniquefieldNames, true);

        Map<String, SforceObjectPair> targetSforceObjPairMap = new HashMap<String, SforceObjectPair>();
        SfdcApiServiceImpl.getSOQLQueryService().queryValidate(sfdcConnection.getTargetConnection(), sForceObjTgt,
                targetSforceObjPairMap, uniquefieldNames, false);

        int srcCounter = 0;
        List<String> missingSrcRecords = new ArrayList<String>();
        for (Map.Entry<String, SforceObjectPair> entry : sourceSforceObjPairMap.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(Utils.PREFIX_COMPOSITE_KEY)) {
                SforceObjectPair pair = entry.getValue();

                // This is the record in source based on unique key. Does it exists in target Org,
                // check it
                if (!targetSforceObjPairMap.containsKey(key)) {
                    missingSrcRecords.add(pair.getSourceId());
                }
            } else {
                srcCounter++;
            }
        }
        int tgtCounter = 0;
        for (Map.Entry<String, SforceObjectPair> entry1 : targetSforceObjPairMap.entrySet()) {
            String key = entry1.getKey();
            if (key.startsWith(Utils.PREFIX_COMPOSITE_KEY)) {
                SforceObjectPair pair = entry1.getValue();
            } else {
                tgtCounter++;
            }
        }

        // Compare number for records:
        System.out.println("Source records for sObjectName: [" + sObjectName + "]: " + srcCounter);
        System.out.println("Target records for sObjectName: [" + sObjectName + "]: " + tgtCounter);

        System.out.println("Records missing in Target\n");

        System.out.println(missingSrcRecords);

        // disconnect from source and target
        sfdcConnection.disconnect();
    }

    public void validateCounts(String args) {
        String[] objectsArray = args.split(",");
        List<String> sObjectNames = Arrays.asList(objectsArray);

        for (String sObjectName : sObjectNames) {
            SforceObject sforceObject = new SforceObject();
            sforceObject.setsObjectName(sObjectName);

            MetadataObjectHolder.getInstance().init(SfdcConnection.ORG_TYPE.SOURCE, sfdcConnection, sObjectName);

            Integer sourceCounts =
                    SfdcApiServiceImpl.getSOQLQueryService().queryValidate(sfdcConnection.getSourceConnection(),
                            sforceObject);

            Integer targetCounts =
                    SfdcApiServiceImpl.getSOQLQueryService().queryValidate(sfdcConnection.getTargetConnection(),
                            sforceObject);

            resultCompareList.add(new ResultCompare(sObjectName, sourceCounts, targetCounts));
        }

        // print counts
        System.out.format("\n\n");
        System.out.format("%-50s%-20s%-20s\n", "Object_Name,", "Source_Records,", "Target_Records");

        for (ResultCompare resultCompare : resultCompareList) {
            System.out.format("%-50s%-20s%-20s\n", resultCompare.sObjectName + ",", resultCompare.sourceRecordsCount
                    + ",", resultCompare.targetRecordsCount);
        }
        System.out.format("\n\n");

        // disconnect from source and target
        sfdcConnection.disconnect();

    }

    public static class ResultCompare {
        String sObjectName;
        Integer sourceRecordsCount;
        Integer targetRecordsCount;

        public ResultCompare(String sObjectName, Integer sourceRecordsCount, Integer targetRecordsCount) {
            this.sObjectName = sObjectName;
            this.sourceRecordsCount = sourceRecordsCount;
            this.targetRecordsCount = targetRecordsCount;
        }
    }

}
