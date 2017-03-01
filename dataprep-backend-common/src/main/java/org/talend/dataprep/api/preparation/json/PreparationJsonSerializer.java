package org.talend.dataprep.api.preparation.json;

import static java.util.stream.Collectors.toList;
import static org.talend.dataprep.api.preparation.Step.ROOT_STEP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.preparation.*;
import org.talend.dataprep.preparation.store.PreparationRepository;
import org.talend.dataprep.transformation.api.action.metadata.common.ActionMetadata;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Serialize preparations in json.
 */
@Component
public class PreparationJsonSerializer extends JsonSerializer<Preparation> {

    /** This class' logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PreparationJsonSerializer.class);

    /** Where to find the preparations. */
    @Autowired(required = false)
    private PreparationRepository versionRepository;

    /** The list of actions to apply in preparations. */
    @Autowired
    private Collection<ActionMetadata> actionMetadata;

    final Predicate<Step> isNotRootStep = step -> !ROOT_STEP.id().equals(step.id());

    /**
     * @see JsonSerializer#serialize(Object, JsonGenerator, SerializerProvider)
     */
    @Override
    public void serialize(Preparation preparation, JsonGenerator generator, SerializerProvider serializerProvider)
            throws IOException {
        generator.writeStartObject();
        {
            generator.writeStringField("id", preparation.id()); //$NON-NLS-1$
            generator.writeStringField("dataSetId", preparation.getDataSetId()); //$NON-NLS-1$
            generator.writeStringField("author", preparation.getAuthor()); //$NON-NLS-1$
            generator.writeStringField("name", preparation.getName()); //$NON-NLS-1$
            generator.writeNumberField("creationDate", preparation.getCreationDate()); //$NON-NLS-1$
            generator.writeNumberField("lastModificationDate", preparation.getLastModificationDate()); //$NON-NLS-1$
            if (versionRepository != null && preparation.getStep() != null) {
                final List<Step> steps = PreparationUtils.listSteps(preparation.getStep(), versionRepository);

                // Steps ids
                final List<String> ids = steps.stream().map(Step::id).collect(toList());
                generator.writeObjectField("steps", ids); //$NON-NLS-1$

                // Steps diff metadata
                final List<StepDiff> diffs = steps.stream().filter(isNotRootStep).map(Step::getDiff).collect(toList());
                generator.writeObjectField("diff", diffs); //$NON-NLS-1$

                // Actions
                final Step head = versionRepository.get(preparation.getStep().id(), Step.class);
                final PreparationActions prepActions = versionRepository.get(head.getContent(), PreparationActions.class);
                final List<Action> actions = prepActions.getActions();
                generator.writeObjectField("actions", actions); //$NON-NLS-1$

                // Actions metadata
                writeActionMetadata(generator, actions, preparation);

            } else {
                LOGGER.debug("No version repository available, unable to serialize steps for preparation {}.", preparation.id());
            }
        }
        generator.writeEndObject();
        generator.flush();
    }

    /**
     * Write the action metadata using the generator for the given preparation.
     *
     * @param generator the json generator.
     * @param actions the actions to write.
     * @param preparation the preparation.
     * @throws IOException if an error occurs.
     */
    private void writeActionMetadata(JsonGenerator generator, List<Action> actions, Preparation preparation) throws IOException {

        if (actionMetadata == null) {
            LOGGER.debug("No action metadata available, unable to serialize action metadata for preparation {}.",
                    preparation.id());
            return;
        }

        List<ActionMetadata> metadataList = new ArrayList<>(actions.size());
        for (Action action : actions) {
            String actionName = action.getAction();
            for (ActionMetadata metadata : actionMetadata) {
                if (metadata.getName().equals(actionName)) {
                    metadataList.add(metadata);
                    break;
                }
            }
        }
        generator.writeObjectField("metadata", metadataList); //$NON-NLS-1$
    }
}