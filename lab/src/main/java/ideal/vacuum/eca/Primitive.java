package ideal.vacuum.eca;

import ideal.vacuum.eca.construct.Appearance;
import ideal.vacuum.eca.construct.Displacement;

/**
 * A primitive interaction.
 * @author Olivier
 */
public interface Primitive 
{
	/**
	 * @return The primitive interaction's label
	 */
    String getLabel();
	
	/**
	 * @return The primitive interaction's value (multiplied by 10)
	 */
    int getValue();
	
	/**
	 * @return the displacement.
	 */
    Displacement getDisplacement();

	/**
	 * @param displacement The displacement.
	 */
    void setDisplacement(Displacement displacement);

		/**
	 * Records the transformation of the preAppearance into the postAppearance.
	 * @param preAppearance The preAppearance.
	 * @param postAppearance The postAppearance.
	 */
        void recordTransfomr(Appearance preAppearance, Appearance postAppearance);
	
	/**
	 * Gives the appearance produced by enacting this interaction in the context of a given appearance
	 * @param appearance The context appearance.
	 * @return The appearance resulting from enacting this interaction with the given appearance
	 */
    Appearance transform(Appearance appearance);
	
	/**
	 * @param displacement The displacement to record to this experiment
	 */
	//public void incDisplacementCounter(Displacement displacement);
	
	/**
	 * @param area The area from which we want to predict the displacement
	 * @return The displacement most probably resulting from this experiment
	 */
	//public Displacement predictDisplacement(Area area);
	
	/**
	 * @return
	 */
	//public String getDisplacementLabels();
	
}
