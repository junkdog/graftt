package net.onedaybeard.graftt;

@Graft.Recipient(SomethingWithBadToString.class)
public class SomethingWithBadToStringTransplant {

    @Graft.Mock
    private int magic;

    @Graft.Fuse
    public final String toString() {
        // "3: hello" after transplant
        return magic + ": " + toString();
    }
}
