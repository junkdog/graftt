package net.onedaybeard.graftt;

@Graft.Recipient(DeclaredField.class)
public class DeclaredFieldBrokenFieldTransplant {
    // N.B. default field values not yet supported
    private String name = "blake";
}
