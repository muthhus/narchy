package nars.guifx;

import nars.NAR;
import nars.guifx.util.CodeInput;

/**
 * TODO use looping state, not nar.running() likely to be false
 */
public class NarseseInput extends CodeInput {
    private final NAR nar;

    public NarseseInput(NAR nar) {
        this.nar = nar;
        codeArea.setPrefRowCount(1);
    }

    @Override
    public boolean onInput(String _s) {

        String s = _s.trim();

        if (!s.isEmpty()) {
            nar.runLater(() -> nar.input(s));
        }

        try {
            nar.step();
        } catch (NAR.AlreadyRunningException e) {
            //no problem it is already running and will get the queued event
        }

        return true;
    }
}
