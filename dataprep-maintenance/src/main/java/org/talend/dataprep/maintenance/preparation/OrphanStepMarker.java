package org.talend.dataprep.maintenance.preparation;

import org.talend.dataprep.preparation.store.PreparationRepository;

public interface OrphanStepMarker {

    enum Result {
        COMPLETED,
        INTERRUPTED
    }

    Result mark(PreparationRepository repository, String marker);
}
