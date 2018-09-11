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

import com.sforce.soap.partner.sobject.SObject;
import migrator.core.service.SforceObject;
import migrator.core.service.SforceObjectPair;

/**
 * Migrable interface :
 *
 * @author anoop.singh
 */
public interface Migrable {

    public enum RelationType {
        LOOKUP("lookup"), MASTERDETAIL("masterdetail"), HIERARCHICAL("hierarchical");

        final String v;

        RelationType(String v) {
            this.v = v;
        }

        public String toString() {
            return v;
        }
    }

    public enum Operation {
        CREATE("create"), DELETE("delete");

        final String v;

        Operation(String v) {
            this.v = v;
        }

        public String toString() {
            return v;
        }
    }

    public void process();

    // Source org
    public void query();

    // Target org
    public void insert();

    public void setup();

    public SObject buildMapping(SforceObject sforceObject, SforceObjectPair sourcePairObj);

}
