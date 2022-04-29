package com.niton.compile.verify;

/**
 * Used to complete the verification process with an action: fail,warn,log or just getting the result
 */
public interface Verifiable
{
    /**
     * Will fail the compilation process if the object is not valid.
     *
     * @return true if the object is valid, false if not.
     */
    boolean failOnViolation();

    /**
     * Will warn the compilation process if the object is not valid..
     *
     * @return true if the object is valid, false if not.
     */
    boolean warnOnViolation();

    /**
     * Will log a message if the object is not valid.
     * @return true if the object is valid, false if not.
     */
    boolean infoOnViolation();

    /**
     * @return true if the object is valid, false if not.
     */
    boolean isValid();
}
