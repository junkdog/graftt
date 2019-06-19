package net.onedaybeard.graftt;

@Graft.Recipient(MockParentFieldImpl.class)
public class MockParentFieldImplTransplant {
    @Graft.Mock
    public int foo = -1;

    @Override
    public String toString() {
        return "" + foo;
    }
}
