/*
 * tuProlog - Copyright (C) 2001-2002  aliCE team at deis.unibo.it
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package alice.tuprolog;

import jcog.list.FasterList;

/**
 * Administrator of flags declared
 * 
 * @author Alex Benini
 */
class FlagManager extends FasterList<Flag> {

    /**
	 * mediator owner of the manager
	 */
    protected Prolog mediator;



    /**
     * Config this Manager
     */
    public void start(Prolog vm) {
        mediator = vm;
    }

    /**
     * Defines a new flag
     */
    public synchronized boolean defineFlag(String name, Struct valueList, Term defValue,
            boolean modifiable, String libName) {
        this.add(new Flag(name, valueList, defValue, modifiable, libName));
        return true;
    }

    public synchronized boolean setFlag(String name, Term value) {
        for (Flag flag : this) {
            if (flag.getName().equals(name)) {
                if (flag.isModifiable() && flag.isValidValue(value)) {
                    flag.setValue(value);
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public synchronized Struct getPrologFlagList() {
        Struct flist = new Struct();
        for (Flag fl : this) {
            flist = new Struct(new Struct("flag", new Struct(fl.getName()), fl
                    .getValue()), flist);
        }
        return flist;
    }

    public synchronized Term getFlag(String name) {
        for (Flag fl : this) {
            if (fl.getName().equals(name)) {
                return fl.getValue();
            }
        }
        return null;
    }

    // restituisce true se esiste un flag di nome name, e tale flag ?
    // modificabile
    public boolean isModifiable(String name) {
        for (Flag flag : this) {
            if (flag.getName().equals(name)) {
                return flag.isModifiable();
            }
        }
        return false;
    }

    // restituisce true se esiste un flag di nome name, e Value ? un valore
    // ammissibile per tale flag
    public boolean isValidValue(String name, Term value) {
        for (Flag flag : this) {
            if (flag.getName().equals(name)) {
                return flag.isValidValue(value);
            }
        }
        return false;
    }

}
