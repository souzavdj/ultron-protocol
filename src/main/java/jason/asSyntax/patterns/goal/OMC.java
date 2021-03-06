package jason.asSyntax.patterns.goal;

import jason.asSemantics.Agent;
import jason.asSyntax.*;
import jason.asSyntax.directives.Directive;
import jason.asSyntax.directives.DirectiveProcessor;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the  Open-Minded Commitment pattern (see DALT 2006 paper)
 *
 * @author jomi
 */
public class OMC implements Directive {

    static Logger logger = Logger.getLogger(OMC.class.getName());

    public Agent process(Pred directive, Agent outerContent, Agent innerContent) {
        try {
            Term goal = directive.getTerm(0);
            Term fail = directive.getTerm(1);
            Term motivation = directive.getTerm(2);
            Pred subDir = Pred.parsePred("bc(" + goal + ")");
            Directive sd = DirectiveProcessor.getDirective(subDir.getFunctor());

            // apply sub directive
            Agent newAg = sd.process(subDir, outerContent, innerContent);
            if (newAg != null) {
                // add +f : true <- .fail_goal(g).
                Plan pf = ASSyntax.parsePlan("+" + fail + " <- .fail_goal(" + goal + ").");
                pf.setSrcInfo(new SourceInfo(outerContent + "/" + directive, 0));
                newAg.getPL().add(pf);

                // add -m : true <- .succeed_goal(g).
                Plan pm = ASSyntax.parsePlan("-" + motivation + " <- .succeed_goal(" + goal + ").");
                pm.setSrcInfo(new SourceInfo(outerContent + "/" + directive, 0));
                newAg.getPL().add(pm);

                return newAg;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Directive error.", e);
        }
        return null;
    }
}
