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

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.io.InputStream;
import java.util.stream.Stream;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.talend.daikon.client.ClientService;
import org.talend.dataprep.api.action.ActionForm;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.service.api.DynamicParamsInput;
import org.talend.dataprep.command.dataset.DataSetGet;
import org.talend.dataprep.metrics.Timed;
import org.talend.dataprep.services.transformation.ExportParameters;
import org.talend.dataprep.services.transformation.ITransformationService;
import org.talend.dataprep.transformation.api.action.dynamic.GenericParameter;

import com.netflix.hystrix.HystrixCommand;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class TransformAPI extends APIService {

    @Autowired
    private ClientService clients;

    /**
     * Get all the possible actions for a given column.
     *
     * Although not rest compliant, this is done via a post in order to pass all the column metadata in the request body
     * without risking breaking the url size limit if GET would be used.
     *
     * @param body the column description (json encoded) in the request body.
     */
    @RequestMapping(value = "/api/transform/actions/column", method = RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all actions for a data set column.", notes = "Returns all actions for the given column.")
    @Timed
    public Stream<ActionForm> columnActions(@ApiParam(value = "Optional column Metadata content as JSON") ColumnMetadata body) {
        return clients.of(ITransformationService.class).columnActions(body);
    }

    /**
     * Suggest the possible actions for a given column.
     *
     * Although not rest compliant, this is done via a post in order to pass all the column metadata in the request body
     * without risking breaking the url size limit if GET would be used.
     *
     * @param body the column description (json encoded) in the request body.
     */
    @RequestMapping(value = "/api/transform/suggest/column", method = RequestMethod.POST, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get suggested actions for a data set column.", notes = "Returns the suggested actions for the given column in decreasing order of likeness.")
    @Timed
    public Stream<ActionForm> suggestColumnActions(@ApiParam(value = "Column Metadata content as JSON") ColumnMetadata body) {
        return clients.of(ITransformationService.class).suggest(body, 5);
    }

    /**
     * Get all the possible actions available on lines.
     */
    @RequestMapping(value = "/api/transform/actions/line", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all actions on line", notes = "Returns all actions for a line.")
    @Timed
    public Stream<ActionForm> lineActions() {
        return clients.of(ITransformationService.class).lineActions();
    }

    /**
     * Get all the possible actions available on the whole dataset.
     */
    @RequestMapping(value = "/api/transform/actions/dataset", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all actions the whole dataset.", notes = "Returns all actions for the whole dataset..")
    @Timed
    public Stream<ActionForm> datasetActions() {
        return clients.of(ITransformationService.class).datasetActions();
    }

    /**
     * Get the suggested action dynamic params. Dynamic params depends on the context (dataset / preparation / actual
     * transformations)
     */
    @RequestMapping(value = "/api/transform/suggest/{action}/params", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get the transformation dynamic parameters", notes = "Returns the transformation parameters.")
    @Timed
    public GenericParameter suggestActionParams(
            @ApiParam(value = "Transformation name.") @PathVariable("action") final String action,
            @ApiParam(value = "Suggested dynamic transformation input (preparation id or dataset id") @Valid final DynamicParamsInput dynamicParamsInput) {
        // get preparation/dataset content
        HystrixCommand<InputStream> inputData;
        final String preparationId = dynamicParamsInput.getPreparationId();
        if (isNotBlank(preparationId)) {
            ExportParameters parameters = new ExportParameters();
            parameters.setPreparationId(preparationId);
            parameters.setStepId(dynamicParamsInput.getStepId());
            parameters.setExportType("JSON");
            parameters.setFrom(ExportParameters.SourceType.HEAD);

            inputData = clients.of(ITransformationService.class).execute(parameters);
        } else {
            inputData = getCommand(DataSetGet.class, dynamicParamsInput.getDatasetId(), false, false);
        }

        return clients.of(ITransformationService.class).dynamicParams(action, dynamicParamsInput.getColumnId(), inputData.execute());
    }

    /**
     * Get the current dictionary (as serialized object).
     */
    @RequestMapping(value = "/api/transform/dictionary", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get current dictionary (as serialized object).", notes = "Returns a DQ dictionary serialized usin Java serialization and GZIP-ed.")
    @Timed
    public StreamingResponseBody getDictionary() {
        return clients.of(ITransformationService.class).getDictionary();
    }
}
