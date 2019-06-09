package net.onedaybeard.graftt;

@Graft.Recipient(MockedField.class)
public class MockedFieldTransplant {

    @Graft.Mock
    private String prepend = null;

    @Graft.Fuse
    public String yolo(String text) {
        // prepend translated to MockedField::prepend
        prepend = new StringBuilder(text).reverse().toString();
        return yolo(text);
    }
}
