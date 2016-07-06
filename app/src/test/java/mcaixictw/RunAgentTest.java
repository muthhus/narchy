package mcaixictw;

import mcaixictw.worldmodels.WorldModel;
import mcaixictw.worldmodels.WorldModelSettings;
import org.junit.*;

import java.util.logging.Logger;

import static junit.framework.TestCase.assertTrue;

abstract public class RunAgentTest {

	private static Logger log = Logger.getLogger(RunAgentTest.class
			.getName());
	private Environment env;

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
		env = environment();

		WorldModelSettings modelSettings = new WorldModelSettings();
		modelSettings.setFacContextTree(true);
		modelSettings.setDepth(9);
		uctSettings.setMcSimulations(200);

		WorldModel model = WorldModel.build(name(), modelSettings, 16384);
		controller = new AIXI(env, controllerSettings, uctSettings, model);

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

		int n = 1000;
		log.info("Play " + n + " rounds against the biased coin environment");

		// A smart agent should learn to always choose the biased side and
		// should come close to an average reward of 0.7
		for (int i = 0; i < n; i++) {
			System.out.println(controller);
			controller.run(env, false);
		}
		double r = controller.averageReward();
		assertTrue(r + " avg reward", r > 0.6f);
		
	}

}
