package nars.experiment.invaders;

import java.io.*;
import java.util.*;
public class SpaceInvaders {

	public static void main(String [] args) throws Exception{
		//showCodes();
		run();

	}
	public static void run() throws Exception{
		Memory mem = new Memory();
		
		load(mem, "invaders.h", 0);
		load(mem, "invaders.g", 2048); //0x800
		load(mem, "invaders.f", 4096); //0x1000
		load(mem, "invaders.e", 6144); //0x1800
		
		
		CPU cpu = new CPU(mem);
		cpu.run();
		
	}
	
	public static void showCodes() throws Exception{// allows us to see valid ROM hex codes 
		ArrayList<String> arr = new ArrayList<String>();
		InputStream is = new FileInputStream("invaders.rom");
		int x = is.read();
		arr.add(toHexString((byte)x));
		while(x!=-1){
			x= is.read();
			arr.add(toHexString((byte)x));
		}
		
		System.out.println(Arrays.toString(arr.toArray()));
//		Disassembler ds = new Disassembler();
//		x = 0;
//		while(x<arr.size()){
//			x +=ds.getCode(arr, x);
//		}
		
	}
	public static void load(Memory mem, String filename, int beginIndex) throws Exception{
		InputStream is = SpaceInvaders.class.getResourceAsStream(filename);
		int x = is.read();
		mem.addMem(x, beginIndex);
		while(x != -1){
			x = is.read();
			if(x==-1){
				return;
			}
			else{
				beginIndex+=1;
				mem.addMem(x, beginIndex);
			}
		}
		is.close();
	}
	public static String toHexString(byte b) {
	    return String.format("%02X", b);
	} 
}
