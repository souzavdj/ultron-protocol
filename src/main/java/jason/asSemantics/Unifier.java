//----------------------------------------------------------------------------
// Copyright (C) 2003  Rafael H. Bordini, Jomi F. Hubner, et al.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// To contact the authors:
// http://www.inf.ufrgs.br/~bordini
// http://www.das.ufsc.br/~jomi
//
//----------------------------------------------------------------------------

package jason.asSemantics;

import jason.asSyntax.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Unifier implements Cloneable, Iterable<VarTerm> {

    private static Logger logger = Logger.getLogger(Unifier.class.getName());

    protected Map<VarTerm, Term> function = new HashMap<VarTerm, Term>();

    /**
     * gets the value for a Var, if it is unified with another var, gets this
     * other's value
     */
    public Term get(String var) {
        return get(new VarTerm(var));
    }

    public Term remove(VarTerm v) {
        return function.remove(v);
    }

    public Iterator<VarTerm> iterator() {
        return function.keySet().iterator();
    }

    /**
     * gets the value for a Var, if it is unified with another var, gets this
     * other's value
     */
    public Term get(VarTerm vtp) {
        Term vl = function.get(vtp);
        if (vl != null && vl.isVar()) { // optimised deref
            return get((VarTerm) vl);
        }
        if (vl == null) { // try negated value of the var
            //System.out.println("for "+vtp+" try "+new VarTerm(vtp.negated(), vtp.getFunctor())+" in "+this);
            vl = function.get(new VarTerm(vtp.negated(), vtp.getFunctor()));
            //System.out.println(" and found "+vl);
            if (vl != null && vl.isVar()) {
                vl = get((VarTerm) vl);
            }
            if (vl != null && vl.isLiteral()) {
                vl = vl.clone();
                ((Literal) vl).setNegated(((Literal) vl).negated());
            }
        }
        return vl;
    }

    public VarTerm getVarFromValue(Term vl) {
        for (VarTerm v : function.keySet()) {
            Term vvl = function.get(v);
            if (vvl.equals(vl)) {
                return v;
            }
        }
        return null;
    }

    public boolean unifies(Trigger te1, Trigger te2) {
        return te1.sameType(te2) && unifies(te1.getLiteral(), te2.getLiteral());
    }

    public boolean unifiesNoUndo(Trigger te1, Trigger te2) {
        return te1.sameType(te2) && unifiesNoUndo(te1.getLiteral(), te2.getLiteral());
    }

    // ----- Unify for Predicates/Literals

    /**
     * this version of unifies undo the variables' mapping
     * if the unification fails.
     * E.g.
     * u.unifier( a(X,10), a(1,1) );
     * does not change u, i.e., u = {}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean unifies(Term t1, Term t2) {
        Map cfunction = cloneFunction();
        if (unifiesNoUndo(t1, t2)) {
            return true;
        } else {
            function = cfunction;
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<VarTerm, Term> cloneFunction() {
        return (Map<VarTerm, Term>) ((HashMap) function).clone();
        //return new HashMap<VarTerm, Term>(function);
    }

    /**
     * this version of unifies does not undo the variables' mapping
     * in case of failure. It is however faster than the version with
     * undo.
     * E.g.
     * u.unifier( a(X,10), a(1,1) );
     * fails, but changes u to {X = 10}
     */
    public boolean unifiesNoUndo(Term t1g, Term t2g) {

        Pred np1 = null;
        Pred np2 = null;

        if (t1g instanceof Pred && t2g instanceof Pred) {
            np1 = (Pred) t1g;
            np2 = (Pred) t2g;

            // tests when np1 or np2 are Vars with annots
            if ((np1.isVar() && np1.hasAnnot()) || np2.isVar() && np2.hasAnnot()) {
                if (!np1.hasSubsetAnnot(np2, this)) {
                    return false;
                }
            }
        }

        if (t1g.isCyclicTerm() && t2g.isCyclicTerm()) { // both are cycled terms
            // unification of cyclic terms:
            // remove the vars (to avoid loops) and test just the structure, then reintroduce the vars
            VarTerm v1 = t1g.getCyclicVar();
            VarTerm v2 = t2g.getCyclicVar();
            remove(v1);
            remove(v2);
            try {
                return unifiesNoUndo(new LiteralImpl((Literal) t1g), new LiteralImpl((Literal) t2g));
            } finally {
                function.put(v1, t1g);
                function.put(v2, t1g);
            }

        } else {
            if (t1g.isCyclicTerm() && get(t1g.getCyclicVar()) == null) // reintroduce cycles in the unifier
            {
                function.put(t1g.getCyclicVar(), t1g);
            }
            if (t2g.isCyclicTerm() && get(t2g.getCyclicVar()) == null) {
                function.put(t2g.getCyclicVar(), t2g);
            }
        }

        // unify as Term
        boolean ok = unifyTerms(t1g, t2g);

        // if np1 is a var that was unified, clear its annots, as in
        //      X[An] = p(1)[a,b]
        // X is mapped to p(1) and not p(1)[a,b]
        // (if the user wants the "remaining" annots, s/he should write
        //      X[An|R] = p(1)[a,b]
        // X = p(1), An = a, R=[b]
        if (ok && np1 != null) { // they are predicates
            if (np1.isVar() && np1.hasAnnot()) {
                np1 = deref((VarTerm) np1);
                Term np1vl = function.get((VarTerm) np1);
                if (np1vl != null && np1vl.isPred()) {
                    Pred pvl = (Pred) np1vl.clone();
                    pvl.clearAnnots();
                    bind((VarTerm) np1, pvl);
                }
            }
            if (np2.isVar() && np2.hasAnnot()) {
                np2 = deref((VarTerm) np2);
                Term np2vl = function.get((VarTerm) np2);
                if (np2vl != null && np2vl.isPred()) {
                    Pred pvl = (Pred) np2vl.clone();
                    pvl.clearAnnots();
                    bind((VarTerm) np2, pvl);
                }
            }
        }
        
        /*if (t1g.isCyclicTerm())
            remove(t1g.getCyclicVar());
        if (t2g.isCyclicTerm()) 
            remove(t2g.getCyclicVar());
        */
        return ok;
    }

    // ----- Unify for Terms

    protected boolean unifyTerms(Term t1g, Term t2g) {
        // if args are expressions, apply them and use their values
        if (t1g.isArithExpr()) {
            t1g = t1g.capply(this);
        }
        if (t2g.isArithExpr()) {
            t2g = t2g.capply(this);
        }

        final boolean t1gisvar = t1g.isVar();
        final boolean t2gisvar = t2g.isVar();

        // one of the args is a var
        if (t1gisvar || t2gisvar) {

            // deref vars
            final VarTerm t1gv = t1gisvar ? deref((VarTerm) t1g) : null;
            final VarTerm t2gv = t2gisvar ? deref((VarTerm) t2g) : null;

            // get their values
            //final Term t1vl = t1gisvar ? function.get(t1gv) : t1g;
            //final Term t2vl = t2gisvar ? function.get(t2gv) : t2g;
            final Term t1vl = t1gisvar ? get(t1gv) : t1g;
            final Term t2vl = t2gisvar ? get(t2gv) : t2g;

            if (t1vl != null && t2vl != null) { // unifies the two values of the vars                
                return unifiesNoUndo(t1vl, t2vl);
            } else if (t1vl != null) { // unifies var with value
                return bind(t2gv, t1vl);
            } else if (t2vl != null) {
                return bind(t1gv, t2vl);
            } else {                 // unify two vars
                return bind(t1gv, t2gv);
            }
        }

        // both terms are not vars

        // if any of the terms is not a literal (is a number or a
        // string), they must be equal
        // (for unification, lists are literals)
        if (!t1g.isLiteral() && !t1g.isList() || !t2g.isLiteral() && !t2g.isList()) {
            return t1g.equals(t2g);
        }

        // both terms are literal

        Literal t1s = (Literal) t1g;
        Literal t2s = (Literal) t2g;

        // different arities
        final int ts = t1s.getArity();
        if (ts != t2s.getArity()) {
            return false;
        }

        // if both are literal, they must have the same negated
        if (t1s.negated() != t2s.negated()) {
            return false;
        }

        // different functor
        if (!t1s.getFunctor().equals(t2s.getFunctor())) {
            return false;
        }

        // unify inner terms
        // do not use iterator! (see ListTermImpl class)
        for (int i = 0; i < ts; i++) {
            if (!unifiesNoUndo(t1s.getTerm(i), t2s.getTerm(i))) {
                return false;
            }
        }

        // the first's annots must be subset of the second's annots
        if (!t1s.hasSubsetAnnot(t2s, this)) {
            return false;
        }

        return true;
    }

    public VarTerm deref(VarTerm v) {
        Term vl = function.get(v);
        // original def (before optimise)
        //   if (vl != null && vl.isVar())
        //      return deref(vl);
        //   return v;

        VarTerm first = v;
        while (vl != null && vl.isVar()) {
            v = (VarTerm) vl;
            vl = function.get(v);
        }
        if (first != v) {
            function.put(first, v); // optimise map
        }
        return v;
    }

    public boolean bind(VarTerm vt1, VarTerm vt2) {
        if (vt1.negated() && vt2.negated()) { // in the case of ~A = ~B, put A=B in the unifier
            vt1 = new VarTerm(vt1.getFunctor());
            vt2 = new VarTerm(vt2.getFunctor());
        }

        final int comp = vt1.compareTo(vt2);
        if (comp < 0) {
            function.put((VarTerm) vt1.clone(), vt2.clone());
        } else if (comp > 0) {
            function.put((VarTerm) vt2.clone(), vt1.clone());
        } // if they are the same (comp == 0), do not bind
        return true;
    }

    public boolean bind(VarTerm vt, Term vl) {
        if (vt.negated()) { // negated vars unifies only with negated literals
            if (vl.isLiteral()) {
                if (!((Literal) vl).negated()) {
                    return false;
                } else {
                    // put also the positive case in the unifier
                    Literal vlp = (Literal) vl.clone();
                    vlp.setNegated(Literal.LPos);
                    unifies(new VarTerm(vt.getFunctor()), vlp);
                }
            } else {
                return false;
            }
        }

        if (!vl.isCyclicTerm() && vl.hasVar(vt, this)) {
            vl = new CyclicTerm((Literal) vl, (VarTerm) vt.clone());
        }

        function.put((VarTerm) vt.clone(), vl);
        return true;
    }

    public void clear() {
        function.clear();
    }

    public String toString() {
        return function.toString();
    }

    public Term getAsTerm() {
        ListTerm lf = new ListTermImpl();
        ListTerm tail = lf;
        for (VarTerm k : function.keySet()) {
            Term vl = function.get(k).clone();
            if (vl instanceof Literal) {
                ((Literal) vl).makeVarsAnnon();
            }
            Structure pair = ASSyntax.createStructure("map", new UnnamedVar("_" + UnnamedVar.getUniqueId() + k),
                    vl); // the var must be changed to avoid cyclic references latter
            tail = tail.append(pair);
        }
        return lf;
    }

    public int size() {
        return function.size();
    }

    /** add all unifications from u */
    public void compose(Unifier u) {
        for (VarTerm k : u.function.keySet()) {
            Term current = get(k);
            Term kValue = u.function.get(k);
            if (current != null && (current.isVar() || kValue.isVar())) { // current unifier has the new var
                unifies(kValue, current);
            } else {
                function.put((VarTerm) k.clone(), kValue.clone());
            }
        }
    }

    public Unifier clone() {
        try {
            Unifier newUn = new Unifier();
            newUn.function = cloneFunction();
            //newUn.compose(this);
            return newUn;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error cloning unifier.", e);
            return null;
        }
    }

    @Override
    public int hashCode() {
        int s = 0;
        for (VarTerm v : function.keySet()) {
            s += v.hashCode();
        }
        return s * 31;
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o instanceof Unifier) {
            return function.equals(((Unifier) o).function);
        }
        return false;
    }

    /** get as XML */
    public Element getAsDOM(Document document) {
        Element u = (Element) document.createElement("unifier");
        for (VarTerm v : function.keySet()) {
            Element ev = v.getAsDOM(document);
            Element vl = (Element) document.createElement("value");
            vl.appendChild(function.get(v).getAsDOM(document));
            Element map = (Element) document.createElement("map");
            map.appendChild(ev);
            map.appendChild(vl);
            u.appendChild(map);
        }
        return u;
    }
}
