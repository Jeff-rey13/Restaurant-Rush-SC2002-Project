package rush.gui;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Short sound effects made in code (no audio files). If the computer has no
 * sound device, the game stays silent and keeps going.
 */
final class SoundFx {

    enum Sound { DOOR, DING, CASH, CLICK, ERROR, LEAVE, SIZZLE, POP, VICTORY, DEFEAT }

    private static final int RATE = 44100;
    private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);
    private static final Map<Sound, byte[]> CACHE = new EnumMap<>(Sound.class);
    private static final ExecutorService PLAYER = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "sound");
        t.setDaemon(true);
        return t;
    });

    private static volatile boolean enabled = true;
    private static volatile boolean unavailable;

    private SoundFx() {
    }

    static boolean isEnabled() {
        return enabled;
    }

    static void setEnabled(boolean on) {
        enabled = on;
    }

    static void play(Sound sound) {
        if (!enabled || unavailable) {
            return;
        }
        PLAYER.submit(() -> {
            try {
                byte[] data = samples(sound);
                SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT);
                line.open(FORMAT, 8820);
                line.start();
                line.write(data, 0, data.length);
                line.drain();
                line.close();
            } catch (Exception | LinkageError e) {
                unavailable = true;
            }
        });
    }

    private static synchronized byte[] samples(Sound sound) {
        return CACHE.computeIfAbsent(sound, SoundFx::synthesise);
    }

    private static byte[] synthesise(Sound sound) {
        switch (sound) {
            case DOOR:
                return mix(bell(1318.5, 0.0, 0.45), bell(1046.5, 0.18, 0.6));
            case DING:
                return bell(1760, 0.0, 1.0);
            case CASH:
                return mix(noise(0.07, 0.0, 0.18), mix(bell(2093, 0.06, 0.45), bell(2637, 0.12, 0.5)));
            case CLICK:
                return tone(900, 0.03, 0.12, false);
            case ERROR:
                return mix(tone(190, 0.16, 0.18, true), tone(150, 0.2, 0.14, true));
            case LEAVE:
                return glide(440, 220, 0.5, 0.18);
            case SIZZLE:
                return noise(0.45, 0.0, 0.07);
            case POP:
                return glide(500, 900, 0.07, 0.15);
            case VICTORY:
                return mix(mix(bell(523.3, 0.0, 0.5), bell(659.3, 0.13, 0.5)),
                        mix(bell(784.0, 0.26, 0.5), mix(bell(1046.5, 0.39, 1.0), bell(784.0, 0.39, 1.0))));
            case DEFEAT:
            default:
                return mix(mix(bell(392.0, 0.0, 0.4), bell(349.2, 0.25, 0.4)),
                        mix(bell(311.1, 0.5, 0.4), bell(261.6, 0.75, 0.9)));
        }
    }

    // ----- synthesis -----

    private static byte[] bell(double freq, double delay, double length) {
        int start = (int) (delay * RATE);
        int n = start + (int) (length * RATE);
        double[] s = new double[n];
        for (int i = start; i < n; i++) {
            double t = (i - start) / (double) RATE;
            double env = Math.exp(-t * 6 / length) * Math.min(1, t * 400);
            s[i] = 0.22 * env * (Math.sin(2 * Math.PI * freq * t) + 0.3 * Math.sin(2 * Math.PI * freq * 2.01 * t));
        }
        return toBytes(s);
    }

    private static byte[] tone(double freq, double length, double volume, boolean square) {
        int n = (int) (length * RATE);
        double[] s = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double env = Math.min(1, t * 300) * Math.min(1, (length - t) * 60);
            double w = Math.sin(2 * Math.PI * freq * t);
            s[i] = volume * env * (square ? Math.signum(w) * 0.6 : w);
        }
        return toBytes(s);
    }

    private static byte[] glide(double from, double to, double length, double volume) {
        int n = (int) (length * RATE);
        double[] s = new double[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) RATE;
            double f = from + (to - from) * t / length;
            phase += 2 * Math.PI * f / RATE;
            double env = Math.min(1, t * 200) * Math.min(1, (length - t) * 20);
            s[i] = volume * env * (2 / Math.PI) * Math.asin(Math.sin(phase));
        }
        return toBytes(s);
    }

    private static byte[] noise(double length, double delay, double volume) {
        Random r = new Random(3);
        int start = (int) (delay * RATE);
        int n = start + (int) (length * RATE);
        double[] s = new double[n];
        double prev = 0;
        for (int i = start; i < n; i++) {
            double t = (i - start) / (double) RATE;
            double env = Math.min(1, t * 200) * (1 - t / length);
            prev = prev * 0.6 + (r.nextDouble() * 2 - 1) * 0.4;
            s[i] = volume * env * prev;
        }
        return toBytes(s);
    }

    private static byte[] toBytes(double[] s) {
        byte[] b = new byte[s.length * 2];
        for (int i = 0; i < s.length; i++) {
            int v = (int) Math.max(-32767, Math.min(32767, s[i] * 32767));
            b[2 * i] = (byte) v;
            b[2 * i + 1] = (byte) (v >> 8);
        }
        return b;
    }

    private static byte[] mix(byte[] a, byte[] b) {
        int n = Math.max(a.length, b.length) / 2;
        double[] s = new double[n];
        for (int i = 0; i < n; i++) {
            double va = 2 * i + 1 < a.length ? (short) ((a[2 * i + 1] << 8) | (a[2 * i] & 0xff)) : 0;
            double vb = 2 * i + 1 < b.length ? (short) ((b[2 * i + 1] << 8) | (b[2 * i] & 0xff)) : 0;
            s[i] = (va + vb) / 32767.0;
        }
        return toBytes(s);
    }
}
