package nars.experiment.invaders;

import java.util.*;

public class Memory {
	public int a, b,c,d,e,h,l; //8 bit registers
	public int int_enable;
	public int sp, pc; //16 bit registers	
	private int[] mem;	
	boolean cy, p, s, z, ac; //flags
	public Memory(){
		mem = new int[16000];
	}
	public void addMem(int x, int pos){
		mem[pos] = x;
	}
	
	public int[] getMem(){
		return mem;
	}
}
