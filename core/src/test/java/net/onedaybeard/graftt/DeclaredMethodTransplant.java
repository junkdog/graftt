package net.onedaybeard.graftt;

@Graft.Recipient(DeclaredMethod.class)
public class DeclaredMethodTransplant {
    private String exclamation() { return "!!!"; }

    @Graft.Fuse
    public String toUpperCase(String text) {
        return toUpperCase(text) + exclamation();
    }
}
