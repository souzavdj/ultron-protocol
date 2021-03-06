package jason.asSyntax.patterns.goal;

import jason.asSemantics.Agent;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Pred;
import jason.asSyntax.Term;
import jason.asSyntax.directives.Directive;
import jason.asSyntax.directives.DirectiveProcessor;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the Single-Minded Commitment pattern (see DALT 2006 paper)
 *
 * @author jomi
 */
public class SMC implements Directive {

    static Logger logger = Logger.getLogger(SMC.class.getName());

    public Agent process(Pred directive, Agent outerContent, Agent innerContent) {
        try {
            Term goal = directive.getTerm(0);
            Term fail = directive.getTerm(1);
            Pred subDir = Pred.parsePred("bc(" + goal + ")");
            //logger.fine("parameters="+goal+","+fail+","+subDir);
            Directive sd = DirectiveProcessor.getDirective(subDir.getFunctor());

            // apply sub directive
            Agent newAg = sd.process(subDir, outerContent, innerContent);
            if (newAg != null) {

                // add +f : true <- .fail_goal(g).
                newAg.getPL().add(ASSyntax.parsePlan("+" + fail + " <- .fail_goal(" + goal + ")."));

                return newAg;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Directive error.", e);
        }
        return null;
    }
}
