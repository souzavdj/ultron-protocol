// ----------------------------------------------------------------------------
// Copyright (C) 2003 Rafael H. Bordini, Jomi F. Hubner, et al.
// 
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
// 
// To contact the authors:
// http://www.inf.ufrgs.br/~bordini
// http://www.das.ufsc.br/~jomi
//
//----------------------------------------------------------------------------

package jason.asSyntax;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an atom (a positive literal with no argument and no annotation, e.g. "tell", "a").
 */
public class Atom extends Literal {

    private static final long serialVersionUID = 1L;

    private static Logger logger = Logger.getLogger(Atom.class.getName());

    private final String functor; // immutable field

    public Atom(String functor) {
        if (functor == null) {
            logger.log(Level.WARNING, "An atom functor should not be null!", new Exception());
        }
        this.functor = functor;
    }

    public Atom(Literal l) {
        this.functor = l.getFunctor();
        predicateIndicatorCache = l.predicateIndicatorCache;
        hashCodeCache = l.hashCodeCache;
        srcInfo = l.srcInfo;
    }

    public String getFunctor() {
        return functor;
    }

    public Term clone() {
        return this; // since this object is immutable
    }

    @Override
    public boolean isAtom() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o instanceof Atom) {
            Atom a = (Atom) o;
            //System.out.println(getFunctor() +" ==== " + a.getFunctor() + " is "+ (a.isAtom())); // && getFunctor()
            // .equals(a.getFunctor())));
            return a.isAtom() && getFunctor().equals(a.getFunctor());
        }
        return false;
    }

    public int compareTo(Term t) {
        if (t == null) {
            return -1; // null should be first (required for addAnnot)
        }
        if (t.isNumeric()) {
            return 1;
        }

        // this is a list and the other not
        if (isList() && !t.isList()) {
            return -1;
        }

        // this is not a list and the other is
        if (!isList() && t.isList()) {
            return 1;
        }

        // both are lists, check the size
        if (isList() && t.isList()) {
            ListTerm l1 = (ListTerm) this;
            ListTerm l2 = (ListTerm) t;
            final int l1s = l1.size();
            final int l2s = l2.size();
            if (l1s > l2s) {
                return 1;
            }
            if (l2s > l1s) {
                return -1;
            }
            return 0; // need to check elements (in Structure class)
        }
        if (t.isVar()) {
            return -1;
        }
        if (t instanceof Literal) {
            Literal tAsLit = (Literal) t;
            final int ma = getArity();
            final int oa = tAsLit.getArity();
            if (ma < oa) {
                return -1;
            } else if (ma > oa) {
                return 1;
            } else {
                return getFunctor().compareTo(tAsLit.getFunctor());
            }
        }

        return super.compareTo(t);
    }

    @Override
    protected int calcHashCode() {
        return getFunctor().hashCode();
    }

    @Override
    public String toString() {
        return functor;
    }

    /** get as XML */
    public Element getAsDOM(Document document) {
        Element u = (Element) document.createElement("structure");
        u.setAttribute("functor", getFunctor());
        return u;
    }
}
