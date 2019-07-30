package net.onedaybeard.graftt;

import java.util.ArrayList;
import java.util.List;

public interface TransplantSubstitution {

    class Bar {
        private List<Bar> children = new ArrayList<>();

        public void add(Bar bar) {
            children.add(bar);
        }
    }

    @Graft.Recipient(Bar.class)
    class BarTransplant {
        @Graft.Mock
        private List<BarTransplant> children = new ArrayList<>();

        @Graft.Fuse
        public void add(BarTransplant bar) {
            children.add(bar);
        }
    }

    class Foo {
        Bar bar1;
        public Bar checkBars() {
            return bar1;
        }

        public void addChildren(Bar a, Bar b) {
            bar1.children.add(a);
            bar1.children.add(b);
        }
    }

    @Graft.Recipient(Foo.class)
    class FooTransplant {


        @Graft.Mock
        BarTransplant bar1;
        Bar bar2;

        public void init() {
            bar1 = new BarTransplant();
            bar1.add(new BarTransplant());
            bar2 = new Bar();
            bar2.add(new Bar());
            addChildren(checkBars(), bar2);
        }

        @Graft.Fuse
        public void addChildren(BarTransplant a, Bar b) {
            addChildren(a, b);
        }


        @Graft.Fuse @SuppressWarnings("EqualsBetweenInconvertibleTypes")
        public BarTransplant checkBars() {
            if (!bar1.getClass().equals(bar2.getClass()))
                throw new IllegalStateException("bar1 and bar2 are not the same class");

            return bar1;
        }
    }
}
