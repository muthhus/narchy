package mcaixictw;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import mcaixictw.worldmodels.WorldModelSettings;
import mcaixictw.worldmodels.WorldModel;

abstract public class RunAgentTest {

	private static Logger log = Logger.getLogger(RunAgentTest.class
			.getName());

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {

		// set up the biased coin environment. The coin lands on one side with a
		// probability of 0.7.
		Environment env = environment();

		WorldModelSettings modelSettings = new WorldModelSettings();
		modelSettings.setFacContextTree(true);
		modelSettings.setDepth(3);
		log.info("depth: " + modelSettings.getDepth());
		log.info("create new model");
		WorldModel model = WorldModel.getInstance(name(), modelSettings);
		controller = new AIXI(env, controllerSettings, uctSettings,
				model);

	}

	protected abstract String name();

	abstract public Environment environment();

	private AIXI controller;

	private ControllerSettings controllerSettings = new ControllerSettings();
	private UCTSettings uctSettings = new UCTSettings();

	@After
	public void tearDown() throws Exception {

	}

	@Test
	public final void test() {

		int n = 10000;
		log.info("Play " + n + " rounds against the biased coin environment");

		// A smart agent should learn to always choose the biased side and
		// should come close to an average reward of 0.7
		controller.play(n, false);
		
	}

}
