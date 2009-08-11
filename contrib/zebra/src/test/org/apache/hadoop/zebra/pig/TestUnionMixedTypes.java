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

package org.apache.hadoop.zebra.pig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.zebra.io.BasicTable;
import org.apache.hadoop.zebra.io.TableInserter;
import org.apache.hadoop.zebra.io.TableScanner;
import org.apache.hadoop.zebra.io.BasicTable.Reader.RangeSplit;
import org.apache.hadoop.zebra.types.ParseException;
import org.apache.hadoop.zebra.types.Schema;
import org.apache.hadoop.zebra.types.TypesUtils;
import org.apache.pig.ExecType;
import org.apache.pig.PigServer;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.Tuple;
import org.apache.pig.test.MiniCluster;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Note:
 * 
 * Make sure you add the build/pig-0.1.0-dev-core.jar to the Classpath of the
 * app/debug configuration, when run this from inside the Eclipse.
 * 
 */
public class TestUnionMixedTypes {
  protected static ExecType execType = ExecType.MAPREDUCE;
  private static MiniCluster cluster;
  protected static PigServer pigServer;
  private static Path pathWorking, pathTable1, pathTable2;
  private static Configuration conf;
  final static String STR_SCHEMA1 = "a:collection(a:string, b:string),b:map(string),c:record(f1:string, f2:string),d";
  final static String STR_STORAGE1 = "[a,d];[b#{k1|k2}];[c]";
  final static String STR_SCHEMA2 = "a:collection(a:string, b:string),b:map(string),c:record(f1:string, f2:string),e";
  final static String STR_STORAGE2 = "[a,e];[b#{k1}];[c.f1]";

  @BeforeClass
  public static void setUpOnce() throws Exception {
    if (System.getProperty("hadoop.log.dir") == null) {
      String base = new File(".").getPath(); // getAbsolutePath();
      System
          .setProperty("hadoop.log.dir", new Path(base).toString() + "./logs");
    }

    if (execType == ExecType.MAPREDUCE) {
      cluster = MiniCluster.buildCluster();
      pigServer = new PigServer(ExecType.MAPREDUCE, cluster.getProperties());
    } else {
      pigServer = new PigServer(ExecType.LOCAL);
    }

    conf = new Configuration();
    FileSystem fs = cluster.getFileSystem();
    pathWorking = fs.getWorkingDirectory();

    /*
     * create 1st basic table;
     */
    pathTable1 = new Path(pathWorking, "1");
    System.out.println("pathTable1 =" + pathTable1);

    BasicTable.Writer writer = new BasicTable.Writer(pathTable1, STR_SCHEMA1,
        STR_STORAGE1, false, conf);
    Schema schema = writer.getSchema();
    Tuple tuple = TypesUtils.createTuple(schema);

    BasicTable.Writer writer1 = new BasicTable.Writer(pathTable1, conf);
    int part = 0;
    TableInserter inserter = writer1.getInserter("part" + part, true);

    TypesUtils.resetTuple(tuple);
    DataBag bag1 = TypesUtils.createBag();
    Schema schColl = schema.getColumn(0).getSchema();
    Tuple tupColl1 = TypesUtils.createTuple(schColl);
    Tuple tupColl2 = TypesUtils.createTuple(schColl);

    int row = 0;
    tupColl1.set(0, "1.1");
    tupColl1.set(1, "1.11");
    bag1.add(tupColl1);
    tupColl2.set(0, "1.111");
    tupColl2.set(1, "1.1111");
    bag1.add(tupColl2);
    tuple.set(0, bag1);

    Map<String, String> m1 = new HashMap<String, String>();
    m1.put("k1", "k11");
    m1.put("b", "b1");
    m1.put("c", "c1");
    tuple.set(1, m1);

    Tuple tupRecord1;
    try {
      tupRecord1 = TypesUtils.createTuple(schema.getColumnSchema("c")
          .getSchema());
    } catch (ParseException e) {
      e.printStackTrace();
      throw new IOException(e);
    }

    tupRecord1.set(0, "1");
    tupRecord1.set(1, "hello1");
    tuple.set(2, tupRecord1);
    tuple.set(3, "world1");

    inserter.insert(new BytesWritable(String.format("k%d%d", part + 1, row + 1)
        .getBytes()), tuple);

    // second row
    row++;
    TypesUtils.resetTuple(tuple);
    TypesUtils.resetTuple(tupRecord1);
    TypesUtils.resetTuple(tupColl1);
    TypesUtils.resetTuple(tupColl2);
    m1.clear();
    bag1.clear();

    row++;
    tupColl1.set(0, "2.2");
    tupColl1.set(1, "2.22");
    bag1.add(tupColl1);
    tupColl2.set(0, "2.222");
    tupColl2.set(1, "2.2222");
    bag1.add(tupColl2);
    tuple.set(0, bag1);

    m1.put("k2", "k22");
    m1.put("k3", "k32");
    m1.put("k1", "k12");
    m1.put("k4", "k42");
    tuple.set(1, m1);

    tupRecord1.set(0, "2");
    tupRecord1.set(1, "hello2");
    tuple.set(2, tupRecord1);
    tuple.set(3, "world2");

    inserter.insert(new BytesWritable(String.format("k%d%d", part + 1, row + 1)
        .getBytes()), tuple);
    inserter.close();
    writer1.finish();
    writer.close();

    /*
     * create 2nd basic table;
     */
    pathTable2 = new Path(pathWorking, "2");
    System.out.println("pathTable2 =" + pathTable2);

    BasicTable.Writer writer2 = new BasicTable.Writer(pathTable2, STR_SCHEMA2,
        STR_STORAGE2, false, conf);
    Schema schema2 = writer.getSchema();

    Tuple tuple2 = TypesUtils.createTuple(schema2);

    BasicTable.Writer writer22 = new BasicTable.Writer(pathTable2, conf);
    part = 0;
    TableInserter inserter2 = writer22.getInserter("part" + part, true);

    TypesUtils.resetTuple(tuple2);
    TypesUtils.resetTuple(tuple);
    TypesUtils.resetTuple(tupRecord1);
    TypesUtils.resetTuple(tupColl1);
    TypesUtils.resetTuple(tupColl2);
    m1.clear();
    bag1.clear();

    row = 0;
    tupColl1.set(0, "3.3");
    tupColl1.set(1, "3.33");
    bag1.add(tupColl1);
    tupColl2.set(0, "3.333");
    tupColl2.set(1, "3.3333");
    bag1.add(tupColl2);
    tuple2.set(0, bag1);

    m1.put("k1", "k13");
    m1.put("b", "b3");
    m1.put("c", "c3");
    tuple2.set(1, m1);

    tupRecord1.set(0, "3");
    tupRecord1.set(1, "hello3");
    tuple2.set(2, tupRecord1);
    tuple2.set(3, "world13");

    inserter2.insert(new BytesWritable(String
        .format("k%d%d", part + 1, row + 1).getBytes()), tuple2);

    // second row
    row++;
    TypesUtils.resetTuple(tuple2);
    TypesUtils.resetTuple(tupRecord1);
    TypesUtils.resetTuple(tupColl1);
    TypesUtils.resetTuple(tupColl2);
    bag1.clear();
    m1.clear();

    row++;
    tupColl1.set(0, "4.4");
    tupColl1.set(1, "4.44");
    bag1.add(tupColl1);
    tupColl2.set(0, "4.444");
    tupColl2.set(1, "4.4444");
    bag1.add(tupColl2);
    tuple2.set(0, bag1);

    m1.put("k2", "k24");
    m1.put("k3", "k34");
    m1.put("k1", "k14");
    m1.put("k4", "k44");
    tuple2.set(1, m1);

    tupRecord1.set(0, "4");
    tupRecord1.set(1, "hello4");
    tuple2.set(2, tupRecord1);
    tuple2.set(3, "world4");

    inserter2.insert(new BytesWritable(String
        .format("k%d%d", part + 1, row + 1).getBytes()), tuple2);
    inserter2.close();
    writer2.finish();
    writer22.close();

  }

  @AfterClass
  public static void tearDownOnce() throws Exception {
    pigServer.shutdown();
  }

  @Test
  // all fields
  public void testReader1() throws ExecException, IOException {
    /*
     * remove hdfs prefix part like "hdfs://localhost.localdomain:42540" pig
     * will fill that in.
     */
    String str1 = pathTable1.toString().substring(
        pathTable1.toString().indexOf("/", 7), pathTable1.toString().length());
    String str2 = pathTable2.toString().substring(
        pathTable2.toString().indexOf("/", 7), pathTable2.toString().length());
    String query = "records = LOAD '"
        + str1
        + ","
        + str2
        + "' USING org.apache.hadoop.zebra.pig.TableLoader('a,b#{k1|k2},c.f1');";
    System.out.println(query);

    pigServer.registerQuery(query);
    Iterator<Tuple> it = pigServer.openIterator("records");

    Tuple cur = null;
    int i = 0;
    int j = 0;
    // total 4 lines
    while (it.hasNext()) {
      cur = it.next();

      i++;
      System.out.println(" line : " + i + " : " + cur.toString());
      /*
       * line : 1 : ({(3.3,3.33),(3.333,3.3333)},[k1#k13,k2#],3) line : 2 :
       * ({(4,4,4.44),(4.444,4,4444),(4,4,4.44),(4.444,4,4444)},[k1#k14,k2
       * #k24],4)
       */
      // first line
      Iterator<Tuple> it2 = ((DataBag) cur.get(0)).iterator();
      while (it2.hasNext()) {

        Tuple cur2 = it2.next();
        j++;

        if (j == 1) {
          System.out.println("j is : " + j);
          Assert.assertEquals(j + "." + j, cur2.get(0));
          Assert.assertEquals(j + "." + j + j, cur2.get(1));
        }
        if (j == 2) {
          System.out.println("j is : " + j);

          Assert.assertEquals((j - 1) + "." + (j - 1) + (j - 1) + (j - 1), cur2
              .get(0));
          Assert.assertEquals((j - 1) + "." + (j - 1) + (j - 1) + (j - 1)
              + (j - 1), cur2.get(1));
        }

        TypesUtils.resetTuple(cur2);

      }// inner while
      if (i == 1) {
        System.out.println("i is : " + i);

        Assert.assertEquals("k11", ((Map) cur.get(1)).get("k1"));
        Assert.assertEquals(null, ((Map) cur.get(1)).get("k2"));
        Assert.assertEquals("1", cur.get(2));
      }

      if (i == 2) {
        System.out.println("i should see this line. ");
        Assert.assertEquals("k12", ((Map) cur.get(1)).get("k1"));
        Assert.assertEquals("k22", ((Map) cur.get(1)).get("k2"));
        Assert.assertEquals("2", cur.get(2));
      }
      if (i == 3) {
        System.out.println("i is : " + i);

        Assert.assertEquals("k13", ((Map) cur.get(1)).get("k1"));
        Assert.assertEquals(null, ((Map) cur.get(1)).get("k2"));
        Assert.assertEquals("3", cur.get(2));
      }

      if (i == 4) {
        System.out.println("i should see this line. ");
        Assert.assertEquals("k14", ((Map) cur.get(1)).get("k1"));
        Assert.assertEquals("k24", ((Map) cur.get(1)).get("k2"));
        Assert.assertEquals("4", cur.get(2));
      }
    }// outer while

    Assert.assertEquals(4, i);
  }

  @Test
  // one common field only
  public void testReader2() throws ExecException, IOException {
    /*
     * remove hdfs prefix part like "hdfs://localhost.localdomain:42540" pig
     * will fill that in.
     */
    String str1 = pathTable1.toString().substring(
        pathTable1.toString().indexOf("/", 7), pathTable1.toString().length());
    String str2 = pathTable2.toString().substring(
        pathTable2.toString().indexOf("/", 7), pathTable2.toString().length());
    String query = "records = LOAD '" + str1 + "," + str2
        + "' USING org.apache.hadoop.zebra.pig.TableLoader('b#{k1}');";
    System.out.println(query);

    pigServer.registerQuery(query);
    Iterator<Tuple> it = pigServer.openIterator("records");

    Tuple cur = null;
    int i = 0;
    int j = 0;
    // total 4 lines
    while (it.hasNext()) {
      cur = it.next();

      i++;
      System.out.println(" line : " + i + " : " + cur.toString());

      // first line

      if (i == 1) {
        System.out.println("i is : " + i);

        Assert.assertEquals("k11", ((Map) cur.get(0)).get("k1"));
        Assert.assertEquals(null, ((Map) cur.get(0)).get("k2"));
      }

      if (i == 2) {
        Assert.assertEquals("k12", ((Map) cur.get(0)).get("k1"));
        Assert.assertEquals(null, ((Map) cur.get(0)).get("k2"));
        try {
          cur.get(1);
          Assert.fail("should throw index out of bound excepiotn");
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      if (i == 3) {
        System.out.println("i is : " + i);

        Assert.assertEquals("k13", ((Map) cur.get(0)).get("k1"));
        Assert.assertEquals(null, ((Map) cur.get(0)).get("k2"));
        try {
          cur.get(1);
          Assert.fail("should throw index out of bound excepiotn");
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      if (i == 4) {
        System.out.println("i should see this line. ");
        Assert.assertEquals("k14", ((Map) cur.get(0)).get("k1"));
        Assert.assertEquals(null, ((Map) cur.get(0)).get("k2"));
        try {
          cur.get(1);
          Assert.fail("should throw index out of bound excepiotn");
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }// outer while

    Assert.assertEquals(4, i);
  }

  @Test
  // one field which exists in one table only
  public void testReader3() throws ExecException, IOException {

    String str1 = pathTable1.toString().substring(
        pathTable1.toString().indexOf("/", 7), pathTable1.toString().length());
    String str2 = pathTable2.toString().substring(
        pathTable2.toString().indexOf("/", 7), pathTable2.toString().length());
    String query = "records = LOAD '" + str1 + "," + str2
        + "' USING org.apache.hadoop.zebra.pig.TableLoader('d');";
    System.out.println(query);

    pigServer.registerQuery(query);
    Iterator<Tuple> it = pigServer.openIterator("records");

    Tuple cur = null;
    int i = 0;
    while (it.hasNext()) {
      cur = it.next();

      i++;
      System.out.println(" line : " + i + " : " + cur.toString());
      if (i == 1) {
        System.out.println("i is : " + i);

        Assert.assertEquals("world1", cur.get(0));
        try {
          cur.get(1);
          Assert.fail("should throw index out of bound excepiotn");
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      if (i == 2) {

        Assert.assertEquals("world2", cur.get(0));
        try {
          cur.get(1);
          Assert.fail("should throw index out of bound excepiotn");
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      if (i == 3) {

        Assert.assertEquals(null, cur.get(0));
        try {
          cur.get(1);
          Assert.fail("should throw index out of bound excepiotn");
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      if (i == 4) {

        Assert.assertEquals(null, cur.get(0));
        try {
          cur.get(1);
          Assert.fail("should throw index out of bound excepiotn");
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }// outer while

    Assert.assertEquals(4, i);
  }

}