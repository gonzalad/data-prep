package org.talend.dataprep.maintenance.preparation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.preparation.Identifiable;
import org.talend.dataprep.api.preparation.Preparation;
import org.talend.dataprep.api.preparation.Step;
import org.talend.dataprep.preparation.store.PreparationRepository;
import org.talend.tql.model.Expression;
import org.talend.tql.parser.Tql;

@Component
public class PreparationOrphanStepMarker implements OrphanStepMarker {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreparationOrphanStepMarker.class);

    @Override
    public Result mark(PreparationRepository repository, String marker) {
        if (repository.exist(Preparation.class, recentlyModified())) {
            LOGGER.info("Not running clean up (at least a preparation modified within last hour).");
            return Result.INTERRUPTED;
        }

        final AtomicBoolean interrupted = new AtomicBoolean(false);
        repository
                .list(Preparation.class) //
                .filter(p -> !interrupted.get()) //
                .forEach(p -> {
                    if (repository.exist(Preparation.class, recentlyModified())) {
                        LOGGER.info("Interrupting clean up (preparation modified within last hour).");
                        interrupted.set(true);
                        return;
                    }
                    final Collection<Identifiable> markedSteps = p
                            .getSteps()
                            .stream() //
                            .filter(s -> !Objects.equals(s, Step.ROOT_STEP))
                            .peek(s -> s.setMarker(marker)) //
                            .collect(Collectors.toList());
                    repository.add(markedSteps);
                });
        return interrupted.get() ? Result.INTERRUPTED : Result.COMPLETED;
    }

    private Expression recentlyModified() {
        return Tql.parse("lastModificationDate < " + Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli());
    }
}
