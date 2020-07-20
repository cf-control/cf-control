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

    /**
     * adds a parsing strategy to the parsing process that will be used in regards to its getMatchingTypes() method
     * @param parsingStrategy the strategy that should be added
     * @throws NullPointerException when the argument is null
     */
    public void addParsingStrategy(ParsingStrategy parsingStrategy) {
        checkNotNull(parsingStrategy);

        for (Class<? extends Change> classType : parsingStrategy.getMatchingTypes()) {
            this.parsers.put(classType, parsingStrategy);
        }
    }

    /**
     * Parse the JaVers change object to a more appropriate custom list of change objects.
     * @param change the JaVers change object
     * @return a list of change objects
     * @throws NullPointerException when the argument is null
     */
    public List<CfChange> parse(Change change) {
        checkNotNull(change);

        if (this.parsers.containsKey(change.getClass())) {
            return  parsers.get(change.getClass()).parse(change);
        }

        log.debug("Ignoring unsupported change type", change.getClass());
        return Collections.emptyList();
    }

}
