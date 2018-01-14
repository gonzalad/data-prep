package org.talend.dataprep.services.dataset;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.springframework.web.bind.annotation.*;
import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.api.dataset.statistics.SemanticDomain;
import org.talend.dataprep.dataset.service.UserDataSetMetadata;
import org.talend.dataprep.exception.json.JsonErrorCodeDescription;
import org.talend.dataprep.metrics.Timed;
import org.talend.dataprep.metrics.VolumeMetered;
import org.talend.dataprep.security.PublicAPI;
import org.talend.dataprep.util.SortAndOrderHelper.Order;
import org.talend.dataprep.util.SortAndOrderHelper.Sort;

public interface IDataSetService {

    String CONTENT_TYPE = "Content-Type";

    @RequestMapping(value = "/datasets", method = RequestMethod.GET)
    @Timed
    Stream<UserDataSetMetadata> list(@RequestParam(defaultValue = "creationDate") Sort sort, //
                                     @RequestParam(defaultValue = "desc") Order order, //
                                     @RequestParam(required = false) String name, //
                                     @RequestParam(defaultValue = "false") boolean nameStrict, //
                                     @RequestParam(defaultValue = "false") boolean certified, //
                                     @RequestParam(defaultValue = "false") boolean favorite, //
                                     @RequestParam(defaultValue = "false") boolean limit);

    /**
     * Returns a list containing all data sets that are compatible with the data set with id <tt>dataSetId</tt>. If no
     * compatible data set is found an empty list is returned. The data set with id <tt>dataSetId</tt> is never returned
     * in the list.
     *
     * @param dataSetId the specified data set id
     * @param sort the sort criterion: either name or date.
     * @param order the sorting order: either asc or desc.
     * @return a list containing all data sets that are compatible with the data set with id <tt>dataSetId</tt> and
     * empty list if no data set is compatible.
     */
    @RequestMapping(value = "/datasets/{id}/compatibledatasets", method = RequestMethod.GET)
    @Timed
    Iterable<UserDataSetMetadata> listCompatibleDatasets(@PathVariable(value = "id") String dataSetId, //
            @RequestParam(defaultValue = "creationDate") Sort sort, //
            @RequestParam(defaultValue = "desc") Order order);

    /**
     * Creates a new data set and returns the new data set id as text in the response.
     *
     * @param name An optional name for the new data set (might be <code>null</code>).
     * @param size An optional size for the newly created data set.
     * @param contentType the request content type.
     * @param content The raw content of the data set (might be a CSV, XLS...) or the connection parameter in case of a
     * remote csv.
     * @return The new data id.
     * @see #get(boolean, boolean, String, String)
     */
    //@formatter:off
    @RequestMapping(value = "/datasets", method = POST, produces = TEXT_PLAIN_VALUE)
    @Timed
    @VolumeMetered
    String create(@RequestParam(defaultValue = "") String name, //
            @RequestParam(defaultValue = "") String tag, //
            @RequestParam(defaultValue = "0") long size, //
            @RequestHeader(CONTENT_TYPE) String contentType, //
            InputStream content) throws IOException;

    /**
     * Returns the <b>full</b> data set content for given id.
     *
     * @param metadata If <code>true</code>, includes data set metadata information.
     * @param dataSetId A data set id.
     * @return The full data set.
     */
    @RequestMapping(value = "/datasets/{id}/content", method = RequestMethod.GET)
    @Timed
    @ResponseBody
    Callable<DataSet> get(@RequestParam(defaultValue = "true") boolean metadata, //
            @RequestParam(defaultValue = "false") boolean includeInternalContent, //
            @RequestParam(value = "filter", defaultValue = "") String filter,
            @PathVariable(value = "id") String dataSetId);

    /**
     * Returns the data set {@link DataSetMetadata metadata} for given <code>dataSetId</code>.
     *
     * @param dataSetId A data set id. If <code>null</code> <b>or</b> if no data set with provided id exits, operation
     * returns {@link org.apache.commons.httpclient.HttpStatus#SC_NO_CONTENT} if metadata does not exist.
     */
    @RequestMapping(value = "/datasets/{id}/metadata", method = RequestMethod.GET)
    @Timed
    @ResponseBody
    DataSetMetadata getMetadata(@PathVariable(value = "id") String dataSetId);

    /**
     * Deletes a data set with provided id.
     *
     * @param dataSetId A data set id. If data set id is unknown, no exception nor status code to indicate this is set.
     */
    @RequestMapping(value = "/datasets/{id}", method = RequestMethod.DELETE)
    @Timed
    void delete(
            @PathVariable(value = "id") String dataSetId);

    /**
     * Copy this dataset to a new one and returns the new data set id as text in the response.
     *
     * @param copyName the name of the copy
     * @return The new data id.
     */
    @RequestMapping(value = "/datasets/{id}/copy", method = POST, produces = TEXT_PLAIN_VALUE)
    @Timed
    String copy(@PathVariable(value = "id") String dataSetId, @RequestParam(required = false) String copyName);

    /**
     * Updates a data set content and metadata. If no data set exists for given id, data set is silently created.
     *
     * @param dataSetId The id of data set to be updated.
     * @param name The new name for the data set. Empty name (or <code>null</code>) does not update dataset name.
     * @param dataSetContent The new content for the data set. If empty, existing content will <b>not</b> be replaced.
     * For delete operation, look at {@link #delete(String)}.
     */
    @RequestMapping(value = "/datasets/{id}/raw", method = PUT)
    @Timed
    @VolumeMetered
    void updateRawDataSet(
            @PathVariable(value = "id") String dataSetId, //
            @RequestParam(value = "name", required = false) String name, //
            @RequestParam(value = "size", required = false, defaultValue = "0") long size, //
            InputStream dataSetContent);

    /**
     * List all dataset related error codes.
     */
    @RequestMapping(value = "/datasets/errors", method = RequestMethod.GET)
    @Timed
    Iterable<JsonErrorCodeDescription> listErrors();

    /**
     * Returns preview of the the data set content for given id (first 100 rows). Service might return
     * {@link org.apache.commons.httpclient.HttpStatus#SC_ACCEPTED} if the data set exists but analysis is not yet fully
     * completed so content is not yet ready to be served.
     *
     * @param metadata If <code>true</code>, includes data set metadata information.
     * @param sheetName the sheet name to preview
     * @param dataSetId A data set id.
     */
    @RequestMapping(value = "/datasets/{id}/preview", method = RequestMethod.GET)
    @Timed
    @ResponseBody
    DataSet preview(
            @RequestParam(defaultValue = "true") boolean metadata, //
            @RequestParam(defaultValue = "") String sheetName, //
            @PathVariable(value = "id") String dataSetId);

    /**
     * Updates a data set metadata. If no data set exists for given id, a {@link TDPException} is thrown.
     *
     * @param dataSetId The id of data set to be updated.
     * @param dataSetMetadata The new content for the data set. If empty, existing content will <b>not</b> be replaced.
     * For delete operation, look at {@link #delete(String)}.
     */
    @RequestMapping(value = "/datasets/{id}", method = PUT)
    @Timed
    void updateDataSet(@PathVariable(value = "id") String dataSetId, @RequestBody DataSetMetadata dataSetMetadata);

    /**
     * list all the favorites dataset for the current user
     *
     * @return a list of the dataset Ids of all the favorites dataset for the current user or an empty list if none
     * found
     */
    @RequestMapping(value = "/datasets/favorites", method = RequestMethod.GET)
    @Timed
    Iterable<String> favorites();

    /**
     * update the current user data dataset favorites list by adding or removing the dataSetId according to the unset
     * flag. The user data for the current will be created if it does not exist. If no data set exists for given id, a
     * {@link TDPException} is thrown.
     *
     * @param unset, if true this will remove the dataSetId from the list of favorites, if false then it adds the
     * dataSetId to the favorite list
     * @param dataSetId, the id of the favorites data set. If the data set does not exists nothing is done.
     */
    @RequestMapping(value = "/datasets/{id}/favorite", method = PUT)
    @Timed
    void setFavorites(@RequestParam(defaultValue = "false") boolean unset, @PathVariable(value = "id") String dataSetId);

    /**
     * Update the column of the data set and computes the
     *
     * @param dataSetId the dataset id.
     * @param columnId the column id.
     * @param parameters the new type and domain.
     */
    @RequestMapping(value = "/datasets/{datasetId}/column/{columnId}", method = POST)
    @Timed
    void updateDatasetColumn(@PathVariable(value = "datasetId") String dataSetId, //
            @PathVariable(value = "columnId") String columnId, //
            @RequestBody UpdateColumnParameters parameters);

    /**
     * Search datasets.
     *
     * @param name what to searched in datasets.
     * @param strict If the searched name should be the full name
     * @return the list of found datasets metadata.
     * @deprecated please, use {@link #list(Sort, Order, String, boolean, boolean, boolean, boolean)} on {@code /datasets} enpoint
     * with name and nameStrict parameters.
     */
    @RequestMapping(value = "/datasets/search", method = GET)
    @Timed
    @Deprecated
    Stream<UserDataSetMetadata> search(@RequestParam String name, @RequestParam boolean strict);

    @RequestMapping(value = "/datasets/encodings", method = GET)
    @Timed
    @PublicAPI
    Stream<String> listSupportedEncodings();

    @RequestMapping(value = "/datasets/imports/{import}/parameters", method = GET, produces = APPLICATION_JSON_VALUE)
    @Timed
    @PublicAPI
    // This method have to return Object because it can either return the legacy List<Parameter> or the new TComp oriented
    // ComponentProperties
    Object getImportParameters(@PathVariable("import") String importType);

    @RequestMapping(value = "/datasets/{id}/datastore/properties", method = GET)
    @Timed
    // This method have to return Object because it can either return the legacy List<Parameter> or the new TComp oriented
    // ComponentProperties
    Object getDataStoreParameters(@PathVariable("id") String dataSetId);

    @RequestMapping(value = "/datasets/imports", method = GET)
    @Timed
    @PublicAPI
    Stream<Import> listSupportedImports();

    /**
     * Return the semantic types for a given dataset / column.
     *
     * @param datasetId the datasetId id.
     * @param columnId the column id.
     * @return the semantic types for a given dataset / column.
     */
    @RequestMapping(value = "/datasets/{datasetId}/columns/{columnId}/types", method = GET)
    @Timed
    @PublicAPI
    List<SemanticDomain> getDataSetColumnSemanticCategories(@PathVariable String datasetId, @PathVariable String columnId);
}
