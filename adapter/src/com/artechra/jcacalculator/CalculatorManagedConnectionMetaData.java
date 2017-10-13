package com.artechra.jcacalculator;

import javax.resource.spi.ManagedConnectionMetaData;

/**
 * A class containing immutable metadata for the Calculator's Managed Connection
 *
 * @author Eoin Woods
 */
public class CalculatorManagedConnectionMetaData implements ManagedConnectionMetaData {

    /**
     * Return the name of the "system" that the adapter connects to
     * @return "Integer Calculator"
     */
    public String getEISProductName() {
        return "Integer Calculator";
    }

    /**
     * Return the version of the "system" that the adapter connects to
     * @return "1.0.0"
     */
    public String getEISProductVersion()  {
        return "1.0.0";
    }

    /**
     * Return the maximum number of connections that this adapter can
     * handle
     * @return 10 (arbitary value)
     */
    public int getMaxConnections()  {
        return 10;
    }

    /**
     * Return the username asociated with this connection
     * @return "NONE"
     */
    public String getUserName()  {
        return "NONE" ;
    }
}
