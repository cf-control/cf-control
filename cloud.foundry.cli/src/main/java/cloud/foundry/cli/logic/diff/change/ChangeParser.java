package cloud.foundry.cli.logic.diff.change;

import static com.google.common.base.Preconditions.checkNotNull;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.logic.diff.change.parsing.ParsingStrategy;
import org.javers.core.diff.Change;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class parses JaVers change objects to custom change objects.
 */
public class ChangeParser {

    private static final Log log = Log.getLog(ChangeParser.class);

    private Map<Class<? extends Change>, ParsingStrategy> parsers;

    public ChangeParser() {
        this.parsers = new HashMap<>();
    }

    public void addParsingStrategy(ParsingStrategy parsingStrategy) {
        for (Class<? extends Change> classType : parsingStrategy.getMatchingTypes()) {
            this.parsers.put(classType, parsingStrategy);
        }
    }

    /**
     * Parse the JaVers change object to a more appropriate custom change object.
     * @param change the JaVers change object
     * @return custom change object or null if the change type is not supported
     * @throws NullPointerException when change is null
     */
    public List<CfChange> parse(Change change) {
        checkNotNull(change);

        if (this.parsers.containsKey(change.getClass())) {
            return parsers.get(change.getClass()).parse(change);
        }

        log.debug("Change type " + change.getClass() + " is not supported for parsing. Ignoring it.");
        return Collections.emptyList();
    }

}
