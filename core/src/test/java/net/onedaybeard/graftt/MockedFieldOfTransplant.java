package net.onedaybeard.graftt;

public interface MockedFieldOfTransplant {

    class Original {
        public boolean invoked = false;
        public boolean isInvoked() {
            return invoked;
        }
    }

    @Graft.Recipient(Original.class)
    class OriginalTransplant {
        @Graft.Mock
        public boolean invoked;

        public String newField;

        @Graft.Fuse
        public boolean isInvoked() {
            invoked = true;
            return isInvoked();
        }
    }

    class Foo {
        public Original original = new Original();

        public String doIt(String s) {
            return "" + original.isInvoked();
        }
    }

    @Graft.Recipient(Foo.class)
    class FooTransplant {
        @Graft.Mock
        public OriginalTransplant original;

        @Graft.Fuse
        public String doIt(String s) {
            original.newField = s;
            return original.newField + ": " + doIt(s);
        }

    }
}
