package nars.audio;

import jcog.learn.markov.MarkovTrack;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.gui.Vis;
import nars.nar.Default;
import nars.nar.NARBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.time.RealTime;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.List;

/**
 * generic MIDI input interface
 */
public class MIDI {
    public static void main(String[] arg) {
        Default d = NARBuilder.newMultiThreadNAR(2,
            new RealTime.CS().durFPS(30f)
        );
        d.termVolumeMax.setValue(16);

        //d.log();
        //Vis.conceptsWindow3D(d, 8, 8).show(800, 600);

        MIDI(d);

        new NAgentX(d) {

            @Override
            public synchronized void init() {
                super.init();
                NAgentX.chart(this);
            }

            @Override
            protected float act() {
                return 0;
            }
        }.runRT(4f);

    }

    public static void MIDI(NAR nar) {
        // Obtain information about all the installed synthesizers.
        MidiDevice device;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        List<Synthesizer> synthInfos = new ArrayList();
        List<MidiDevice> midis = new ArrayList();

        for (int i = 0; i < infos.length; i++) {
            try {
                MidiDevice.Info ii = infos[i];

                device = MidiSystem.getMidiDevice(ii);

                System.out.println(device + "\t" + device.getClass());
                System.out.println("\t" + device.getDeviceInfo());
                System.out.println("\ttx: " + device.getTransmitters());
                System.out.println("\trx: " + device.getReceivers());

                if (receive(device)) {
                    new MidiInReceiver(device, nar);
                }

                /*if (device instanceof Synthesizer) {
                    synthInfos.add((Synthesizer) ii);
                } else if (device instanceof MidiDevice) {
                    midis.add((MidiDevice) ii);
                }*/
            } catch (MidiUnavailableException e) {
                // Handle or throw exception...
            }
        }

    }

    public static boolean receive(MidiDevice device) {
        return device.getDeviceInfo().getName().startsWith("MPD218");
    }

    private static class MidiInReceiver implements Receiver {

        private final MidiDevice device;
        private final NAR nar;

        public MidiInReceiver(MidiDevice device, NAR nar) throws MidiUnavailableException {
            this.device = device;
            this.nar = nar;

            if (!device.isOpen()) {
                device.open();
            }

            device.getTransmitter().setReceiver(this);
        }

        @Override
        public void send(MidiMessage m, long timeStamp) {

            float freq = 0.5f, conf = 0f;

            Compound t = null;
            if (m instanceof ShortMessage) {
                ShortMessage s = (ShortMessage)m;
                int cmd = s.getCommand();
                switch (cmd) {
                    case ShortMessage.NOTE_OFF:
                        t = $.inh (channelKey(s), $.the("on"));
                        freq = 0f;
                        conf = 0.5f;
                        break;
                    case ShortMessage.NOTE_ON:
                        t = $.inh(channelKey(s), $.the("on"));
                        freq = 1f;
                        conf = 0.5f + 0.5f * s.getData2()/64f;
                        break;
                        case ShortMessage.CONTROL_CHANGE:
                }
            }

            if (t!=null) {
                nar.believe(t, Tense.Present, freq, conf);
            }
        }

        public @NotNull Compound channelKey(ShortMessage s) {
            return $.p($.the(s.getChannel()), $.the(s.getData1() /* key */));
        }

        @Override
        public void close() {

        }
    }
}
