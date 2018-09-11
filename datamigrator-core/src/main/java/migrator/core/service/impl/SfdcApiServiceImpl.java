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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import org.apache.log4j.Logger;
import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.SaveResult;
import com.sforce.soap.partner.UpsertResult;
import com.sforce.ws.ConnectionException;
import com.sforce.soap.partner.sobject.SObject;
import migrator.core.buffer.SObjectBuffer;
import migrator.core.buffer.SObjectDeleteBuffer;
import migrator.core.connect.SfdcConnection;
import migrator.core.connect.SfdcConnectionFactory;
import migrator.core.service.ISourceLoader;
import migrator.core.service.MetadataObjectHolder;
import migrator.core.service.PropertiesReader;
import migrator.core.service.SfdcApiService;
import migrator.core.service.SforceLookupProperties;
import migrator.core.service.SforceMasterDetail;
import migrator.core.service.SforceObject;
import migrator.core.service.SforceObjectPair;
import migrator.core.utils.Utils;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * MigrationConfigServiceImpl :
 *
 * @author anoop.singh
 */
public class SfdcApiServiceImpl implements SfdcApiService {

    private static SfdcApiService service = new SfdcApiServiceImpl();
    static Logger log = Logger.getLogger(SfdcApiServiceImpl.class.getName());

    public static SfdcApiService getSOQLQueryService() {
        return service;
    }

    public Map<String, SforceObjectPair> persistJson(PartnerConnection connection, SforceObject sforceObject,
            Map<String, SforceObjectPair> sObjectRecordsMap, boolean bSource) {
        ArrayList<QueryResult> queryResults = new ArrayList<QueryResult>();
        queryResults = new SourceLoaderFactory().getLoader(bSource).querySource(connection, sforceObject);
        new SfdcSerializerService().serialize(sforceObject, queryResults);
        return null;
    }

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
            Map<String, SforceObjectPair> sObjectRecordsMap, boolean bSource) {

        fakeReLogin();

        long t1 = System.currentTimeMillis();
        log.debug("Running SF query for [" + sforceObject.getsObjectName() + "]");

        ArrayList<QueryResult> queryResults = new ArrayList<QueryResult>();
        queryResults = new SourceLoaderFactory().getLoader(bSource).querySource(connection, sforceObject);

        try {

            for (int iQueryResult = 0; iQueryResult < queryResults.size(); iQueryResult++) {
                QueryResult queryResult = queryResults.get(iQueryResult);
                SObject[] records = queryResult.getRecords();
                for (int i = 0; i < records.length; i++) {
                    SObject sObject = (SObject) records[i];

                    SforceObjectPair pair = sObjectRecordsMap.get(sObject.getId());
                    if (pair == null) {
                        pair = new SforceObjectPair();
                    }

                    if (bSource) {
                        pair.setSourceId(sObject.getId());
                        pair.setSourceSObject(sObject);

                        // Note: only for source
                        sObjectRecordsMap.put(sObject.getId(), pair);
                    }
                }
            }
        } catch (Exception ce) {
            ce.printStackTrace();
        }

        log.debug("SF query for type [" + sforceObject.getsObjectName() + "] took ["
                + (System.currentTimeMillis() - t1) + "]ms");
        return sObjectRecordsMap;
    }

    /**
     * Queries the Object based on the its API name. The results are stored in the sObjectRecords
     * List
     *
     * @param PartnerConnection connection: connection to Salesforce org
     * @param String sObjectAPIName: Object to be queried (API name)
     * @param Map sObjectRecordsMap: List to park the results of the query (SObjects)
     * @param boolean source: True for source environment, otherwise its for target environment
     *
     */
    public String query(PartnerConnection connection, SforceObject sforceObject,
            final Map<String, SforceObjectPair> sObjectRecordsMap, List<String> compositeKeys, boolean bSource) {

        log.debug("SOQLQueryServiceImpl.reIssue Query: SF query for type [" + sforceObject.getsObjectName() + "]");

        fakeReLogin();

        long t1 = System.currentTimeMillis();
        log.debug("Running SF query for [" + sforceObject.getsObjectName() + "]");

        String compositeKey = "";
        ArrayList<QueryResult> queryResults = new ArrayList<QueryResult>();
        queryResults = new SourceLoaderFactory().getLoader(bSource).querySource(connection, sforceObject);

        try {

            for (int iQueryResult = 0; iQueryResult < queryResults.size(); iQueryResult++) {
                QueryResult queryResult = queryResults.get(iQueryResult);
                SObject[] records = queryResult.getRecords();
                for (int i = 0; i < records.length; i++) {

                    SObject sObject = (SObject) records[i];

                    compositeKey = Utils.getKey(sforceObject.getsObjectName(), sObject, compositeKeys);

                    SforceObjectPair pair = (SforceObjectPair) sObjectRecordsMap.get(compositeKey);
                    if (pair == null) {
                        pair = new SforceObjectPair();
                    }

                    if (bSource) {
                        pair.setSourceId(sObject.getId());
                        pair.setSourceSObject(sObject);
                        // Also duplicate the pair with the source id
                        sObjectRecordsMap.put(sObject.getId(), pair);
                    } else {
                        pair.setTargetId(sObject.getId());
                        pair.setTargetSObject(sObject);
                    }
                    sObjectRecordsMap.put(compositeKey, pair);

                }
            }
        } catch (Exception ce) {
            ce.printStackTrace();
        }

        log.debug("SF query for type [" + sforceObject.getsObjectName() + "] took ["
                + (System.currentTimeMillis() - t1) + "]ms");

        return compositeKey;
    }

    /**
     * Queries the Object based on the its API name. The results are stored in the sObjectRecords
     * List
     *
     * @param PartnerConnection connection: connection to Salesforce org
     * @param String sObjectAPIName: Object to be queried (API name)
     * @param Map sObjectRecordsMap: List to park the results of the query (SObjects)
     * @param boolean source: True for source environment, otherwise its for target environment
     *
     */
    public String queryLookup(PartnerConnection connection, SforceObject sforceObject,
            Map<String, List<SforceLookupProperties>> lookupPropertiesMap, boolean bSource) {
        fakeReLogin();

        long t1 = System.currentTimeMillis();
        log.debug("Lookup: Running SF query for [" + sforceObject.getsObjectName() + "]");

        String compositeKey = "";
        ArrayList<QueryResult> queryResults = new ArrayList<QueryResult>();
        queryResults = new SourceLoaderFactory().getLoader(bSource).querySource(connection, sforceObject);

        try {

            for (int iQueryResult = 0; iQueryResult < queryResults.size(); iQueryResult++) {
                QueryResult queryResult = queryResults.get(iQueryResult);
                SObject[] records = queryResult.getRecords();
                for (int i = 0; i < records.length; i++) {

                    SObject sObject = (SObject) records[i];

                    for (Map.Entry<String, List<SforceLookupProperties>> entry1 : lookupPropertiesMap.entrySet()) {
                        List<SforceLookupProperties> lookupList = entry1.getValue();
                        for (SforceLookupProperties loopProperty : lookupList) {

                            if (loopProperty.getsLookupSObjectName().equalsIgnoreCase(sforceObject.getsObjectName())) {
                                Map<String, SforceObjectPair> sforceObjPairMap = sforceObject.getLookupRecordsMap();
                                if (sforceObjPairMap == null) {
                                    sforceObjPairMap = new HashMap<String, SforceObjectPair>();
                                }

                                compositeKey =
                                        Utils.getKey(sforceObject.getsObjectName(), sObject,
                                                loopProperty.getCompositeKeyFields());

                                SforceObjectPair pair = (SforceObjectPair) sforceObjPairMap.get(compositeKey);
                                if (pair == null) {
                                    pair = new SforceObjectPair();
                                }

                                if (bSource) {
                                    pair.setSourceId(sObject.getId());
                                    pair.setSourceSObject(sObject);
                                    // Also duplicate the pair with the source
                                    // id
                                    sforceObjPairMap.put(sObject.getId(), pair);
                                } else {
                                    pair.setTargetId(sObject.getId());
                                    pair.setTargetSObject(sObject);
                                }
                                sforceObjPairMap.put(compositeKey, pair);
                            }
                        }
                    }
                }

            }
        } catch (Exception ce) {
            ce.printStackTrace();
        }

        log.debug("SF query for type [" + sforceObject.getsObjectName() + "] took ["
                + (System.currentTimeMillis() - t1) + "]ms");

        return compositeKey;
    }

    class SourceLoaderFactory {
        ISourceLoader sourceLoader;

        public ISourceLoader getLoader(boolean bSource) {
            String sourceType =
                    PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "source.type");
            if (bSource == false || sourceType == null || sourceType.equals("org")) {
                sourceLoader = new OrgSourceLoader();
            } else if (sourceType.equals("json")) {
                sourceLoader = new JsonSourceLoader();
            }
            return sourceLoader;
        }
    }

    class OrgSourceLoader implements ISourceLoader {

        public ArrayList<QueryResult> querySource(PartnerConnection connection, SforceObject sforceObject) {
            return queryResults(connection, sforceObject, new ArrayList<QueryResult>());
        }
    }

    class JsonSourceLoader implements ISourceLoader {

        public ArrayList<QueryResult> querySource(PartnerConnection connection, SforceObject sforceObject) {
            return new SfdcSerializerService().deSerialize(sforceObject);
        }
    }


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
            List<SforceObject> sForceObjectList, boolean bSource) {

        fakeReLogin();

        long t1 = System.currentTimeMillis();
        log.debug("Running SF query for [" + sforceObject.getsObjectName() + "]");

        ArrayList<QueryResult> queryResults = new ArrayList<QueryResult>();
        queryResults = new SourceLoaderFactory().getLoader(bSource).querySource(connection, sforceObject);

        log.debug("Running SF query for [" + sforceObject.getsObjectName() + "] queryResults.size()="
                + queryResults.size());

        try {

            for (int iQueryResult = 0; iQueryResult < queryResults.size(); iQueryResult++) {
                QueryResult queryResult = queryResults.get(iQueryResult);
                SObject[] records = queryResult.getRecords();
                for (int i = 0; i < records.length; i++) {

                    SObject sChildObject = (SObject) records[i];

                    SforceObjectPair childPair = (SforceObjectPair) sObjectRecordsMap.get(sChildObject.getId());
                    if (childPair == null) {
                        childPair = new SforceObjectPair();
                    }

                    if (bSource) {
                        childPair.setSourceId(sChildObject.getId());
                        childPair.setSourceSObject(sChildObject);
                        // Also duplicate the pair with the source id
                        sObjectRecordsMap.put(sChildObject.getId(), childPair);
                    } else {
                        childPair.setTargetId(sChildObject.getId());
                        childPair.setTargetSObject(sChildObject);
                    }

                    // Get the source parent and attach in the logical pair
                    // object
                    findAndAttachParents(sforceObject.getsObjectName(), sChildObject, childPair, masterDetail,
                            sForceObjectList);

                }
            }

        } catch (Exception ce) {
            ce.printStackTrace();
        }

        log.debug("SF query for type [" + sforceObject.getsObjectName() + "] took ["
                + (System.currentTimeMillis() - t1) + "]ms");

        return sObjectRecordsMap;
    }

    /**
     * Queries the Object based on the its API name. Returns the List of QueryResults
     *
     * @param PartnerConnection connection: connection to Salesforce org
     * @param String sObjectAPIName: Object to be queried (API name)
     *
     */
    public ArrayList<QueryResult> getQueryResults(PartnerConnection connection, SforceObject sforceObject) {

        fakeReLogin();

        long t1 = System.currentTimeMillis();
        log.debug("Running SF query for [" + sforceObject.getsObjectName() + "]");

        ArrayList<QueryResult> queryResults = new ArrayList<QueryResult>();

        queryResults = queryResults(connection, sforceObject, queryResults);

        log.debug("SF query for type [" + sforceObject.getsObjectName() + "] took ["
                + (System.currentTimeMillis() - t1) + "]ms");

        return queryResults;
    }

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
            final SObject[] sObjectInsertRecords, boolean relateSourceTargetRecs) {

        fakeReLogin();

        int size = 0;
        if (sObjectInsertRecords != null && sObjectInsertRecords.length > 0) {
            size = sObjectInsertRecords.length;
        } else {
            return null;
        }

        String sObjectAPIName = sforceObject.getsObjectName();
        String[] result = new String[size];
        List<ErrorResult> errorList = new ArrayList<ErrorResult>();
        List<RetryRecords> retryList = new ArrayList<RetryRecords>();

        log.info(sObjectAPIName + " Records: " + sObjectInsertRecords.length);

        int resultCounter = 0;

        try {

            SaveResult[] saveResults = divideWork(sforceObject, sObjectInsertRecords, false);

            log.info("START Printing " + sObjectAPIName + " Record IDs:");
            // Iterate through the results.
            for (int i = 0; i < saveResults.length; i++) {
                if (saveResults[i].isSuccess()) {
                    log.debug("Successfully created" + " at Index: " + i + " Target Record ID: "
                            + saveResults[i].getId());

                    result[resultCounter] = saveResults[i].getId();
                } else {
                    String sourceId = ((SforceObjectPair) sObjectSourceRecords.get(resultCounter)).getSourceId();

                    String errorMsg = saveResults[i].getErrors()[0].getMessage();
                    String statusCode = saveResults[i].getErrors()[0].getStatusCode().toString();

                    Set<String> errorCodesSet = new HashSet<String>();
                    for (com.sforce.soap.partner.Error error : saveResults[i].getErrors()) {
                        errorCodesSet.add(error.getStatusCode().toString());
                    }

                    String logErrorMsg =
                            sObjectAPIName + " " + " SourceId: " + sourceId + " ," + " could not create Record "
                                    + "for array element " + i + ". \n" + " The error reported was: "
                                    // + errorMsg
                                    + " statusCode: " + statusCode;

                    if (statusCode != null) {
                        if ((errorCodesSet.contains("UNABLE_TO_LOCK_ROW"))
                                || (errorCodesSet.contains("INVALID_CROSS_REFERENCE_KEY"))) {
                            retryList.add(new RetryRecords(i, sObjectAPIName, sourceId, statusCode));

                            log.debug(logErrorMsg);
                        } else {
                            errorList.add(new ErrorResult(i, sObjectAPIName, sourceId, errorMsg));
                            log.error("Error: " + logErrorMsg + " errorMsg:" + errorMsg);
                        }
                    } else {
                        errorList.add(new ErrorResult(i, sObjectAPIName, sourceId, errorMsg));
                        log.error("Error: " + logErrorMsg + " errorMsg:" + errorMsg);
                    }

                    result[resultCounter] = saveResults[i].getId();
                }
                resultCounter++;
            }

            log.info("END Printing " + sObjectAPIName + " Record IDs: \n\n");

            // Process Retries
            if (retryList.size() > 0) {

                log.info("Retries(Create): Single Threaded: retryList.size(): " + retryList.size());

                SObject[] sObjectInsertRetryRecords = new SObject[retryList.size()];

                for (int i = 0; i < retryList.size(); i++) {
                    sObjectInsertRetryRecords[i] = sObjectInsertRecords[retryList.get(i).originlIndex];
                }
                SaveResult[] saveResultsRetries = divideWork(sforceObject, sObjectInsertRetryRecords, true);

                log.info("START Printing Retries " + sObjectAPIName + " Record IDs:");
                // Iterate through the results.
                // for (int i = 0; i < saveResultsRetries.length; i++) {
                for (int i = 0; i < retryList.size(); i++) {
                    String sourceId =
                            ((SforceObjectPair) sObjectSourceRecords.get(retryList.get(i).originlIndex)).getSourceId();

                    if (saveResultsRetries[i].isSuccess()) {
                        log.debug("Successfully created Retries:" + " Original Index was: "
                                + retryList.get(i).originlIndex + " SourceId: " + sourceId + " Target Record ID: "
                                + saveResultsRetries[i].getId());
                        result[retryList.get(i).originlIndex] = saveResultsRetries[i].getId();
                    } else {

                        String errorMsg = saveResultsRetries[i].getErrors()[0].getMessage();

                        log.error("Error Retry: " + sObjectAPIName + " SourceId: " + sourceId + " ,"
                                + " could not create Record " + "for array element " + i + ". \n"
                                + " The error reported was: " + errorMsg + "\n");

                        errorList
                                .add(new ErrorResult(retryList.get(i).originlIndex, sObjectAPIName, sourceId, errorMsg));

                        result[retryList.get(i).originlIndex] = saveResultsRetries[i].getId();
                    }
                    resultCounter++;
                }

                log.info("END Printing Retries " + sObjectAPIName + " Record IDs: \n\n");
            }


            if (relateSourceTargetRecs) {
                // Print results and relate source-target
                for (int i = 0; i < result.length; i++) {
                    ((SforceObjectPair) sObjectSourceRecords.get(i)).setTargetId(result[i]);
                }
            }

        } catch (Exception ce) {
            errorList.add(new ErrorResult(-1, sObjectAPIName, "", ce.getStackTrace().toString()));
            log.error("create.Exception: sObjectAPIName: " + sObjectAPIName, ce);
            ce.printStackTrace();
        }

        reportError(sObjectAPIName, errorList);

        return result;
    }

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
    public String[] upsertWithExternalId(PartnerConnection connection, SforceObject sforceObject,
            String sExternalIdField, final List<SforceObjectPair> sObjectSourceRecords,
            final SObject[] sObjectInsertRecords, boolean relateSourceTargetRecs, boolean bUpdate) {

        fakeReLogin();
        int size = 0;
        if (sObjectInsertRecords != null && sObjectInsertRecords.length > 0) {
            size = sObjectInsertRecords.length;
        } else {
            return null;
        }

        if ((sExternalIdField == null || sExternalIdField.equals(""))) {
            if (bUpdate) {
                return update(connection, sforceObject, sObjectSourceRecords, sObjectInsertRecords,
                        relateSourceTargetRecs);
            } else {
                return create(connection, sforceObject, sObjectSourceRecords, sObjectInsertRecords,
                        relateSourceTargetRecs);
            }
        }

        String[] result = new String[size];
        List<ErrorResult> errorList = new ArrayList<ErrorResult>();
        List<RetryRecords> retryList = new ArrayList<RetryRecords>();
        List<RetryRecords> retryUpdateableList = new ArrayList<RetryRecords>();

        try {

            log.info(sforceObject.getsObjectName() + " Records: " + sObjectInsertRecords.length);

            int resultCounter = 0;

            UpsertResult[] upsertResults =
                    divideWorkUpsert(sforceObject, sObjectInsertRecords, sExternalIdField, false);

            // Iterate through the results.
            if (upsertResults != null) {
                for (int i = 0; i < upsertResults.length; i++) {
                    if (upsertResults[i].isSuccess()) {
                        log.debug("Successfully Created/Updated" + " at Index: " + i + " Target Record ID: "
                                + upsertResults[i].getId());

                        result[resultCounter] = upsertResults[i].getId();
                    } else {

                        String sourceId = ((SforceObjectPair) sObjectSourceRecords.get(resultCounter)).getSourceId();

                        Set<String> errorCodesSet = new HashSet<String>();
                        for (com.sforce.soap.partner.Error error : upsertResults[i].getErrors()) {
                            errorCodesSet.add(error.getStatusCode().toString());
                        }

                        String errorMsg = upsertResults[i].getErrors()[0].getMessage();
                        String statusCode = upsertResults[i].getErrors()[0].getStatusCode().toString();

                        String logErrorMsg =
                                sforceObject.getsObjectName() + " " + " SourceId: " + sourceId + " ,"
                                        + " could not Create/Upsert Record " + "for array element " + i + ". \n"
                                        + " The error reported was: "
                                        // + errorMsg
                                        + " statusCode: " + statusCode;

                        if (errorCodesSet.size() > 0) {
                            if (errorCodesSet.contains("UNABLE_TO_LOCK_ROW")
                                    || errorCodesSet.contains("INVALID_CROSS_REFERENCE_KEY")
                                    || errorCodesSet.contains("INVALID_FIELD_FOR_INSERT_UPDATE")) {

                                retryList.add(new RetryRecords(i, sforceObject.getsObjectName(), sourceId, statusCode));
                                log.error("Error: " + logErrorMsg + " errorMsg:" + errorMsg);
                            } else {
                                errorList.add(new ErrorResult(i, sforceObject.getsObjectName(), sourceId, errorMsg));
                                log.error("Error: " + logErrorMsg + " errorMsg:" + errorMsg);
                            }
                        } else {
                            errorList.add(new ErrorResult(i, sforceObject.getsObjectName(), sourceId, errorMsg));
                            log.error("Error: " + logErrorMsg + " errorMsg:" + errorMsg);
                        }

                        result[resultCounter] = upsertResults[i].getId();

                    }
                    resultCounter++;
                }
            }

            log.info("END Printing " + sforceObject.getsObjectName() + " Record IDs: \n\n");

            // Process Retries
            if (retryList.size() > 0) {

                log.info("Retries(Create/Upsert): Single Threaded: retryList.size(): " + retryList.size());

                MetadataObjectHolder.MetadataRefObject descRefObj =
                        MetadataObjectHolder.getInstance().get(sforceObject.getsObjectName());

                if (!descRefObj.getUpdateableFieldSet().isEmpty())
                    descRefObj.getCreateableFieldSet().removeAll(descRefObj.getUpdateableFieldSet());

                SObject[] sObjectInsertRetryRecords = new SObject[retryList.size()];

                for (int i = 0; i < retryList.size(); i++) {
                    sObjectInsertRetryRecords[i] = sObjectInsertRecords[retryList.get(i).originlIndex];

                    // Remove fields that are non-updateable
                    if (retryList.get(i).statusCode.equals("INVALID_FIELD_FOR_INSERT_UPDATE")) {
                        for (String updateableField : descRefObj.getCreateableFieldSet()) {
                            sObjectInsertRetryRecords[i].removeField(updateableField);
                        }
                    }
                }
                UpsertResult[] upsertResultsRetries =
                        divideWorkUpsert(sforceObject, sObjectInsertRetryRecords, sExternalIdField, true);

                log.info("START Printing Crete/Upsert Retries: " + sforceObject.getsObjectName() + " Record IDs:");

                // Iterate through the retry results.
                for (int i = 0; i < retryList.size(); i++) {
                    String sourceId =
                            ((SforceObjectPair) sObjectSourceRecords.get(retryList.get(i).originlIndex)).getSourceId();

                    if (upsertResultsRetries[i].isSuccess()) {
                        log.debug("Successfully Created/Upserted Retries:" + " Original Index was: "
                                + retryList.get(i).originlIndex + " SourceId: " + sourceId + " Target Record ID: "
                                + upsertResultsRetries[i].getId());
                        result[retryList.get(i).originlIndex] = upsertResultsRetries[i].getId();

                    } else {

                        String errorMsg = upsertResultsRetries[i].getErrors()[0].getMessage();

                        log.error("Error create/upsert retry: " + sforceObject.getsObjectName() + " SourceId: "
                                + sourceId + " ," + " could not create/upsert Record " + "for array element "
                                + retryList.get(i).originlIndex + ". \n" + " The error reported was: " + errorMsg
                                + "\n");

                        errorList.add(new ErrorResult(retryList.get(i).originlIndex, sforceObject.getsObjectName(),
                                sourceId, errorMsg));

                        result[retryList.get(i).originlIndex] = upsertResultsRetries[i].getId();
                    }
                    resultCounter++;
                }

                log.info("END Printing Retries " + sforceObject.getsObjectName() + " Record IDs: \n\n");
            }

            if (relateSourceTargetRecs) {
                // Print results and relate source-target
                for (int i = 0; i < result.length; i++) {
                    ((SforceObjectPair) sObjectSourceRecords.get(i)).setTargetId(result[i]);
                }
            }
        } catch (Exception ce) {
            errorList.add(new ErrorResult(-1, sforceObject.getsObjectName(), "", ce.getStackTrace().toString()));
            log.error("upsertWithExternalId.Exception: sObjectAPIName: " + sforceObject.getsObjectName(), ce);
            ce.printStackTrace();
        }

        reportError(sforceObject.getsObjectName(), errorList);

        return result;
    }

    public String[] update(PartnerConnection connection, SforceObject sforceObject,
            final List<SforceObjectPair> sObjectSourceRecords, final SObject[] sObjectInsertRecords,
            boolean relateSourceTargetRecs) {

        fakeReLogin();
        int size = 0;
        if (sObjectInsertRecords != null && sObjectInsertRecords.length > 0) {
            size = sObjectInsertRecords.length;
        } else {
            return null;
        }

        String[] result = new String[size];
        List<ErrorResult> errorList = new ArrayList<ErrorResult>();
        List<RetryRecords> retryList = new ArrayList<RetryRecords>();

        try {

            log.info(sforceObject.getsObjectName() + " Records: " + sObjectInsertRecords.length);

            int resultCounter = 0;

            SaveResult[] updateResults = divideWorkUpdate(sforceObject, sObjectInsertRecords, false);

            // Iterate through the results.
            if (updateResults != null) {
                for (int i = 0; i < updateResults.length; i++) {
                    if (updateResults[i].isSuccess()) {
                        log.debug("Successfully Created/Updated" + " at Index: " + i + " Target Record ID: "
                                + updateResults[i].getId());

                        result[resultCounter] = updateResults[i].getId();
                    } else {

                        String sourceId = ((SforceObjectPair) sObjectSourceRecords.get(resultCounter)).getSourceId();

                        Set<String> errorCodesSet = new HashSet<String>();
                        for (com.sforce.soap.partner.Error error : updateResults[i].getErrors()) {
                            errorCodesSet.add(error.getStatusCode().toString());
                        }

                        String errorMsg = updateResults[i].getErrors()[0].getMessage();
                        String statusCode = updateResults[i].getErrors()[0].getStatusCode().toString();

                        String logErrorMsg =
                                sforceObject.getsObjectName() + " " + " SourceId: " + sourceId + " ,"
                                        + " could not Create/Upsert Record " + "for array element " + i + ". \n"
                                        + " The error reported was: "
                                        // + errorMsg
                                        + " statusCode: " + statusCode;

                        if (errorCodesSet.size() > 0) {
                            if (errorCodesSet.contains("UNABLE_TO_LOCK_ROW")
                                    || errorCodesSet.contains("INVALID_CROSS_REFERENCE_KEY")) {

                                retryList.add(new RetryRecords(i, sforceObject.getsObjectName(), sourceId, statusCode));
                                log.debug(logErrorMsg);
                            } else {
                                errorList.add(new ErrorResult(i, sforceObject.getsObjectName(), sourceId, errorMsg));
                                log.error("Error: " + logErrorMsg + " errorMsg:" + errorMsg);
                            }
                        } else {
                            errorList.add(new ErrorResult(i, sforceObject.getsObjectName(), sourceId, errorMsg));
                            log.error("Error: " + logErrorMsg + " errorMsg:" + errorMsg);
                        }

                        result[resultCounter] = updateResults[i].getId();

                    }
                    resultCounter++;
                }
            }

            log.info("END Printing " + sforceObject.getsObjectName() + " Record IDs: \n\n");

            // Process Retries
            if (retryList.size() > 0) {

                log.info("Retries(Create/Upsert): Single Threaded: retryList.size(): " + retryList.size());

                // fakeReLogin();

                SObject[] sObjectInsertRetryRecords = new SObject[retryList.size()];

                for (int i = 0; i < retryList.size(); i++) {
                    sObjectInsertRetryRecords[i] = sObjectInsertRecords[retryList.get(i).originlIndex];
                }
                SaveResult[] updateResultsRetries = divideWorkUpdate(sforceObject, sObjectInsertRetryRecords, true);

                log.info("START Printing Crete/update Retries: " + sforceObject.getsObjectName() + " Record IDs:");

                // Iterate through the retry results.

                // for (int i = 0; i < saveResultsRetries.length; i++) {
                for (int i = 0; i < retryList.size(); i++) {
                    String sourceId =
                            ((SforceObjectPair) sObjectSourceRecords.get(retryList.get(i).originlIndex)).getSourceId();

                    if (updateResultsRetries[i].isSuccess()) {
                        log.debug("Successfully Created/Upserted Retries:" + " Original Index was: "
                                + retryList.get(i).originlIndex + " SourceId: " + sourceId + " Target Record ID: "
                                + updateResultsRetries[i].getId());
                        result[retryList.get(i).originlIndex] = updateResultsRetries[i].getId();

                    } else {

                        String errorMsg = updateResultsRetries[i].getErrors()[0].getMessage();

                        log.error("Error create/upsert retry: " + sforceObject.getsObjectName() + " SourceId: "
                                + sourceId + " ," + " could not create/upsert Record " + "for array element "
                                + retryList.get(i).originlIndex + ". \n" + " The error reported was: " + errorMsg
                                + "\n");

                        errorList.add(new ErrorResult(retryList.get(i).originlIndex, sforceObject.getsObjectName(),
                                sourceId, errorMsg));

                        result[retryList.get(i).originlIndex] = updateResultsRetries[i].getId();
                    }
                    resultCounter++;
                }

                log.info("END Printing Retries " + sforceObject.getsObjectName() + " Record IDs: \n\n");
            }

            if (relateSourceTargetRecs) {
                // Print results and relate source-target
                for (int i = 0; i < result.length; i++) {
                    ((SforceObjectPair) sObjectSourceRecords.get(i)).setTargetId(result[i]);
                }
            }
        } catch (Exception ce) {
            errorList.add(new ErrorResult(-1, sforceObject.getsObjectName(), "", ce.getStackTrace().toString()));
            log.error("update.Exception: sObjectAPIName: " + sforceObject.getsObjectName(), ce);
            ce.printStackTrace();

            // log.error(connection.getDebuggingInfo());
        }

        reportError(sforceObject.getsObjectName(), errorList);

        return result;
    }

    /**
     * Queries the Object based on the its API name. Returns the List of QueryResults
     *
     * @param PartnerConnection connection: connection to Salesforce org
     * @param String sObjectAPIName: Object to be queried (API name)
     *
     */
    private ArrayList<QueryResult> queryResults(PartnerConnection connection, SforceObject sforceObject,
            ArrayList<QueryResult> queryResults) {

        String query = null;
        try {

            query = buildQuery(connection, sforceObject, false);

            log.debug("query:" + query);

            QueryResult qr = connection.query(query);
            int records = qr.getRecords().length;

            queryResults.add(qr);

            if (qr.getSize() > 0) {
                boolean done = false;
                while (!done) {
                    if (qr.isDone()) {
                        done = true;
                    } else {
                        qr = connection.queryMore(qr.getQueryLocator());
                        records += qr.getRecords().length;
                        queryResults.add(qr);
                    }
                }
                log.debug("\nLogged-in user can see " + records + " " + sforceObject.getsObjectName() + " records.");
            } else {
                log.debug("No records found.");
            }
        } catch (ConnectionException ce) {
            log.error("ConnectionException..." + ce.getMessage());
            log.error("ConnectionException...", ce);
            ce.printStackTrace();
        }
        return queryResults;
    }

    /**
     * Builds the SOQL query. Returns the comma separated fields in SObject
     *
     * @param PartnerConnection connection: connection to Salesforce org
     * @param String sObjectAPIName: Object to be queried (API name)
     *
     */
    private String buildQuery(PartnerConnection connection, SforceObject sforceObject, boolean lookup) {
        MetadataObjectHolder.MetadataRefObject descRefObj =
                MetadataObjectHolder.getInstance().get(sforceObject.getsObjectName());

        String fields = "";
        if (lookup) {
            if (sforceObject.getCompositeKeyFields() != null && sforceObject.getCompositeKeyFields().size() > 0) {
                for (String keyField : sforceObject.getCompositeKeyFields()) {
                    fields += keyField + ",";
                }
                if (fields.length() > 1) {
                    fields += "Id";
                }
            }

        } else {
            Set<String> fieldList = sforceObject.getCommonFields();
            for (String field : fieldList) {
                fields = fields + field + ",";
            }
            if (fields.length() > 1) {
                fields = fields.substring(0, fields.length() - 1);
            }
        }

        String query = "SELECT " + fields + "  FROM " + sforceObject.getsObjectName();

        String whereClause = "";
        if (sforceObject.getWhere() != null && !sforceObject.getWhere().equals("")) {
            whereClause = " WHERE " + sforceObject.getWhere();
            query = query + whereClause;
        }
        log.debug("sforceObject.query=" + query);

        return query;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void findAndAttachParents(String sChildObjectAPIName, SObject sChildObjectRecord,
            SforceObjectPair childPair, SforceMasterDetail masterDetail, List<SforceObject> sForceObjectList) {

        Class params[] = {};
        Object paramsObj[] = {};

        // Find parent sforceObject
        for (Map.Entry<String, String> parentFieldObj : masterDetail.getParentFieldObjectMap().entrySet()) {
            String masterFieldName = parentFieldObj.getKey();
            String masterObjectName = parentFieldObj.getValue();

            // Get the parent SforceObject
            SforceObject parentSforceObj = getSforceObject(masterObjectName, sForceObjectList);
            try {

                Object sParentObj = sChildObjectRecord.getField(masterFieldName);
                // Attach parent
                if (sParentObj != null) {

                    // Find the master SforceObject records
                    Map<String, SforceObjectPair> parentPairMap = parentSforceObj.getRecordsMap();
                    if (parentPairMap.containsKey(sParentObj.toString())) {
                        SforceObjectPair parentPair = parentPairMap.get(sParentObj.toString());

                        Map<String, SObject> parentsourceSObject = childPair.getSourceSObjectParentMap();
                        parentsourceSObject.put(masterFieldName, parentPair.getSourceSObject());
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("findAndAttachParents...", e);
            }
        }

    }

    /**
     * Delete the sobject records
     *
     * @param String sObjectAPIName: Object to be queried (API name)
     * @param sfdcConnection : delete from TARGET Org only
     *
     */
    public void delete(String sObjectName, SfdcConnection sfdcConnection) {
        log.info("SOQLQueryServiceImpl.delete START: sObjectName: " + sObjectName);

        List<String> ids = new ArrayList<String>();

        ArrayList<QueryResult> queryResults = new ArrayList<QueryResult>();
        String query = null;
        try {
            query = "SELECT Id FROM " + sObjectName;
            QueryResult qr = sfdcConnection.getTargetConnection().query(query);
            queryResults.add(qr);

            if (qr.getSize() > 0) {
                boolean done = false;
                while (!done) {
                    if (qr.isDone()) {
                        done = true;
                    } else {
                        qr = sfdcConnection.getTargetConnection().queryMore(qr.getQueryLocator());
                        queryResults.add(qr);
                    }
                }
            } else {
                log.debug("No records found.");
            }

            for (int iQueryResult = 0; iQueryResult < queryResults.size(); iQueryResult++) {
                QueryResult queryResult = queryResults.get(iQueryResult);
                SObject[] records = queryResult.getRecords();

                for (int i = 0; i < records.length; i++) {
                    SObject af = (SObject) records[i];
                    ids.add(af.getId());
                }
            }

            log.info("\nLogged-in user can see " + ids.size() + " " + sObjectName + " delete records.");

            String[] array = ids.toArray(new String[0]);

            divideWorkDelete(sObjectName, array, false);


        } catch (ConnectionException ce) {
            log.error("ConnectionException..." + ce.getMessage());
            ce.printStackTrace();
            log.error("ConnectionException...", ce);
        }

    }

    /**
     * Queries the Object based on the its API name. The results are stored in the sObjectRecords
     * List
     *
     * @param PartnerConnection connection: connection to Salesforce org
     * @param String sObjectAPIName: Object to be queried (API name)
     * @param Map sObjectRecordsMap: List to park the results of the query (SObjects)
     * @param boolean source: True for source environment, otherwise its for target environment
     *
     */
    public String queryValidate(PartnerConnection connection, SforceObject sforceObject,
            final Map<String, SforceObjectPair> sObjectRecordsMap, List<String> compositeKeys, boolean bSource) {

        log.debug("SOQLQueryServiceImpl.reIssue Query: SF query for type [" + sforceObject.getsObjectName() + "]");

        long t1 = System.currentTimeMillis();
        log.debug("Running SF query for [" + sforceObject.getsObjectName() + "]");

        String compositeKey = "";
        ArrayList<QueryResult> queryResults = new ArrayList<QueryResult>();

        queryResults = queryResults(connection, sforceObject, queryResults);

        try {

            for (int iQueryResult = 0; iQueryResult < queryResults.size(); iQueryResult++) {
                QueryResult queryResult = queryResults.get(iQueryResult);
                SObject[] records = queryResult.getRecords();
                for (int i = 0; i < records.length; i++) {

                    SObject sObject = (SObject) records[i];

                    compositeKey = Utils.getKey(sforceObject.getsObjectName(), sObject, compositeKeys);

                    SforceObjectPair pair = (SforceObjectPair) sObjectRecordsMap.get(compositeKey);
                    if (pair == null) {
                        pair = new SforceObjectPair();
                    }

                    if (bSource) {
                        pair.setSourceId(sObject.getId());
                        pair.setSourceSObject(sObject);
                        // Also duplicate the pair with the source id
                        sObjectRecordsMap.put(sObject.getId(), pair);
                    } else {
                        pair.setTargetId(sObject.getId());
                        pair.setTargetSObject(sObject);
                        sObjectRecordsMap.put(sObject.getId(), pair);
                    }
                    sObjectRecordsMap.put(compositeKey, pair);

                }
            }
        } catch (Exception ce) {
            ce.printStackTrace();
        }

        log.debug("SF query for type [" + sforceObject.getsObjectName() + "] took ["
                + (System.currentTimeMillis() - t1) + "]ms");

        return compositeKey;
    }

    /**
     * Counts number of records
     *
     * @param PartnerConnection connection: connection to Salesforce org
     * @param String sObjectAPIName: Object to be queried (API name)
     *
     */
    public Integer queryValidate(PartnerConnection connection, SforceObject sforceObject) {

        int records = 0;
        try {

            String query = "SELECT COUNT()  FROM " + sforceObject.getsObjectName();
            QueryResult qr = connection.query(query);
            records = qr.getSize();

        } catch (ConnectionException ce) {
            log.error("ConnectionException...", ce);
            ce.printStackTrace();
        }

        return records;
    }


    private SforceObject getSforceObject(String sObjectName, List<SforceObject> sForceObjectList) {
        for (SforceObject sforceObj : sForceObjectList) {
            // Don't relate to lookup, since this is for parent-child, so relate
            // to parent(which is non-lookup)
            if (!sforceObj.isLookup() && sObjectName.equalsIgnoreCase(sforceObj.getsObjectName())) {
                return sforceObj;
            }
        }
        return null;
    }

    private void reportError(String sObjectName, List<ErrorResult> errorList) {
        if (errorList == null || errorList.size() == 0) {
            return;
        }

        log.error("\nError Report for Object: " + sObjectName);
        for (ErrorResult error : errorList) {
            log.error(error);
        }
        log.error("\n\n");
    }

    /**
     * Forced login to avoid timeout
     */
    public void fakeReLogin() {
        SfdcConnection sfdcConnection = SfdcConnectionFactory.getConnection();

        long elpsed = System.currentTimeMillis() - sfdcConnection.getLastSalesforceLoginTime();
        int min = (int) (((elpsed / 1000) / 60) % 60);
        if (min >= 14) {
            fakeTimeoutQuery(sfdcConnection.getSourceConnection());
            fakeTimeoutQuery(sfdcConnection.getTargetConnection());
            for (PartnerConnection conn : sfdcConnection.getConnectionTargets()) {
                fakeTimeoutQuery(conn);
            }
            sfdcConnection.setLastSalesforceLoginTime(System.currentTimeMillis());
        }
    }

    private void fakeTimeoutQuery(PartnerConnection connection) {
        if (connection == null)
            return;
        java.text.SimpleDateFormat time_formatter = new java.text.SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");
        String current_time_str = time_formatter.format(System.currentTimeMillis());

        log.info(">>> fakeTimeoutQuery at " + current_time_str);

        try {
            String query = "SELECT COUNT() FROM Pricebook2";
            QueryResult qr = connection.query(query);
        } catch (ConnectionException ce) {
            log.error("ConnectionException..." + ce.getMessage());
            ce.printStackTrace();
        }
    }

    private static class ErrorResult {
        public int originalIndex;
        public String sObjectName;
        public String sourceId;
        public String errorMsg;

        public ErrorResult(int originalIndex, String sObjectName, String sourceId, String errorMsg) {
            this.originalIndex = originalIndex;
            this.sObjectName = sObjectName;
            this.sourceId = sourceId;
            this.errorMsg = errorMsg;
        }

        public String toString() {
            return "ObjectName: " + sObjectName + " ,originalIndex: " + originalIndex + " ,sourceId: " + sourceId
                    + " ,Error Message: " + errorMsg;
        }
    }

    private static class RetryRecords {
        public String sObjectName;
        public String sourceId;
        public String statusCode;
        public int originlIndex;

        public RetryRecords(int originlIndex, String sObjectName, String sourceId, String statusCode) {
            this.originlIndex = originlIndex;
            this.sObjectName = sObjectName;
            this.sourceId = sourceId;
            this.statusCode = statusCode;
        }

        public String toString() {
            return "originlIndex: " + originlIndex + " ,ObjectName: " + sObjectName + " ,sourceId: " + sourceId
                    + " ,Status Code: " + statusCode;
        }
    }

    public SaveResult[] divideWork(SforceObject sforceObject, final SObject[] sObjectInsertRecords,
            boolean singleThreaded) {

        String sObjectAPIName = sforceObject.getsObjectName();
        int threadCount =
                new Integer(PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD,
                        "target.threads.count")).intValue();

        if (!singleThreaded) {
            singleThreaded = isSingleThreaded(sObjectAPIName);
        }

        SfdcConnection sfdcConnection = SfdcConnectionFactory.getConnection();
        List<PartnerConnection> connections = sfdcConnection.getConnectionTargets();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<CreateWorkerResponse>> list = new ArrayList<Future<CreateWorkerResponse>>();

        int workerSize = 0;

        if (sObjectInsertRecords.length >= threadCount && !singleThreaded) {
            workerSize = sObjectInsertRecords.length / threadCount;

            // Create Callables (It can be 1 if number of records are less than threadCount )
            int startIndex = 0;
            int endIndex = 0;
            int sequence = 1;
            for (startIndex = 0; startIndex < sObjectInsertRecords.length;) {

                // If last sequence, then add everything remaining
                int leftOuts = 0;
                if (sequence == threadCount) {
                    leftOuts = sObjectInsertRecords.length % threadCount;
                }

                endIndex = startIndex + workerSize + leftOuts;

                SObject[] subList = new SObject[workerSize + leftOuts];
                for (int j = 0; (startIndex + j) < endIndex; j++) {
                    subList[j] = sObjectInsertRecords[startIndex + j];
                }

                Callable<CreateWorkerResponse> worker =
                        new CreateWorkerThread(connections.get(sequence - 1), sequence, startIndex, endIndex, subList,
                                sforceObject);
                Future<CreateWorkerResponse> submit = executor.submit(worker);
                list.add(submit);

                startIndex = startIndex + workerSize;

                // get out if we are at last sequence since we are adding left overs in the last one
                if (sequence == threadCount) {
                    break;
                }

                sequence++;

            }
        } else {
            Callable<CreateWorkerResponse> worker =
                    new CreateWorkerThread(sfdcConnection.getTargetConnection(), 1, 0, sObjectInsertRecords.length,
                            sObjectInsertRecords, sforceObject);
            Future<CreateWorkerResponse> submit = executor.submit(worker);
            list.add(submit);
        }

        int results = 0;
        for (Future<CreateWorkerResponse> future : list) {
            try {
                CreateWorkerResponse resp = future.get();
                results = results + resp.recordsProcessed;

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        SaveResult[] saveResults = new SaveResult[results];
        int resultCounter = 0;

        for (Future<CreateWorkerResponse> future : list) {
            try {
                CreateWorkerResponse resp = future.get();

                List<SaveResult[]> saveResultslist = resp.saveResultsList;

                for (int i = 0; i < saveResultslist.size(); i++) {
                    for (int j = 0; j < saveResultslist.get(i).length; j++) {
                        saveResults[resultCounter] = saveResultslist.get(i)[j];
                        resultCounter++;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
        return saveResults;
    }

    private static class CreateWorkerResponse {
        List<SaveResult[]> saveResultsList;
        int sequence;
        int startIndex;
        int endIndex;

        int recordsProcessed;
    }

    class CreateWorkerThread implements Callable<CreateWorkerResponse> {
        PartnerConnection connection;

        List<SaveResult[]> saveResultsList;
        int sequence;
        int startIndex;
        int endIndex;
        SforceObject sforceObject;

        SObject[] subList;

        public CreateWorkerThread(PartnerConnection connection, int sequence, int startIndex, int endIndex,
                SObject[] subList1, SforceObject sforceObject) {
            this.connection = connection;
            this.sequence = sequence;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.subList = subList1;
            this.sforceObject = sforceObject;

            this.saveResultsList = new ArrayList<SaveResult[]>();
        }

        @Override
        public CreateWorkerResponse call() throws Exception {
            CreateWorkerResponse resp = new CreateWorkerResponse();

            resp.sequence = sequence;
            resp.startIndex = startIndex;
            resp.endIndex = endIndex;

            resp.saveResultsList = saveResultsList;

            try {
                SObjectBuffer sObjectBuffer = new SObjectBuffer(subList, sforceObject.getBatchSize());

                log.info(sforceObject.getsObjectName() + " : Thread[" + sequence + "] Records: " + subList.length);

                while (sObjectBuffer.hasAvailable()) {

                    SObject[] buffernn = sObjectBuffer.getBuffer();
                    SObject[] buffer = Arrays.copyOf(buffernn, buffernn.length, SObject[].class);

                    SaveResult[] saveResults = connection.create(buffer);

                    saveResultsList.add(saveResults);
                    resp.recordsProcessed = resp.recordsProcessed + saveResults.length;

                    log.info("Thread[" + sequence + "] Batch Records: " + resp.recordsProcessed);
                }

            } catch (Exception e) {
                e.printStackTrace();
                log.error("Exception...", e);

                Thread.currentThread().interrupt();
                throw e;
            } finally {

            }
            return resp;
        }

        public String toString() {
            return "CreateWorkerResponse[Thread[" + sequence + "] ,startIndex: " + startIndex + " ,endIndex:"
                    + endIndex + "]";
        }
    }

    public UpsertResult[] divideWorkUpsert(SforceObject sforceObject, final SObject[] sObjectInsertRecords,
            String sExternalIdField, boolean singleThreaded) {

        String sObjectAPIName = sforceObject.getsObjectName();
        int threadCount =
                new Integer(PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD,
                        "target.threads.count")).intValue();

        if (!singleThreaded) {
            singleThreaded = isSingleThreaded(sObjectAPIName);
        }

        SfdcConnection sfdcConnection = SfdcConnectionFactory.getConnection();
        List<PartnerConnection> connections = sfdcConnection.getConnectionTargets();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<UpsertWorkerResponse>> list = new ArrayList<Future<UpsertWorkerResponse>>();

        int workerSize = 0;

        if (sObjectInsertRecords.length >= threadCount && !singleThreaded) {
            workerSize = sObjectInsertRecords.length / threadCount;

            // Create Callables (It can be 1 if number of records are less threadCount )
            int startIndex = 0;
            int endIndex = 0;
            int sequence = 1;
            for (startIndex = 0; startIndex < sObjectInsertRecords.length;) {

                // If last sequence, then add everything remaining
                int leftOuts = 0;
                if (sequence == threadCount) {
                    leftOuts = sObjectInsertRecords.length % threadCount;
                }

                endIndex = startIndex + workerSize + leftOuts;

                System.out.println("Thread[" + sequence + "], startIndex: " + startIndex + ", endIndex: " + endIndex);

                SObject[] subList = new SObject[workerSize + leftOuts];
                for (int j = 0; (startIndex + j) < endIndex; j++) {
                    subList[j] = sObjectInsertRecords[startIndex + j];
                }

                Callable<UpsertWorkerResponse> worker =
                        new UpsertWorkerThread(connections.get(sequence - 1), sequence, startIndex, endIndex, subList,
                                sforceObject, sExternalIdField);
                Future<UpsertWorkerResponse> submit = executor.submit(worker);
                list.add(submit);

                startIndex = startIndex + workerSize;

                // get out if we are at last sequence since we are adding left overs in the last one
                if (sequence == threadCount) {
                    break;
                }
                sequence++;
            }
        } else {
            Callable<UpsertWorkerResponse> worker =
                    new UpsertWorkerThread(sfdcConnection.getTargetConnection(), 1, 0, sObjectInsertRecords.length,
                            sObjectInsertRecords, sforceObject, sExternalIdField);
            Future<UpsertWorkerResponse> submit = executor.submit(worker);
            list.add(submit);
        }

        int results = 0;
        for (Future<UpsertWorkerResponse> future : list) {
            try {
                UpsertWorkerResponse resp = future.get();

                results = results + resp.recordsProcessed;

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        UpsertResult[] saveResults = new UpsertResult[results];
        int resultCounter = 0;

        for (Future<UpsertWorkerResponse> future : list) {
            try {
                UpsertWorkerResponse resp = future.get();

                List<UpsertResult[]> saveResultslist = resp.saveResultsList;

                for (int i = 0; i < saveResultslist.size(); i++) {
                    for (int j = 0; j < saveResultslist.get(i).length; j++) {
                        saveResults[resultCounter] = saveResultslist.get(i)[j];
                        resultCounter++;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
        return saveResults;
    }

    private static class UpsertWorkerResponse {
        List<UpsertResult[]> saveResultsList;
        int sequence;
        int startIndex;
        int endIndex;

        int recordsProcessed;
    }

    class UpsertWorkerThread implements Callable<UpsertWorkerResponse> {
        PartnerConnection connection;

        List<UpsertResult[]> saveResultsList;
        int sequence;
        int startIndex;
        int endIndex;
        String sObjectAPIName;
        SforceObject sforceObject;
        String sExternalIdField;

        SObject[] subList;

        public UpsertWorkerThread(PartnerConnection connection, int sequence, int startIndex, int endIndex,
                SObject[] subList1, SforceObject sforceObject, String sExternalIdField) {
            this.connection = connection;
            this.sequence = sequence;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.subList = subList1;
            this.sforceObject = sforceObject;
            this.sExternalIdField = sExternalIdField;

            this.saveResultsList = new ArrayList<UpsertResult[]>();
        }

        @Override
        public UpsertWorkerResponse call() throws Exception {
            UpsertWorkerResponse resp = new UpsertWorkerResponse();

            resp.sequence = sequence;
            resp.startIndex = startIndex;
            resp.endIndex = endIndex;

            resp.saveResultsList = saveResultsList;

            try {

                SObjectBuffer sObjectBuffer = new SObjectBuffer(subList, sforceObject.getBatchSize());

                log.info(sforceObject.getsObjectName() + " : Thread[" + sequence + "] Create/Upsert Records: "
                        + subList.length);

                while (sObjectBuffer.hasAvailable()) {

                    SObject[] buffernn = sObjectBuffer.getBuffer();
                    SObject[] buffer = Arrays.copyOf(buffernn, buffernn.length, SObject[].class);

                    UpsertResult[] saveResults = connection.upsert(sExternalIdField, buffer);

                    saveResultsList.add(saveResults);
                    resp.recordsProcessed = resp.recordsProcessed + saveResults.length;

                    log.info("Thread[" + sequence + "] Batch Records: " + resp.recordsProcessed);
                }

            } catch (Exception e) {
                e.printStackTrace();
                log.error("Exception...", e);

                Thread.currentThread().interrupt();
                throw e;
            } finally {

            }
            return resp;
        }

        public String toString() {
            return "UpsertWorkerResponse[Thread[" + sequence + "] ,startIndex: " + startIndex + " ,endIndex:"
                    + endIndex + "]";
        }
    }

    public SaveResult[] divideWorkUpdate(SforceObject sforceObject, final SObject[] sObjectInsertRecords,
            boolean singleThreaded) {

        int threadCount =
                new Integer(PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD,
                        "target.threads.count")).intValue();

        if (!singleThreaded) {
            singleThreaded = isSingleThreaded(sforceObject.getsObjectName());
        }

        SfdcConnection sfdcConnection = SfdcConnectionFactory.getConnection();
        List<PartnerConnection> connections = sfdcConnection.getConnectionTargets();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<UpdateWorkerResponse>> list = new ArrayList<Future<UpdateWorkerResponse>>();

        int workerSize = 0;

        if (sObjectInsertRecords.length >= threadCount && !singleThreaded) {
            workerSize = sObjectInsertRecords.length / threadCount;

            // Create Callables (It can be 1 if number of records are less threadCount )
            int startIndex = 0;
            int endIndex = 0;
            int sequence = 1;
            for (startIndex = 0; startIndex < sObjectInsertRecords.length;) {

                // If last sequence, then add everything remaining
                int leftOuts = 0;
                if (sequence == threadCount) {
                    leftOuts = sObjectInsertRecords.length % threadCount;
                }

                endIndex = startIndex + workerSize + leftOuts;

                System.out.println("Thread[" + sequence + "], startIndex: " + startIndex + ", endIndex: " + endIndex);

                SObject[] subList = new SObject[workerSize + leftOuts];
                for (int j = 0; (startIndex + j) < endIndex; j++) {
                    subList[j] = sObjectInsertRecords[startIndex + j];
                }

                Callable<UpdateWorkerResponse> worker =
                        new UpdateWorkerThread(connections.get(sequence - 1), sequence, startIndex, endIndex, subList,
                                sforceObject);
                Future<UpdateWorkerResponse> submit = executor.submit(worker);
                list.add(submit);

                startIndex = startIndex + workerSize;

                // get out if we are at last sequence since we are adding left overs in the last one
                if (sequence == threadCount) {
                    break;
                }

                sequence++;

            }
        } else {
            Callable<UpdateWorkerResponse> worker =
                    new UpdateWorkerThread(sfdcConnection.getTargetConnection(), 1, 0, sObjectInsertRecords.length,
                            sObjectInsertRecords, sforceObject);
            Future<UpdateWorkerResponse> submit = executor.submit(worker);
            list.add(submit);
        }

        int results = 0;
        for (Future<UpdateWorkerResponse> future : list) {
            try {
                UpdateWorkerResponse resp = future.get();

                results = results + resp.recordsProcessed;

                System.out.println("WorkerResponse[sequence: " + resp.sequence + " ,startIndex: " + resp.startIndex
                        + " ,endIndex:" + resp.endIndex + " ,recordsProcessed:" + resp.recordsProcessed + "]");

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        SaveResult[] saveResults = new SaveResult[results];
        int resultCounter = 0;

        for (Future<UpdateWorkerResponse> future : list) {
            try {
                UpdateWorkerResponse resp = future.get();

                List<SaveResult[]> saveResultslist = resp.saveResultsList;

                for (int i = 0; i < saveResultslist.size(); i++) {
                    for (int j = 0; j < saveResultslist.get(i).length; j++) {
                        saveResults[resultCounter] = saveResultslist.get(i)[j];
                        resultCounter++;
                    }
                }

                System.out.println("UpdateWorkerResponse[sequence: " + resp.sequence + " ,startIndex: "
                        + resp.startIndex + " ,endIndex:" + resp.endIndex + " ,recordsProcessed:"
                        + resp.recordsProcessed + "]");

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
        return saveResults;
    }

    private static class UpdateWorkerResponse {
        List<SaveResult[]> saveResultsList;
        int sequence;
        int startIndex;
        int endIndex;

        int recordsProcessed;
    }

    class UpdateWorkerThread implements Callable<UpdateWorkerResponse> {
        PartnerConnection connection;

        List<SaveResult[]> saveResultsList;
        int sequence;
        int startIndex;
        int endIndex;
        SforceObject sforceObject;

        SObject[] subList;

        public UpdateWorkerThread(PartnerConnection connection, int sequence, int startIndex, int endIndex,
                SObject[] subList1, SforceObject sforceObject) {
            this.connection = connection;
            this.sequence = sequence;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.subList = subList1;
            this.sforceObject = sforceObject;

            this.saveResultsList = new ArrayList<SaveResult[]>();
        }

        @Override
        public UpdateWorkerResponse call() throws Exception {
            UpdateWorkerResponse resp = new UpdateWorkerResponse();

            resp.sequence = sequence;
            resp.startIndex = startIndex;
            resp.endIndex = endIndex;

            resp.saveResultsList = saveResultsList;

            try {

                SObjectBuffer sObjectBuffer = new SObjectBuffer(subList, sforceObject.getBatchSize());

                log.info(sforceObject.getsObjectName() + " : Thread[" + sequence + "] Create/update Records: "
                        + subList.length);

                while (sObjectBuffer.hasAvailable()) {

                    SObject[] buffernn = sObjectBuffer.getBuffer();
                    SObject[] buffer = Arrays.copyOf(buffernn, buffernn.length, SObject[].class);

                    SaveResult[] saveResults = connection.update(buffer);

                    saveResultsList.add(saveResults);
                    resp.recordsProcessed = resp.recordsProcessed + saveResults.length;

                    log.info("Thread[" + sequence + "] Batch Records: " + resp.recordsProcessed);
                }

            } catch (Exception e) {
                e.printStackTrace();
                log.error("Exception...", e);

                Thread.currentThread().interrupt();
                throw e;
            } finally {

            }
            return resp;
        }

        public String toString() {
            return "UpdateWorkerResponse[Thread[" + sequence + "] ,startIndex: " + startIndex + " ,endIndex:"
                    + endIndex + "]";
        }
    }


    public UpsertResult[] divideWorkDelete(String sObjectAPIName, final String[] deleteIds, boolean singleThreaded) {

        int threadCount =
                new Integer(PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD,
                        "target.threads.count")).intValue();

        if (!singleThreaded) {
            singleThreaded = isSingleThreaded(sObjectAPIName);
        }

        SfdcConnection sfdcConnection = SfdcConnectionFactory.getConnection();
        List<PartnerConnection> connections = sfdcConnection.getConnectionTargets();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<DeleteWorkerResponse>> list = new ArrayList<Future<DeleteWorkerResponse>>();

        int workerSize = 0;

        if (deleteIds.length >= threadCount && !singleThreaded) {
            workerSize = deleteIds.length / threadCount;

            // Create Callables (It can be 1 if number of records are less threadCount )
            int startIndex = 0;
            int endIndex = 0;
            int sequence = 1;
            for (startIndex = 0; startIndex < deleteIds.length;) {

                // If last sequence, then add everything remaining
                int leftOuts = 0;
                if (sequence == threadCount) {
                    leftOuts = deleteIds.length % threadCount;
                }

                endIndex = startIndex + workerSize + leftOuts;

                System.out.println("Thread[" + sequence + "], startIndex: " + startIndex + ", endIndex: " + endIndex);

                String[] subList = new String[workerSize + leftOuts];
                for (int j = 0; (startIndex + j) < endIndex; j++) {
                    subList[j] = deleteIds[startIndex + j];
                }

                Callable<DeleteWorkerResponse> worker =
                        new DeleteWorkerThread(connections.get(sequence - 1), sequence, startIndex, endIndex, subList,
                                sObjectAPIName);
                Future<DeleteWorkerResponse> submit = executor.submit(worker);
                list.add(submit);

                startIndex = startIndex + workerSize;

                // get out if we are at last sequence since we are adding left overs in the last one
                if (sequence == threadCount) {
                    break;
                }

                sequence++;

            }
        } else {
            Callable<DeleteWorkerResponse> worker =
                    new DeleteWorkerThread(sfdcConnection.getTargetConnection(), 1, 0, deleteIds.length, deleteIds,
                            sObjectAPIName);
            Future<DeleteWorkerResponse> submit = executor.submit(worker);
            list.add(submit);
        }

        int results = 0;
        for (Future<DeleteWorkerResponse> future : list) {
            try {
                DeleteWorkerResponse resp = future.get();

                results = results + resp.recordsProcessed;

                System.out.println("DeleteWorkerResponse[sequence: " + resp.sequence + " ,startIndex: "
                        + resp.startIndex + " ,endIndex:" + resp.endIndex + " ,recordsProcessed:"
                        + resp.recordsProcessed + "]");

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        UpsertResult[] saveResults = new UpsertResult[results];
        int resultCounter = 0;

        for (Future<DeleteWorkerResponse> future : list) {
            try {
                DeleteWorkerResponse resp = future.get();

                List<UpsertResult[]> saveResultslist = resp.saveResultsList;

                for (int i = 0; i < saveResultslist.size(); i++) {
                    for (int j = 0; j < saveResultslist.get(i).length; j++) {
                        saveResults[resultCounter] = saveResultslist.get(i)[j];
                        resultCounter++;
                    }
                }

                System.out.println("DeleteWorkerResponse[sequence: " + resp.sequence + " ,startIndex: "
                        + resp.startIndex + " ,endIndex:" + resp.endIndex + " ,recordsProcessed:"
                        + resp.recordsProcessed + "]");

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
        return saveResults;
    }

    private static class DeleteWorkerResponse {
        List<UpsertResult[]> saveResultsList;
        int sequence;
        int startIndex;
        int endIndex;

        int recordsProcessed;
    }

    class DeleteWorkerThread implements Callable<DeleteWorkerResponse> {
        PartnerConnection connection;

        List<UpsertResult[]> saveResultsList;
        int sequence;
        int startIndex;
        int endIndex;
        String sObjectAPIName;

        String[] subList;

        public DeleteWorkerThread(PartnerConnection connection, int sequence, int startIndex, int endIndex,
                String[] subList1, String sObjectAPIName) {
            this.connection = connection;
            this.sequence = sequence;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.subList = subList1;
            this.sObjectAPIName = sObjectAPIName;

            this.saveResultsList = new ArrayList<UpsertResult[]>();
        }

        @Override
        public DeleteWorkerResponse call() throws Exception {
            DeleteWorkerResponse resp = new DeleteWorkerResponse();

            resp.sequence = sequence;
            resp.startIndex = startIndex;
            resp.endIndex = endIndex;

            resp.saveResultsList = saveResultsList;

            try {

                log.info(sObjectAPIName + " : Thread[" + sequence + "] Delete Records: " + subList.length);

                SObjectDeleteBuffer sObjectBuffer = new SObjectDeleteBuffer(subList);
                while (sObjectBuffer.hasAvailable()) {

                    String[] deleteBuffer = sObjectBuffer.getBuffer();

                    DeleteResult[] deleteResults = connection.delete(deleteBuffer);
                    for (int i = 0; i < deleteResults.length; i++) {
                        DeleteResult deleteResult = deleteResults[i];
                        if (deleteResult.isSuccess()) {
                        } else {
                            // Handle the errors.
                            com.sforce.soap.partner.Error[] errors = deleteResult.getErrors();
                            if (errors.length > 0) {
                                log.error("Error: could not delete " + "Record ID " + deleteResult.getId() + ".");
                                log.error("   The error reported was: (" + errors[0].getStatusCode() + ") "
                                        + errors[0].getMessage() + "\n");
                            }
                        }
                    }
                }


            } catch (Exception e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                throw e;
            } finally {

            }
            return resp;
        }

        public String toString() {
            return "DeleteWorkerResponse[Thread[" + sequence + "] ,startIndex: " + startIndex + " ,endIndex:"
                    + endIndex + "]";
        }
    }

    private boolean isSingleThreaded(String sObjectAPIName) {
        String excludedObjectProp =
                PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD,
                        "exclude.multithread.obejcts");
        if (excludedObjectProp == null || excludedObjectProp.equals("")) {
            return true;
        }
        String[] excludedObjects = excludedObjectProp.split(",");

        if (excludedObjects != null && excludedObjects.length > 0) {
            for (String excludedObject : excludedObjects) {
                if (excludedObject.equalsIgnoreCase(sObjectAPIName)) {
                    return true;
                }
            }
        }
        return false;
    }

}
