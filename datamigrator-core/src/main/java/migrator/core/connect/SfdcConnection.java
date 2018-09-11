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

import java.util.List;
import java.util.ArrayList;
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
public class SfdcConnection {

    PartnerConnection connectionSource;
    PartnerConnection connectionTarget;

    List<PartnerConnection> connectionTargets;

    String sourceUserName = "";
    String sourcePassword = "";
    String sourceAuthEndPoint = "";
    String targetUserName = "";
    String targetPassword = "";
    String targetAuthEndPoint = "";
    String proxyHost = "";
    String proxyPort = "";

    long lastSalesforceLoginTime;

    static Logger log = Logger.getLogger(SfdcConnection.class.getName());

    public enum ORG_TYPE {
        SOURCE, TARGET
    }

    public SfdcConnection() {
        lastSalesforceLoginTime = System.currentTimeMillis();
        this.connectionTargets = new ArrayList<PartnerConnection>();
        readProperties();
    }

    // Constructor

    public SfdcConnection(String sourceAuthEndPoint, String targetAuthEndPoint) {
        lastSalesforceLoginTime = System.currentTimeMillis();
        this.connectionTargets = new ArrayList<PartnerConnection>();

        this.sourceAuthEndPoint = sourceAuthEndPoint;
        this.targetAuthEndPoint = targetAuthEndPoint;
        readProperties();
    }

    public PartnerConnection getSourceConnection() {
        return connectionSource;
    }

    public PartnerConnection getTargetConnection() {
        return connectionTarget;
    }

    public boolean login() {
        boolean isSuccess = loginTarget();
        if (!isSuccess)
            return false;

        String sourceType =
                PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "source.type");
        if (sourceType == null || sourceType.equals("org")) {
            // Make a login call
            isSuccess = loginSource();
            if (!isSuccess)
                return false;
        }
        return true;
    }

    public boolean loginSource() {
        System.out.println("\nloginSource START...");

        boolean success = false;

        try {
            ConnectorConfig config = new ConnectorConfig();
            config.setUsername(sourceUserName);
            config.setPassword(sourcePassword);

            System.out.println("AuthEndPoint: " + sourceAuthEndPoint);
            config.setAuthEndpoint(sourceAuthEndPoint);
            if (!proxyHost.equals("")) {
                config.setProxy(proxyHost, new Integer(proxyPort));
            }

            connectionSource = new PartnerConnection(config);

            // Add source to blacklisted urls. We never want to
            // create/update/delete anything in source
            Utils.updateBlackListedUrls(config.getServiceEndpoint().toLowerCase());

            printUserInfo(config);

            success = true;
        } catch (ConnectionException ce) {
            ce.printStackTrace();
        }
        System.out.println("loginSource END...\n\n");
        return success;
    }

    public boolean loginTarget() {
        System.out.println("\nloginTarget START ...");

        boolean success = false;

        try {
            ConnectorConfig config = new ConnectorConfig();
            config.setUsername(targetUserName);
            config.setPassword(targetPassword);

            System.out.println("AuthEndPoint: " + targetAuthEndPoint);
            config.setAuthEndpoint(targetAuthEndPoint);
            if (!proxyHost.equals("")) {
                config.setProxy(proxyHost, new Integer(proxyPort));
            }

            connectionTarget = new PartnerConnection(config);

            printUserInfoImport(config, connectionTarget);

            success = true;
        } catch (ConnectionException ce) {
            ce.printStackTrace();
        }

        loginTargetMulti();

        System.out.println("loginTarget END...");
        return success;
    }

    private void loginTargetMulti() {
        if (connectionTargets.size() > 0) {
            return;
        }

        try {
            int threadCount = 1;

            System.out.println("target.threads.count="
                    + PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD,
                            "target.threads.count"));
            if (PropertiesReader.getInstance()
                    .getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "target.threads.count") != null
                    && !PropertiesReader.getInstance()
                            .getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "target.threads.count").equals("")) {
                threadCount =
                        new Integer(PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD,
                                "target.threads.count")).intValue();
            }

            if (threadCount <= 1) {
                connectionTargets.add(connectionTarget);
                return;
            }

            for (int i = 0; i < threadCount; i++) {

                ConnectorConfig config = new ConnectorConfig();
                config.setUsername(PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD,
                        "target.sfdc.login"));
                config.setPassword(PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD,
                        "target.sfdc.password"));

                System.out.println("AuthEndPoint:: " + targetAuthEndPoint);
                config.setAuthEndpoint(targetAuthEndPoint);

                PartnerConnection connTarget = new PartnerConnection(config);
                connectionTargets.add(connTarget);

                printUserInfoImport(config, connTarget);
            }

        } catch (ConnectionException ce) {
            ce.printStackTrace();
        }

    }

    private void readProperties() {
        // get the property value and print it out
        sourceUserName =
                PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "source.sfdc.login");
        sourcePassword =
                PropertiesReader.getInstance()
                        .getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "source.sfdc.password");
        sourceAuthEndPoint =
                PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "source.env.endpoint");
        targetUserName =
                PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "target.sfdc.login");
        targetPassword =
                PropertiesReader.getInstance()
                        .getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "target.sfdc.password");
        targetAuthEndPoint =
                PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "target.env.endpoint");
        proxyHost = PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "env.proxy.host");
        proxyPort = PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "env.proxy.port");
    }

    private void printUserInfo(ConnectorConfig config) {
        try {
            GetUserInfoResult userInfo = connectionSource.getUserInfo();

            System.out.println("\nSource Logging in ...\n");
            System.out.println("Source UserID: " + userInfo.getUserId());
            System.out.println("Source User Name: " + userInfo.getUserName());
            System.out.println("Source User Email: " + userInfo.getUserEmail());
            System.out.println();

            System.out.println("Source Auth End Point: " + config.getAuthEndpoint());
            System.out.println("Source Service End Point: " + config.getServiceEndpoint());
            System.out.println();
        } catch (ConnectionException ce) {
            ce.printStackTrace();
        }
    }

    private void printUserInfoImport(ConnectorConfig config, PartnerConnection connectionTarget) {
        try {
            GetUserInfoResult userInfoImport = connectionTarget.getUserInfo();

            System.out.println("\nTarget Logging in ...\n");
            System.out.println("Target UserID: " + userInfoImport.getUserId());
            System.out.println("Target User Name: " + userInfoImport.getUserName());
            System.out.println("Target User Email: " + userInfoImport.getUserEmail());
            System.out.println();

            System.out.println("Target Auth End Point: " + config.getAuthEndpoint());
            System.out.println("Target Service End Point: " + config.getServiceEndpoint());
            System.out.println();
        } catch (ConnectionException ce) {
            ce.printStackTrace();
        }
    }

    /**
     * @return the lastSalesforceLoginTime
     */
    public long getLastSalesforceLoginTime() {
        return lastSalesforceLoginTime;
    }

    /**
     * @param lastSalesforceLoginTime the lastSalesforceLoginTime to set
     */
    public void setLastSalesforceLoginTime(long lastSalesforceLoginTime) {
        this.lastSalesforceLoginTime = lastSalesforceLoginTime;
    }

    /**
     * @return the connectionTargets
     */
    public List<PartnerConnection> getConnectionTargets() {
        return connectionTargets;
    }

    /**
     * @param connectionTargets the connectionTargets to set
     */
    public void setConnectionTargets(List<PartnerConnection> connectionTargets) {
        this.connectionTargets = connectionTargets;
    }

    private void logout(PartnerConnection connection) {
        /*
         * try { connection.logout();
         * 
         * for(PartnerConnection conn : connectionTargets) { conn.logout(); }
         * connectionTargets.clear();
         * 
         * } catch (ConnectionException ce) { ce.printStackTrace(); }
         */
    }

    public void disconnect() {
        log.info("Logging out...");
        logout(connectionSource);
        // logout(connectionTarget);
    }

}
