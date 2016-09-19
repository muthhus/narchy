package nars.experiment.asteroids;

public class Config {
    private static boolean teamDamage = false;
    private static boolean showDamage = true;

    public static boolean teamDamage() {
        return teamDamage;
    }

    public static boolean showDamage() {
        return showDamage;
    }
}
