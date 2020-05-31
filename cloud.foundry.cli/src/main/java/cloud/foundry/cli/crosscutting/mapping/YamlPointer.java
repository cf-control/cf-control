package cloud.foundry.cli.crosscutting.mapping;

public class YamlPointer {

    private static final String POINTER_DELIMITER = "/";
    private static final String POINTER_START = "#";

    String pointer;
    String[] nodeNames;

    public YamlPointer(String pointer) {
        this.pointer = pointer;

        checkValidPointer();
        initializeNodeNames();
    }

    private void initializeNodeNames() {
        if (pointer.length() <= POINTER_START.length()) {
            // empty pointer contents
            nodeNames = new String[0];
            return;
        }

        nodeNames = pointer.split(POINTER_DELIMITER);

        for (int index = 0; index < nodeNames.length; ++index) {
            nodeNames[index] = resolveEscapeCharacters(nodeNames[index]);
        }
    }

    public String getNodeName(int index) {
        ++index; // skip pointer start
        if (index <= 0 || index >= nodeNames.length) {
            throw new IndexOutOfBoundsException("The node index is out of bounds");
        }
        return nodeNames[index];
    }

    public int getNumberOfNodeNames() {
        return Math.max(0, nodeNames.length - 1);
    }

    private static String resolveEscapeCharacters(String pointerContent) {
        pointerContent = pointerContent.replaceAll("~0", "~")
                .replaceAll("~1", "/");

        return pointerContent;
    }

    private void checkValidPointer() {
        if (!pointer.startsWith(POINTER_START)) {
            throw new IllegalArgumentException("The pointer does not start with '" + POINTER_START + "'");
        }
        if (pointer.length() > POINTER_START.length() + POINTER_DELIMITER.length() &&
                !pointer.startsWith(POINTER_START + POINTER_DELIMITER)) {
            throw new IllegalArgumentException("The pointer misses a '" + POINTER_DELIMITER + "' at the beginning");
        }
        if (pointer.contains(POINTER_DELIMITER + POINTER_DELIMITER)) {
            throw new IllegalArgumentException("The pointer contains an empty node name");
        }
        if (pointer.matches(".*~[^01].*")) {
            throw new IllegalArgumentException("The pointer contains an illegal escape sequence " +
                    "(a '~'-character is not followed by '0' or '1')");
        }
    }
}
