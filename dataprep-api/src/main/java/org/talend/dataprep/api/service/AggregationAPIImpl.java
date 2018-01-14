// ============================================================================
//
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

package org.talend.dataprep.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.talend.daikon.annotation.ServiceImplementation;
import org.talend.daikon.client.ClientService;
import org.talend.dataprep.services.api.AggregationAPI;
import org.talend.dataprep.services.api.AggregationParameters;
import org.talend.dataprep.services.transformation.AggregationResult;
import org.talend.dataprep.services.transformation.ITransformationService;

@ServiceImplementation
public class AggregationAPIImpl extends APIService implements AggregationAPI {

    @Autowired
    private ClientService clients;

    public AggregationResult compute(AggregationParameters input) {
        return clients.of(ITransformationService.class).aggregate(input);
    }

}
