package nars.audio;

import nars.$;
import nars.NAR;
import nars.NAgentX;
import nars.Narsese;
import nars.gui.Vis;
import nars.nar.Default;
import nars.nar.NARBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.RealTime;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;

import javax.sound.midi.*;
import javax.sound.sampled.LineUnavailableException;
import java.io.FileNotFoundException;
import java.util.List;

import static nars.audio.MIDI.MidiInReceiver.channelKey;


/**
 * generic MIDI input interface
 */
public class MIDI {

    public static void main(String[] arg) throws LineUnavailableException, Narsese.NarseseException, FileNotFoundException {
        Default d = NARBuilder.newMultiThreadNAR(2,
            new RealTime.CS(true).durFPS(10f)
        );
        //d.nal(4);
        //d.log();
        d.termVolumeMax.setValue(32);
        MidiInReceiver midi = MIDI(d);

        d.mix.stream("Derive").setValue(0.25f);

        SoNAR s = new SoNAR(d);
        //s.audio.record("/tmp/midi2.raw");

//        d.onCycle(()->{
//            s.termListeners.forEach((x, v) -> {
//                System.out.println(v);
//            });
//        });

        SoNAR.SampleDirectory sd = new SoNAR.SampleDirectory();
        sd.samples("/home/me/wav/legoweltkord");

        new NAgentX("MIDI", d) {

            final List<Term> keys = $.newArrayList();

            @Override
            public synchronized void init() {
                super.init();

                for (int i = 36; i <= 51; i ++) {
                    Compound key =
                        channelKey(9, i);

                    Compound on2 =
                            $.inh(key, Atomic.the("on"));

                    keys.add(on2);//senseNumber(on2, midi.key(key) ));
                    s.listen(on2, sd::byHash);
                }

                SpaceGraph.window(Vis.beliefCharts(64, keys, nar), 500, 500);
                NAgentX.chart(this);
            }

            @Override
            protected float act() {
                return 0;
            }
        }.runRT(5f);

        //d.loop();

    }

    public static MidiInReceiver MIDI(NAR nar) {
        // Obtain information about all the installed synthesizers.
        MidiDevice device;
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();



        for (int i = 0; i < infos.length; i++) {
            try {
                MidiDevice.Info ii = infos[i];

                device = MidiSystem.getMidiDevice(ii);

                System.out.println(device + "\t" + device.getClass());
                System.out.println("\t" + device.getDeviceInfo());
                System.out.println("\ttx: " + device.getTransmitters());
                System.out.println("\trx: " + device.getReceivers());

                if (receive(device)) {
                    return new MidiInReceiver(device, nar);
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

        return null;
    }

    public static boolean receive(MidiDevice device) {
        return device.getDeviceInfo().getName().startsWith("MPD218");
    }

    public static class MidiInReceiver implements Receiver {

        //public final Map<Term,FloatParam> key = new ConcurrentHashMap<>();

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

            if (m instanceof ShortMessage) {
                ShortMessage s = (ShortMessage)m;
                int cmd = s.getCommand();
                switch (cmd) {
                    case ShortMessage.NOTE_OFF:
                        Compound t = $.inh(channelKey(s), Atomic.the("on"));
                        nar.believe($.neg(t), Tense.Present);
                        //System.out.println(key(t));
                        break;
                    case ShortMessage.NOTE_ON:
                        Compound u = $.inh(channelKey(s), Atomic.the("on"));
                        nar.believe(u, Tense.Present);
                        //key(u, 0.5f + 0.5f * s.getData2()/64f);
                        //System.out.println(key(t));
                        break;
                        //case ShortMessage.CONTROL_CHANGE:
                }
            }

        }

//        public FloatParam key(Compound t) {
//            return key.computeIfAbsent(t, tt -> new FloatParam(Float.NaN));
//        }
//
//        public void key(Compound t, float v) {
//            v = Util.unitize(v);
//            MutableFloat m = key(t);
//            m.setValue(v);
//        }

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
