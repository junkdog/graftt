package net.onedaybeard.graftt;

@Graft.Recipient(MockParentMethodImpl.class)
public class MockParentMethodImplTransplant {
    @Graft.Mock
    private final int bar() { return -1; }

    @Override
    public String toString() {
        return "" + bar();
    }
}
