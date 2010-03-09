package org.apache.pig.piggybank.test.evaluation.string;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.piggybank.evaluation.string.Split;
import org.junit.Test;

public class TestSplit {


    private static final Split splitter_ = new Split();
    private static Tuple test1_ = TupleFactory.getInstance().newTuple(1);
    private static Tuple test2_ = TupleFactory.getInstance().newTuple(2);
    private static Tuple test3_ = TupleFactory.getInstance().newTuple(3);
    
    @Test 
    public void testSplit() throws IOException {
       // test no delims
        test2_.set(0, "foo");
        test2_.set(1, ":");
        Tuple splits = splitter_.exec(test2_);
        assertEquals("no matches should return tuple with original string", 1, splits.size());
        assertEquals("no matches should return tuple with original string", "foo", 
                splits.get(0));
        
        // test default delimiter
        test1_.set(0, "f ooo bar");
        splits = splitter_.exec(test1_);
        assertEquals("split on default value ", 3, splits.size());
        assertEquals("f", splits.get(0));
        assertEquals("ooo", splits.get(1));
        assertEquals("bar", splits.get(2));
        
        // test trimming of whitespace
        test1_.set(0, "foo bar  ");
        splits = splitter_.exec(test1_);
        assertEquals("whitespace trimmed if no length arg", 2, splits.size());
        
        // test forcing null matches with length param
        test3_.set(0, "foo bar   ");
        test3_.set(1, "\\s");
        test3_.set(2, 10);
        splits = splitter_.exec(test3_);
        assertEquals("length forces empty string matches on end", 5, splits.size());
        
        // test limiting results with limit
        test3_.set(0, "foo:bar:baz");
        test3_.set(1, ":");
        test3_.set(2, 2);
        splits = splitter_.exec(test3_);
        assertEquals(2, splits.size());
        assertEquals("foo", splits.get(0));
        assertEquals("bar:baz", splits.get(1));
    }
}
