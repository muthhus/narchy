package ideal.vacuum.ernest;

import ideal.vacuum.eca.ActInstance;
import ideal.vacuum.eca.ActInstanceImpl;
import ideal.vacuum.eca.Primitive;
import ideal.vacuum.eca.PrimitiveImpl;
import ideal.vacuum.eca.construct.egomem.Aspect;
import ideal.vacuum.eca.construct.egomem.AspectImpl;
import ideal.vacuum.tracing.ITracer;
import spacegraph.math.v3;
import spacegraph.phys.math.Transform;


/**
 * This class implements the environment proposed by Roesch et al. in 
 * "Exploration of the Functional Properties of Interaction: 
 * Computer Models and Pointers for Theory"
 *  
 * @author ogeorgeon
 */
public class Roesch2 implements IEnvironment
{
	private static final int WIDTH = 10;	
	private int position;
	private Transform transform = new Transform();
	
	private final int[] board = {6, 3, 5, 4, 7, 3, 5, 3, 9, 5};

	/**
	 * Process a primitive schema and return its enaction status.
	 * @param s The string code that represents the primitive schema to enact.
	 * @return The boolean feedback resulting from the schema enaction.
	 */

	
	/**
	 * Step forward
	 * @return true if the agent went up
	 */
	private Effect step(){
		Effect effect = new EffectImpl();
		effect.setLabel("f");
		effect.setColor(0xFF0000);
		effect.setLocation(new v3(1, 0, 0));
		effect.setTransformation(0f, -1f);

		if (position < WIDTH -1){
			if (board[position] <= board[position + 1])
				effect.setLabel("t");
			position++;
		}else {
			if (board[position] <= board[0])
				effect.setLabel("t");
			position = 0;
			effect.setLocation(new v3(0, 0, 0)); // End of Line is experienced at the agent's position. 
		}
		
		if (effect.getLabel().equals("t"))
			effect.setColor(0xFFFFFF);
		
		return effect;
	}
	
	/**
	 * @return true if the next item is greater than the current item
	 */
	private Effect feel(){
		Effect effect = new EffectImpl();
		effect.setLabel("f");
		effect.setColor(0xFF0000);
		effect.setLocation(new v3(1, 0, 0));
		effect.setTransformation(0f, 0f);
		
		if (position < WIDTH -1){
			if (board[position] <= board[position +1])
				effect.setLabel("t");
		}else {
			if (board[position] <= board[0])
				effect.setLabel("t");
			effect.setLocation(new v3(0, 0, 0)); // End of Line is experienced at the agent's position. 
		}

		if (effect.getLabel().equals("t"))
			effect.setColor(0xFFFFFF);

		return effect;		
	}
	
	/**
	 * Invert the next item and the current item
	 * @return true if the next item is greater than the current item
	 */
	private Effect swap(){
		Effect effect = new EffectImpl();
		effect.setLabel("f");
		effect.setLocation(new v3(1, 0, 0));
		effect.setTransformation(0f, 0f); // simulates a displacement because the environment changes
		effect.setColor(0xFF0000);
		int temp = board[position];
		if (position < WIDTH -1){
			if (board[position] > board[position +1])
			{
				board[position] = board[position + 1];
				board[position + 1] = temp;
				effect.setLabel("t");
				effect.setTransformation(0f, -1f); // simulates a displacement because the environment changes
			}
		}
		else {
			effect.setLocation(new v3(0, 0, 0)); // End of Line is experienced at the agent's position. 
//			if (board[m_x] < board[0])
//			{
//				board[m_x] = board[0];
//				board[0] = temp;
//				effect.setLabel("t");
//			}
//			if (board[m_x] > board[0]){
//				effect.setLabel("t");
//			}
		}
		if (effect.getLabel().equals("t"))
			effect.setColor(0xFFFFFF);
		

		return effect;		
	}
	
	public Effect enact(String s)
	{
		Effect effect = null;

		switch (s) {
			case ">":
				effect = step();
				break;
			case "-":
				effect = feel();
				break;
			case "i":
				effect = swap();
				break;
		}
		
		System.out.println("enacted " + s + effect.getLabel());
		printEnv();
		return effect;
	}
	
	private void printEnv(){
		// print the board
		for (int i = 0; i < WIDTH; i++)
			System.out.print(board[i] + " ");
		System.out.println();

		// print the agent
		for (int i = 0; i < position; i++)
			System.out.print("  ");
		System.out.print(">");

		System.out.println();
	}

	@Override
	public void initErnest(IErnest ernest) {
		ernest.addInteraction(">t", 4);   // step up
		ernest.addInteraction(">f", -10); // step down
		ernest.addInteraction("-t", -4);  // feel up
		ernest.addInteraction("-f", -4);  // feel down
		ernest.addInteraction("it", 4);   // swap
		ernest.addInteraction("if", -10); // not swap
	}

	@Override
	public void trace(ITracer tracer) {
		Object e = tracer.addEventElement("environment");
		String stringBoard = "";
		for (int i = 0; i < WIDTH; i++)
			stringBoard += this.board[i] + " ";
		tracer.addSubelement(e,"board", stringBoard);

		String stringAgent = "";
		for (int i = 0; i < position; i++)
			stringAgent +=".. ";
		stringAgent += ">";
		tracer.addSubelement(e,"agent", stringAgent);
		tracer.addSubelement(e,"position", position + "");
		if (position < WIDTH - 1)
			tracer.addSubelement(e,"next", this.board[position + 1] + "");
	}

	@Override
	public ActInstance enact(Primitive primitive) {
		Effect effect = enact(primitive.getLabel().substring(0,1));
		Primitive enactedPrimitive = PrimitiveImpl.createOrGet(primitive.getLabel().substring(0,1) + effect.getLabel(), 0);
		//ActInstance enactedActInstance = new ActInstanceImpl(enactedPrimitive, effect.getLocation());
		ActInstance enactedActInstance = new ActInstanceImpl(enactedPrimitive, new v3());
		Aspect aspect = AspectImpl.createOrGet(effect.getColor());
		enactedActInstance.setAspect(aspect);
		this.transform = effect.getTransformation();
		return enactedActInstance;
	}

	@Override
	public Transform getTransformation(){
		return this.transform;
	}

}
