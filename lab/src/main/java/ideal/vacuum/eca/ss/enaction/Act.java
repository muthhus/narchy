package ideal.vacuum.eca.ss.enaction;

import ideal.vacuum.eca.Primitive;
import ideal.vacuum.eca.construct.egomem.Area;

/**
 * An Act is an Interaction with spatial information (Area) attached in order to characterize its enaction in space.
 * An Act is enacted if it is afforded by the corresponding Area, 
 *   meaning that there is an appearance of PhenomenonType that affords this Interaction in this Area.  
 *   This appearance can be anticipated based on previous experiments enacting this act in the same context (post-appearance).
 *
 * A primitive Act is a Primitive interaction attached with an Area.
 * A composite Act is a tuple of two acts (pre-act, post-act)
 * ?? A composite Act is enactable if all its subacts concern the same PhenomenonType. ??
 * ?? When the pre-act and post-act have the same Area then a serial interaction can be constructed as the series of their interactions  
 * @author Olivier
 */
public interface Act 
{
	/**
	 * @return The label of this interaction (unique identifier)
	 */
    String getLabel();
	
	/**
	 * @return true if primitive, false if composite
	 */
    boolean isPrimitive();
	
	/**
	 * @return The value of enacting this interaction
	 */
    int getEnactionValue();
	
	/**
	 * @param enactionWeight The weight of this interaction.
	 */
    void setWeight(int enactionWeight);
	
	/**
	 * @return The weight of this interaction.
	 */
    int getWeight();
	
	/**
	 * @return This interaction's pre-interaction (null if primitive).
	 */
    Act getPreAct();
	
	/**
	 * @return This interaction's post-interaction (null if primitive).
	 */
    Act getPostAct();
	
	/**
	 * @return The number of primitive interactions that compose this interaction
	 */
    int getLength();
	
	/**
	 * @param step The current step during the enaction of this interaction.
	 */
    void setStep(int step);
	
	/**
	 * @return The current step during the enaction of this interaction.
	 */
    int getStep();
	
	/**
	 * @param prescriber The interaction that prescribes the enaction of this interaction.
	 */
    void setPrescriber(Act prescriber);
	
	/**
	 * @return The interaction that prescribes the enaction of this interaction.
	 */
    Act getPrescriber();
	
	/**
	 * Prescribe this interaction's preInteraction
	 * This method applies recursively to all this interaction's sub interactions.
	 * @return The primitive interaction at the bottom of the hierarchy
	 */
    Act prescribe();

	/**
	 * Update the prescriber if this interaction was enacted
	 * @return The next top level interaction to enact or null if the current enaction is over.
	 */
    Act updatePrescriber();
	
	/**
	 * Clear the prescriber hierarchy
	 */
    void terminate();
	
	/**
	 * @return The primitive interaction enacted by this act.
	 */
    Primitive getPrimitive();
	
	/**
	 * @param primitive The primitive interaction enacted by this act.
	 */
    void setPrimitive(Primitive primitive);
	
	/**
	 * @return The area where this act is enacted.
	 */
    Area getArea();
	
	/**
	 * @param area The area where this act is enacted.
	 */
    void setArea(Area area);
	
	/**
	 * @param color The color used to display this act in sequential and spatial trace.
	 */
    void setColor(int color);
	
	/**
	 * @param The color used to display this act in sequential and spatial trace.
	 */
    int getColor();
	
	int getValue();
	
	//public void initPrimitive();
}
