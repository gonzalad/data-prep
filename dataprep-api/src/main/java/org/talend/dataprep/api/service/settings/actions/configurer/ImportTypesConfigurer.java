// ============================================================================
// Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.api.service.settings.actions.configurer;

import static org.talend.dataprep.api.service.settings.actions.provider.DatasetActions.DATASET_CREATE;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.daikon.client.ClientService;
import org.talend.dataprep.api.service.settings.AppSettingsConfigurer;
import org.talend.dataprep.api.service.settings.actions.api.ActionSettings;
import org.talend.dataprep.api.service.settings.actions.api.ActionSplitDropdownSettings;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.services.dataset.IDataSetService;
import org.talend.dataprep.services.dataset.Import;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Settings configurer that insert the imports types as the DATASET_CREATE split dropdown items.
 */
@Component
public class ImportTypesConfigurer extends AppSettingsConfigurer<ActionSettings> {

    @Autowired
    private ClientService clients;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public boolean isApplicable(final ActionSettings actionSettings) {
        return actionSettings == DATASET_CREATE;
    }

    @Override
    public ActionSettings configure(final ActionSettings actionSettings) {
        return ActionSplitDropdownSettings
                .from((ActionSplitDropdownSettings) actionSettings) //
                .items(getImportTypes().collect(Collectors.toList())) //
                .build();
    }

    private Stream<Import> getImportTypes() {
        try {
            return clients.of(IDataSetService.class).listSupportedImports();
        } catch (final TDPException e) {
            LOGGER.error("Unable to get import types", e);
            return Stream.empty();
        }
    }
}
