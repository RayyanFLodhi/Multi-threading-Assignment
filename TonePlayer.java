public class TonePlayer {

    // Adjust these paths to match your actual .wav files
    private static final String DO        = "sounds/do.wav";
    private static final String RE        = "sounds/re.wav";
    private static final String MI        = "sounds/mi.wav";
    private static final String FA        = "sounds/fa.wav";
    private static final String SOL       = "sounds/sol.wav";
    private static final String LA        = "sounds/la.wav";
    private static final String SI        = "sounds/si.wav";
    private static final String DO_OCTAVE = "sounds/do-octave.wav";

    // Global sequence for the FIRST 7 notes
    private static final String[] MAIN_SEQUENCE = {
            DO, RE, MI, FA, SOL, LA, SI
    };

    // Shared synchronization objects / state
    private static final Object lock = new Object();         // for 1st–7th notes
    private static final Object lastNoteBarrier = new Object(); // for do-octave
    private static int currentIndex = 0;                     // which note (0–6) is next
    private static int readyForLast = 0;                     // how many threads reached last note

    private static final FilePlayer player = new FilePlayer();

    // Helper so we don't repeat try/catch
    private static void playNote(String filePath) {
        player.play(filePath);
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws InterruptedException {

        int[] t1Indices = {0, 2, 4, 6};
        int[] t2Indices = {1, 3, 5};

        Thread t1 = new Thread(() -> {
            for (int idx : t1Indices) {
                synchronized (lock) {
                    while (currentIndex != idx) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    // PRINT STATEMENT FOR DEBUG
                    System.out.println("Thread-1 playing: " + MAIN_SEQUENCE[currentIndex]);

                    playNote(MAIN_SEQUENCE[currentIndex]);
                    currentIndex++;
                    lock.notifyAll();
                }
            }

            // Barrier: wait for Thread 2
            synchronized (lastNoteBarrier) {
                readyForLast++;
                if (readyForLast < 2) {
                    while (readyForLast < 2) {
                        try {
                            lastNoteBarrier.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                } else {
                    lastNoteBarrier.notifyAll();
                }
            }

            // Both threads reach here
            System.out.println("Thread-1 playing FINAL note: " + DO_OCTAVE);
            playNote(DO_OCTAVE);

        }, "Thread-1");

        Thread t2 = new Thread(() -> {
            for (int idx : t2Indices) {
                synchronized (lock) {
                    while (currentIndex != idx) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    // PRINT STATEMENT FOR DEBUG
                    System.out.println("Thread-2 playing: " + MAIN_SEQUENCE[currentIndex]);

                    playNote(MAIN_SEQUENCE[currentIndex]);
                    currentIndex++;
                    lock.notifyAll();
                }
            }

            // Barrier: wait for Thread 1
            synchronized (lastNoteBarrier) {
                readyForLast++;
                if (readyForLast < 2) {
                    while (readyForLast < 2) {
                        try {
                            lastNoteBarrier.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                } else {
                    lastNoteBarrier.notifyAll();
                }
            }

            System.out.println("Thread-2 playing FINAL note: " + DO_OCTAVE);
            playNote(DO_OCTAVE);

        }, "Thread-2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }
}
