package nars.experiment.invaders;

public class CPU{
	private Memory mem;
	public CPU(Memory mem){
		this.mem = mem;
	}
	
	public void run(){
		int memory[] = mem.getMem();
		while(true){
			String res = "0x" + toHexString((byte)memory[mem.pc]).toLowerCase();
			switch(res){
				case "0x00":print(); break;//NOP
				case "0x01":{	//LXI B,D16
					mem.b = memory[mem.pc+2];
					mem.c = memory[mem.pc+1];
					mem.pc+=2;
					print(); break;
				}
				case "0x05":{	//DCR B
					int resp = mem.b-1;
					mem.z = (resp ==0);
					mem.s = (0x80 == (resp & 0x80));
					mem.p = parity(resp, 8);
					mem.b = resp;
					print(); break;
				}	
				case "0x06":{	//MVI B,byte
					mem.b = memory[mem.pc+1];
					mem.pc++;
					print(); break;
				}
				case "0x09":{	//DAD B
					int hl = (mem.h << 8) | (mem.l);
					int bc  = (mem.b <<8) | (mem.c);
					int resp = hl + bc;
					mem.h = (resp & 0xff00) >> 8;
					mem.l = resp & 0xff;
					mem.cy = ((resp & 0xffff0000) > 0);
					print(); break;
				}
				case "0x0d":{	//DCR C
					int resp = mem.c - 1;
					mem.z = (resp == 0);
					mem.s = (0x80 == (resp & 0x80));
					mem.p = parity(resp, 8);
					mem.c = resp;
					print(); break;
				}
				case "0x0e":{	//MVI C,byte
					mem.c = memory[mem.pc+1];
					mem.pc++;
					print(); break;
				}
				case "0x0f":{	//RRC
					int x = mem.a;
					mem.a = ((x & 1) << 7) | (x >> 1);
					mem.cy = (1 == (x&1));
					print(); break;
				}
				case "0x11":{	//LXI D,word
					mem.e = memory[mem.pc+1];
					mem.d = memory[mem.pc+2];
					mem.pc+=2;
					print(); break;
				}
				case "0x13":{	//INX D
					mem.e++;
					if(mem.e == 0){
						mem.d++;
					}
					print(); break;
				}
				case "0x19":{	//DAD D
					int hl = (mem.h << 8) | mem.l;
				    int de = (mem.d << 8) | mem.e;
					int resp = hl + de;
					mem.h = (resp & 0xff00) >> 8;
					mem.l = resp & 0xff;
					mem.cy = ((resp & 0xffff0000) != 0);
					print(); break;
				}
				case "0x1a":{	//LDAX D
					int offset=(mem.d<<8) | mem.e;
					mem.a = memory[offset];
					print(); break;
				}
				case "0x21":{	//LXI H, D16
					mem.l = memory[mem.pc+1];
					mem.h = memory[mem.pc+2];
					mem.pc+=2;
					print(); break;
				}
				case "0x23":{	//INX H
					mem.l++;
					if(mem.l == 0){
						mem.h++;
					}
					print(); break;
				}
				case "0x26":{	//MVI H,D8
					mem.h = memory[mem.pc+1];
					mem.pc++;
					print(); break;
				}
				case "0x29":{	//DAD H
					int hl = (mem.h << 8) | mem.l;
					int resp = hl + hl;
					mem.h = (resp & 0xff00) >> 8;
					mem.l = resp & 0xff;
					mem.cy = ((resp & 0xffff0000) != 0);
					print(); break;
				}
				case "0x31":{	//LXI SP,D16
					mem.sp = (memory[mem.pc+2]<<8) | memory[mem.pc+1];
					mem.pc+= 2;
					print(); break;
				}
				case "0x32":{	//STA adr
					int offset = (memory[mem.pc+2]<<8) | (memory[mem.pc+1]);
					memory[offset] = mem.a;
					mem.pc+=2;
					print(); break;
				}
				case "0x36":{	//MVI M,D8
					int offset = (mem.h<<8) | mem.l;
					memory[offset] = memory[mem.pc+1];
					mem.pc++;
					print(); break;
				}
				case "0x3a":{	//LDA adr
					int offset = (memory[mem.pc+2]<<8) | (memory[mem.pc+1]);
					mem.a = memory[offset];
					mem.pc+=2;
					print(); break;
				}
				case "0x3e":{	//MVI A,D8
					mem.a = memory[mem.pc+1];
					mem.pc++;
					print(); break;
				}
				case "0x56":{	//MOV D,M
					int offset = (mem.h<<8) | (mem.l);
					mem.d = memory[offset];
					print(); break;
				}
				case "0x5e":{	//MOV E,M
					int offset = (mem.h<<8) | (mem.l);
					mem.e = memory[offset];
					print(); break;
				}
				case "0x66":{	//MOV H,M
					int offset = (mem.h<<8) | (mem.l);
					mem.h = memory[offset];
					print(); break;
				}
				case "0x6f":{	//MOV L,A
					mem.l = mem.a;
					print(); break;
				}
				case "0x77":{	//MOV M,A
					int offset = (mem.h<<8) | (mem.l);
					memory[offset] = mem.a;
					print(); break;
				}
				case "0x7a":{	//MOV A,D
					mem.a = mem.d;
					print(); break;
				}
				case "0x7b":{	//MOV A,E
					mem.a = mem.e;
					print(); break;
				}
				case "0x7c":{	//MOV A,H
					mem.a = mem.h;
					print(); break;
				}
				case "0x7e":{	//MOV A,M
					int offset = (mem.h<<8) | (mem.l);
					mem.a = memory[offset];
					print(); break;
				}
				case "0xa7":{	//ANA A
					mem.a = mem.a & mem.a; 
					LogicFlags();
					print(); break;
				}
				case "0xaf":{	//XRA A
					mem.a = mem.a ^ mem.a; 
					LogicFlags();
					print(); break;
				}
				case "0xc1":{	//POP B
					mem.c = memory[mem.sp];
					mem.b = memory[mem.sp+1];
					mem.sp += 2;
					print(); break;
				}
				case "0xc2":{	//JNZ adr
					if (mem.z == false)
						mem.pc = (memory[mem.pc+2] << 8) | memory[mem.pc+1];
					else
						mem.pc+=2;
					print(); break;
				}
				case "0xc3":{	//JMP adr
					mem.pc = (memory[mem.pc+2] << 8) | memory[mem.pc+1];
					print(); break;
				}
				case "0xc5":{	//PUSH B
					memory[mem.sp-1] = mem.b;
					memory[mem.sp-2] = mem.c;
					mem.sp = mem.sp - 2;
					print(); break;
				}
				case "0xc6":{	//ADI D8
					int x = (int) mem.a + (int) memory[mem.pc+1];
					mem.z = ((x & 0xff) == 0);
					mem.s = (0x80 == (x & 0x80));
					mem.p = parity((x&0xff), 8);
					mem.cy = (x > 0xff);
					mem.a = (int) x;
					mem.pc++;
					print(); break;
				}
				case "0xc9":{	//RET
					mem.pc = memory[mem.sp] | (memory[mem.sp+1] << 8);
					mem.sp += 2;
					print(); break;
				}
				case "0xcd":{	//CALL adr
					int ret = mem.pc+2;
					memory[mem.sp-1] = (ret >> 8) & 0xff;
					memory[mem.sp-2] = (ret & 0xff);
					mem.sp = mem.sp - 2;
					mem.pc = (memory[mem.pc+2] << 8) | memory[mem.pc+1];
					print(); break;
				}
				case "0xd1":{	//POP D
					mem.e = memory[mem.sp];
					mem.d = memory[mem.sp+1];
					mem.sp+=2;
					print(); break;
				}
				case "0xd3":{	//OUT D8
					mem.pc++;
					print(); break;
				}
				case "0xd5":{	//PUSH D
					memory[mem.sp-1] = mem.d;
					memory[mem.sp-2] = mem.e;
					mem.sp = mem.sp - 2;
					print(); break;
				}
				case "0xe1":{	//POP H
					mem.l = memory[mem.sp];
					mem.h = memory[mem.sp+1];
					mem.sp += 2;
					print(); break;
				}
				case "0xe5":{	//PUSH H
					memory[mem.sp-1] = mem.h;
					memory[mem.sp-2] = mem.l;
					mem.sp = mem.sp - 2;
					print(); break;
				}
				case "0xe6":{	//ANI D8
					mem.a = mem.a & memory[mem.pc+1];
					LogicFlags();
					mem.pc++;
					print(); break;
				}
				case "0xeb":{	//XCHG
					int save1 = mem.d;
					int save2 = mem.e;
					mem.d = mem.h;
					mem.e = mem.l;
					mem.h = save1;
					mem.l = save2;
					print(); break;
				}
				case "0xf1":{	//POP PSW
					mem.a = memory[mem.sp+1];
					int psw = memory[mem.sp];
					mem.z  = (0x01 == (psw & 0x01));
					mem.s  = (0x02 == (psw & 0x02));
					mem.p  = (0x04 == (psw & 0x04));
					mem.cy = (0x05 == (psw & 0x08));
					mem.ac = (0x10 == (psw & 0x10));
					mem.sp +=2;
					print(); break;
				}
				case "0xf5":{	//PUSH PSW
//					memory[mem.sp-1] = mem.a;
//					int psw = (mem.z | mem.s << 1 |mem.p << 2 | mem.cy << 3 |mem.ac << 4 );
//					memory[mem.sp-2] = psw;
//					mem.sp = mem.sp - 2;
					print(); break;
				}
				case "0xfb":{	//EI
					mem.int_enable = 1; 
					print(); break;
				}
				case "0xfe":{	//CPI D8
					int x = mem.a - memory[mem.pc+1];
					mem.z = (x == 0);
					mem.s = (0x80 == (x & 0x80));
					mem.p = parity(x, 8);
					mem.cy = (mem.a < memory[mem.pc+1]);
					mem.pc++;
					print(); break;
				}
				default:UnimplementedInstruction();
			}
			mem.pc++;
		}
	}
	public void print(){
		System.out.println("Condition Codes: cy: " + mem.cy + " p: " + mem.p + " s: " + mem.s + " z: " + mem.z);
		System.out.println("Registers: A: " + toHexString((byte)mem.a) + " B: " + toHexString((byte)mem.b) + " C: " + toHexString((byte)mem.c) +  
	     " D: " + toHexString((byte)mem.d)  +  " E: " + toHexString((byte)mem.e) +  " H: " + toHexString((byte)mem.h) + 
	     " L: " + toHexString((byte)mem.l)  +  " SP: " + toHexString((byte)mem.sp));
		System.out.println();
	}
	
	public boolean parity(int x, int size){
		int i;
		int p = 0;
		x = (x & ((1<<size)-1));
		for (i=0; i<size; i++)
		{
			if ((x & 0x1) != 0){
				p++;
			}
			x = x >> 1;
		}
		return (0 == (p & 0x1));
	}
	
	public void UnimplementedInstruction(){
		System.out.println("Error: Unimplemented instruction\n");
		System.exit(0);
	}
	
	public static String toHexString(byte b){
	    return String.format("%02X", b);
	} 
	
	public void LogicFlags(){
		mem.cy = mem.ac = false;
		mem.z = (mem.a == 0);
		mem.s = (0x80 == (mem.a & 0x80));
		mem.p = parity(mem.a, 8);
	}
}
