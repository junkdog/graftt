package net.onedaybeard.graftt;

@Graft.Recipient(MockedMethod.class)
public class MockedMethodTransplant {

    @Graft.Mock
    private String more() {
        return null;
    }

    @Graft.Fuse
    public final String withMethod(String text) {
        return withMethod(text) + " " + more();
    }
}
