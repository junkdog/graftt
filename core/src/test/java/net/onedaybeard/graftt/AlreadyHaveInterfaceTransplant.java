package net.onedaybeard.graftt;

@Graft.Recipient(AlreadyHaveInterface.class)
public class AlreadyHaveInterfaceTransplant implements Point {
    @Override public int x() { return -1; }
    @Override public int y() { return -1; }
}
