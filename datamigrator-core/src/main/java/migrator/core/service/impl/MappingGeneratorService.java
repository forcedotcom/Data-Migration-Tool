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
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import migrator.core.connect.SfdcConnection;
import migrator.core.service.MetadataObjectHolder;

/**
 * MappingGeneratorService - Generates the Java mapping from source to target
 *
 * @author anoop.singh
 */
public class MappingGeneratorService {

    public enum Type {
        METHOD, CLASS
    }

    public enum ImplType {
        LOOKUP, MASTERDETAIL, HIERARCHICAL
    }

    SfdcConnection sfdcConnection;
    List<MetadataObjectHolder.MetadataRefObject> allRefObjects = null;

    public MappingGeneratorService() {
        sfdcConnection = new SfdcConnection();
        if (!sfdcConnection.loginTarget()) {
            System.out.println("Login failed!");
            return;
        }
    }

    public String generate(String args) {
        String[] objectsArray = args.split(",");
        List<String> migrableObjects = Arrays.asList(objectsArray);

        for (String migrableObject : migrableObjects) {
            MetadataObjectHolder.getInstance().initTarget(SfdcConnection.ORG_TYPE.TARGET, sfdcConnection,
                    migrableObject, migrableObjects);
        }
        // disconnect from source and target
        sfdcConnection.disconnect();

        Map<String, MetadataObjectHolder.MetadataRefObject> refObjectMap =
                MetadataObjectHolder.getInstance().getTargetDescribeRefObjectMap();

        allRefObjects = new ArrayList<MetadataObjectHolder.MetadataRefObject>(refObjectMap.values());
        Collections.sort(allRefObjects, new Comparator<MetadataObjectHolder.MetadataRefObject>() {
            @Override
            public int compare(MetadataObjectHolder.MetadataRefObject p1, MetadataObjectHolder.MetadataRefObject p2) {
                return -(p1.getChildRelationships().size() - p2.getChildRelationships().size()); // Ascending
            }
        });

        attachParents(allRefObjects);
        attachLookups(allRefObjects);

        // Objects that don't have any parent, top-level
        List<MetadataObjectHolder.MetadataRefObject> topRefObjects = getTopLevelObjects(allRefObjects);

        String json = generateJSON(topRefObjects);
        System.out.println("####### json #######: \n\n" + json);

        return json;
    }

    private void attachParents(List<MetadataObjectHolder.MetadataRefObject> refObjects) {
        for (MetadataObjectHolder.MetadataRefObject refObject : refObjects) {

            if (refObject.getChildRelationships().size() > 0) {
                // This is parent object, get child and related parent-child
                for (MetadataObjectHolder.MetadataRefObject.ChildRelationship cr : refObject.getChildRelationships()) {
                    MetadataObjectHolder.MetadataRefObject childRefObject =
                            getRefObject(cr.getChildSObject(), refObjects);
                    List<MetadataObjectHolder.MetadataRefObject> masters = childRefObject.getMasters();
                    masters.add(refObject);
                }

            }
        }
    }

    private void attachLookups(List<MetadataObjectHolder.MetadataRefObject> refObjects) {
        for (MetadataObjectHolder.MetadataRefObject refObject : refObjects) {

            if (refObject.getReferecensToMap().size() > 0) {
                // There are lookups, check if they are master detail, if so, just ignore them
                for (Map.Entry<String, String> entry : refObject.getReferecensToMap().entrySet()) {

                    String key = entry.getKey();
                    String value = entry.getValue();

                    if (key.equalsIgnoreCase("OwnerId") || key.equalsIgnoreCase("OwnerId")
                            || key.equalsIgnoreCase("RecordTypeId") || key.equalsIgnoreCase("RecordType")
                            || key.equalsIgnoreCase("User") || key.equalsIgnoreCase("Owner")) {
                        continue;
                    }

                    // check if value is an actual object (parent or child), if so, ignore it,
                    // otherwise, its a lookup (hack)
                    if (getRefObject(value, refObjects) == null) {
                        Map<String, String> lookupMap = refObject.getLookupsMap();
                        lookupMap.put(key, value);
                    } else {
                        continue;
                    }
                }
            }
        }
    }

    private MetadataObjectHolder.MetadataRefObject getRefObject(String childSObject,
            List<MetadataObjectHolder.MetadataRefObject> refObjects) {
        for (MetadataObjectHolder.MetadataRefObject refObect : refObjects) {
            if (refObect.getObjectName().equalsIgnoreCase(childSObject)) {
                return refObect;
            }
        }
        return null;
    }

    private List<MetadataObjectHolder.MetadataRefObject> getTopLevelObjects(
            List<MetadataObjectHolder.MetadataRefObject> refObjects) {
        List<MetadataObjectHolder.MetadataRefObject> topRefObjects =
                new ArrayList<MetadataObjectHolder.MetadataRefObject>();
        for (MetadataObjectHolder.MetadataRefObject refObject : refObjects) {
            if (refObject.getMasters() == null || refObject.getMasters().size() == 0) {
                topRefObjects.add(refObject);
            }
        }
        return topRefObjects;
    }

    private String generateJSON(List<MetadataObjectHolder.MetadataRefObject> topRefObjects) {
        String json = "[\n";

        for (MetadataObjectHolder.MetadataRefObject topRefObject : topRefObjects) {
            json = json + "  {\n";
            json = json + "    parent: " + topRefObject.getObjectName() + " ,\n";

            if (topRefObject.getChildRelationships() != null && topRefObject.getChildRelationships().size() > 0) {
                json = json + "    childs: [ \n";
                for (MetadataObjectHolder.MetadataRefObject.ChildRelationship cr : topRefObject.getChildRelationships()) {
                    json = generateJSONChild(json, cr, 1);
                }
                json = json + "    ]";
            }
            if (topRefObject.getLookupsMap() != null && topRefObject.getLookupsMap().size() > 0) {

                json = json + ",\n";
                json = json + "    lookups: [ \n";
                for (String key : topRefObject.getLookupsMap().keySet()) {
                    json = generateJSONLookup(json, key, topRefObject.getLookupsMap().get(key));
                }
                json = json + "    ] \n";
            }
            json = json + "  },\n";
        }
        json = json + "]";
        return json;
    }

    private String generateJSONChild(String json, MetadataObjectHolder.MetadataRefObject.ChildRelationship cr,
            int sequence) {

        if (sequence >= 8) {
            return json;
        }
        json = json + "     {\n";
        json = json + "      parentMappedField:  " + cr.getField() + ",\n";
        json = json + "      childObject:  " + cr.getChildSObject() + ",\n";
        json = json + "      sequence:  " + sequence + ",\n";

        MetadataObjectHolder.MetadataRefObject childRefObject = getRefObject(cr.getChildSObject());
        // Does this child have another child?
        if (childRefObject.getChildRelationships() != null && childRefObject.getChildRelationships().size() > 0) {
            json = json + "      childs: [ \n";
            for (MetadataObjectHolder.MetadataRefObject.ChildRelationship crChild : childRefObject
                    .getChildRelationships()) {
                json = generateJSONChild(json, crChild, sequence + 1);
            }
            json = json + "      ] \n";
        }

        // Does this child have lookups?
        if (childRefObject.getLookupsMap() != null && childRefObject.getLookupsMap().size() > 0) {

            json = json + ",\n";
            json = json + "      lookups: [ \n";
            for (String key : childRefObject.getLookupsMap().keySet()) {
                json = generateJSONLookup(json, key, childRefObject.getLookupsMap().get(key));
            }
            json = json + "      ] \n";
        }
        json = json + "     },\n";
        return json;
    }

    private String generateJSONLookup(String json, String field, String sobject) {
        json = json + "    {\n";

        json = json + "     lookupMappedField : " + field + ",\n";
        json = json + "     lookupObject : " + sobject + ",\n";
        json = json + "     keys : [External_Id_Field__c], //*** edit this  \n";

        json = json + "    },\n";

        return json;
    }

    private MetadataObjectHolder.MetadataRefObject getRefObject(String sObjectName) {
        for (MetadataObjectHolder.MetadataRefObject refObject : allRefObjects) {
            if (refObject.getObjectName().equalsIgnoreCase(sObjectName)) {
                return refObject;
            }
        }
        return null;
    }

}
