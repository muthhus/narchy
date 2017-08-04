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

import alice.tuprolog.event.OutputListener;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Provides a prolog virtual machine embedded in a separate thread.
 * It needs a theory and optionally a goal.
 * It parses the theory, solves the goal and stops.
 *
 * @see alice.tuprolog.Prolog
 *
 */
public class PrologAgent extends Prolog {
    
    private String theoryText;
    private String goalText;
    
  
    private final static OutputListener defaultOutputListener = ev -> System.out.print(ev.getMsg());
    
    
    /**
     * Builds a prolog agent providing it a theory
     *
     * @param theory the text representing the theory
     */
    public PrologAgent(String theory, ClauseIndex dyn){
        super(dyn);
        theoryText=theory;
        addOutputListener(defaultOutputListener);
    }

//    public PrologAgent(String theory){
//        this(theory, (String)null);
//    }

    /**
     * Builds a prolog agent providing it a theory and a goal
     */
    @Deprecated public PrologAgent(String theory, String goal){

        theoryText=theory;
        goalText=goal;

        addOutputListener(defaultOutputListener);
    }
    
//    /**
//     * Constructs the Agent with a theory provided
//     * by an input stream
//     */
//    public PrologAgent(InputStream is, ClauseIndex dynamics){
//        super(dynamics);
//        theoryInputStream=is;
//        addOutputListener(defaultOutputListener);
//    }
//
//    /**
//     * Constructs the Agent with a theory provided
//     * by an input stream and a goal
//     */
//    public PrologAgent(InputStream is, String goal){
//        theoryInputStream=is;
//        goalText=goal;
//        addOutputListener(defaultOutputListener);
//    }
//
    /**
     * Starts agent execution in another thread
     */
    final public Thread spawn(){
        Thread t = new Thread(this::run);
        t.start();
        return t;
    }

    public Solution run(String goal) {
        this.goalText = goal;
        return run();
    }

    public List<Term> solutions(String goal) {
        return Lists.newArrayList( iterate(goal) );
    }

    public Iterator<Term> iterate(String goal) {


        try {

            return new Iterator<>() {

                final Solution s = run(goal);

                public Term next = s.getSolution();

                @Override
                public boolean hasNext() {

                    return next != null;
                }

                @Override
                public Term next() {
                    Term next = this.next;

                    try {
                        this.next = hasOpenAlternatives() ? solveNext().getSolution() : null;
                    } catch (NoMoreSolutionException | NoSolutionException e) {
                        this.next = null;
                    }
                    return next;
                }
            };
        } catch (Exception e) {
            return Collections.emptyIterator();
        }
    }


//    /**
//     * Adds a listener to ouput events
//     *
//     * @param l the listener
//     */
//    public synchronized void addOutputListener(OutputListener l) {
//        addOutputListener(l);
//    }
//
//    /**
//     * Removes a listener to ouput events
//     *
//     * @param l the listener
//     */
//    public synchronized void removeOutputListener(OutputListener l) {
//        removeOutputListener(l);
//    }
//
//    /**
//     * Removes all output event listeners
//     */
//    public void removeAllOutputListener(){
//        removeAllOutputListeners();
//    }
//

    /**
     * if called directly, Starts agent execution in current thread
     */
    public Solution run(){
        try {

            setTheory( (theoryText==null) ?
                new Theory() :
                new Theory(theoryText));

            if (goalText!=null){
                return solve(goalText);
            }
        } catch (Exception ex){
            System.err.println("invalid theory or goal.");
            ex.printStackTrace();
        }
        return null;
    }


//    public static void main(String args[]){
//        if (args.length==1 || args.length==2){
//
//            //FileReader fr;
//            try {
//                String text = Tools.loadText(args[0]);
//                if (args.length==1){
//                    new Agent(text).spawn();
//                } else {
//                    new Agent(text,args[1]).spawn();
//                }
//            } catch (Exception ex){
//                System.err.println("invalid theory.");
//            }
//        } else {
//            System.err.println("args: <theory file> { goal }");
//            System.exit(-1);
//        }
//    }
    
    
}