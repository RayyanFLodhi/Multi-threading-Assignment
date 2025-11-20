import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class TonePlayer {

    private static final String DO = "sounds/do.wav";
    private static final String RE = "sounds/re.wav";
    private static final String MI = "sounds/mi.wav";
    private static final String FA = "sounds/fa.wav";
    private static final String SOL = "sounds/sol.wav";
    private static final String LA = "sounds/la.wav";
    private static final String SI = "sounds/si.wav";
    private static final String DO_OCTAVE = "sounds/do-octave.wav";

    // Global sequence for the FIRST 7 notes
    private static List<String> MAIN_SEQ;


    /*
     * 
     * do do so| so| la la so| fa fa mi mi re re do
        so| so| fa fa mi mi re so| so| fa fa mi mi re
        do do so| so| la la so| fa fa mi mi re re do
     */
    private static final String[] TWINKLE_TWINKLE = {
        DO, DO, SOL, SOL, LA, LA, SOL, FA, FA, MI, MI, RE,
        RE, DO, SOL, SOL, FA, FA, MI, MI, RE, SOL, SOL,
        FA, FA, MI, MI, RE, DO, DO, SOL, SOL, LA, LA, SOL,
        FA, FA, MI, MI, RE, RE, DO

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
        MAIN_SEQ = new ArrayList<String>();

        // Add notes to list for easier indexing and playing
        MAIN_SEQ.add(0, DO);
        MAIN_SEQ.add(1, RE);
        MAIN_SEQ.add(2, MI);
        MAIN_SEQ.add(3, FA);
        MAIN_SEQ.add(4, SOL);
        MAIN_SEQ.add(5, LA);
        MAIN_SEQ.add(6, SI);


        
        playMain();
        playTwinkleTwinkle();


    }

    public static void playMain() throws InterruptedException {
        System.out.println("Main Sequence:");
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
                    System.out.println("Thread-1 playing: " + MAIN_SEQ.get(currentIndex));

                    playNote(MAIN_SEQ.get(currentIndex));
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
                    System.out.println("Thread-2 playing: " + MAIN_SEQ.get(currentIndex));

                    playNote(MAIN_SEQ.get(currentIndex));
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
        System.out.println("done");
    }   

    public static void playTwinkleTwinkle() throws InterruptedException {
        currentIndex = 0;                     // which note (0–6) is next
        readyForLast = 0;
        System.out.println("Twinkle Twinkle Little Star:");

        /*
     * 
     * do do so| so| la la so| fa fa mi mi re re do
        so| so| fa fa mi mi re so| so| fa fa mi mi re
        do do so| so| la la so| fa fa mi mi re re do
     */

        // t1 can only play do, mi, sol, si
        // t2 can only play re, fa, la, 


        int[] t1Indices = new int[24];
        int[] t2Indices = new int[18];

        int t1index = 0;
        int t2index = 0;

        for (int i=0; i < TWINKLE_TWINKLE.length; i++) {
            if (MAIN_SEQ.indexOf(TWINKLE_TWINKLE[i]) % 2 == 0) {
                
                t1Indices[t1index] = MAIN_SEQ.indexOf(TWINKLE_TWINKLE[i]);
                t1index++;
            }

            else {
                t2Indices[t2index] = MAIN_SEQ.indexOf(TWINKLE_TWINKLE[i]);
                t2index++;
            }
        } 
        

        Thread t1 = new Thread(() -> {
            for (int idx : t1Indices) {
                synchronized (lock) {
                    while (idx % 2 != 0) { // Wait until the index is divisible by 2 (note playable by thread 1)
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    

                    // PRINT STATEMENT FOR DEBUG
                    System.out.println("Thread-1 playing: " + TWINKLE_TWINKLE[currentIndex]);

                    playNote(TWINKLE_TWINKLE[currentIndex]);
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
            

        }, "Thread-1");

        Thread t2 = new Thread(() -> {
            for (int idx : t2Indices) {
                synchronized (lock) {
                    while (idx % 2 == 0) { // Wait until the index is not divisible by 2 (note playable by thread 2)
                        try {
                            lock.wait();
                            System.out.println("thread 2 waiting for note...");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    // PRINT STATEMENT FOR DEBUG
                    System.out.println("Thread-2 playing: " + TWINKLE_TWINKLE[currentIndex]);

                    playNote(TWINKLE_TWINKLE[currentIndex]);
                    currentIndex++;
                    lock.notifyAll();
                }
            }

            // Barrier: wait for Thread 1
            synchronized (lastNoteBarrier) {
                readyForLast++;
                lastNoteBarrier.notifyAll();
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


        }, "Thread-2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();
        System.out.println("done");
    }
}
