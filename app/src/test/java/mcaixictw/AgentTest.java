package mcaixictw;

import com.gs.collections.impl.list.mutable.primitive.BooleanArrayList;
import mcaixictw.worldmodels.WorldModel;
import mcaixictw.worldmodels.WorldModelSettings;
import org.junit.*;

import static mcaixictw.Util.asInt;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

abstract public class AgentTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {

		// 5, 5, false, 3, 8, 3

		int actions = 5;
		modelSettings = new WorldModelSettings();
		modelSettings.setDepth(5);

		WorldModel model = WorldModel.build("AgentTestModel",
				modelSettings, 16384);

		agent = new AIXIModel(actions, obsBits, rewBits, model);
	}

	AIXIModel agent;
	double eps = 0.01;
	int obsBits = 8;
	int rewBits = 3;
	WorldModelSettings modelSettings;

	@After
	public void tearDown() throws Exception {
	}

	// Encodes a percept (observation, reward) as a list of symbols
	@Test
	public final void testEncodePercept() {

		int observation = 165;
		int reward = 4;

		BooleanArrayList result = Util.encode(observation, obsBits);

		// 10100101101
		assertTrue(result.get(0) == true);
		assertTrue(result.get(1) == false);
		assertTrue(result.get(2) == true);
		assertTrue(result.get(3) == false);
		assertTrue(result.get(4) == false);
		assertTrue(result.get(5) == true);
		assertTrue(result.get(6) == false);
		assertTrue(result.get(7) == true);

		BooleanArrayList rb = Util.encode(reward, rewBits);
		assertTrue(rb.get(0) == true);
		assertTrue(rb.get(1) == false);
		assertTrue(rb.get(2) == false);

		result = agent.encodePercept(observation, reward);
		// 10100101101
		assertTrue(result.get(0));
		assertFalse(result.get(1));
		assertTrue(result.get(2) == true);
		assertTrue(result.get(3) == false);
		assertTrue(result.get(4) == false);
		assertTrue(result.get(5) == true);
		assertTrue(result.get(6) == false);
		assertTrue(result.get(7) == true);
		assertTrue(result.get(8) == true);
		assertTrue(result.get(9) == false);
		assertTrue(result.get(10) == false);
	}

	@Test
	public final void testGenPerceptAndUpdate() {
		System.out.println(modelSettings);
		System.out.println("history size: " + agent.getModel().historySize());
		agent.modelUpdate(0,0);
		act(0);
		agent.genPerceptAndUpdate();
	}

	@Test
	public final void testGetLastPercept() {
		int obs = 77;
		int rew = 2;
		int perception = (obs << rewBits) | rew;
		agent.modelUpdate(obs, rew);
		int lastPercept = asInt(agent.getLastPercept());
		assertTrue(perception == lastPercept);
		agent.genRandomActionAndUpdate();
		lastPercept = asInt(agent.getLastPercept());
		assertTrue(perception == lastPercept);
	}

	@Test
	public final void testGetLastAction() {
		agent.modelUpdate(0,0);
		act(0);
		agent.genPerceptAndUpdate();
		int a = asInt(agent.genRandomActionAndUpdate());
		assertTrue(a == asInt(agent.getLastAction()));
		agent.genPerceptAndUpdate();
		assertTrue(a == asInt(agent.getLastAction()));
	}

	@Test
	public final void testModelRevert() {
		agent.modelUpdate(0,0);
		int obs = 1;
		int rew = 1;
		act(0);
		double p = agent.perceptProbability(obs, rew);
		ModelUndo mu = new ModelUndo(agent);

		for (int i = 0; i < 10; i++) {
			agent.genPerceptAndUpdate();
			agent.genRandomActionAndUpdate();
		}
		agent.modelRevert(mu);
		double p2 = agent.perceptProbability(obs, rew);

		// System.out.println("p: " + p + " p2: " + p2 + " p2-p: " + (p2 - p));

		assertTrue(Math.abs(p2 - p) < eps);
	}

	@Test
	public final void testHistoryRevert() {
		agent.modelUpdate(0,0);
		act(0);
		int obs = 1;
		int rew = 1;
		double p = agent.perceptProbability(obs, rew);
		ModelUndo mu = new ModelUndo(agent);
		for (int i = 0; i < 10; i++) {
			agent.genPerceptAndUpdateHistory();
			agent.genRandomActionAndUpdate();
		}
		agent.historyRevert(mu);
		assertTrue(agent.perceptProbability(obs, rew) - p < eps);
	}

	public void act(int a) {
		agent.modelUpdate(Util.encode(a, agent.getActionBits()));
	}
}
