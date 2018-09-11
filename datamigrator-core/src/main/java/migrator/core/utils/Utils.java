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
package migrator.core.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.sobject.SObject;
import migrator.core.service.SforceObjectPair;

/**
 * Utils
 *
 * @author anoop.singh
 */
public class Utils {

    public static String PREFIX_COMPOSITE_KEY = "KEY_";

    private static String[] blackListedEndPoints = new String[] {"https://login.salesforce.com".toLowerCase()};

    // We never want to create/update/delete anything in source
    public static Set<String> BLACK_LISTED_TARGET_ENDPOINTS = new HashSet<String>(Arrays.asList(blackListedEndPoints));

    private static String[] exclude = new String[] {"Id", "OwnerId", "IsDeleted", "CurrencyIsoCode", "CreatedDate",
            "CreatedById", "LastModifiedDate", "LastModifiedById", "SystemModstamp", "MayEdit", "IsLocked",
            "LastViewedDate", "LastReferencedDate", "ConnectionReceivedId", "ConnectionSentId"};
    public static Set<String> EXCLUDE_FIELDS = new HashSet<String>(Arrays.asList(exclude));

    private static String[] excludeQuery = new String[] {"IsDeleted", "CreatedDate", "CreatedById", "LastModifiedDate",
            "LastModifiedById", "SystemModstamp", "MayEdit", "IsLocked", "LastViewedDate", "LastReferencedDate",
            "ConnectionReceivedId", "ConnectionSentId"};
    public static Set<String> EXCLUDE_QUERY_FIELDS = new HashSet<String>(Arrays.asList(excludeQuery));

    private static String[] excludeChildRelationships = new String[] {"ActivityHistories", "Tags",
            "AttachedContentDocuments", "AttachedContentNotes", "Attachments", "RecordAssociatedGroups",
            "CombinedAttachments", "ContentDocumentLinks", "DuplicateRecordItems", "FeedSubscriptionsForEntity",
            "Events", "GoogleDocs", "ParentEntities", "Notes", "NotesAndAttachments", "OpenActivities",
            "ProcessInstances", "ProcessSteps", "Tasks", "TopicAssignments", "Histories"};
    public static Set<String> EXCLUDE_CHILD_RELATIONSHIPS = new HashSet<String>(
            Arrays.asList(excludeChildRelationships));

    public static SforceObjectPair getSObjectParent(String parentId, List<SforceObjectPair> sObjectRecords) {

        for (SforceObjectPair source : sObjectRecords) {
            if (source.getSourceId().equalsIgnoreCase(parentId)) {
                return source;
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static String getKey(String sObjectAPIName, SObject sObject, List<String> compositeKey) {

        StringBuffer sbKey = new StringBuffer("");

        try {
            for (String key : compositeKey) {
                Object fieldValue = sObject.getField(key);
                sbKey.append(fieldValue);
            }
        } catch (Exception e) {
            System.out.println("Utils.getKey failed: sObjectAPIName" + sObjectAPIName);
            e.printStackTrace();
        }
        // System.out.println("Composite Key:****" + sbKey.toString());
        return PREFIX_COMPOSITE_KEY + sbKey.toString();
    }

    public static void updateBlackListedUrls(String url) {
        BLACK_LISTED_TARGET_ENDPOINTS.add(url.toLowerCase());
    }

    public static boolean isBlackListedTarget(PartnerConnection connection) {
        return isBlackListedTarget(connection.getConfig().getServiceEndpoint());
    }

    public static boolean isBlackListedTarget(String url) {
        if (url != null && url.toLowerCase().contains(".com")) {
            url = url.toLowerCase().substring(0, url.toLowerCase().indexOf(".com"));
            if (BLACK_LISTED_TARGET_ENDPOINTS.toString().toLowerCase().contains(url.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

}
