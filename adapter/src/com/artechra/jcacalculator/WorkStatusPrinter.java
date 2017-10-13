package com.artechra.jcacalculator;

import javax.resource.spi.work.WorkEvent;
import java.util.Map;
import java.util.HashMap;

/**
 * This class is just for developer convenience as it allows the WorkEvent's
 * constants to be displayed easily for reference while debugging.
 *
 * @author Eoin Woods
 */
public class WorkStatusPrinter {

    public static void main(String[] args) {
        Map<Integer, String> values = new HashMap<Integer, String>() ;
        values.put(WorkEvent.WORK_ACCEPTED, "WORK_ACCEPTED") ;
        values.put(WorkEvent.WORK_COMPLETED, "WORK_COMPLETED") ;
        values.put(WorkEvent.WORK_REJECTED, "WORK_REJECTED") ;
        values.put(WorkEvent.WORK_STARTED, "WORK_STARTED") ;
        System.out.println("WorkEvent event values: " + values) ;
    }
}
