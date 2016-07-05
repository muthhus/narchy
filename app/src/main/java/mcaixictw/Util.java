package mcaixictw;

import com.gs.collections.api.list.primitive.BooleanList;
import com.gs.collections.impl.list.mutable.primitive.BooleanArrayList;

import java.util.List;

public class Util {
	
	public static int bool2Int(boolean b) {
		return b ? 1 : 0;
	}

	public static String toString(List<Boolean> symlist) {
		String s = "";
		for (boolean b : symlist) {
			String one = b ? "1" : "0";
			s += one;
		}
		return s;
	}

	/**
	 * Return a random integer between [0, end)
	 * 
	 * @param range
	 * @return
	 */
	public static int randRange(int end) {
		return ((int) Math.round(Math.random() * end)) % end;
	}

	public static boolean randSym() {
		return Math.random() < 0.5;
	}

	/**
	 * Decodes the value encoded on the end of a list of symbols
	 * 
	 * @param symlist
	 * @return
	 */
	@Deprecated public static BooleanArrayList asBitSet(List<Boolean> symlist) {
		int l = symlist.size();

		BooleanArrayList fb = new BooleanArrayList(l);

		for (int i = 0; i < l; i++) {
			fb.add(symlist.get(i));
		}

		return fb;
	}


	/**
	 * Encodes a value onto the end of a symbol list using "bits" symbols. the
	 * most significant bit is at position 0 of the list.
	 * 
	 * @param value
	 * @param bits
	 * @return
	 */
	public static void encode(int value, int bits, BooleanArrayList target) {
		for (int i = 0; i < bits; i++) {
			boolean sym = (value & (1 << i)) > 0;
			//System.out.println(value + " bit " + i + " = " + sym + "(" + (1 << i) + ":" + (value & (i << i)) + ")");
			target.add(sym);
		}
	}
	public static BooleanArrayList encode(int value, int bits) {
		BooleanArrayList f = new BooleanArrayList(bits);
		encode(value, bits, f);
		return f;
	}

	/** input array normalized to 0..+1.0, applies simple threshold at 0.5 */
	public static void encode(float[] f, int bitsPerInput, BooleanArrayList target) {
		if (bitsPerInput!=1)
			throw new UnsupportedOperationException();

		for (int i = 0; i < f.length; i++) {
			boolean sym = f[i] > 0.5f;
			target.add(sym);
		}

	}


	public static int asInt(BooleanList b) {
		if (b.size() > 31)
			throw new UnsupportedOperationException();

		int m = 1;
		int total = 0;
		for (int i = 0; i < b.size(); i++) { //HACK avoid iterating this far
			if (b.get(i))
				total += m;
			m*=2;
		}

		return total;
	}

	/**
	 * decrease the resolution of the given value to a number with the given
	 * number of bits. the given value should be a number between 0 and 1. for
	 * example if the number of bits is 3 then 1 would be encoded as 7 (111).
	 * 
	 * @param val
	 * @param bits
	 * @return
	 */
	public static int decreaseResolution(double val, int bits) {
		assert (val <= 1.0 && val >= 0.0);
		// maximal possible value, all bits set to 1.
		int max = (1 << bits) - 1;
		return (int) Math.round(val * max);
	}

	/**
	 * returns the number of bits needed to encode the given amount of states.
	 * 
	 * @param states
	 * @return
	 */
	public static int bitsNeeded(int states) {
		assert (states > 1);
		int i, c;
		for (i = 1, c = 1; i < states; i *= 2, c++) {
		}
		assert (c - 1 > 0);
		return c - 1;
	}

	public static boolean DebugOutput = false;

}

