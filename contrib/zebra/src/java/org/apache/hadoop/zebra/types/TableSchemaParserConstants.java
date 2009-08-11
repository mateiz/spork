/**
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

/* Generated By:JavaCC: Do not edit this line. TableSchemaParserConstants.java */
package org.apache.hadoop.zebra.types;


/**
 * Token literal values and constants.
 * Generated by org.javacc.parser.OtherFilesGen#start()
 */
public interface TableSchemaParserConstants {

  /** End of File. */
  int EOF = 0;
  /** RegularExpression Id. */
  int INT = 7;
  /** RegularExpression Id. */
  int BOOL = 8;
  /** RegularExpression Id. */
  int LONG = 9;
  /** RegularExpression Id. */
  int FLOAT = 10;
  /** RegularExpression Id. */
  int DOUBLE = 11;
  /** RegularExpression Id. */
  int STRING = 12;
  /** RegularExpression Id. */
  int BYTES = 13;
  /** RegularExpression Id. */
  int COLLECTION = 14;
  /** RegularExpression Id. */
  int RECORD = 15;
  /** RegularExpression Id. */
  int MAP = 16;
  /** RegularExpression Id. */
  int LETTER = 17;
  /** RegularExpression Id. */
  int DIGIT = 18;
  /** RegularExpression Id. */
  int SPECIALCHAR = 19;
  /** RegularExpression Id. */
  int IDENTIFIER = 20;

  /** Lexical state. */
  int DEFAULT = 0;

  /** Literal token values. */
  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "\"\\r\"",
    "\"\\t\"",
    "\"\\n\"",
    "<token of kind 5>",
    "<token of kind 6>",
    "\"int\"",
    "\"bool\"",
    "\"long\"",
    "\"float\"",
    "\"double\"",
    "\"string\"",
    "\"bytes\"",
    "\"collection\"",
    "\"record\"",
    "\"map\"",
    "<LETTER>",
    "<DIGIT>",
    "<SPECIALCHAR>",
    "<IDENTIFIER>",
    "\":\"",
    "\"(\"",
    "\")\"",
    "\",\"",
  };

}
