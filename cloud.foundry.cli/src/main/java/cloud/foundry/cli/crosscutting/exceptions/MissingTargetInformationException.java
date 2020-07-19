package cloud.foundry.cli.crosscutting.exceptions;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isBlank;

import cloud.foundry.cli.crosscutting.mapping.beans.TargetBean;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Indicates that the provided target information is incomplete.
 */
public class MissingTargetInformationException extends RuntimeException {

    /**
     * Initializes the exception with an error message that is generated from the provided arguments.
     * Either the endpoint, the organization or the space (or multiple of them) has to be blank.
     *
     * @param targetBean the bean holding the incomplete target information
     * @throws IllegalArgumentException if the provided target information is fully specified
     */
    public MissingTargetInformationException(TargetBean targetBean) {
        super(determineErrorMessage(targetBean));
    }

    private static String determineErrorMessage(TargetBean targetBean) {
        String endpoint = targetBean.getEndpoint();
        String organization = targetBean.getOrg();
        String space = targetBean.getSpace();

        checkArgument(Stream.of(endpoint, organization, space).anyMatch(StringUtils::isBlank),
                "Either the endpoint, the organization or the space has to be blank");

        List<String> allMissingInformation = new LinkedList<>();

        if (isBlank(endpoint)) allMissingInformation.add("Endpoint");
        if (isBlank(organization)) allMissingInformation.add("Organization");
        if (isBlank(space)) allMissingInformation.add("Space");

        // this holds due to the previous argument check
        assert !allMissingInformation.isEmpty();

        StringBuilder errorMessageBuilder = new StringBuilder("The following target information is not provided: ");

        Iterator<String> missingInformationIterator = allMissingInformation.iterator();
        while (missingInformationIterator.hasNext()) {
            String missingInformation = missingInformationIterator.next();

            errorMessageBuilder.append(missingInformation);

            // to get the commas right
            if (missingInformationIterator.hasNext()) {
                errorMessageBuilder.append(", ");
            }
        }

        return errorMessageBuilder.toString();
    }
}
