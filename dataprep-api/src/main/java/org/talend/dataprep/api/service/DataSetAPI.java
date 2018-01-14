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

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.talend.daikon.client.ClientService;
import org.talend.dataprep.api.action.ActionForm;
import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.api.dataset.statistics.SemanticDomain;
import org.talend.dataprep.api.preparation.Preparation;
import org.talend.dataprep.api.service.api.EnrichedDataSetMetadata;
import org.talend.dataprep.dataset.service.UserDataSetMetadata;
import org.talend.dataprep.metrics.Timed;
import org.talend.dataprep.preparation.service.UserPreparation;
import org.talend.dataprep.security.PublicAPI;
import org.talend.dataprep.services.dataset.IDataSetService;
import org.talend.dataprep.services.dataset.Import;
import org.talend.dataprep.services.dataset.UpdateColumnParameters;
import org.talend.dataprep.services.preparation.IPreparationService;
import org.talend.dataprep.services.transformation.ITransformationService;
import org.talend.dataprep.util.SortAndOrderHelper.Order;
import org.talend.dataprep.util.SortAndOrderHelper.Sort;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class DataSetAPI extends APIService {

    @Autowired
    private ClientService clients;

    /**
     * Create a dataset from request body content.
     *
     * @param name The dataset name.
     * @param contentType the request content type used to distinguish dataset creation or import.
     * @param dataSetContent the dataset content from the http request body.
     * @return The dataset id.
     */
    @RequestMapping(value = "/api/datasets", method = POST, produces = TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Create a data set", produces = TEXT_PLAIN_VALUE,
            notes = "Create a new data set based on content provided in POST body. For documentation purposes, body is typed as 'text/plain' but operation accepts binary content too. Returns the id of the newly created data set.")
    @Timed
    public Callable<String> create(@ApiParam(
            value = "User readable name of the data set (e.g. 'Finance Report 2015', 'Test Data Set').") @RequestParam(
                    defaultValue = "", required = false) String name,
            @ApiParam(value = "An optional tag to be added in data set metadata once created.") @RequestParam(
                    defaultValue = "", required = false) String tag,
            @ApiParam(value = "Size of the data set, in bytes.") @RequestParam(defaultValue = "0") long size,
            @RequestHeader(CONTENT_TYPE) String contentType, @ApiParam(value = "content") InputStream dataSetContent) {
        return () -> {
            LOG.debug("Creating dataset...");
            try {
                return clients.of(IDataSetService.class).create(name, tag, size, contentType, dataSetContent);
            } finally {
                LOG.debug("Dataset creation done.");
            }
        };
    }

    @RequestMapping(value = "/api/datasets/{id}", method = PUT, produces = TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Update a data set by id.", produces = TEXT_PLAIN_VALUE, //
            notes = "Create or update a data set based on content provided in PUT body with given id. For documentation purposes, body is typed as 'text/plain' but operation accepts binary content too. Returns the id of the newly created data set.")
    @Timed
    public Callable<String> createOrUpdateById(@ApiParam(
            value = "User readable name of the data set (e.g. 'Finance Report 2015', 'Test Data Set').") @RequestParam(
                    defaultValue = "", required = false) String name,
            @ApiParam(value = "Id of the data set to update / create") @PathVariable(value = "id") String id,
            @ApiParam(value = "Size of the data set, in bytes.") @RequestParam(defaultValue = "0") long size,
            @ApiParam(value = "content") InputStream dataSetContent) {
        return () -> {
            LOG.debug("Creating or updating dataset #{}...", id);
            clients.of(IDataSetService.class).updateRawDataSet(name, id, size, dataSetContent);
            return id;
        };
    }

    @RequestMapping(value = "/api/datasets/{id}/copy", method = POST, produces = TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Copy the dataset.", produces = TEXT_PLAIN_VALUE,
            notes = "Copy the dataset, returns the id of the copied created data set.")
    @Timed
    public Callable<String> copy(@ApiParam(value = "Name of the copy") @RequestParam(required = false) String name,
            @ApiParam(value = "Id of the data set to update / create") @PathVariable(value = "id") String id) {
        return () -> {
            LOG.debug("Copying {}...", id);
            final String copyId = clients.of(IDataSetService.class).copy(id, name);
            LOG.info("Dataset {} copied --> {} named '{}'", id, copyId, name);
            return copyId;
        };
    }

    @RequestMapping(value = "/api/datasets/{id}/metadata", method = PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Update a data set metadata by id.", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE, //
            notes = "Update a data set metadata based on content provided in PUT body with given id. For documentation purposes. Returns the id of the updated data set metadata.")
    @Timed
    public void updateMetadata(
            @ApiParam(value = "Id of the data set metadata to be updated") @PathVariable(value = "id") String id,
            @ApiParam(value = "content") DataSetMetadata dataSetContent) {
        LOG.debug("Creating or updating dataset #{}...", id);
        clients.of(IDataSetService.class).updateDataSet(id, dataSetContent);
        LOG.debug("Dataset creation or update for #{} done.", id);
    }

    @RequestMapping(value = "/api/datasets/{id}", method = POST, produces = TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Update a dataset.", produces = TEXT_PLAIN_VALUE, //
            notes = "Update a data set based on content provided in POST body with given id. For documentation purposes, body is typed as 'text/plain' but operation accepts binary content too.")
    @Timed
    public Callable<String> update(
            @ApiParam(value = "Id of the data set to update / create") @PathVariable(value = "id") String id,
            @ApiParam(value = "content") InputStream dataSetContent) {
        return () -> {
            LOG.debug("Creating or updating dataset #{}...", id);
            clients.of(IDataSetService.class).updateRawDataSet(id, "", 0, dataSetContent);
            LOG.debug("Dataset creation or update for #{} done.", id);
            return id;
        };
    }

    @RequestMapping(value = "/api/datasets/{datasetId}/column/{columnId}", method = POST,
            consumes = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Update a dataset.", consumes = APPLICATION_JSON_VALUE, //
            notes = "Update a data set based on content provided in POST body with given id. For documentation purposes, body is typed as 'text/plain' but operation accepts binary content too.")
    @Timed
    public void updateColumn(
            @PathVariable(value = "datasetId") @ApiParam(value = "Id of the dataset to update") final String datasetId,
            @PathVariable(value = "columnId") @ApiParam(value = "Id of the column to update") final String columnId,
            @RequestBody final UpdateColumnParameters parameters) {
        LOG.debug("Creating or updating dataset #{}...", datasetId);
        clients.of(IDataSetService.class).updateDatasetColumn(datasetId, columnId, parameters);
        LOG.debug("Dataset creation or update for #{} done.", datasetId);
    }

    @RequestMapping(value = "/api/datasets/{id}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get a data set by id.", produces = APPLICATION_JSON_VALUE,
            notes = "Get a data set based on given id.")
    @Timed
    public Callable<DataSet> get(@ApiParam(value = "Id of the data set to get") @PathVariable(value = "id") String id,
            @ApiParam(value = "Whether output should be the full data set (true) or not (false).") @RequestParam(
                    value = "fullContent", defaultValue = "false") boolean fullContent,
            @ApiParam(value = "Filter for retrieved content.") @RequestParam(value = "filter",
                    defaultValue = "") String filter,
            @ApiParam(value = "Whether to include internal technical properties (true) or not (false).") @RequestParam(
                    value = "includeTechnicalProperties", defaultValue = "false") boolean includeTechnicalProperties) {
        LOG.debug("Requesting dataset #{}...", id);
        try {
            return clients.of(IDataSetService.class).get(true, includeTechnicalProperties, filter, id);
        } finally {
            LOG.debug("Request dataset #{} (pool: {}) done.", id);
        }
    }

    /**
     * Return the dataset metadata.
     *
     * @param id the wanted dataset metadata.
     * @return the dataset metadata or no content if not found.
     */
    @RequestMapping(value = "/api/datasets/{id}/metadata", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get a data set metadata by id.", produces = APPLICATION_JSON_VALUE,
            notes = "Get a data set metadata based on given id.")
    @Timed
    public DataSetMetadata
            getMetadata(@ApiParam(value = "Id of the data set to get") @PathVariable(value = "id") String id) {
        LOG.debug("Requesting dataset metadata #{}...", id);
        try {
            return clients.of(IDataSetService.class).getMetadata(id);
        } finally {
            LOG.debug("Request dataset metadata #{} done.", id);
        }
    }

    @RequestMapping(value = "/api/datasets/preview/{id}", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get a data set by id.", produces = APPLICATION_JSON_VALUE,
            notes = "Get a data set based on given id.")
    @Timed
    public DataSet preview(@ApiParam(value = "Id of the data set to get") @PathVariable(value = "id") String id,
            @RequestParam(defaultValue = "true") @ApiParam(name = "metadata",
                    value = "Include metadata information in the response") boolean metadata,
            @RequestParam(defaultValue = "") @ApiParam(name = "sheetName",
                    value = "Sheet name to preview") String sheetName) {
        LOG.debug("Requesting dataset #{}...", id);
        try {
            return clients.of(IDataSetService.class).preview(metadata, sheetName, id);
        } finally {
            LOG.debug("Request dataset #{} (pool: {}) done.", id);
        }
    }

    @RequestMapping(value = "/api/datasets", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List data sets.", produces = APPLICATION_JSON_VALUE,
            notes = "Returns a list of data sets the user can use.")
    @Timed
    public Callable<Stream<UserDataSetMetadata>> list(
            @ApiParam(value = "Sort key (by name or date), defaults to 'date'.") @RequestParam(
                    defaultValue = "creationDate") Sort sort,
            @ApiParam(value = "Order for sort key (desc or asc), defaults to 'desc'.") @RequestParam(
                    defaultValue = "desc") Order order,
            @ApiParam(value = "Filter on name containing the specified name") @RequestParam(
                    defaultValue = "") String name,
            @ApiParam(value = "Filter on certified data sets") @RequestParam(defaultValue = "false") boolean certified,
            @ApiParam(value = "Filter on favorite data sets") @RequestParam(defaultValue = "false") boolean favorite,
            @ApiParam(value = "Filter on recent data sets") @RequestParam(defaultValue = "false") boolean limit) {
        return () -> {
            try {
                return clients.of(IDataSetService.class).list(sort, order, name, false, certified, favorite, limit);
            } finally {
                LOG.info("listing datasets done [favorite: {}, certified: {}, name: {}, limit: {}]", favorite,
                        certified, name, limit);
            }
        };
    }

    @RequestMapping(value = "/api/datasets/summary", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List data sets summary.", produces = APPLICATION_JSON_VALUE,
            notes = "Returns a list of data sets summary the user can use.")
    @Timed
    public Callable<Stream<EnrichedDataSetMetadata>> listSummary(
            @ApiParam(value = "Sort key (by name or date), defaults to 'date'.") @RequestParam(
                    defaultValue = "creationDate") Sort sort,
            @ApiParam(value = "Order for sort key (desc or asc), defaults to 'desc'.") @RequestParam(
                    defaultValue = "desc") Order order,
            @ApiParam(value = "Filter on name containing the specified name") @RequestParam(
                    defaultValue = "") String name,
            @ApiParam(value = "Filter on certified data sets") @RequestParam(defaultValue = "false") boolean certified,
            @ApiParam(value = "Filter on favorite data sets") @RequestParam(defaultValue = "false") boolean favorite,
            @ApiParam(value = "Filter on recent data sets") @RequestParam(defaultValue = "false") boolean limit) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Listing datasets summary (pool: {})...", getConnectionStats());
        }
        return () -> {
            final Stream<UserDataSetMetadata> dataSetList = clients.of(IDataSetService.class).list(sort, order, name, false, certified, favorite, limit);
            return dataSetList.map(m -> {
                LOG.debug("found dataset {} in the summary list" + m.getName());
                // Add the related preparations list to the given dataset metadata.
                final List<Preparation> preparations = clients.of(IPreparationService.class) //
                        .searchPreparations(m.getId(), "", "", false, "", Sort.LAST_MODIFICATION_DATE, Order.DESC) //
                        .filter(p -> p.getSteps() != null) //
                        .collect(Collectors.toList());
                return new EnrichedDataSetMetadata(m, preparations);
            });
        };
    }

    /**
     * Returns a list containing all data sets metadata that are compatible with the data set with id <tt>id</tt>. If no
     * compatible data set is found an empty list is returned. The data set with id <tt>dataSetId</tt> is never returned
     * in the list.
     *
     * @param id the specified data set id
     * @param sort the sort criterion: either name or date.
     * @param order the sorting order: either asc or desc
     * @return a list containing all data sets metadata that are compatible with the data set with id <tt>id</tt> and
     * empty list if no data set is compatible.
     */
    @RequestMapping(value = "/api/datasets/{id}/compatibledatasets", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List compatible data sets.", produces = APPLICATION_JSON_VALUE,
            notes = "Returns a list of data sets that are compatible with the specified one.")
    @Timed
    public Iterable<UserDataSetMetadata> listCompatibleDatasets(
            @ApiParam(value = "Id of the data set to get") @PathVariable(value = "id") String id,
            @ApiParam(value = "Sort key (by name or date), defaults to 'date'.") @RequestParam(
                    defaultValue = "creationDate") Sort sort,
            @ApiParam(value = "Order for sort key (desc or asc), defaults to 'desc'.") @RequestParam(
                    defaultValue = "desc") Order order) {
        return clients.of(IDataSetService.class).listCompatibleDatasets(id, sort, order);
    }

    /**
     * Returns a list containing all preparations that are compatible with the data set with id <tt>id</tt>. If no
     * compatible preparation is found an empty list is returned.
     *
     * @param dataSetId the specified data set id
     * @param sort the sort criterion: either name or date.
     * @param order the sorting order: either asc or desc
     */
    @RequestMapping(value = "/api/datasets/{id}/compatiblepreparations", method = GET,
            produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List compatible preparations.", produces = APPLICATION_JSON_VALUE,
            notes = "Returns a list of data sets that are compatible with the specified one.")
    @Timed
    public Callable<Stream<UserPreparation>> listCompatiblePreparations(
            @ApiParam(value = "Id of the data set to get") @PathVariable(value = "id") String dataSetId,
            @ApiParam(value = "Sort key (by name or date), defaults to 'modification'.") @RequestParam(
                    defaultValue = "lastModificationDate") Sort sort,
            @ApiParam(value = "Order for sort key (desc or asc), defaults to 'desc'.") @RequestParam(
                    defaultValue = "desc") Order order) {
        return () -> {
            LOG.debug("Listing compatible preparations...");
            // get the list of compatible data sets
            final List<UserDataSetMetadata> dataSets = IterableUtils.toList(clients.of(IDataSetService.class).listCompatibleDatasets(dataSetId, sort, order));
            // get list of preparations
            final Stream<UserPreparation> stream =
                    clients.of(IPreparationService.class).listAll("", "", "", sort, order);
            return stream.filter(p -> dataSets
                    .flatMapIterable(l -> l) //
                    .map(DataSetMetadata::getId) //
                    .any(id -> StringUtils.equals(id, p.getDataSetId()) || dataSetId.equals(p.getDataSetId())) //
                    .block() //
            );
        };
    }

    @RequestMapping(value = "/api/datasets/{id}", method = DELETE)
    @ApiOperation(value = "Delete a data set by id",
            notes = "Delete a data set content based on provided id. Id should be a UUID returned by the list operation. Not valid or non existing data set id returns empty content.")
    @Timed
    public void delete(@PathVariable(value = "id") @ApiParam(name = "id",
            value = "Id of the data set to delete") String dataSetId) {
            LOG.debug("Delete dataset #{}...", dataSetId);
        clients.of(IDataSetService.class).delete(dataSetId);
            LOG.debug("Listing datasets done.");
    }

    @RequestMapping(value = "/api/datasets/{id}/actions", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get suggested actions for a whole data set.",
            notes = "Returns the suggested actions for the given dataset in decreasing order of likeness.")
    @Timed
    public List<ActionForm> suggestDatasetActions(@PathVariable(value = "id") @ApiParam(name = "id",
            value = "Data set id to get suggestions from.") String dataSetId) {
        // Get dataset metadata
        final DataSetMetadata metadata = clients.of(IDataSetService.class).getMetadata(dataSetId);
        // Asks transformation service for suggested actions for column type and domain...
        final List<ActionForm> suggestions = clients.of(ITransformationService.class).suggest(metadata);
        // ... also adds lookup actions
        clients.of(ITransformationService.class).suggest()
        // Returns actions
        return suggestions;
    }

    @RequestMapping(value = "/api/datasets/favorite/{id}", method = POST, produces = TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Set or Unset the dataset as favorite for the current user.", produces = TEXT_PLAIN_VALUE, //
            notes = "Specify if a dataset is or is not a favorite for the current user.")
    @Timed
    public Callable<String> favorite(
            @ApiParam(value = "Id of the favorite data set ") @PathVariable(value = "id") String id,
            @RequestParam(defaultValue = "false") @ApiParam(name = "unset",
                    value = "When true, will remove the dataset from favorites, if false (default) this will set the dataset as favorite.") boolean unset) {
        return () -> {
                LOG.debug((unset ? "Unset" : "Set") + " favorite dataset #{}...", id);
                clients.of(IDataSetService.class).setFavorites(unset, id);
            LOG.debug("Set Favorite for user #{} done.", id);
            return id;
        };
    }

    @RequestMapping(value = "/api/datasets/encodings", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List supported dataset encodings.", notes = "Returns the supported dataset encodings.")
    @Timed
    @PublicAPI
    public Stream<String> listEncodings() {
        return clients.of(IDataSetService.class).listSupportedEncodings();
    }

    @RequestMapping(value = "/api/datasets/imports/{import}/parameters", method = GET,
            produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Fetch the parameters needed to imports a dataset.",
            notes = "Returns the parameters needed to imports a dataset.")
    @Timed
    @PublicAPI
    public Object getImportParameters(@PathVariable("import") final String importType) {
        return clients.of(IDataSetService.class).getImportParameters(importType);
    }

    @RequestMapping(value = "/api/datasets/imports", method = GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List supported imports for a dataset.", notes = "Returns the supported import types.")
    @Timed
    @PublicAPI
    public Callable<Stream<Import>> listImports() {
        return () -> clients.of(IDataSetService.class).listSupportedImports();
    }

    /**
     * Return the semantic types for a given dataset / column.
     *
     * @param datasetId the dataset id.
     * @param columnId the column id.
     * @return the semantic types for a given dataset / column.
     */
    @RequestMapping(value = "/api/datasets/{datasetId}/columns/{columnId}/types", method = GET,
            produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "list the types of the wanted column",
            notes = "This list can be used by user to change the column type.")
    @Timed
    @PublicAPI
    public Callable<List<SemanticDomain>> getDataSetColumnSemanticCategories(
            @ApiParam(value = "The dataset id") @PathVariable String datasetId,
            @ApiParam(value = "The column id") @PathVariable String columnId) {
        return () -> {
            LOG.debug("listing semantic types for dataset {}, column {}", datasetId, columnId);
            return clients.of(IDataSetService.class).getDataSetColumnSemanticCategories(datasetId, columnId);
        };
    }
}
