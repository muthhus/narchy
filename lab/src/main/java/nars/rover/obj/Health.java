package nars.rover.obj;

import com.artemis.Component;

/**
 * @author Daan van Yperen
 */
public class Health extends Component {

    public float nutrition;
    public float damage;


    /*public String[] deathSfxId;
    public String[] damageSfxId;
    public String woundParticle;*/

    public Health() { }

    public Health(float nutrition) {
        this.nutrition = nutrition;
    }

    public void ingest(Edible e) {
        nutrition = Math.min(1,nutrition + e.nutrients);
        e.nutrients = 0.01f;
        damage += Math.min(1, damage + e.poison);
        e.poison = 0.01f;

        //System.out.println("Ingested: health=" + health());
    }

    public float health() { return nutrition - damage; }

    public void update() {
        nutrition*=0.95f;
        damage*=0.95f;
    }
}
