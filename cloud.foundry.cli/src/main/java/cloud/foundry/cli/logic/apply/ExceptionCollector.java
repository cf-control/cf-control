package cloud.foundry.cli.logic.apply;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * TODO
 * will be an object that collects all exceptions that occur during each change/delete/create request to the cf instance
 */
public class ExceptionCollector {

    /**
     *  TODO determine list type
     */
    private List<?> errors;

    ExceptionCollector() {
        this.errors = Collections.synchronizedList(new LinkedList<>());
    }

    /**
     * TODO determine method parameter
     */

    public void add() {

    }

    /**
     * TODO determine method parameter
     */
    public List<?> getErrors() {
        return null;
    }

}
