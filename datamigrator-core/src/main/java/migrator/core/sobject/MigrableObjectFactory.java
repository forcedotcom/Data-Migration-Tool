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

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import migrator.core.service.PropertiesReader;
import migrator.core.sobject.MigrableObject;
import migrator.core.utils.CommandVO;
import migrator.core.utils.ForceFileUtils;

/**
 * MigrableObjectFactory
 *
 * @author anoop.singh
 */
public class MigrableObjectFactory {

    static Logger log = Logger.getLogger(MigrableObjectFactory.class.getName());

    public static MigrableObject migrate(CommandVO comamndArgs, boolean jsonfilePath) {
        MigrableObject migrable = getMigrableObject(comamndArgs, jsonfilePath);
        migrable.process();
        return migrable;
    }

    public static MigrableObject migrate(CommandVO comamndArgs) {
        MigrableObject migrable = getMigrableObject(comamndArgs, true);
        migrable.process();
        return migrable;
    }

    public static List<MigrableObject> migrate(List<CommandVO> comamndArgs) {
        List<MigrableObject> mirableObjects = new ArrayList<MigrableObject>();
        for (CommandVO vo : comamndArgs) {
            MigrableObject migrable = getMigrableObject(vo, true);
            mirableObjects.add(migrable);
        }
        // Cleanup
        for (CommandVO vo : comamndArgs) {
            vo = null;
        }
        for (MigrableObject migrable : mirableObjects) {
            migrable.process();
            // Cleanup
            migrable = null;
        }
        return mirableObjects;
    }

    private static MigrableObject getMigrableObject(CommandVO comamndArgs, boolean jsonfilePath) {
        MigrableObject migrable = null;

        String mappingFile = comamndArgs.getMapping();
        if (jsonfilePath) {
            String mappingPath = "/object-mappings/";
            if (PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD, "object.mapping.dir") != null) {
                mappingPath =
                        PropertiesReader.getInstance().getProperty(PropertiesReader.PROPERTY_TYPE.BUILD,
                                "object.mapping.dir");
            }
            mappingFile = new ForceFileUtils().getFileAsString(mappingPath + comamndArgs.getMapping());
        }
        log.debug("mappingFile=\n" + mappingFile);

        ObjectMappingConfig objectMapping = ObjectMappingConfig.getMapping(mappingFile);
        log.debug("objectMapping.getRelationType()=" + objectMapping.getRelationType());

        if (objectMapping.getRelationType() == Migrable.RelationType.LOOKUP) {
            migrable = new MigrableLookupObject(objectMapping, comamndArgs.getOperation());
        } else if (objectMapping.getRelationType() == Migrable.RelationType.MASTERDETAIL) {
            migrable = new MigrableMasterDetailObject(objectMapping, comamndArgs.getOperation());
        } else if (objectMapping.getRelationType() == Migrable.RelationType.HIERARCHICAL) {
            migrable = new MigrableHierarchicalObject(objectMapping, comamndArgs.getOperation());
        }
        return migrable;
    }

}
