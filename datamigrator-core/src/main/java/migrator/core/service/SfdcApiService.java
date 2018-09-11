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

import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;
import migrator.core.connect.SfdcConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SfdcApiService
 *
 * @author anoop.singh
 */
public interface SfdcApiService {

    public Map<String, SforceObjectPair> persistJson(PartnerConnection connection, SforceObject sforceObject,
            Map<String, SforceObjectPair> sObjectRecordsMap, boolean bSource);


    /**
     * Queries the Object based on the its API name. The results are stored in the sObjectRecords
     * List This method queries the standalone objects only (aka parent).
     *
     * @param PartnerConnection connection: connection to Salesforce org
     * @param String sObjectAPIName: Object to be queried (API name)
     * @param Map <String, SforceObjectPair> sObjectRecordsMap: Map to park the results of the query
     *        (SObjects) with source id being the key
     * @param boolean source: True for source environment, otherwise its for target environment
     *
     */
    public Map<String, SforceObjectPair> query(PartnerConnection connection, SforceObject sforceObject,
            Map<String, SforceObjectPair> sObjectRecordsMap, boolean bSource);

    /**
     * Queries the Object based on the its API name. The results are stored in the sObjectRecords
     * List This method queries the standalone objects only (aka parent).
     *
     * @param PartnerConnection connection: connection to Salesforce org
     * @param String sObjectAPIName: Object to be queried (API name)
     * @param Map sObjectRecordsMap: List to park the results of the query (SObjects)
     * @param boolean source: True for source environment, otherwise its for target environment
     *
     */
    public String query(PartnerConnection connection, SforceObject sforceObject,
            final Map<String, SforceObjectPair> sObjectRecordsMap, List<String> compositeKeys, boolean source);

    public String queryValidate(PartnerConnection connection, SforceObject sforceObject,
            final Map<String, SforceObjectPair> sObjectRecordsMap, List<String> compositeKeys, boolean source);

    /**
     * Counts number of records
     *
     * @param PartnerConnection connection: connection to Salesforce org
     * @param String sObjectAPIName: Object to be queried (API name)
     *
     */
    public Integer queryValidate(PartnerConnection connection, SforceObject sforceObject);

    public String queryLookup(PartnerConnection connection, SforceObject sforceObject,
            Map<String, List<SforceLookupProperties>> lookupPropertiesMap, boolean bSource);

    /**
     * Queries the Object based on the its API name. The results are stored in the sObjectRecords
     * List. This method queries the children objects.
     *
     * @param PartnerConnection connection: connection to Salesforce org
     * @param String sObjectAPIName: Object to be queried (API name)
     * @param Map sObjectRecordsMap: List to park the results of the query (SObjects)
     * @param SforceMasterDetail masterDetail: The field name and object names of the parents in
     *        child SObject
     *
     */
    public Map<String, SforceObjectPair> queryChildren(PartnerConnection connection, SforceObject sforceObject,
            Map<String, SforceObjectPair> sObjectRecordsMap, // Current object
                                                             // records
            SforceMasterDetail masterDetail, // All the master records (few
                                             // cases may have more than 1
                                             // master)
            List<SforceObject> sForceObjectList, boolean bSource);

    /**
     * Queries the Object based on the its API name. Returns the List of QueryResults
     *
     * @param PartnerConnection connection: connection to Salesforce org
     * @param String sObjectAPIName: Object to be queried (API name)
     *
     */
    public ArrayList<QueryResult> getQueryResults(PartnerConnection connection, SforceObject sforceObject);

    /**
     * Creates the records in the Target Salesforce Org.
     *
     * @param PartnerConnection connection: connection to Salesforce org
     * @param String sObjectAPIName: Object to be queried (API name)
     * @param List sObjectSourceRecords: SObject records from the source Org - Used to relate
     *        records in source and target Orgs
     * @param SObject [] sObjectInsertRecords: SObject records to insert in the Target Org - Used to
     *        fetch
     * @param Boolean relateSourceTargetRecs: True if source-target records needs to be mapped 1-1
     *
     */
    public String[] create(PartnerConnection connection, SforceObject sforceObject, final List sObjectSourceRecords,
            final SObject[] sObjectInsertRecords, boolean relateSourceTargetRecs);

    public String[] upsertWithExternalId(PartnerConnection connection, SforceObject sforceObject,
            String sExternalIdField, final List<SforceObjectPair> sObjectSourceRecords,
            final SObject[] sObjectInsertRecords, boolean relateSourceTargetRecs, boolean bUpdate);

    /**
     * Delete the sobject records
     *
     * @param String sObjectAPIName: Object to be queried (API name)
     * @param sfdcConnection : always TARGET Org
     *
     */
    public void delete(String sObjectName, SfdcConnection sfdcConnection);

}
