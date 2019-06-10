package net.onedaybeard.graftt;

@Graft.Recipient(MockedField.class)
public class MockedFieldTransplant {

    @Graft.Mock
    private String prepend = null;

    @Graft.Fuse
    public String withPrependField(String text) {
        // prepend translated to MockedField::prepend
        prepend = new StringBuilder(prepend).reverse().toString();
        return withPrependField(text);
    }
}
