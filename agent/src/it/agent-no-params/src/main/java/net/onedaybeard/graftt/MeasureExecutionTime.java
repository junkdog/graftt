package net.onedaybeard.graftt;

public interface MeasureExecutionTime {

    interface Timer {
        void measured(String name, int ms);

        static void report(String name, int ms) {
            if (TimerHolder.timer != null)
                TimerHolder.timer.measured(name, ms);
        }
    }

    class TimerHolder {
        static Timer timer = null;
    }

    class Foo {
        public void expensiveOperation() {
            try {
                Thread.sleep(500l);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Graft.Recipient(Foo.class)
    class FooTransplant {

        @Graft.Fuse
        public void expensiveOperation() {
            long start = System.nanoTime() / 1_000_000;
            expensiveOperation();
            long end = System.nanoTime() / 1_000_000;

            Timer.report("expensiveOperation", (int)(end - start));
        }

    }
}
