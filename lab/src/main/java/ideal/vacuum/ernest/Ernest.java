package ideal.vacuum.ernest;

import ideal.vacuum.eca.ActInstance;
import ideal.vacuum.eca.Primitive;
import ideal.vacuum.eca.PrimitiveImpl;
import ideal.vacuum.eca.construct.egomem.AreaImpl;
import ideal.vacuum.eca.decider.Decider;
import ideal.vacuum.eca.decider.DeciderImpl;
import ideal.vacuum.eca.spas.Placeable;
import ideal.vacuum.eca.spas.Spas;
import ideal.vacuum.eca.spas.SpasImpl;
import ideal.vacuum.eca.ss.IImos;
import ideal.vacuum.eca.ss.Imos;
import ideal.vacuum.eca.ss.enaction.Act;
import ideal.vacuum.eca.ss.enaction.ActImpl;
import ideal.vacuum.eca.ss.enaction.Enaction;
import ideal.vacuum.eca.ss.enaction.EnactionImpl;
import ideal.vacuum.tracing.ITracer;
import spacegraph.math.v3;
import spacegraph.phys.math.Transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * The main Ernest class used to create an Ernest agent in the environment.
 * @author ogeorgeon
 */
public class Ernest implements IErnest
{
	/** A big value that can represent infinite for diverse purpose. */	
	public static final int INFINITE = 1000 ;//* INT_FACTOR;

	/** Color unanimated */
	public static int UNANIMATED_COLOR = 0x808080;

	/** Ernest's current enaction */
	private Enaction m_enaction = new EnactionImpl();
	
	/** Ernest's spatial system. */
	private final Spas m_spas = new SpasImpl();

	/** Ernest's Intrinsically motivated Schema Mechanism. */
	private final IImos m_imos = new Imos();
	
	/** Ernest's tracing system. */
	private ITracer m_tracer;
	
	private static int clock;
	
	private final Transform transformToAnim = new Transform();
	
	/** Ernest's decisional Mechanism. */
	private final Decider m_decider = new DeciderImpl(m_imos, m_spas); // Regular decider for Ernest 7.
	
	/**
	 * Set Ernest's fundamental learning parameters.
	 * @param regularityThreshold The Regularity Sensibility Threshold.
	 * @param maxSchemaLength The Maximum Schema Length
	 */
	@Override
    public void setParameters(int regularityThreshold, int maxSchemaLength)
	{
		m_imos.setRegularityThreshold(regularityThreshold);
		m_decider.setRegularityThreshold(regularityThreshold);
		m_decider.setMaxSchemaLength(maxSchemaLength);
	}

	/**
	 * Let the environment set the tracer.
	 * @param tracer The tracer.
	 */
	@Override
    public void setTracer(ITracer tracer)
	{ 
		m_tracer = tracer;
		m_imos.setTracer(m_tracer); 
		m_spas.setTracer(m_tracer);
		m_decider.setTracer(m_tracer);
	}

	@Override
    public String step(Effect input)
	{
		
		// Trace a new interaction cycle.
		if (m_tracer != null){
            //m_tracer.startNewEvent(m_imos.getCounter());
			//m_tracer.addEventElement("clock", m_imos.getCounter() + "");
            m_tracer.startNewEvent(clock);
			m_tracer.addEventElement("clock", clock + "");
			input.trace(m_tracer);		
		}                

		clock++;

		// track the enaction 
		
		m_enaction.track(input);
		m_imos.track(m_enaction);
		m_spas.track(m_enaction);			
		m_enaction.traceTrack(m_tracer);

		
		// Decision cycle
		if (m_enaction.isOver()){
			m_imos.terminate(m_enaction);
			m_enaction = m_decider.decide(m_enaction);
		}

		// Carry out the current enaction
		
		m_decider.carry(m_enaction);
		
		return m_enaction.getIntendedPrimitiveAct().getLabel();		
	}
	
	@Override
    public Primitive step(List<ActInstance> actInstances, Transform transform){
		
		// Trace a new interaction cycle.
		if (m_tracer != null){
            m_tracer.startNewEvent(clock);
			m_tracer.addEventElement("clock", clock + "");

			Object ep = m_tracer.addEventElement("enacted_places");
			for (ActInstance p : actInstances){
				p.trace(m_tracer, ep);
			}
		}                

		clock++;

		// track the enaction 
		
		this.transformToAnim.set(transform);
		m_enaction.track(actInstances, transform, this.m_spas.getFocusPhenomenonInstance());
		m_imos.track(m_enaction);
		m_spas.track(m_enaction);			
		m_enaction.traceTrack(m_tracer);

		
		// Decision cycle
		if (m_enaction.isOver()){
			m_imos.terminate(m_enaction);
			m_enaction = m_decider.decide(m_enaction);
		}

		// Carry out the current enaction
		
		m_decider.carry(m_enaction);
		
		return m_enaction.getIntendedPrimitiveAct().getPrimitive();		
		
	}
	
	@Override
    public int getDisplayCode(){
		return this.m_spas.getDisplayCode();
	}

	@Override
    public Primitive addInteraction(String label, int value)
	{
		Primitive primitive = PrimitiveImpl.createOrGet(label, value * 10);
		
		Act act = ActImpl.createOrGetPrimitiveAct(primitive, AreaImpl.createOrGet(new v3(0,0,0)));
		//Action action = ActionImpl.createOrGet("[a" + act.getLabel() + "]");
		//action.addAct(act);
		return primitive;
	}

	@Override
    public ArrayList<Placeable> getPlaceList()
	{
		return m_spas.getPlaceableClones();
	}

	@Override
    public int getClock(){
		return clock;
	}

	@Override
    public int getUpdateCount(){
		return clock;
	}

	@Override
    public Collection<Primitive> getPrimitives()
	{
		return PrimitiveImpl.getINTERACTIONS();
	}
	
	/**
	 * Get a description of Ernest's internal state (to display in the environment).
	 * @return A representation of Ernest's internal state
	 */
	@Override
    public String internalState()
	{
		return ""; //m_imos.getInternalState();
	}

	@Override
    public Transform getTransformToAnim() {
		return this.transformToAnim;
	}
		
}
