package jason.stdlib;

import jason.JasonException;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.InternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.NumberTermImpl;
import jason.asSyntax.Term;

import java.util.Iterator;

/**
 * <p>Internal action: <b><code>.range(<i>Var</i>,<i>Start</i>,<i>End</i>)</code></b>.
 *
 * <p>Description: backtrack all values for <i>Var</i> starting at <i>Start</i>
 * and finishing at <i>End</i>.
 *
 * <p>Parameters:<ul>
 *
 * <li>+/- var (Variable): the variable that unifies with all values.</li>
 * <li>+ start (number): initial value.</li>
 * <li>+ end (number): last value.</li>
 *
 * </ul>
 *
 * <p>Examples:<ul>
 *
 * <li> <code>.range(3,1,5)</code>: true.</li>
 * <li> <code>.range(6,1,5)</code>: false.</li>
 * <li> <code>.range(X,1,5)</code>: unifies X with 1, 2, 3, 4, and 5.</li>
 *
 * </ul>
 *
 * @see jason.stdlib.foreach for
 */
public class range extends DefaultInternalAction {

    private static InternalAction singleton = null;

    public static InternalAction create() {
        if (singleton == null) {
            singleton = new range();
        }
        return singleton;
    }

    @Override
    public int getMinArgs() {
        return 3;
    }

    @Override
    public int getMaxArgs() {
        return 3;
    }

    @Override
    protected void checkArguments(Term[] args) throws JasonException {
        super.checkArguments(args); // check number of arguments
        if (!args[1].isNumeric()) {
            throw JasonException.createWrongArgument(this, "second parameter ('" + args[1] + "') must be a number!");
        }
        if (!args[2].isNumeric()) {
            throw JasonException.createWrongArgument(this, "third parameter ('" + args[2] + "') must be a number!");
        }
    }

    @Override
    public Object execute(TransitionSystem ts, final Unifier un, Term[] args) throws Exception {
        checkArguments(args);

        final int start = (int) ((NumberTerm) args[1]).solve();
        final int end = (int) ((NumberTerm) args[2]).solve();
        if (!args[0].isVar()) {
            // first arg is not a var
            int vl = (int) ((NumberTerm) args[0]).solve();
            return vl >= start && vl <= end;
        } else {
            // first arg is a var, backtrack
            final Term var = args[0];

            return new Iterator<Unifier>() {
                int vl = start - 1;

                public boolean hasNext() {
                    return vl < end;
                }

                public Unifier next() {
                    vl++;
                    Unifier c = un.clone();
                    c.unifiesNoUndo(var, new NumberTermImpl(vl));
                    return c;
                }

                public void remove() {
                }
            };
        }
    }
}
