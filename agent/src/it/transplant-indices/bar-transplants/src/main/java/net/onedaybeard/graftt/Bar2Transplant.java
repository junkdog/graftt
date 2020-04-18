package net.onedaybeard.graftt;


@Graft.Recipient(Bar2.class)
public class Bar2Transplant {
    @Graft.Fuse
    public static void bar2() {
        // ok
    }
}
