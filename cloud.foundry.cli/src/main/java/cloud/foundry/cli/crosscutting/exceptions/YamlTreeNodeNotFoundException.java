package cloud.foundry.cli.crosscutting.exceptions;

import cloud.foundry.cli.crosscutting.mapping.YamlPointer;

public class YamlTreeNodeNotFoundException extends RuntimeException {

    YamlPointer pointer;
    int nodeIndex;

    public YamlTreeNodeNotFoundException(String message, YamlPointer pointer, int nodeIndex) {
        super(message);
        this.pointer = pointer;
        this.nodeIndex = nodeIndex;
    }
}
