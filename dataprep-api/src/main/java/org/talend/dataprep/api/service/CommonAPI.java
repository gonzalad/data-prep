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

package org.talend.dataprep.api.service;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.IterableUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.talend.daikon.client.ClientService;
import org.talend.daikon.exception.error.ErrorCode;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.exception.error.APIErrorCodes;
import org.talend.dataprep.exception.error.CommonErrorCodes;
import org.talend.dataprep.metrics.Timed;
import org.talend.dataprep.services.dataset.IDataSetService;
import org.talend.dataprep.services.preparation.IPreparationService;
import org.talend.dataprep.services.transformation.ITransformationService;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.ApiOperation;

/**
 * Common API that does not stand in either DataSet, Preparation nor Transform.
 */
@RestController
public class CommonAPI extends APIService {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ClientService clients;

    /**
     * Describe the supported error codes.
     */
    @RequestMapping(value = "/api/errors", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all supported errors.", notes = "Returns the list of all supported errors.")
    @Timed
    public List<ErrorCode> listErrors() {

        LOG.debug("Listing supported error codes");

        // write the direct known errors
        List<ErrorCode> allErrors = new ArrayList<>();
        allErrors.addAll(Arrays.asList(CommonErrorCodes.values()));
        allErrors.addAll(Arrays.asList(APIErrorCodes.values()));
        allErrors.addAll(IterableUtils.toList(clients.of(IDataSetService.class).listErrors()));
        allErrors.addAll(IterableUtils.toList(clients.of(IPreparationService.class).listErrors()));
        allErrors.addAll(IterableUtils.toList(clients.of(ITransformationService.class).listErrors()));

        return allErrors;
    }

    /**
     * Describe the supported Types
     */
    @RequestMapping(value = "/api/types", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all types.")
    @Timed
    public Type[] listTypes() {
        LOG.debug("Listing supported types");
        return Arrays.stream(Type.values()) //
                .filter(type -> type != Type.UTC_DATETIME) //
                .collect(Collectors.toList()) //
                .toArray(new Type[0]);
    }
}
