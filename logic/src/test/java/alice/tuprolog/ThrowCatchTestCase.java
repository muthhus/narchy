package alice.tuprolog;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Matteo Iuliani
 * 
 *         Test del funzionamento dei predicati throw/1 e catch/3
 */
public class ThrowCatchTestCase {

	// verifico che handler venga eseguito con le sostituzioni effettuate
	// nell'unificazione
	@Test
	public void test_catch_3_1() throws Exception {
		Prolog engine = new Prolog();
		String theory = "p(0) :- p(1). p(1) :- throw(error).";
		engine.setTheory(new Theory(theory));
		String goal = "atom_length(err, 3), catch(p(0), E, (atom_length(E, Length), X is 2+3)), Y is X+5.";
		Solution info = engine.solve(goal);
		assertTrue(info.isSuccess());
		Struct e = (Struct) info.getTerm("E");
		assertTrue(e.isEqual(new Struct("error")));
		Int length = (Int) info.getTerm("Length");
        assertEquals(5, length.intValue());
		Int x = (Int) info.getTerm("X");
        assertEquals(5, x.intValue());
		Int y = (Int) info.getTerm("Y");
        assertEquals(10, y.intValue());
	}

	// verifico che venga eseguito il piu' vicino antenato catch/3 nell'albero di
	// risoluzione il cui secondo argomento unifica con l'argomento di throw/1
	@Test public void test_catch_3_2() throws Exception {
		Prolog engine = new Prolog();
		String theory = "p(0) :- throw(error). p(1).";
		engine.setTheory(new Theory(theory));
		String goal = "catch(p(1), E, fail), catch(p(0), E, atom_length(E, Length)).";
		Solution info = engine.solve(goal);
		assertTrue(info.isSuccess());
		Struct e = (Struct) info.getTerm("E");
		assertTrue(e.isEqual(new Struct("error")));
		Int length = (Int) info.getTerm("Length");
        assertEquals(5, length.intValue());
	}

	// verifico che l'esecuzione fallisce se si verifica un errore durante
	// l'esecuzione di un goal e non viene trovato nessun nodo catch/3 il cui
	// secondo argomento unifica con l'argomento dell'eccezione lanciata
	@Test public void test_catch_3_3() throws Exception {
		Prolog engine = new Prolog();
		String theory = "p(0) :- throw(error).";
		engine.setTheory(new Theory(theory));
		String goal = "catch(p(0), error(X), true).";
		Solution info = engine.solve(goal);
		assertFalse(info.isSuccess());
		assertTrue(info.isHalted());
	}

	// verifico che catch/3 fallisce se Handler e' falso
	@Test public void test_catch_3_4() throws Exception {
		Prolog engine = new Prolog();
		String theory = "p(0) :- throw(error).";
		engine.setTheory(new Theory(theory));
		String goal = "catch(p(0), E, E == err).";
		Solution info = engine.solve(goal);
		assertFalse(info.isSuccess());
	}

	// verifico che un Goal non deterministico venga rieseguito, e nel caso
	// venga lanciata una eccezione durante l'esecuzione di Goal allora tutti i
	// punti di scelta devono essere tagliati e Handler deve essere eseguito una
	// sola volta
	@Test public void test_catch_3_5() throws Exception {
		Prolog engine = new Prolog();
		String theory = "p(0). p(1) :- throw(error). p(2).";
		engine.setTheory(new Theory(theory));
		String goal = "catch(p(X), E, E == error).";
		Solution info = engine.solve(goal);
		assertTrue(info.isSuccess());
		assertTrue(info.hasOpenAlternatives());
		info = engine.solveNext();
		assertTrue(info.isSuccess());
		assertFalse(info.hasOpenAlternatives());
	}

	// verifico che catch/3 fallisce se si verifica un'eccezione durante Handler
	@Test public void test_catch_3_6() throws Exception {
		Prolog engine = new Prolog();
		String theory = "p(0) :- throw(error).";
		engine.setTheory(new Theory(theory));
		String goal = "catch(p(0), E, throw(err)).";
		Solution info = engine.solve(goal);
		assertFalse(info.isSuccess());
		assertTrue(info.isHalted());
	}

}