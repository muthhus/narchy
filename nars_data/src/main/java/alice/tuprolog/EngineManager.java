package alice.tuprolog;

//import java.io.File;
//import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class EngineManager implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private Prolog vm;
    private static final ThreadLocal<Integer> threads = new ThreadLocal<>();    //key: pid; obj: id
    @Deprecated
    private final static int rootID = 0;
    private final EngineRunner er1 = new EngineRunner(rootID);
    private int id;

    private final ConcurrentHashMap<Integer, EngineRunner> runners
            = new ConcurrentHashMap<>();    //key: id; obj: runner
    private final ConcurrentHashMap<String, TermQueue> queues
            = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> locks
            = new ConcurrentHashMap<>();

    public void initialize(Prolog vm) {
        er1.initialize(this.vm = vm);
    }

    public boolean threadCreate(PTerm threadID, PTerm goal) {
        id = id + 1;

        if (goal == null)
            return false;

        if (goal instanceof Var)
            goal = goal.getTerm();

        EngineRunner er = new EngineRunner(id) {
            @Override
            public void run() {
                threads.set(id);
                super.run();
            }
        };
        er.initialize(vm);

        if (!vm.unify(threadID, new Int(id)))
            return false;

        er.setGoal(goal);

        synchronized (runners) {
            //synchronized (runners) {
            runners.put(id, er);
            //}
        }

        Thread t = new Thread(er);
        //addThread(id); //called in EngineRunner.run method

        t.start();
        return true;
    }

    public Solution join(int id) {
        EngineRunner er = runner(id);
        if (er == null || er.isDetached()) return null;
        /*toSPY
		 * System.out.println("Thread id "+runnerId()+" - prelevo la soluzione (join)");*/
        Solution solution = er.read();
		/*toSPY
		 * System.out.println("Soluzione: "+solution);*/
        removeRunner(id);
        return solution;
    }

    public Solution read(int id) {
        EngineRunner er = runner(id);
        if (er == null || er.isDetached()) return null;
		/*toSPY
		 * System.out.println("Thread id "+runnerId()+" - prelevo la soluzione (read) del thread di id: "+er.getId());
		 */
        Solution solution = er.read();
		/*toSPY
		 * System.out.println("Soluzione: "+solution);
		 */
        return solution;
    }

    public boolean hasNext(int id) {
        EngineRunner er = runner(id);
        return er == null || er.isDetached() ? false : er.hasOpenAlternatives();
    }

    public boolean nextSolution(int id) {
        EngineRunner er = runner(id);
        /*toSPY
         * System.out.println("Thread id "+runnerId()+" - next_solution: risveglio il thread di id: "+er.getId());
         */
        return er == null || er.isDetached() ? false : er.nextSolution();
    }

    public void detach(int id) {
        EngineRunner er = runner(id);
        if (er != null)
            er.detach();
    }

    public boolean sendMsg(int dest, PTerm msg) {
        EngineRunner er = runner(dest);
        if (er == null) return false;
        PTerm msgcopy = msg.copy(new LinkedHashMap<>(), 0);
        er.sendMsg(msgcopy);
        return true;
    }

    public boolean sendMsg(String name, PTerm msg) {
        TermQueue queue = queues.get(name);
        if (queue == null) return false;
        PTerm msgcopy = msg.copy(new LinkedHashMap<>(), 0);
        queue.store(msgcopy);
        return true;
    }

    public boolean getMsg(int id, PTerm msg) {
        EngineRunner er = runner(id);
        if (er == null) return false;
        return er.getMsg(msg);
    }

    public boolean getMsg(String name, PTerm msg) {
        EngineRunner er = runner();
        if (er == null) return false;
        TermQueue queue = queues.get(name);
        if (queue == null) return false;
        return queue.get(msg, vm, er);
    }

    public boolean waitMsg(int id, PTerm msg) {
        EngineRunner er = runner(id);
        if (er == null) return false;
        return er.waitMsg(msg);
    }

    public boolean waitMsg(String name, PTerm msg) {
        EngineRunner er = runner();
        if (er == null) return false;
        TermQueue queue = queues.get(name);
        if (queue == null) return false;
        return queue.wait(msg, vm, er);
    }

    public boolean peekMsg(int id, PTerm msg) {
        EngineRunner er = runner(id);
        if (er == null) return false;
        return er.peekMsg(msg);
    }

    public boolean peekMsg(String name, PTerm msg) {
        TermQueue queue = queues.get(name);
        if (queue == null) return false;
        return queue.peek(msg, vm);
    }

    public boolean removeMsg(int id, PTerm msg) {
        EngineRunner er = runner(id);
        if (er == null) return false;
        return er.removeMsg(msg);
    }

    public boolean removeMsg(String name, PTerm msg) {
        TermQueue queue = queues.get(name);
        if (queue == null) return false;
        return queue.remove(msg, vm);
    }

    private void removeRunner(int id) {
        runners.remove(id);
        //if (er == null) return;
        //synchronized (runners) {
            //runners.remove(id);
        //}
		/*int pid = er.getPid();
		synchronized (threads) {
			threads.set(null);
		}*/
    }

    void cut() {
        runner().cut();
    }

    ExecutionContext getCurrentContext() {
        return runner().getCurrentContext();
    }

    boolean hasOpenAlternatives() {
        return runner().hasOpenAlternatives();
    }

    boolean isHalted() {
        return runner().isHalted();
    }

    void pushSubGoal(SubGoalTree goals) {
        runner().pushSubGoal(goals);
    }

    public Solution solve(PTerm query) {
        this.clearSinfoSetOf();
        synchronized (er1) {
            er1.setGoal(query);

            //System.out.println("ENGINE MAN solve(Term) risultato: "+s);
            return er1.solve();
        }

        //return er1.solve();
    }

    public void solveEnd() {
        er1.solveEnd();
        if (!runners.isEmpty()) {
            java.util.Enumeration<EngineRunner> ers = runners.elements();
            while (ers.hasMoreElements()) {
                EngineRunner current = ers.nextElement();
                current.solveEnd();
            }
            runners.clear();
            //threads= new Hashtable<>();
            queues.clear();
            locks.clear();
            id = 0;
        }
    }

    public void solveHalt() {
        er1.solveHalt();
        if (!runners.isEmpty()) {
            java.util.Enumeration<EngineRunner> ers = runners.elements();
            while (ers.hasMoreElements()) {
                EngineRunner current = ers.nextElement();
                current.solveHalt();
            }
        }
    }

    public Solution solveNext() throws NoMoreSolutionException {
        synchronized (er1) {
            return er1.solveNext();
        }
    }

    void spy(String action, Engine env) {
        EngineRunner runner = runner();
        runner.spy(action, env);
    }

    /**
     * @return L'EngineRunner associato al thread di id specificato.
     */

    private final EngineRunner runner(int id) {
        //if (!runners.containsKey(id)) return null;
        //synchronized (runners) {
        synchronized (runners) {
            return runners.get(id);
        }
        //}
    }

    private final EngineRunner runner() {
        //int pid = (int) Thread.currentThread().getId();
		/*if(!threads.containsKey(pid))
			return er1;*/
        //synchronized(threads){
        Integer id = threads.get();
        //synchronized (runners) {
        if (id!=null) {
            return runner(id);
        } else {
            synchronized (er1) {
                return er1;
            }
        }

        //}
        //}
    }

    //Ritorna l'identificativo del thread corrente
    public int runnerId() {
        return runner().getId();
    }

    public boolean createQueue(String name) {
        //synchronized (queues) {
            queues.computeIfAbsent(name, (n) -> new TermQueue());
        //}
        return true;
    }

    public void destroyQueue(String name) {
        //synchronized (queues) {
            queues.remove(name);
        //}
    }

    public int queueSize(int id) {
        return runner(id).msgQSize();
    }

    public int queueSize(String name) {
        TermQueue q = queues.get(name);
        return q == null ? -1 : q.size();
    }

    public boolean createLock(String name) {
        //synchronized (locks) {

        locks.computeIfAbsent(name, (n) -> new ReentrantLock());

//            if (locks.containsKey(name)) return true;
//            ReentrantLock mutex = new ReentrantLock();
//            locks.put(name, mutex);
        //}
        return true;
    }

    public void destroyLock(String name) {
        //synchronized (locks) {
            locks.remove(name);
        //}
    }

    public boolean mutexLock(String name) {
        while (true) {
            ReentrantLock mutex = locks.get(name);
            if (mutex == null) {
                createLock(name);
                continue;
            } else {
                mutex.lock();
            /*toSPY
             * System.out.println("Thread id "+runnerId()+ " - mi sono impossessato del lock");
             */
                return true;
            }
        }
    }


    public boolean mutexTryLock(String name) {
        ReentrantLock mutex = locks.get(name);
        return mutex == null ? false : mutex.tryLock();
        /*toSPY
		 * System.out.println("Thread id "+runnerId()+ " - provo ad impossessarmi del lock");
		 */
    }

    public boolean mutexUnlock(String name) {
        ReentrantLock mutex = locks.get(name);
        if (mutex == null) return false;
        try {
            mutex.unlock();
			/*toSPY
			 * System.out.println("Thread id "+runnerId()+ " - Ho liberato il lock");
			 */
            return true;
        } catch (IllegalMonitorStateException e) {
            return false;
        }
    }

    public boolean isLocked(String name) {
        ReentrantLock mutex = locks.get(name);
        return mutex == null ? false : mutex.isLocked();
    }

    public void unlockAll() {
        //synchronized (locks) {
            Set<String> mutexList = locks.keySet();
            Iterator<String> it = mutexList.iterator();

            while (it.hasNext()) {
                ReentrantLock mutex = locks.get(it.next());
                boolean unlocked = false;
                while (!unlocked) {
                    try {
                        mutex.unlock();
                    } catch (IllegalMonitorStateException e) {
                        unlocked = true;
                    }
                }
            }
        //}
    }

    Engine getEnv() {
        return runner().env;
    }

    public void identify(PTerm t) {
        runner().identify(t);
    }

    public boolean getRelinkVar() {
        return this.runner().getRelinkVar();
    }

    public void setRelinkVar(boolean b) {
        this.runner().setRelinkVar(b);
    }

    public ArrayList<PTerm> getBagOFres() {
        return this.runner().getBagOFres();
    }

    public void setBagOFres(ArrayList<PTerm> l) {
        this.runner().setBagOFres(l);
    }

    public ArrayList<String> getBagOFresString() {
        return this.runner().getBagOFresString();
    }

    public void setBagOFresString(ArrayList<String> l) {
        this.runner().setBagOFresString(l);
    }

    public PTerm getBagOFvarSet() {
        return this.runner().getBagOFvarSet();
    }

    public void setBagOFvarSet(PTerm l) {
        this.runner().setBagOFvarSet(l);
    }

    public PTerm getBagOFgoal() {
        return this.runner().getBagOFgoal();
    }

    public void setBagOFgoal(PTerm l) {
        this.runner().setBagOFgoal(l);
    }

    public PTerm getBagOFbag() {
        return this.runner().getBagOFBag();
    }

    public void setBagOFbag(PTerm l) {
        this.runner().setBagOFBag(l);
    }

    public String getSetOfSolution() {
        return this.runner().getSetOfSolution();
    }

    public void setSetOfSolution(String s) {
        this.runner().setSetOfSolution(s);
    }

    public void clearSinfoSetOf() {
        this.runner().clearSinfoSetOf();
    }
}

