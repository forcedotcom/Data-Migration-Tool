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
package migrator.core.connect;

import org.apache.log4j.Logger;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.GetUserInfoResult;
import com.sforce.ws.ConnectorConfig;
import com.sforce.ws.ConnectionException;
import migrator.core.service.PropertiesReader;
import migrator.core.sobject.MigrableObject;
import migrator.core.utils.Utils;

/**
 * SfdcConnection
 *
 * @author anoop.singh
 */
public class SfdcConnectionFactory {

    private static SfdcConnection conn;

    static Logger log = Logger.getLogger(SfdcConnectionFactory.class.getName());

    public static SfdcConnection getConnection() {
        if (conn == null)
            conn = new SfdcConnection();
        return conn;
    }

    public static SfdcConnection getConnection(String sourceAccessToken, String targetAccessToken) {
        if (conn == null)
            conn = new SfdcConnection(sourceAccessToken, targetAccessToken);
        return conn;
    }

}
