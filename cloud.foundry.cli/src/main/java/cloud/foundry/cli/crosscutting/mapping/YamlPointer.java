package cloud.foundry.cli.crosscutting.mapping;

import cloud.foundry.cli.crosscutting.exceptions.InvalidPointerException;

public class YamlPointer {

    private static final String POINTER_DELIMITER = "/";
    private static final String POINTER_START = "#/";

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

        String cutPointer = pointer.substring(POINTER_START.length());
        nodeNames = cutPointer.split(POINTER_DELIMITER);

        for (int index = 0; index < nodeNames.length; ++index) {
            nodeNames[index] = resolveEscapeCharacters(nodeNames[index]);
        }
    }

    public String getNodeName(int index) {
        if (index < 0 || index >= nodeNames.length) {
            throw new IndexOutOfBoundsException("The node index is out of bounds");
        }
        return nodeNames[index];
    }

    public int getNumberOfNodeNames() {
        return nodeNames.length;
    }

    private static String resolveEscapeCharacters(String pointerContent) {
        pointerContent = pointerContent.replaceAll("~0", "~")
                .replaceAll("~1", "/");

        return pointerContent;
    }

    private void checkValidPointer() {
        if (!pointer.startsWith(POINTER_START)) {
            throw new InvalidPointerException("The pointer does not start with '" + POINTER_START + "'", pointer);
        }
        if (pointer.contains(POINTER_DELIMITER + POINTER_DELIMITER)) {
            throw new InvalidPointerException("The pointer contains an empty node name", pointer);
        }
        if (pointer.matches(".*~[^01].*")) {
            throw new InvalidPointerException("The pointer contains an illegal escape sequence " +
                    "(a '~'-character is not followed by '0' or '1')", pointer);
        }
    }
}
