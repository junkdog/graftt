package net.onedaybeard.graftt;

@Graft.Recipient(DeclaredMethod.class)
public class DeclaredMethodTransplant {
    private String suffix() { return "blake"; }

    @Graft.Fuse
    public String toUpperCase(String text) {
        return toUpperCase(text) + suffix();
    }
}
