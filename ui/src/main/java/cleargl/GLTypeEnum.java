package cleargl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;

public enum GLTypeEnum {
	Byte(GL.GL_BYTE),
	UnsignedByte(GL.GL_UNSIGNED_BYTE),
	Short(GL.GL_SHORT),
	UnsignedShort(GL.GL_UNSIGNED_SHORT),
	Int(GL2ES2.GL_INT),
	UnsignedInt(GL.GL_UNSIGNED_INT),
//	Long(-1),
//	UnsignedLong(-1),
//	HalfFloat(-1),
	Float(GL.GL_FLOAT),
	Double(-1);

	private final int type;

	private GLTypeEnum(final int glType) {
		this.type = glType;
	}

	public int glType() {
		return type;
	}
}
