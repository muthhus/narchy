package nars.audio;

import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.concept.Concept;
import nars.concept.SensorConcept;
import nars.gui.Vis;
import nars.nar.NARBuilder;
import nars.nar.NARS;
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
import java.util.Arrays;
import java.util.List;

import static nars.Op.BELIEF;


/**
 * generic MIDI input interface
 */
public class MIDI {

    float volume[] = new float[128];

    public MIDI() throws LineUnavailableException {
        NARS nar = NARBuilder.newMultiThreadNAR(3,
                new RealTime.CS(true).durFPS(20f)
        );
        //d.nal(4);
        //nar.log();

        nar.termVolumeMax.setValue(32);
        MidiInReceiver midi = MIDI(nar);

        Arrays.fill(volume, Float.NaN);

        SoNAR s = new SoNAR(nar);
        //s.audio.record("/tmp/midi2.raw");

//        d.onCycle(()->{
//            s.termListeners.forEach((x, v) -> {
//                System.out.println(v);
//            });
//        });

        SoNAR.SampleDirectory sd = new SoNAR.SampleDirectory();
        sd.samples("/home/me/wav/legoweltkord");

        final List<SensorConcept> keys = $.newArrayList();
        for (int i = 36; i <= 51; i++) {
            Term key =
                    channelKey(9, i);

            Compound keyTerm =
                    $.p(key);

            int finalI = i;
            SensorConcept c = new SensorConcept(keyTerm, nar, () -> {
                float v = volume[finalI];
                if (v == 0)
                    volume[finalI] = Float.NaN;
                return v;
            }, (v) -> $.t(v, nar.confDefault(BELIEF)));
            nar.on(c);
            keys.add(c);//senseNumber(on2, midi.key(key) ));
            s.listen(c, sd::byHash);

        }


        nar.onCycle(()->{
            keys.forEach(k -> nar.input(k.apply(nar)));
        });


        SpaceGraph.window(Vis.beliefCharts(64, keys, nar), 500, 500);


        nar.startFPS(20f);


    }

    public static void main(String[] arg) throws LineUnavailableException, Narsese.NarseseException, FileNotFoundException {
        new MIDI();
    }

    public MidiInReceiver MIDI(NAR nar) {
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

    public class MidiInReceiver implements Receiver {

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
                ShortMessage s = (ShortMessage) m;
                int cmd = s.getCommand();
                switch (cmd) {
                    case ShortMessage.NOTE_OFF:
                        volume[s.getData1()] = 0;

//                        Compound t = $.inh(channelKey(s), Atomic.the("on"));
//
//                        nar.believe($.neg(t), Tense.Present);
                        //System.out.println(key(t));
                        break;
                    case ShortMessage.NOTE_ON:
                        volume[s.getData1()] = 0.5f + 0.5f * s.getData2()/128f;

//                        Compound u = $.inh(channelKey(s), Atomic.the("on"));
//                        nar.believe(u, Tense.Present);
                        //key(u, 0.5f + 0.5f * s.getData2()/64f);
                        //System.out.println(key(t));
                        break;
                    default:
                        //System.out.println("unknown command: " + s);
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

        @Override
        public void close() {

        }
    }

//    public static @NotNull Compound channelKey(ShortMessage s) {
//        return channelKey(s.getChannel(), s.getData1() /* key */);
//    }

    public static @NotNull Term channelKey(int channel, int key) {
        return $.the(key);
        //return $.p($.the(channel), $.the(key));
    }


}
