package de.unihannover.se.processSimulation.preCommitPostCommit;

import java.util.concurrent.ExecutionException;

import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;

class TestFiber extends Fiber<Integer> {
    @Override
    public Integer run() throws SuspendExecution {
        for (int i = 0; i < 10; i++) {
            System.out.println("in fiber before park, i=" + i);
            Fiber.park();
        }
        System.out.println("in fiber after loop");
        return 1;
    }
};

public class FiberTest {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final TestFiber f = new TestFiber();
        System.out.println("about to start");
        f.start();
        System.out.println("started");
        for (int i = 0; i < 10; i++) {
            Thread.sleep(2000);
            System.out.println("slept, i=" + i);
            realUnpark(f);
        }
        Debug.dumpRecorder();
        System.out.println("got " + f.get());
    }

    private static synchronized void realUnpark(final TestFiber f) {
        f.unpark();
        System.out.println("unparked");
//        f.unpark();
//        System.out.println("unparked again");
    }

//    @Test
//    public void testLowLevelStuff() throws InterruptedException {
//
//        final Thread t = new Thread() {
//            @Override
//            public void run() {
//                boolean parked = false;
//                do {
//                    if (parked) {
//                        parkData.restore
//                    }
//                    parked = false;
//                    try {
//                        this.doA();
//                        this.doB();
//                    } catch (final Park park) {
//                        parked = true;
//                    } catch (final SuspendExecution e) {
//                        throw new RuntimeException("should not happen", e);
//                    }
//                }
//                while (parked);
//            }
//
//            private void doA() {
//                int i = 0;
//                System.out.println("zaehler in a = " + i++);
//                doPark();
//                System.out.println("zaehler in a = " + i++);
//                System.out.println("zaehler in a = " + i++);
//            }
//
//            private void doB() {
//                int i = 0;
//                System.out.println("zaehler in b = " + i++);
//                System.out.println("zaehler in b = " + i++);
//                System.out.println("zaehler in b = " + i++);
//            }
//
//            private void doPark() {
//                parkData = ParkData.create();
//                throw ParkableForkJoinTask.PARK;
//            }
//        };
//
//        t.start();
//        t.join();
//
////        final Thread t = Thread.currentThread();
////
////        final Object threadLocals = ThreadAccess.getThreadLocals(t);
////        final Object inheritableThreadLocals = ThreadAccess.getInheritableThreadLocals(t);
////
////        ThreadAccess.setThreadLocals(t, threadLocals);
////        ThreadAccess.setInheritablehreadLocals(t, inheritableThreadLocals);
//    }
//
//    private static final class ParkData {
//        final Object threadLocals;
//        final Object inheritableThreadLocals;
//
//        private ParkData(Object tl, Object itl) {
//            this.threadLocals = tl;
//            this.inheritableThreadLocals = itl;
//        }
//
//        public static ParkData create() {
//            final Thread currentThread = Thread.currentThread();
//            final Object threadLocals = ThreadAccess.getThreadLocals(currentThread);
//            final Object inheritableThreadLocals = ThreadAccess.getInheritableThreadLocals(currentThread);
//            return new ParkData(threadLocals, inheritableThreadLocals);
//        }
//
//        public void restore() {
//            final Thread currentThread = Thread.currentThread();
//            ThreadAccess.setThreadLocals(currentThread, this.threadLocals);
//            ThreadAccess.setInheritablehreadLocals(currentThread, this.inheritableThreadLocals);
//        }
//    }

}
