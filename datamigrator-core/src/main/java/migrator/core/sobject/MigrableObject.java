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

import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;
import migrator.core.connect.SfdcConnection;
import migrator.core.connect.SfdcConnectionFactory;
import migrator.core.service.PropertiesReader;
import migrator.core.service.SforceObject;
import migrator.core.service.impl.SfdcApiServiceImpl;

/**
 * MigrableObject : Abstract class to support relationships
 *
 * @author anoop.singh
 */
public abstract class MigrableObject implements Migrable {
    protected SfdcConnection sfdcConnection;

    protected ObjectMappingConfig objectMapping = null;
    protected String operation;
    protected List<SforceObject> sForceObjectList = new ArrayList<SforceObject>();

    static {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hhmmss");
        System.setProperty("current.date", dateFormat.format(new Date()));
    }

    static Logger log = Logger.getLogger(MigrableObject.class.getName());

    public MigrableObject() {}

    public MigrableObject(ObjectMappingConfig objectMapping, String operation) {
        this.objectMapping = objectMapping;

        this.operation = operation;
    }

    @Override
    public void process() {
        if (!connect()) {
            return;
        }
        setup();
        if (handleDelete()) {
            return;
        }
        query();
        insert();
        disconnect();
    }

    /**
     * Setup the migrable objects: 1. Gets the Describe details for each SObject 2. Creates a
     * SforceObject (holder of sobjectName, describe details and records) 3. Add SforceObject into a
     * map
     *
     */
    @Override
    public void setup() {}

    public SforceObject getSforceObject(String sObjectName) {
        for (SforceObject sforceObj : sForceObjectList) {
            if (sObjectName.equalsIgnoreCase(sforceObj.getsObjectName())) {
                return sforceObj;
            }
        }
        return null;
    }

    public boolean handleDelete() {
        if (operation.equals(Migrable.Operation.DELETE.toString())) {
            for (int counter = 0; counter < 2; counter++) {
                for (ListIterator<SforceObject> iterator = sForceObjectList.listIterator(sForceObjectList.size()); iterator
                        .hasPrevious();) {
                    SforceObject sforceObject = iterator.previous();
                    if (!sforceObject.isLookup()) {
                        SfdcApiServiceImpl.getSOQLQueryService().delete(sforceObject.getsObjectName(), sfdcConnection);
                    }
                }
            }
            return true;
        }
        return false;
    }

    public boolean connect() {
        PropertiesReader.getInstance();

        this.sfdcConnection = SfdcConnectionFactory.getConnection();
        if (!sfdcConnection.login()) {
            System.out.println("Login failed!");
            return false;
        }
        return true;
    }

    public void disconnect() {
        // disconnect from source and target
        sfdcConnection.disconnect();
    }

}
