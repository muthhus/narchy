package alice.tuprologx.runtime.tcp;
import alice.tuprolog.*;
import  java.io.*;
import  java.net.*;

public class Proxy implements alice.tuprologx.runtime.tcp.Prolog {

    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream  in;

    public Proxy(String host) throws IOException  {
        socket=new Socket(host,alice.tuprologx.runtime.tcp.Daemon.DEFAULT_PORT);
        out=new ObjectOutputStream(socket.getOutputStream());
        in=new ObjectInputStream(socket.getInputStream());
    }

    public Proxy(String host, int port) throws IOException  {
        socket=new Socket(host,port);
        out=new ObjectOutputStream(socket.getOutputStream());
        in=new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void clearTheory() throws Exception {
        out.writeObject(new NetMsg("clearTheory"));
        out.flush();
    }

    @Override
    public Theory getTheory() throws Exception {
        out.writeObject(new NetMsg("getTheory"));
        out.flush();
        Boolean b=(Boolean)in.readObject();
        if (b.booleanValue()){
            Theory th=(Theory)in.readObject();
            return th;
        }
        return null;
    }

    @Override
    public void setTheory(Theory th) throws Exception {
        out.writeObject(new NetMsg("setTheory"));
        out.writeObject(th);
        out.flush();
        Boolean b=(Boolean)in.readObject();
        if (!b.booleanValue()){
            throw new InvalidTheoryException();
        }
    }

    @Override
    public void addTheory(Theory th) throws Exception {
        out.writeObject(new NetMsg("addTheory"));
        out.writeObject(th);
        out.flush();
        Boolean b=(Boolean)in.readObject();
        if (!b.booleanValue()){
            throw new InvalidTheoryException();
        }
    }


    @Override
    public SolveInfo solve(String st) throws Exception {
        out.writeObject(new NetMsg("solveString"));
        out.writeObject(st);
        out.flush();
        Boolean b=(Boolean)in.readObject();
        if (b.booleanValue()){
            SolveInfo info=(SolveInfo)in.readObject();
            return info;
        } else {
            throw new MalformedGoalException();
        }
    }

    @Override
    public SolveInfo solve(PTerm term) throws Exception {
        out.writeObject(new NetMsg("solveTerm"));
        out.writeObject(term);
        out.flush();
        Boolean b=(Boolean)in.readObject();
        if (b.booleanValue()){
            SolveInfo info=(SolveInfo)in.readObject();
            return info;
        } else {
            throw new MalformedGoalException();
        }
    }

    @Override
    public SolveInfo solveNext() throws Exception {
        out.writeObject(new NetMsg("solveNext"));
        out.flush();
        Boolean b=(Boolean)in.readObject();
        if (b.booleanValue()){
            SolveInfo info=(SolveInfo)in.readObject();
            return info;
        } else {
            throw new NoSolutionException();
        }
    }

    @Override
    public boolean hasOpenAlternatives() throws Exception {
        out.writeObject(new NetMsg("hasOpenAlternatives"));
        out.flush();
        Boolean b=(Boolean)in.readObject();
        return b.booleanValue();
    }

    @Override
    public void solveHalt() throws Exception {
        out.writeObject(new NetMsg("solveHalt"));
        out.flush();
    }

    @Override
    public void solveEnd()  throws Exception {
        out.writeObject(new NetMsg("solveEnd"));
        out.flush();
    }



    @Override
    public void loadLibrary(String st) throws Exception{
        out.writeObject(new NetMsg("loadLibrary"));
        out.writeObject(st);
        out.flush();
        Boolean b=(Boolean)in.readObject();
        if (!b.booleanValue()){
            throw new InvalidLibraryException();
        }
    }

    @Override
    public void unloadLibrary(String st) throws Exception {
        out.writeObject(new NetMsg("unloadLibrary"));
        out.writeObject(st);
        out.flush();
        Boolean b=(Boolean)in.readObject();
        if (!b.booleanValue()){
            throw new InvalidLibraryException();
        }
    }
}
