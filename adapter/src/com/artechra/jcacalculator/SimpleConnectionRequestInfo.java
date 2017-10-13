package com.artechra.jcacalculator;

import com.artechra.jcacalculator.CalculationType;

import javax.resource.spi.ConnectionRequestInfo;

/**
 * The ConnectionRequestInfo class used to contain the data needed to
 * create a Calculator Adapter connection.  This class is used by the
 * Connection Factory to hold the information used when allocating
 * a connection so that it can be passed into the container's
 * Connection Manager and then in turn passed on to the Managed
 * Connection Factory for its use.
 *
 * @author Eoin Woods
 */
public class SimpleConnectionRequestInfo implements ConnectionRequestInfo {
    private CalculationType calculationType;

    /**
     * Create an initialised connection request info object
     * @param type the calculation type that this connection is for
     */
    public SimpleConnectionRequestInfo(CalculationType type) {
        this.calculationType = type;
    }

    /**
     * Accessor for the calculationType property
     * @return the calculation type
     */
    public CalculationType getCalculationType() {
        return this.calculationType;
    }

    /**
     * Mutator for the calculationType property
     * @param type the new value for the property
     */
    public void setCalculationType(CalculationType type) {
        this.calculationType = type;
    }

    /**
     * Override of java.lang.Object#equals()
     * @param o the object to compare this one to
     * @return true if they're equal, false otherwise
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleConnectionRequestInfo that = (SimpleConnectionRequestInfo) o;

        return calculationType == that.calculationType;

    }

    /**
     * Override of java.lang.Object#hashCode()
     * @return a hash value for this object
     */
    public int hashCode() {
        return (calculationType != null ? calculationType.hashCode() : 0);
    }

    /**
     * Override of java.lang.Object#toString()
     * @return a human readable description of this object
     */
    public String toString() {
        return "SimpleConnectionRequestInfo[type=" + this.calculationType + "]" ;
    }
}
