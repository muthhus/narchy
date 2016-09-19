package nars.experiment.asteroids;

public interface Realization {
    boolean shouldTrigger(double playerX, double playerY);
    void trigger(double playerX, double playerY);
}
