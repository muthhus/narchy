package nars.rover.obj.util.external;

/**
 * Created by me on 8/3/15.
 */
public interface Health {
	void hurt(float amount);

	void heal(float amount);

	float currentHealth();
}
