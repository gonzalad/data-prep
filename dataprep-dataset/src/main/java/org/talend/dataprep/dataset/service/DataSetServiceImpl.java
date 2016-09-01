// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.dataset.service;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.talend.dataprep.exception.error.DataSetErrorCodes.DATASET_NAME_ALREADY_USED;
import static org.talend.dataprep.exception.error.DataSetErrorCodes.UNABLE_TO_CREATE_OR_UPDATE_DATASET;
import static org.talend.dataprep.util.SortAndOrderHelper.getDataSetMetadataComparator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.jms.Message;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.talend.daikon.annotation.ServiceImplementation;
import org.talend.daikon.exception.ExceptionContext;
import org.talend.dataprep.api.dataset.*;
import org.talend.dataprep.api.dataset.DataSetGovernance.Certification;
import org.talend.dataprep.api.dataset.location.LocalStoreLocation;
import org.talend.dataprep.api.dataset.location.SemanticDomain;
import org.talend.dataprep.api.dataset.location.locator.DataSetLocatorService;
import org.talend.dataprep.api.dataset.row.DataSetRow;
import org.talend.dataprep.api.dataset.row.FlagNames;
import org.talend.dataprep.api.service.info.VersionService;
import org.talend.dataprep.api.user.UserData;
import org.talend.dataprep.configuration.EncodingSupport;
import org.talend.dataprep.dataset.event.DataSetMetadataBeforeUpdateEvent;
import org.talend.dataprep.dataset.event.DataSetRawContentUpdateEvent;
import org.talend.dataprep.dataset.service.analysis.DataSetAnalyzer;
import org.talend.dataprep.dataset.service.analysis.asynchronous.AsyncBackgroundAnalysis;
import org.talend.dataprep.dataset.service.analysis.asynchronous.AsynchronousDataSetAnalyzer;
import org.talend.dataprep.dataset.service.analysis.asynchronous.SyncBackgroundAnalyzer;
import org.talend.dataprep.dataset.service.analysis.synchronous.*;
import org.talend.dataprep.dataset.service.api.Import;
import org.talend.dataprep.dataset.service.api.UpdateColumnParameters;
import org.talend.dataprep.dataset.store.content.ContentStoreRouter;
import org.talend.dataprep.dataset.store.metadata.DataSetMetadataRepository;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.DataSetErrorCodes;
import org.talend.dataprep.exception.json.JsonErrorCodeDescription;
import org.talend.dataprep.grants.AccessGrantChecker;
import org.talend.dataprep.grants.CommonRestrictedActions;
import org.talend.dataprep.http.HttpResponseContext;
import org.talend.dataprep.lock.DistributedLock;
import org.talend.dataprep.log.Markers;
import org.talend.dataprep.parameters.Parameter;
import org.talend.dataprep.schema.DraftValidator;
import org.talend.dataprep.schema.FormatFamily;
import org.talend.dataprep.schema.Schema;
import org.talend.dataprep.security.Security;
import org.talend.dataprep.user.store.UserDataRepository;
import org.talend.dataprep.util.StringsHelper;
import org.talend.services.dataprep.DataSetService;


@ServiceImplementation
public class DataSetServiceImpl extends BaseDataSetService implements DataSetService {

    /**
     * This class' logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DataSetServiceImpl.class);

    /**
     * Date format to use.
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-YYYY HH:mm"); // $NON-NLS-1

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
    }

    /**
     * Quality analyzer needed to compute quality on dataset.
     */
    @Autowired
    protected QualityAnalysis qualityAnalyzer;

    /**
     * Dataset metadata repository.
     */
    @Autowired
    protected DataSetMetadataRepository dataSetMetadataRepository;

    /**
     * Dataset content store.
     */
    @Autowired
    protected ContentStoreRouter contentStore;

    @Autowired
    private ApplicationEventPublisher publisher;

    /**
     * DQ asynchronous analyzers.
     */
    @Autowired(required = false)
    private AsynchronousDataSetAnalyzer[] asynchronousAnalyzers = new AsynchronousDataSetAnalyzer[0];

    /**
     * DQ synchronous analyzers.
     */
    @Autowired
    private List<SynchronousDataSetAnalyzer> synchronousAnalyzers;

    /**
     * Format analyzer needed to update the schema.
     */
    @Autowired
    private FormatAnalysis formatAnalyzer;

    /**
     * JMS template used to call asynchronous analysers.
     */
    @Autowired
    private JmsTemplate jmsTemplate;

    /**
     * User repository.
     */
    @Autowired
    private UserDataRepository userDataRepository;

    /**
     * Format guess factory.
     */
    @Autowired
    private FormatFamily.Factory formatFamilyFactory;

    /**
     * Dataset locator (used for remote datasets).
     */
    @Autowired
    private DataSetLocatorService datasetLocator;

    /**
     * DataPrep abstraction to the underlying security (whether it's enabled or not).
     */
    @Autowired
    private Security security;

    /**
     * Encoding support service.
     */
    @Autowired
    private EncodingSupport encodings;

    /**
     * All possible data set locations.
     */
    @Autowired
    private List<DataSetLocation> locations;

    /**
     * DataSet metadata builder.
     */
    @Autowired
    private DataSetMetadataBuilder metadataBuilder;

    @Autowired
    private VersionService versionService;

    @Autowired
    private AccessGrantChecker accessGrantChecker;

    @Value("#{'${dataset.imports}'.split(',')}")
    private Set<String> enabledImports;

    @Value("${dataset.list.limit:10}")
    private int datasetListLimit;

    /**
     * Sort the synchronous analyzers.
     */
    @PostConstruct
    public void initialize() {
        synchronousAnalyzers.sort((analyzer1, analyzer2) -> analyzer1.order() - analyzer2.order());
    }

    /**
     * Performs the analysis on the given dataset id.
     *
     * @param id the dataset id.
     * @param analysersToSkip the list of analysers to skip.
     */
    @SafeVarargs
    private final void queueEvents(String id, Class<? extends DataSetAnalyzer>... analysersToSkip) {

        List<Class<? extends DataSetAnalyzer>> toSkip = Arrays.asList(analysersToSkip);

        // Calls all synchronous analysis first
        for (SynchronousDataSetAnalyzer synchronousDataSetAnalyzer : synchronousAnalyzers) {
            if (toSkip.contains(synchronousDataSetAnalyzer.getClass())) {
                continue;
            }
            LOG.info("Running {}", synchronousDataSetAnalyzer.getClass());
            synchronousDataSetAnalyzer.analyze(id);
            LOG.info("Done running {}", synchronousDataSetAnalyzer.getClass());
        }

        // Then use JMS queue for all optional analysis
        for (AsynchronousDataSetAnalyzer asynchronousDataSetAnalyzer : asynchronousAnalyzers) {
            if (toSkip.contains(asynchronousDataSetAnalyzer.getClass())) {
                continue;
            }
            jmsTemplate.send(asynchronousDataSetAnalyzer.destination(), session -> {
                Message message = session.createMessage();
                message.setStringProperty("dataset.id", id);
                message.setStringProperty("security.token", security.getAuthenticationToken());
                return message;
            });
        }
    }

    @RequestMapping(value = "/datasets", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "List all data sets and filters on certified, or favorite or a limited number when asked", notes = "Returns the list of data sets (and filters) the current user is allowed to see. Creation date is a Epoch time value (in UTC time zone).")
    @Timed
    public Callable<List<DataSetMetadata>> list(
            @ApiParam(value = "Sort key (by name, creation or modification date)") @RequestParam(defaultValue = "DATE") String sort,
            @ApiParam(value = "Order for sort key (desc or asc or modif)") @RequestParam(defaultValue = "DESC") String order,
            @ApiParam(value = "Filter on name containing the specified name") @RequestParam(defaultValue = "") String name,
            @ApiParam(value = "Filter on certified data sets") @RequestParam(defaultValue = "false") boolean certified,
            @ApiParam(value = "Filter on favorite data sets") @RequestParam(defaultValue = "false") boolean favorite,
            @ApiParam(value = "Only return a limited number of data sets") @RequestParam(defaultValue = "false", required = false) boolean limit) {
        return () -> {
            // Build filter for data sets
            String userId = security.getUserId();
            final UserData userData = userDataRepository.get(userId);
            final List<String> predicates = new ArrayList<>();
            predicates.add("lifecycle.importing = false");
            if (favorite) {
                if (userData != null && !userData.getFavoritesDatasets().isEmpty()) {
                    predicates.add("id in [" + userData.getFavoritesDatasets().stream().map(ds -> '\'' + ds + '\'')
                            .collect(Collectors.joining(",")) + "]");
                } else {
                    predicates.add("isFavorite = 'true'");
                }
            }
            if (certified) {
                predicates.add("governance.certificationStep = '" + Certification.CERTIFIED + "'");
            }
            if (!StringUtils.isEmpty(name)) {
                predicates.add("name contains '" + name + "'");
            }
            final String tqlFilter = predicates.stream().collect(Collectors.joining(" and "));
            LOG.debug("TQL Filter in use: {}", tqlFilter);

            // Get all data sets according to filter
            try (Stream<DataSetMetadata> stream = dataSetMetadataRepository.list(tqlFilter)) {
                final Comparator<DataSetMetadata> comparator = getDataSetMetadataComparator(sort, order);
                return stream.sorted(comparator) //
                        .map(metadata -> {
                            if (userData != null) {
                                metadata.setFavorite(userData.getFavoritesDatasets().contains(metadata.getId()));
                            }
                            return metadata;
                        }) //
                        .limit(limit ? datasetListLimit : Long.MAX_VALUE) //
                        .collect(Collectors.toList());
            }
        };
    }

    @Override
    public Iterable<DataSetMetadata> listCompatibleDatasets(String dataSetId, String sort, String order) {

        Spliterator<DataSetMetadata> iterator = dataSetMetadataRepository.listCompatible(dataSetId).spliterator();

        final Comparator<DataSetMetadata> comparator = getDataSetMetadataComparator(sort, order);

        // Return sorted results
        try (Stream<DataSetMetadata> stream = stream(iterator, false)) {
            String userId = security.getUserId();
            final UserData userData = userDataRepository.get(userId);
            return stream.filter(metadata -> !metadata.getLifecycle().importing()) //
                    .map(metadata -> {
                        if (userData != null) {
                            metadata.setFavorite(userData.getFavoritesDatasets().contains(metadata.getId()));
                        }
                        return metadata;
                    }) //
                    .sorted(comparator) //
                    .collect(Collectors.toList());
        }
    }

    @Override
    public String create(String name, String tag, String contentType, InputStream content) {

        HttpResponseContext.header(CONTENT_TYPE, TEXT_PLAIN_VALUE);

        final String id = UUID.randomUUID().toString();
        final Marker marker = Markers.dataset(id);
        LOG.debug(marker, "Creating...");

        // check that the name is not already taken
        checkIfNameIsAvailable(id, name);

        // get the location out of the content type and the request body
        final DataSetLocation location;
        try {
            location = datasetLocator.getDataSetLocation(contentType, content);
        } catch (IOException e) {
            throw new TDPException(DataSetErrorCodes.UNABLE_TO_READ_DATASET_LOCATION, e);
        }
        try {
            DataSetMetadata dataSetMetadata = metadataBuilder.metadata() //
                    .id(id) //
                    .name(name) //
                    .author(security.getUserId()) //
                    .location(location) //
                    .created(System.currentTimeMillis()) //
                    .tag(tag) //
                    .build();

            dataSetMetadata.getLifecycle().importing(true); // Indicate data set is being imported

            // Save data set content
            LOG.debug(marker, "Storing content...");
            contentStore.storeAsRaw(dataSetMetadata, content);
            LOG.debug(marker, "Content stored.");

            // Create the new data set
            dataSetMetadataRepository.add(dataSetMetadata);
            LOG.debug(marker, "dataset metadata stored {}", dataSetMetadata);

            // Queue events (format analysis, content indexing for search...)
            queueEvents(id);

            LOG.debug(marker, "Created!");
            return id;
        } catch (TDPException e) {
            dataSetMetadataRepository.remove(id);
            throw e;
        } catch (Exception e) {
            dataSetMetadataRepository.remove(id);
            throw new TDPException(DataSetErrorCodes.UNABLE_CREATE_DATASET, e);
        }
    }

    @Override
    public Callable<DataSet> get(boolean metadata, boolean includeInternalContent, String dataSetId) {
        return () -> {
            final Marker marker = Markers.dataset(dataSetId);
            LOG.debug(marker, "Get data set #{}", dataSetId);
            try {
                DataSetMetadata dataSetMetadata = dataSetMetadataRepository.get(dataSetId);
                assertDataSetMetadata(dataSetMetadata, dataSetId);
                // Build the result
                DataSet dataSet = new DataSet();
                if (metadata) {
                    completeWithUserData(dataSetMetadata);
                    dataSet.setMetadata(dataSetMetadata);
                }
                Stream<DataSetRow> stream = contentStore.stream(dataSetMetadata, -1);  // Disable line limit
                if (!includeInternalContent) {
                    LOG.debug("Skip internal content when serving data set #{} content.", dataSetId);
                    stream = stream.map(r -> {
                        final Map<String, Object> values = r.values();
                        final Map<String, Object> filteredValues = new HashMap<>(values);
                        values.forEach((k,v) -> {
                            if (k != null && k.startsWith(FlagNames.INTERNAL_PROPERTY_PREFIX)) { // Removes technical properties from returned values.
                                filteredValues.remove(k);
                            }
                        });
                        filteredValues.put(FlagNames.TDP_ID, r.getTdpId()); // Include TDP_ID anyway
                        return new DataSetRow(r.getRowMetadata(), filteredValues);
                    });
                }
                dataSet.setRecords(stream);
                return dataSet;
            } finally {
                LOG.debug(marker, "Get done.");
            }
        };
    }

    @Override
    public DataSet getMetadata(String dataSetId) {
        if (dataSetId == null) {
            HttpResponseContext.status(HttpStatus.NO_CONTENT);
            return null;
        }

        LOG.debug("get dataset metadata for {}", dataSetId);

        DataSetMetadata metadata = dataSetMetadataRepository.get(dataSetId);
        if (metadata == null) {
            throw new TDPException(DataSetErrorCodes.DATASET_DOES_NOT_EXIST, ExceptionContext.build().put("id", dataSetId));
        }
        if (!metadata.getLifecycle().schemaAnalyzed()) {
            HttpResponseContext.status(HttpStatus.ACCEPTED);
            return DataSet.empty();
        }
        DataSet dataSet = new DataSet();
        completeWithUserData(metadata);
        dataSet.setMetadata(metadata);
        LOG.info("found dataset {} for #{}", dataSet.getMetadata().getName(), dataSetId);
        return dataSet;
    }

    @Override
    public void delete(String dataSetId) {
        DataSetMetadata metadata = dataSetMetadataRepository.get(dataSetId);
        final DistributedLock lock = dataSetMetadataRepository.createDatasetMetadataLock(dataSetId);
        try {
            lock.lock();
            if (metadata != null) {
                dataSetMetadataRepository.remove(dataSetId); // first remove the metadata as there may be additional check
                contentStore.delete(metadata);
            } // do nothing if the dataset does not exists
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String copy(String dataSetId, String copyName) {

        HttpResponseContext.header(CONTENT_TYPE, TEXT_PLAIN_VALUE);

        DataSetMetadata original = dataSetMetadataRepository.get(dataSetId);
        if (original == null) {
            return StringUtils.EMPTY;
        }

        // use a default name if empty (original name + " Copy" )
        final String newName;
        if (StringUtils.isBlank(copyName)) {
            newName = original.getName() + " Copy";
        } else {
            newName = copyName;
        }

        final DistributedLock lock = dataSetMetadataRepository.createDatasetMetadataLock(dataSetId);
        try {
            lock.lock(); // lock to ensure any asynchronous analysis is completed.

            // check that the name is not already taken
            checkIfNameIsAvailable(dataSetId, newName);

            // Create copy (based on original data set metadata)
            final String newId = UUID.randomUUID().toString();
            final Marker marker = Markers.dataset(newId);
            LOG.debug(marker, "Cloning...");
            DataSetMetadata target = metadataBuilder.metadata() //
                    .copy(original) //
                    .id(newId) //
                    .name(newName) //
                    .author(security.getUserId()) //
                    .location(original.getLocation()) //
                    .created(System.currentTimeMillis()) //
                    .build();

            // Save data set content
            LOG.debug(marker, "Storing content...");
            try (InputStream content = contentStore.getAsRaw(original)) {
                contentStore.storeAsRaw(target, content);
            } catch (IOException e) {
                throw new TDPException(DataSetErrorCodes.UNABLE_TO_CREATE_OR_UPDATE_DATASET, e);
            }

            LOG.debug(marker, "Content stored.");

            // Create the new data set
            dataSetMetadataRepository.add(target);

            LOG.info(marker, "Copy done --> {}", newId);

            return newId;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Make sure the given name is not used by another dataset. If yes, throws a TDPException.
     *
     * @param id the dataset id to
     * @param name the name to check.
     */
    private void checkIfNameIsAvailable(String id, String name) {
        if (dataSetMetadataRepository.exist("name = '" + name + "'")) {
            final ExceptionContext context = ExceptionContext.build() //
                    .put("id", id) //
                    .put("name", name);
            throw new TDPException(DATASET_NAME_ALREADY_USED, context, true);
        }
    }

    @Override
    public void processCertification(String dataSetId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Ask certification for dataset #{}", dataSetId);
        }

        // Check if the user has sufficient grants to perform the action
        accessGrantChecker.allowed(CommonRestrictedActions.CERTIFICATION);

        DistributedLock datasetLock = dataSetMetadataRepository.createDatasetMetadataLock(dataSetId);
        datasetLock.lock();
        try {
            DataSetMetadata dataSetMetadata = dataSetMetadataRepository.get(dataSetId);
            if (dataSetMetadata != null) {
                LOG.trace("Current certification step is " + dataSetMetadata.getGovernance().getCertificationStep());

                if (dataSetMetadata.getGovernance().getCertificationStep() == Certification.NONE) {
                    dataSetMetadata.getGovernance().setCertificationStep(Certification.PENDING);
                    dataSetMetadataRepository.add(dataSetMetadata);
                } else if (dataSetMetadata.getGovernance().getCertificationStep() == Certification.PENDING) {
                    dataSetMetadata.getGovernance().setCertificationStep(Certification.CERTIFIED);
                    dataSetMetadataRepository.add(dataSetMetadata);
                } else if (dataSetMetadata.getGovernance().getCertificationStep() == Certification.CERTIFIED) {
                    dataSetMetadata.getGovernance().setCertificationStep(Certification.NONE);
                    dataSetMetadataRepository.add(dataSetMetadata);
                }

                LOG.debug("New certification step is " + dataSetMetadata.getGovernance().getCertificationStep());
            } // else do nothing if the dataset does not exists
        } finally {
            datasetLock.unlock();
        }
    }

    @Override
    public void updateRawDataSet(String dataSetId, String name, InputStream dataSetContent) {
        final DistributedLock lock = dataSetMetadataRepository.createDatasetMetadataLock(dataSetId);
        try {
            lock.lock();
            final DataSetMetadataBuilder datasetBuilder = metadataBuilder.metadata().id(dataSetId);
            final DataSetMetadata metadataForUpdate = dataSetMetadataRepository.get(dataSetId);
            if (metadataForUpdate != null) {
                datasetBuilder.copyNonContentRelated(metadataForUpdate);
                datasetBuilder.modified(System.currentTimeMillis());
            }
            if (!StringUtils.isEmpty(name)) {
                datasetBuilder.name(name);
            }
            final DataSetMetadata dataSetMetadata = datasetBuilder.build();

            // Save data set content
            contentStore.storeAsRaw(dataSetMetadata, dataSetContent);
            dataSetMetadataRepository.add(dataSetMetadata);
            publisher.publishEvent(new DataSetRawContentUpdateEvent(dataSetMetadata));
        } finally {
            lock.unlock();
        }
        // Content was changed, so queue events (format analysis, content indexing for search...)
        queueEvents(dataSetId);
    }

    @Override
    public Iterable<JsonErrorCodeDescription> listErrors() {
        // need to cast the typed dataset errors into mock ones to use json parsing
        List<JsonErrorCodeDescription> errors = new ArrayList<>(DataSetErrorCodes.values().length);
        for (DataSetErrorCodes code : DataSetErrorCodes.values()) {
            errors.add(new JsonErrorCodeDescription(code));
        }
        return errors;
    }

    @Override
    public DataSet preview(boolean metadata, String sheetName, String dataSetId) {

        DataSetMetadata dataSetMetadata = dataSetMetadataRepository.get(dataSetId);

        if (dataSetMetadata == null) {
            HttpResponseContext.status(HttpStatus.NO_CONTENT);
            return DataSet.empty(); // No data set, returns empty content.
        }
        if (!dataSetMetadata.isDraft()) {
            // Moved to get data set content operation
            HttpResponseContext.status(HttpStatus.MOVED_PERMANENTLY);
            HttpResponseContext.header("Location", "/datasets/" + dataSetId + "/content");
            return DataSet.empty(); // dataset not anymore a draft so preview doesn't make sense.
        }
        if (StringUtils.isNotEmpty(sheetName)) {
            dataSetMetadata.setSheetName(sheetName);
        }
        // take care of previous data without schema parser result
        if (dataSetMetadata.getSchemaParserResult() != null) {
            // sheet not yet set correctly so use the first one
            if (StringUtils.isEmpty(dataSetMetadata.getSheetName())) {
                String theSheetName = dataSetMetadata.getSchemaParserResult().getSheetContents().get(0).getName();
                LOG.debug("preview for dataSetMetadata: {} with sheetName: {}", dataSetId, theSheetName);
                dataSetMetadata.setSheetName(theSheetName);
            }

            String theSheetName = dataSetMetadata.getSheetName();

            Optional<Schema.SheetContent> sheetContentFound = dataSetMetadata.getSchemaParserResult().getSheetContents().stream()
                    .filter(sheetContent -> theSheetName.equals(sheetContent.getName())).findFirst();

            if (!sheetContentFound.isPresent()) {
                HttpResponseContext.status(HttpStatus.NO_CONTENT);
                return DataSet.empty(); // No sheet found, returns empty content.
            }

            List<ColumnMetadata> columnMetadatas = sheetContentFound.get().getColumnMetadatas();

            if (dataSetMetadata.getRowMetadata() == null) {
                dataSetMetadata.setRowMetadata(new RowMetadata(Collections.emptyList()));
            }

            dataSetMetadata.getRowMetadata().setColumns(columnMetadatas);
        } else {
            LOG.warn("dataset#{} has draft status but any SchemaParserResult");
        }
        // Build the result
        DataSet dataSet = new DataSet();
        if (metadata) {
            completeWithUserData(dataSetMetadata);
            dataSet.setMetadata(dataSetMetadata);
        }
        dataSet.setRecords(contentStore.stream(dataSetMetadata).limit(100));
        return dataSet;
    }

    /**
     * This gets the current user data related to the dataSetMetadata and updates the dataSetMetadata accordingly. First
     * check for favorites dataset
     *
     * @param dataSetMetadata, the metadata to be updated
     */
    void completeWithUserData(DataSetMetadata dataSetMetadata) {
        String userId = security.getUserId();
        UserData userData = userDataRepository.get(userId);
        if (userData != null) {
            dataSetMetadata.setFavorite(userData.getFavoritesDatasets().contains(dataSetMetadata.getId()));
        } // no user data related to the current user to do nothing
    }

    @Override
    public void updateDataSet(String dataSetId, DataSetMetadata dataSetMetadata) {
        final DistributedLock lock = dataSetMetadataRepository.createDatasetMetadataLock(dataSetId);
        lock.lock();
        try {
            LOG.debug("updateDataSet: {}", dataSetMetadata);
            publisher.publishEvent(new DataSetMetadataBeforeUpdateEvent(dataSetMetadata));

            //
            // Only part of the metadata can be updated, so the original dataset metadata is loaded and updated
            //
            DataSetMetadata metadataForUpdate = dataSetMetadataRepository.get(dataSetId);
            DataSetMetadata original = metadataBuilder.metadata().copy(metadataForUpdate).build();

            if (metadataForUpdate == null) {
                // No need to silently create the data set metadata: associated content will most likely not exist.
                throw new TDPException(DataSetErrorCodes.DATASET_DOES_NOT_EXIST, ExceptionContext.build().put("id", dataSetId));
            }

            try {
                // update the name
                metadataForUpdate.setName(dataSetMetadata.getName());

                // update the sheet content (in case of a multi-sheet excel file)
                if (metadataForUpdate.getSchemaParserResult() != null) {
                    Optional<Schema.SheetContent> sheetContentFound = metadataForUpdate.getSchemaParserResult().getSheetContents()
                            .stream().filter(sheetContent -> dataSetMetadata.getSheetName().equals(sheetContent.getName()))
                            .findFirst();

                    if (sheetContentFound.isPresent()) {
                        List<ColumnMetadata> columnMetadatas = sheetContentFound.get().getColumnMetadatas();
                        if (metadataForUpdate.getRowMetadata() == null) {
                            metadataForUpdate.setRowMetadata(new RowMetadata(Collections.emptyList()));
                        }
                        metadataForUpdate.getRowMetadata().setColumns(columnMetadatas);
                    }

                    metadataForUpdate.setSheetName(dataSetMetadata.getSheetName());
                    metadataForUpdate.setSchemaParserResult(null);
                }

                // Location updates
                metadataForUpdate.setLocation(dataSetMetadata.getLocation());

                // update parameters & encoding (so that user can change import parameters for CSV)
                metadataForUpdate.getContent().setParameters(dataSetMetadata.getContent().getParameters());
                metadataForUpdate.setEncoding(dataSetMetadata.getEncoding());

                // update limit
                final Optional<Long> newLimit = dataSetMetadata.getContent().getLimit();
                if (newLimit.isPresent()) {
                    metadataForUpdate.getContent().setLimit(newLimit.get());
                }

                // Validate that the new data set metadata and removes the draft status
                final String formatFamilyId = dataSetMetadata.getContent().getFormatFamilyId();
                if (formatFamilyFactory.hasFormatFamily(formatFamilyId)) {
                    FormatFamily format = formatFamilyFactory.getFormatFamily(formatFamilyId);
                    try {
                        DraftValidator draftValidator = format.getDraftValidator();
                        DraftValidator.Result result = draftValidator.validate(dataSetMetadata);
                        if (result.isDraft()) {
                            // This is not an exception case: data set may remain a draft after update (although rather
                            // unusual)
                            LOG.warn("Data set #{} is still a draft after update.", dataSetId);
                            return;
                        }
                        // Data set metadata to update is no longer a draft
                        metadataForUpdate.setDraft(false);
                    } catch (UnsupportedOperationException e) {
                        // no need to validate draft here
                    }
                }

                // update schema
                formatAnalyzer.update(original, metadataForUpdate);

                // save the result
                dataSetMetadataRepository.add(metadataForUpdate);

                // all good mate!! so send that to jms
                // Asks for a in depth schema analysis (for column type information).
                queueEvents(dataSetId, FormatAnalysis.class);
            } catch (TDPException e) {
                throw e;
            } catch (Exception e) {
                throw new TDPException(UNABLE_TO_CREATE_OR_UPDATE_DATASET, e);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterable<String> favorites() {
        String userId = security.getUserId();
        UserData userData = userDataRepository.get(userId);
        return userData != null ? userData.getFavoritesDatasets() : Collections.emptyList();
    }

    @Override
    public void setFavorites(boolean unset, String dataSetId) {
        String userId = security.getUserId();
        // check that dataset exists
        DataSetMetadata dataSetMetadata = dataSetMetadataRepository.get(dataSetId);
        if (dataSetMetadata != null) {
            LOG.debug("{} favorite dataset for #{} for user {}", unset ? "Unset" : "Set", dataSetId, userId); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

            UserData userData = userDataRepository.get(userId);
            if (unset) {// unset the favorites
                if (userData != null) {
                    userData.getFavoritesDatasets().remove(dataSetId);
                    userDataRepository.save(userData);
                } // no user data for this user so nothing to unset
            } else {// set the favorites
                if (userData == null) {// let's create a new UserData
                    userData = new UserData(userId, versionService.version().getVersionId());
                } // else already created so just update it.
                userData.addFavoriteDataset(dataSetId);
                userDataRepository.save(userData);
            }
        } else {// no dataset found so throws an error
            throw new TDPException(DataSetErrorCodes.DATASET_DOES_NOT_EXIST, ExceptionContext.build().put("id", dataSetId));
        }
    }

    @Override
    public void updateDatasetColumn(final String dataSetId, final String columnId, final UpdateColumnParameters parameters) {

        final DistributedLock lock = dataSetMetadataRepository.createDatasetMetadataLock(dataSetId);
        lock.lock();
        try {

            // check that dataset exists
            final DataSetMetadata dataSetMetadata = dataSetMetadataRepository.get(dataSetId);
            if (dataSetMetadata == null) {
                throw new TDPException(DataSetErrorCodes.DATASET_DOES_NOT_EXIST, ExceptionContext.build().put("id", dataSetId));
            }

            LOG.debug("update dataset column for #{} with type {} and/or domain {}", dataSetId, parameters.getType(),
                    parameters.getDomain());

            // get the column
            final ColumnMetadata column = dataSetMetadata.getRowMetadata().getById(columnId);
            if (column == null) {
                throw new TDPException(DataSetErrorCodes.COLUMN_DOES_NOT_EXIST, //
                        ExceptionContext.build() //
                                .put("id", dataSetId) //
                                .put("columnid", columnId));
            }

            // update type/domain
            if (parameters.getType() != null) {
                column.setType(parameters.getType());
            }
            if (parameters.getDomain() != null) {
                // erase domain to let only type
                if (parameters.getDomain().isEmpty()) {
                    column.setDomain("");
                    column.setDomainLabel("");
                    column.setDomainFrequency(0);
                }
                // change domain
                else {
                    final SemanticDomain semanticDomain = column.getSemanticDomains() //
                            .stream() //
                            .filter(dom -> StringUtils.equals(dom.getId(), parameters.getDomain())) //
                            .findFirst().orElse(null);
                    if (semanticDomain != null) {
                        column.setDomain(semanticDomain.getId());
                        column.setDomainLabel(semanticDomain.getLabel());
                        column.setDomainFrequency(semanticDomain.getFrequency());
                    }
                }
            }

            // save
            dataSetMetadataRepository.add(dataSetMetadata);

            // analyze the updated dataset (not all analysis are performed)
            queueEvents(dataSetId, //
                    ContentAnalysis.class, //
                    FormatAnalysis.class, //
                    SchemaAnalysis.class, //
                    SyncBackgroundAnalyzer.class, //
                    AsyncBackgroundAnalysis.class);

        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterable<DataSetMetadata> search(final String name, final boolean strict) {

        LOG.debug("search datasets metadata for {}", name);

        final String filter;
        if (strict) {
            filter = "name = '" + name + "'";
        } else {
            filter = "name contains '" + name + "'";
        }
        final Set<DataSetMetadata> found = dataSetMetadataRepository.list(filter).collect(toSet());

        LOG.info("found {} dataset while searching {}", found.size(), name);

        return found;
    }

    @Override
    public List<String> listSupportedEncodings() {
        return encodings.getSupportedCharsets().stream().map(Charset::displayName).collect(Collectors.toList());
    }

    @Override
    public List<Parameter> getImportParameters(@PathVariable("import") final String importType) {
        if (StringUtils.isEmpty(importType)) {
            return Collections.emptyList();
        }
        for (DataSetLocation location : locations) {
            if (importType.equals(location.getLocationType())) {
                return location.getParameters();
            }
        }
        return Collections.emptyList();
    }

    @Override
    public List<Import> listSupportedImports() {
        final List<Import> supportedImports = locations.stream() //
                .filter(l -> enabledImports.contains(l.getLocationType())) //
                .filter(DataSetLocation::isEnabled) //
                .map(l -> { //
                    final boolean defaultImport = LocalStoreLocation.NAME.equals(l.getLocationType());
                    if (l.isDynamic()) {
                        return new Import(l.getLocationType(), //
                                l.getAcceptedContentType(), //
                                Collections.emptyList(), //
                                l.isDynamic(), //
                                defaultImport);
                    } else {
                        return new Import(l.getLocationType(), //
                                l.getAcceptedContentType(), //
                                l.getParameters(), //
                                l.isDynamic(), //
                                defaultImport);
                    }
                }) //
                .sorted((i1, i2) -> { //
                    int i1Value = i1.isDefaultImport() ? 1 : -1;
                    int i2Value = i2.isDefaultImport() ? 1 : -1;
                    final int compare = i2Value - i1Value;
                    if (compare == 0) {
                        // Same level, use location type alphabetical order to determine order.
                        return i1.getLocationType().compareTo(i2.getLocationType());
                    } else {
                        return compare;
                    }
                }) //
                .collect(Collectors.toList());

        LOG.debug("found {} supported import type", supportedImports.size());

        return supportedImports;
    }

}