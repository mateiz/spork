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
package org.apache.pig.tools.grunt;

import java.io.File;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.FileOutputStream;

import jline.ConsoleReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pig.PigServer;
import org.apache.pig.impl.PigContext;
import org.apache.pig.tools.grunt.GruntParser;
import org.apache.pig.PigException;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.tools.pigscript.parser.*;
import org.apache.pig.impl.logicalLayer.parser.TokenMgrError;


public class Grunt 
{
    private final Log log = LogFactory.getLog(getClass());
    
    BufferedReader in;
    PigServer pig;
    GruntParser parser;    

    public Grunt(BufferedReader in, PigContext pigContext) throws ExecException
    {
        this.in = in;
        this.pig = new PigServer(pigContext);
        
        if (in != null)
        {
            parser = new GruntParser(in);
            parser.setParams(pig);    
        }
    }

    public void setConsoleReader(ConsoleReader c)
    {
        parser.setConsoleReader(c);
    }
    public void run() {        
        //parser.parseContOnError();
        String trueString = "true";
        boolean verbose = trueString.equalsIgnoreCase(pig.getPigContext().getProperties().getProperty("verbose"));
        boolean append = false;
        while(true) {
            try {
                parser.setInteractive(true);
                parser.parseStopOnError();
                break;                            
            } catch(Throwable t) {
                writeLog(t, verbose, append);
                append = true;
                parser.ReInit(in);
            }
        }
    }

    public void exec() throws Throwable {
        String trueString = "true";
        boolean verbose = trueString.equalsIgnoreCase(pig.getPigContext().getProperties().getProperty("verbose"));
        try {
            parser.setInteractive(false);
            parser.parseStopOnError();
        } catch (Throwable t) {
            writeLog(t, verbose, false);
            throw (t);
        }
    }
    
    private void writeLog(Throwable t, boolean verbose, boolean append) {
        
    	String message = null;
    	
        if(t instanceof Exception) {
            Exception pe = Utils.getPermissionException((Exception)t);
            if (pe != null) {
                log.error("You don't have permission to perform the operation. Error from the server: " + pe.getMessage());
            }
        }

        PigException pigException = Utils.getPigException(t);

        if(pigException != null) {
        	message = "ERROR " + pigException.getErrorCode() + ": " + pigException.getMessage();
        } else {
            if((t instanceof ParseException 
            		|| t instanceof org.apache.pig.tools.pigscript.parser.TokenMgrError 
            		|| t instanceof org.apache.pig.impl.logicalLayer.parser.TokenMgrError)) {
            	message = "ERROR 1000: Error during parsing. " + t.getMessage();
            } else if (t instanceof RuntimeException) {
            	message = "ERROR 2999: Unexpected internal error. " + t.getMessage();
            } else {
            	message = "ERROR 2998: Unhandled internal error. " + t.getMessage();
            }
        }

    	
    	FileOutputStream fos = null;
        ByteArrayOutputStream bs = new ByteArrayOutputStream();        
        t.printStackTrace(new PrintStream(bs));

        log.error(message);
        
        if(verbose) {
            log.error(bs.toString());
        }
        
        String logFileName = pig.getPigContext().getProperties().getProperty("pig.logfile");
        
        if(logFileName == null) {
            //if exec is invoked programmatically then logFileName will be null
            log.warn("There is no log file to write to");
            log.error(bs.toString());
            return;
        }
        
        
        File logFile = new File(logFileName);
        try {            
            fos = new FileOutputStream(logFile, append);
            fos.write((message + "\n").getBytes("UTF-8"));
            fos.write(bs.toString().getBytes("UTF-8"));           
            fos.close();
            if(verbose) {
                System.err.println("Details also at logfile: " + logFileName);
            } else {
                System.err.println("Details at logfile: " + logFileName);
            }
        } catch (IOException ioe) {
            log.warn("Could not write to log file: " + logFileName + " :" + ioe.getMessage());
            log.error(bs.toString());
        }
    }    
}
