package mcaixictw;


import com.gs.collections.api.list.primitive.BooleanList;
import com.gs.collections.impl.list.mutable.primitive.BooleanArrayList;

import java.util.ArrayList;

public enum Bits {
	;


	//TODO make these immutable/read-only
	public final static BooleanList one = new BooleanArrayList(1).with(true);
	public final static BooleanList zero = new BooleanArrayList(1).with(false);


	public static BooleanArrayList rand() {
		return rand(1);
	}

	public static BooleanArrayList rand(int length) {
		BooleanArrayList f = new BooleanArrayList(length);
		for (int i = 0; i < length; i++)
			f.add(Util.randSym());
		return f;
	}
}