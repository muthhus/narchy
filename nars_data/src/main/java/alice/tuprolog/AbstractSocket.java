package alice.tuprolog;
import java.net.InetAddress;
import java.util.AbstractMap;
import java.util.ArrayList;

public abstract class AbstractSocket extends PTerm {
	private static final long serialVersionUID = 1L;
	public abstract boolean isClientSocket();
	
	public abstract boolean isServerSocket();
	
	public abstract boolean isDatagramSocket();
	
	public abstract Object getSocket();
	
	public abstract InetAddress getAddress();
	
	@Override
	public boolean isNumber() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isStruct() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isVar() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isEmptyList() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAtomic() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCompound() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAtom() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isList() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isGround() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isGreater(PTerm t) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isGreaterRelink(PTerm t, ArrayList<String> vorder) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isEqual(PTerm t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public PTerm getTerm() {
		return this;
	}

	@Override
	public void free() {
		// TODO Auto-generated method stub
		
	}

	@Override
	long resolveTerm(long count) {
		return count;
	}

	@Override
	PTerm copy(AbstractMap<Var, Var> vMap, int idExecCtx) {
		return this;
	}

	@Override
	PTerm copy(AbstractMap<Var, Var> vMap, AbstractMap<PTerm, Var> substMap) {
		return this;
	}



	@Override
	public void accept(TermVisitor tv) {
		// TODO Auto-generated method stub
		
	}

}


