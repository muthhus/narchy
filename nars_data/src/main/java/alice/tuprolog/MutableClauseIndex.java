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

import java.util.*;

/**
 * Customized HashMap for storing clauses in the TheoryManager
 *
 * @author ivar.orstavik@hist.no
 *
 * Reviewed by Paolo Contessi
 */

public class MutableClauseIndex extends HashMap<String,FamilyClausesList> implements ClauseIndex {

	private static final long serialVersionUID = 1L;

	public void add(String key, ClauseInfo d, boolean first) {
		FamilyClausesList family = computeIfAbsent(key, (k)->new FamilyClausesList());
		family.add(d, first);
	}

//	public void addLast(String key, ClauseInfo d) {
//		FamilyClausesList family = get(key);
//		if (family == null)
//			put(key, family = new FamilyClausesList());
//		family.addLast(d);
//	}

/*	FamilyClausesList abolish(String key)
	{
		return remove(key);
	}*/

	/**
	 * Retrieves a list of the predicates which has the same name and arity
	 * as the goal and which has a compatible first-arg for matching.
	 *
	 * @param headt The goal
	 * @return  The list of matching-compatible predicates
	 */
	public List<ClauseInfo> getPredicates(Term headt) {
		FamilyClausesList family = get(((Struct) headt).getPredicateIndicator());
		//new ReadOnlyLinkedList<>();
		return family == null ? Collections.EMPTY_LIST : family.get(headt);
	}

//	/**
//	 * Retrieves the list of clauses of the requested family
//	 *
//	 * @param key   Goal's Predicate Indicator
//	 * @return      The family clauses
//	 */
//	List<ClauseInfo> getPredicates(String key){
//		FamilyClausesList family = get(key);
//		if(family == null){
//			return new ReadOnlyLinkedList<>();
//		}
//		return new ReadOnlyLinkedList<>(family);
//	}

	@Override
	public Iterator<ClauseInfo> iterator() {
		return new CompleteIterator(this);
	}

	private static class CompleteIterator implements Iterator<ClauseInfo> {
		Iterator<FamilyClausesList> values;
		Iterator<ClauseInfo> workingList;
		//private boolean busy = false;

		public CompleteIterator(ClauseIndex clauseDatabase) {
			values = clauseDatabase.values().iterator();
		}

		@Override
		public boolean hasNext() {
			while (true) {
				if (workingList != null && workingList.hasNext())
					return true;
				if (values.hasNext()) {
					workingList = values.next().iterator();
					continue;
				}
				return false;
			}
		}

		@Override
		public synchronized ClauseInfo next() {
			return workingList.hasNext() ? workingList.next() : null;
		}

		@Override
		public void remove() {
			workingList.remove();
		}
	}

}