package spacegraph.input;

abstract public class FingerDragging extends Fingering {

    private final int button;

    public FingerDragging(int button) {
        super();
        this.button = button;
    }

    @Override
    public boolean update(Finger finger) {
        return drag(finger) && finger.buttonDown.length > 0 && finger.buttonDown[button];
    }

    /** return false to cancel the operation */
    abstract protected boolean drag(Finger f);
}
