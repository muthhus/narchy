package nars.inter.gnutella;
import java.util.Random;

/**
 * Clase que generara ids.
 * 
 * @author Ismael Fernández
 * @author Miguel Vilchis
 * @version 1.0
 * 
 */

public class IdGenerator {

	private int counter;

	public IdGenerator() {
		counter = 0;
	}

	/**
	 * Devuelve el siguienete id disponible de este objeto IdGenerator, los ids
	 * son generados secuencialmente.
	 * 
	 * @return el id
	 */
	public synchronized int nextId() {
		++counter;
		return counter;
	}

	/**
	 * Devuelve un id para un Servent generado aleatoriamente
	 * 
	 * @return el id
	 */
	public static byte[] getIdServent() {
		byte id[] = new byte[GnutellaConstants.ID_LENGTH];
		Random r = new Random();
		r.nextBytes(id);
		return id;

	}

	/**
	 * Genera un id random y lo guarda en un arreglo de bytes de longitud 16,
	 * para los mensajes del Protocolo Gnutella
	 * 
	 * @return el id
	 */
	public synchronized static byte[] getIdMessage() {
		byte id[] = new byte[GnutellaConstants.ID_LENGTH];
		Random r = new Random();
		r.nextBytes(id);
		return id;
	}

}
