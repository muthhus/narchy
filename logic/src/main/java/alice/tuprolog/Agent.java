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

import java.io.*;

import alice.util.Tools;
import alice.tuprolog.event.OutputListener;

/**
 * Provides a prolog virtual machine embedded in a separate thread.
 * It needs a theory and optionally a goal.
 * It parses the theory, solves the goal and stops.
 *
 * @see alice.tuprolog.Prolog
 *
 */
public class Agent extends Prolog {
    
    private String theoryText;
    private InputStream theoryInputStream;
    private String goalText;
    
  
    private final static OutputListener defaultOutputListener = ev -> System.out.print(ev.getMsg());
    
    
    /**
     * Builds a prolog agent providing it a theory
     *
     * @param theory the text representing the theory
     */
    public Agent(String theory, ClauseIndex dynamics){
        super(dynamics);
        theoryText=theory;
        addOutputListener(defaultOutputListener);
    }
    
    /**
     * Builds a prolog agent providing it a theory and a goal
     */
    public Agent(String theory,String goal){

        theoryText=theory;
        goalText=goal;

        addOutputListener(defaultOutputListener);
    }
    
    /**
     * Constructs the Agent with a theory provided
     * by an input stream
     */
    public Agent(InputStream is, ClauseIndex dynamics){
        super(dynamics);
        theoryInputStream=is;
        addOutputListener(defaultOutputListener);
    }
    
    /**
     * Constructs the Agent with a theory provided
     * by an input stream and a goal
     */
    public Agent(InputStream is,String goal){
        theoryInputStream=is;
        goalText=goal;
        addOutputListener(defaultOutputListener);
    }
    
    /**
     * Starts agent execution in another thread
     */
    final public AgentThread spawn(){
        AgentThread t = new AgentThread(this);
        t.start();
        return t;
    }


    /**
     * Starts agent execution in current thread
     */
    public Solution run() {
        return body();
    }
    public Solution run(String goal) {
        this.goalText = goal;
        return run();
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
    
    /**
     * Removes all output event listeners
     */
    public void removeAllOutputListener(){
        removeAllOutputListeners();
    }
    
    
    protected Solution body(){
        try {

            setTheory( (theoryText==null) ?
                new Theory(theoryInputStream) :
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
    
    
    final class AgentThread extends Thread {
        Agent agent;
        AgentThread(Agent agent){
            this.agent=agent;
        }
        @Override
        final public void run(){
            agent.body();
        }
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