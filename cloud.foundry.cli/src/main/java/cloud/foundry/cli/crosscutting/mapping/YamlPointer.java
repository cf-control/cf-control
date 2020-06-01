package cloud.foundry.cli.crosscutting.mapping;

/**
 * This class is a representation of yaml pointers. Yaml pointers point to a specific node in a yaml tree.
 * They are noted in a path-like syntax (like {@code #/persons/0/name}). Strings in a pointer denote keys of mappings,
 * whereas integers denote indices of sequence elements.
 */
public class YamlPointer {

    private static final String POINTER_DELIMITER = "/";
    private static final String POINTER_START = "#";

    private final String[] nodeNames;

    /**
     * TODO documentation
     */
    public YamlPointer(String pointer) {
        checkValidPointer(pointer);

        String pointerWithoutStart = pointer.substring(POINTER_START.length());
        if (pointerWithoutStart.startsWith(POINTER_DELIMITER)) {
            pointerWithoutStart = pointerWithoutStart.substring(POINTER_DELIMITER.length());
        }

        if (pointerWithoutStart.isEmpty()) {
            nodeNames = new String[0];
            return;
        }

        nodeNames = pointerWithoutStart.split(POINTER_DELIMITER);

        for (int index = 0; index < nodeNames.length; ++index) {
            nodeNames[index] = resolveEscapeCharacters(nodeNames[index]);
        }
    }

    /**
     * TODO documentation
     */
    public String getNodeName(int index) {
        if (index < 0 || index >= nodeNames.length) {
            throw new IndexOutOfBoundsException("The node index is out of bounds");
        }
        return nodeNames[index];
    }

    /**
     * TODO documentation
     */
    public int getNumberOfNodeNames() {
        return nodeNames.length;
    }

    private static String resolveEscapeCharacters(String pointerContent) {
        pointerContent = pointerContent.replaceAll("~0", "~")
                .replaceAll("~1", "/");

        return pointerContent;
    }

    private void checkValidPointer(String pointer) {
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
