package net.onedaybeard.graftt;

@Graft.Recipient(ReplaceOriginal.class)
public class ReplaceOriginalTransplant {

    @Graft.Fuse
    public boolean hmm() {
        // never calling original hmm(); the original
        // method is thus deleted from the ReplaceOriginal
        return true;
    }
}
