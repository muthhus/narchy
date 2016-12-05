package cleargl;

import com.jogamp.opengl.GLException;

public interface GLCloseable extends AutoCloseable {
	@Override
	public void close() throws GLException;
}
