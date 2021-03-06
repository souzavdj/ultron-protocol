package jason.asSyntax;

import jason.NoValueException;

/**
 * The interface for numeric terms of AgentSpeak language
 */
public interface NumberTerm extends Term {

    /** returns the numeric value of the term */
    public double solve() throws NoValueException;
}
