package cloud.foundry.cli.crosscutting.mapping;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class is a representation of yaml pointers. Yaml pointers point to a specific node in a yaml tree.
 * They are noted in a path-like syntax (like {@code #/persons/0/name}) which is similar to JSON pointers. Strings in a
 * pointer denote keys of mappings, whereas integers denote indices of sequence elements.
 */
public class YamlPointer {

    /**
     * The string that denotes the beginning of every yaml pointer.
     */
    public static final String POINTER_START = "#";

    private static final String POINTER_DELIMITER = "/";

    private final String[] nodeNames;

    /**
     * Creates a yaml pointer instance from a string.
     * @param pointer the string to be parsed as yaml pointer
     * @throws IllegalArgumentException if the pointer parameter has an invalid syntax
     * @throws NullPointerException if the pointer parameter is null
     */
    public YamlPointer(String pointer) {
        checkNotNull(pointer);
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
     * @param index index of the node name to return
     * @return the name of the node at the specified position
     * @throws IndexOutOfBoundsException if the index parameter is out of range
     */
    public String getNodeName(int index) {
        if (index < 0 || index >= nodeNames.length) {
            throw new IndexOutOfBoundsException("The node index is out of bounds");
        }
        return nodeNames[index];
    }

    /**
     * @return the number of node names in this pointer.
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
