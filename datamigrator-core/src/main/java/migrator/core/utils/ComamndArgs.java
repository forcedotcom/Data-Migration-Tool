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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

/**
 * ComamndArgs
 *
 * @author anoop.singh
 */
public class ComamndArgs {

    private String[] args = null;

    private HashMap<String, Integer> switchIndexes = new HashMap<String, Integer>();
    private TreeSet<Integer> takenIndexes = new TreeSet<Integer>();

    private List<String> targets = new ArrayList<String>();

    public CommandVO getCommandVO() {
        CommandVO vo = new CommandVO();
        vo.setOperation(getOperation());
        vo.setMapping(getMapping());

        return vo;
    }

    /**
     * @return the operation
     */
    public String getOperation() {
        return switchValue("-operation");
    }

    /**
     * @return the mapping
     */
    public String getMapping() {
        return switchValue("-mapping");
    }

    /**
     * @return the type
     */
    public String getRelationType() {
        return switchValue("-relationType");
    }

    public String toString() {
        return usage() + "\n\n" + "ComamndArgs[operation:" + switchValue("-operation") + " mapping:"
                + switchValue("-mapping") + " relationType:" + switchValue("-relationType") + "]";
    }

    public String usage() {
        return "-operation create|delete -mapping <mapping.json> -relationType lookup|masterdetail|hierarchical";
    }

    public ComamndArgs(String[] args) {

        parse(args);
    }

    public void parse(String[] arguments) {
        this.args = arguments;
        // locate switches.
        switchIndexes.clear();
        takenIndexes.clear();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                switchIndexes.put(args[i], i);
                takenIndexes.add(i);
            }
        }
    }

    public String[] args() {
        return args;
    }

    public String arg(int index) {
        return args[index];
    }

    public boolean switchPresent(String switchName) {
        return switchIndexes.containsKey(switchName);
    }

    public String switchValue(String switchName) {
        return switchValue(switchName, null);
    }

    public String switchValue(String switchName, String defaultValue) {
        if (!switchIndexes.containsKey(switchName))
            return defaultValue;

        int switchIndex = switchIndexes.get(switchName);
        if (switchIndex + 1 < args.length) {
            takenIndexes.add(switchIndex + 1);
            return args[switchIndex + 1];
        }
        return defaultValue;
    }

    public Long switchLongValue(String switchName) {
        return switchLongValue(switchName, null);
    }

    public Long switchLongValue(String switchName, Long defaultValue) {
        String switchValue = switchValue(switchName, null);

        if (switchValue == null)
            return defaultValue;
        return Long.parseLong(switchValue);
    }

    public Double switchDoubleValue(String switchName) {
        return switchDoubleValue(switchName, null);
    }

    public Double switchDoubleValue(String switchName, Double defaultValue) {
        String switchValue = switchValue(switchName, null);

        if (switchValue == null)
            return defaultValue;
        return Double.parseDouble(switchValue);
    }


    public String[] switchValues(String switchName) {
        if (!switchIndexes.containsKey(switchName))
            return new String[0];

        int switchIndex = switchIndexes.get(switchName);

        int nextArgIndex = switchIndex + 1;
        while (nextArgIndex < args.length && !args[nextArgIndex].startsWith("-")) {
            takenIndexes.add(nextArgIndex);
            nextArgIndex++;
        }

        String[] values = new String[nextArgIndex - switchIndex - 1];
        for (int j = 0; j < values.length; j++) {
            values[j] = args[switchIndex + j + 1];
        }
        return values;
    }

    public <T> T switchPojo(Class<T> pojoClass) {
        try {
            T pojo = pojoClass.newInstance();

            Field[] fields = pojoClass.getFields();
            for (Field field : fields) {
                Class fieldType = field.getType();
                String fieldName = "-" + field.getName().replace('_', '-');

                if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
                    field.set(pojo, switchPresent(fieldName));
                } else if (fieldType.equals(String.class)) {
                    if (switchValue(fieldName) != null) {
                        field.set(pojo, switchValue(fieldName));
                    }
                } else if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
                    if (switchLongValue(fieldName) != null) {
                        field.set(pojo, switchLongValue(fieldName));
                    }
                } else if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
                    if (switchLongValue(fieldName) != null) {
                        field.set(pojo, switchLongValue(fieldName).intValue());
                    }
                } else if (fieldType.equals(Short.class) || fieldType.equals(short.class)) {
                    if (switchLongValue(fieldName) != null) {
                        field.set(pojo, switchLongValue(fieldName).shortValue());
                    }
                } else if (fieldType.equals(Byte.class) || fieldType.equals(byte.class)) {
                    if (switchLongValue(fieldName) != null) {
                        field.set(pojo, switchLongValue(fieldName).byteValue());
                    }
                } else if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
                    if (switchDoubleValue(fieldName) != null) {
                        field.set(pojo, switchDoubleValue(fieldName));
                    }
                } else if (fieldType.equals(Float.class) || fieldType.equals(float.class)) {
                    if (switchDoubleValue(fieldName) != null) {
                        field.set(pojo, switchDoubleValue(fieldName).floatValue());
                    }
                } else if (fieldType.equals(String[].class)) {
                    String[] values = switchValues(fieldName);
                    if (values.length != 0) {
                        field.set(pojo, values);
                    }
                }
            }

            return pojo;
        } catch (Exception e) {
            throw new RuntimeException("Error creating switch POJO", e);
        }
    }

    public String[] targets() {
        String[] targetArray = new String[args.length - takenIndexes.size()];
        int targetIndex = 0;
        for (int i = 0; i < args.length; i++) {
            if (!takenIndexes.contains(i)) {
                targetArray[targetIndex++] = args[i];
            }
        }

        return targetArray;
    }

}
