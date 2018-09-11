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
package migrator.core.buffer;

import com.sforce.soap.partner.sobject.SObject;

/**
 * Acts as a buffer
 *
 * @author anoop.singh
 */
public class SObjectBuffer {
    private SObject[] inserts = null;
    int index = 0;

    int size = 0;
    // adjust the buffer size as per your need
    int batchSize = 200;

    public SObjectBuffer(SObject[] inserts) {
        this.inserts = inserts;
        this.size = inserts.length;
    }

    public SObjectBuffer(SObject[] inserts, int batchSize) {
        this.inserts = inserts;
        this.size = inserts.length;
        this.batchSize = batchSize;
    }

    public boolean hasAvailable() {
        if (index == size) {
            return false;
        }
        if (index < size)
            return true;
        return false;
    }

    public SObject[] getBuffer() {
        int newSize = 0;
        if ((size - index) > batchSize) {
            newSize = batchSize;
        } else {
            newSize = size - index;
        }
        SObject[] buffer = new SObject[newSize];

        for (int i = 0; i < newSize; i++) {
            buffer[i] = inserts[index];
            index++;
        }
        return buffer;
    }

    /**
     * @return the inserts
     */
    public SObject[] getInserts() {
        return inserts;
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * @return the batchSize
     */
    public int getBatchSize() {
        return batchSize;
    }
}
