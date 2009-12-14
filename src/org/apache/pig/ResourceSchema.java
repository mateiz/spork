/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pig;

import java.util.Arrays;
import java.util.List;

import org.apache.pig.data.DataType;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.apache.pig.impl.logicalLayer.schema.Schema.FieldSchema;

public class ResourceSchema {

    /* Array Getters intentionally return mutable arrays instead of copies,
     * to simplify updates without unnecessary copying.
     * Setters make a copy of the arrays in order to prevent an array
     * from being shared by two objects, with modifications in one
     * accidentally changing the other.
     */
    
    // initializing arrays to empty so we don't have to worry about NPEs
    // setters won't set to null
    private ResourceFieldSchema[] fields = new ResourceFieldSchema[0];

    public enum Order { ASCENDING, DESCENDING }
    private int[] sortKeys = new int[0]; // each entry is an offset into the fields array.
    private Order[] sortKeyOrders = new Order[0];
        
    private int version = 0;

    public static class ResourceFieldSchema {
        private String name;
        
        // values are constants from DataType
        private byte type;
        
        private String description;

        // nested tuples and bags will have their own schema
        private ResourceSchema schema; 

        public ResourceFieldSchema() {
            
        }
        
        public ResourceFieldSchema(FieldSchema fieldSchema) {
            type = fieldSchema.type;
            name = fieldSchema.alias;
            description = "autogenerated from Pig Field Schema";
            if (type == DataType.BAG && fieldSchema.schema != null) { // allow partial schema
                List<FieldSchema> slst = fieldSchema.schema.getFields();
                if (slst.size() != 1 || slst.get(0).type != DataType.TUPLE) {
                    throw new IllegalArgumentException("Invalid Pig schema: " +
                    		"bag schema must have tuple as its field.");
                }
            }
            // XXX allow partial schema 
            if (type == DataType.BAG || type == DataType.TUPLE) {
                schema = new ResourceSchema(fieldSchema.schema);
            } else {
                schema = null;
            }
        }
        
        public String getName() {
            return name;
        }
        public ResourceFieldSchema setName(String name) {
            this.name = name;
            return this;
        }
        
        public byte getType() {
            return type;
        }
    
        public ResourceFieldSchema setType(byte type) {
            this.type = type;
            return this;
        }
        
        public String getDescription() {
            return description;
        }
     
        public ResourceFieldSchema setDescription(String description) {
            this.description = description;
            return this;
        }

        public ResourceSchema getSchema() {
            return schema;
        }

        public ResourceFieldSchema setSchema(ResourceSchema schema) {
            this.schema = schema;
            return this;
        }
                
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.name).append(":");
            if (DataType.isAtomic(this.type)) {
                sb.append(DataType.findTypeName(this.type));
            } else {
                stringifyResourceSchema(sb, this.schema, this.type);
            }
            return sb.toString();
        }
    }


    public ResourceSchema() {
        
    }
    
    public ResourceSchema(Schema pigSchema) {
        List<FieldSchema> pigSchemaFields = pigSchema.getFields();
        fields = new ResourceFieldSchema[pigSchemaFields.size()];
        for (int i=0; i<fields.length; i++) {
            fields[i] = new ResourceFieldSchema(pigSchemaFields.get(i));
        }        
    }
    
    public int getVersion() {
        return version;
    }
    
    public ResourceSchema setVersion(int version) {
        this.version = version;
        return this;
    }
    
    public ResourceFieldSchema[] getFields() {
        return fields;
    }
    
    public String[] fieldNames() {
        String[] names = new String[fields.length];
        for (int i=0; i<fields.length; i++) {
            names[i] = fields[i].getName();
        }
        return names;
    }
    
    public ResourceSchema setFields(ResourceFieldSchema[] fields) {
        if (fields != null)
            this.fields = Arrays.copyOf(fields, fields.length);
        return this;
    }
    
    public int[] getSortKeys() {
        return sortKeys;
    }
    
    public  ResourceSchema setSortKeys(int[] sortKeys) {
        if (sortKeys != null)
            this.sortKeys = Arrays.copyOf(sortKeys, sortKeys.length);
        return this;
    }
    
    public Order[] getSortKeyOrders() {
        return sortKeyOrders;
    }
    
    public ResourceSchema setSortKeyOrders(Order[] sortKeyOrders) {
        if (sortKeyOrders != null) 
            this.sortKeyOrders = Arrays.copyOf(sortKeyOrders, sortKeyOrders.length);
        return this;
    } 
            
    public static boolean equals(ResourceSchema rs1, ResourceSchema rs2) {
        if (rs1 == null) {
            return rs2 == null ? true : false;
        }
        
        if (rs2 == null) {
            return false;
        }
        
        if (rs1.getVersion() != rs2.getVersion() 
                || !Arrays.equals(rs1.getSortKeys(), rs2.getSortKeys())
                || !Arrays.equals(rs1.getSortKeyOrders(), rs2.getSortKeyOrders())) {            
            return false;
        }            
        
        ResourceFieldSchema[] rfs1 = rs1.getFields();
        ResourceFieldSchema[] rfs2 = rs1.getFields();
        
        if (rfs1.length != rfs2.length) return false;
        
        for (int i=0; i<rfs1.length; i++) {
            if (!rfs1[i].getName().equals(rfs2[i].getName()) 
                    || rfs1[i].getType() != rfs2[i].getType()) {
                return false;
            }
            if (!equals(rfs1[i].getSchema(), rfs2[i].getSchema())) {
                return false;
            } 
        }
        
        return true;
    }
      
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("rs:");
        stringifyResourceSchema(sb, this, DataType.UNKNOWN) ;
        return sb.toString();
    }
    
    private static void stringifyResourceSchema(StringBuilder sb, 
            ResourceSchema rs, byte type) {
        if (type == DataType.UNKNOWN) {
            sb.append("<");
        } else if (type == DataType.BAG) {
            sb.append("{");
        } else if (type == DataType.TUPLE) {
            sb.append("(");
        }
        
        for (int i=0; i<rs.getFields().length; i++) {
            sb.append(rs.getFields()[i].toString());
            if (i < rs.getFields().length - 1) {
                sb.append(",");
            }
        }
                
        if (type == DataType.UNKNOWN) {
            sb.append(">");
        } else if (type == DataType.BAG) {
            sb.append("}");
        } else if (type == DataType.TUPLE) {
            sb.append(")");
        }
    }
}
