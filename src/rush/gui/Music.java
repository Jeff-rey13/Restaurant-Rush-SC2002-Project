package rush.gui;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * A quiet looping jazz tune (electric piano, bass, ride cymbal) played with
 * Java's built-in MIDI synthesiser as background music. Stays silent if the
 * computer has no MIDI synthesiser or sound device.
 */
final class Music {

    private static final int PPQ = 480;
    private static Sequencer sequencer;
    private static volatile boolean wanted = true;

    // Eight bars of chords: Cmaj7 Am7 Dm7 G7 Em7 A7 Dm7 G7 (as MIDI notes).
    private static final int[][] CHORDS = {
        {60, 64, 67, 71}, {57, 60, 64, 67}, {62, 65, 69, 72}, {59, 62, 65, 67},
        {59, 62, 64, 67}, {57, 61, 64, 67}, {62, 65, 69, 72}, {59, 62, 65, 67}
    };
    private static final int[] ROOTS = {36, 33, 38, 31, 40, 33, 38, 31};

    private Music() {
    }

    static boolean isOn() {
        return wanted;
    }

    static synchronized void toggle() {
        wanted = !wanted;
        if (sequencer == null) {
            if (wanted) {
                start();
            }
            return;
        }
        if (wanted) {
            sequencer.start();
        } else {
            sequencer.stop();
        }
    }

    /** Starts the loop on a background thread. If anything fails, the game just stays silent. */
    static void start() {
        Thread t = new Thread(() -> {
            try {
                synchronized (Music.class) {
                    if (sequencer != null || !wanted) {
                        return;
                    }
                    Sequencer s = MidiSystem.getSequencer();
                    s.open();
                    s.setSequence(buildSequence());
                    s.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
                    s.setTempoInBPM(84);
                    s.start();
                    sequencer = s;
                }
            } catch (Exception | LinkageError e) {
                wanted = false;
            }
        }, "music");
        t.setDaemon(true);
        t.start();
    }

    static synchronized void stop() {
        if (sequencer != null) {
            sequencer.stop();
            sequencer.close();
            sequencer = null;
        }
    }

    private static Sequence buildSequence() throws Exception {
        Sequence seq = new Sequence(Sequence.PPQ, PPQ);
        Track track = seq.createTrack();
        add(track, ShortMessage.PROGRAM_CHANGE, 0, 4, 0, 0);
        add(track, ShortMessage.PROGRAM_CHANGE, 1, 32, 0, 0);
        add(track, ShortMessage.CONTROL_CHANGE, 0, 7, 62, 0);
        add(track, ShortMessage.CONTROL_CHANGE, 1, 7, 80, 0);
        add(track, ShortMessage.CONTROL_CHANGE, 9, 7, 55, 0);
        int swing = PPQ * 2 / 3;
        for (int bar = 0; bar < CHORDS.length; bar++) {
            long barStart = (long) bar * 4 * PPQ;
            int[] chord = CHORDS[bar];
            // piano: long chord on beat 1, short stab on the swung "and" of 2
            for (int note : chord) {
                note(track, 0, note, 34, barStart, PPQ * 2 - 40);
                note(track, 0, note, 26, barStart + PPQ + swing, PPQ / 3);
            }
            // walking bass
            int root = ROOTS[bar];
            int nextRoot = ROOTS[(bar + 1) % ROOTS.length];
            int[] walk = {root, root + 4, root + 7, nextRoot + (nextRoot > root ? -1 : 1)};
            for (int beat = 0; beat < 4; beat++) {
                note(track, 1, walk[beat], beat == 0 ? 52 : 44, barStart + (long) beat * PPQ, PPQ - 30);
            }
            // ride cymbal with swing, soft hi-hat on 2 and 4
            for (int beat = 0; beat < 4; beat++) {
                long b = barStart + (long) beat * PPQ;
                note(track, 9, 51, 22, b, PPQ / 4);
                if (beat == 1 || beat == 3) {
                    note(track, 9, 51, 16, b + swing, PPQ / 4);
                    note(track, 9, 44, 18, b, PPQ / 4);
                }
            }
        }
        // end marker so the loop is exactly 8 bars long
        add(track, ShortMessage.CONTROL_CHANGE, 0, 64, 0, (long) CHORDS.length * 4 * PPQ);
        return seq;
    }

    private static void note(Track t, int channel, int note, int velocity, long tick, long length) throws Exception {
        add(t, ShortMessage.NOTE_ON, channel, note, velocity, tick);
        add(t, ShortMessage.NOTE_OFF, channel, note, 0, tick + length);
    }

    private static void add(Track t, int command, int channel, int d1, int d2, long tick) throws Exception {
        ShortMessage m = new ShortMessage();
        m.setMessage(command, channel, d1, d2);
        t.add(new MidiEvent(m, tick));
    }
}
