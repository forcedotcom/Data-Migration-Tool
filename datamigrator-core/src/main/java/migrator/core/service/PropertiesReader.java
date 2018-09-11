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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

/**
 * PropertiesReader
 *
 * @author anoop.singh
 */
public class PropertiesReader {

    public static enum PROPERTY_TYPE {
        BUILD
    }

    private final Properties buildProp = new Properties();

    private PropertiesReader() {
        InputStream in2 = this.getClass().getClassLoader().getResourceAsStream("build.properties");
        try {
            buildProp.load(in2);
            // Load override if available as system properties
            for (Object key : this.buildProp.keySet()) {
                String override = System.getProperty((String) key);
                if (override != null)
                    buildProp.put(key, override);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class LazyHolder {
        private static final PropertiesReader INSTANCE = new PropertiesReader();
    }

    public static PropertiesReader getInstance() {
        return LazyHolder.INSTANCE;
    }

    public String getProperty(PROPERTY_TYPE propType, String key) {
        switch (propType) {
            case BUILD:
                return buildProp.getProperty(key) != null ? buildProp.getProperty(key).trim() : buildProp
                        .getProperty(key);
        }
        return "";
    }

    public Set<String> getAllPropertyNames(PROPERTY_TYPE propType) {

        switch (propType) {
            case BUILD:
                return buildProp.stringPropertyNames();
        }
        return null;
    }

    public boolean containsKey(PROPERTY_TYPE propType, String key) {
        switch (propType) {
            case BUILD:
                return buildProp.containsKey(key);
        }
        return false;
    }

}
