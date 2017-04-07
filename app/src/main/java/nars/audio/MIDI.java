package nars.audio;

import jcog.Util;
import jcog.learn.markov.MarkovTrack;
import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.Narsese;
import nars.gui.Vis;
import nars.nar.Default;
import nars.nar.NARBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.time.RealTime;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import spacegraph.audio.granular.Granulize;

import javax.sound.midi.*;
import javax.sound.sampled.LineUnavailableException;
import java.util.ArrayList;
import java.util.List;

import static nars.audio.MIDI.MidiInReceiver.channelKey;


/**
 * generic MIDI input interface
 */
public class MIDI {
    public static void main(String[] arg) throws LineUnavailableException, Narsese.NarseseException {
        Default d = NARBuilder.newMultiThreadNAR(1,
            new RealTime.CS().durFPS(30f)
        );
        d.nal(4);
        d.termVolumeMax.setValue(12);
        MIDI(d);

        SoNAR s = new SoNAR(d);

        d.onCycle(()->{
            s.termListeners.forEach((x, v) -> {
                System.out.println(v);
            });
        });

        s.samples("/home/me/wav");
        for (int i = 36; i <= 51; i ++) {
            Compound on =
                    channelKey(9, i);
                    //$.inh(channelKey(9, i), $.the("on"));
            System.out.println(on);
            s.listen(on);
        }
        new NAgentX("MIDI", d) {

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

        //d.loop();

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

    public static class MidiInReceiver implements Receiver {

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
                        t = $.inh(channelKey(s), $.the("on"));
                        freq = 0f;
                        conf = 0.5f;
                        break;
                    case ShortMessage.NOTE_ON:
                        t = $.inh(channelKey(s), $.the("on"));
                        freq = 1f;
                        conf = Util.clamp(0.5f + 0.5f * s.getData2()/64f, 0f, 0.9f);
                        break;
                        //case ShortMessage.CONTROL_CHANGE:
                }
            }

            if (t!=null) {
                nar.believe(t, Tense.Present, freq, conf);
            }
        }

        public static @NotNull Compound channelKey(ShortMessage s) {
            return channelKey(s.getChannel(), s.getData1() /* key */);
        }

        public static @NotNull Compound channelKey(int channel, int key) {
            return $.p($.the(channel), $.the(key));
        }

        @Override
        public void close() {

        }
    }
}
