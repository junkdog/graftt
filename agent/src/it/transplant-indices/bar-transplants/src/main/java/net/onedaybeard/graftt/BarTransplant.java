package net.onedaybeard.graftt;


@Graft.Recipient(Bar.class)
public class BarTransplant {
    @Graft.Fuse
    public static void bar() {
        // ok
    }
}
