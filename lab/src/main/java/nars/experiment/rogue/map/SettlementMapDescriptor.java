package nars.experiment.rogue.map;

import java.io.Serializable;
import java.util.ArrayList;

public class SettlementMapDescriptor extends MapDescriptor implements Serializable {
    public SettlementMapDescriptor(int x, int y, int z, String name) {
        super(x, y, z);
        this.name = name;
        this.size = 1;
        ports = null;
    }

    public void addPort(int x, int y) {
        if (ports == null)
            ports = new ArrayList<>();
        ports.add(new int[]{x, y});
    }

    public void enlarge() {
        this.size++;
    }

    public String toString() {
        String s = this.name;
        if (getSurface() != null
                && !getSurface().isEmpty())
            s += " (" + getSurface() + ")";
        return s;
    }


    private int size;
    private ArrayList<int[]> ports; //ports, that are reachable from this city
    private final String name;
}
