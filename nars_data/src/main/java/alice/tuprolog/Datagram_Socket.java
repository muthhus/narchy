package alice.tuprolog;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

public class Datagram_Socket extends AbstractSocket {
	private static final long serialVersionUID = 1L;

	private final DatagramSocket socket;

	
	public Datagram_Socket(DatagramSocket socket) {
		super();
		this.socket = socket;
	}

	@Override
	public boolean isClientSocket() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isServerSocket() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDatagramSocket() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public DatagramSocket getSocket() {
		// TODO Auto-generated method stub
		return socket;
	}

	@Override
	public InetAddress getAddress() {
		return socket.isBound() ? socket.getInetAddress() : null;
	}

	@Override
	boolean unify(List<Var> varsUnifiedArg1, List<Var> varsUnifiedArg2, PTerm t) {
		t = t.getTerm();
        if (t instanceof Var) {
            return t.unify(varsUnifiedArg1, varsUnifiedArg2, this);
        } else if (t instanceof AbstractSocket && ((AbstractSocket) t).isDatagramSocket()) {
        	InetAddress addr= ((AbstractSocket) t).getAddress();
            return socket.getInetAddress().toString().equals(addr.toString());
        } else {
            return false;
        }
	}
	
	@Override
	public String toString(){
		return socket.toString();
	}

}
