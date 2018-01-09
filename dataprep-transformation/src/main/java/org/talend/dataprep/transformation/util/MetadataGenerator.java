package org.talend.dataprep.transformation.util;

import java.io.IOException;
import java.util.Collections;

import org.apache.commons.io.output.NullOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.export.ExportParameters;
import org.talend.dataprep.api.preparation.PreparationMessage;
import org.talend.dataprep.cache.ContentCache;
import org.talend.dataprep.cache.ContentCacheKey;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.TransformationErrorCodes;
import org.talend.dataprep.transformation.api.transformer.ConfiguredCacheWriter;
import org.talend.dataprep.transformation.api.transformer.TransformerFactory;
import org.talend.dataprep.transformation.api.transformer.TransformerWriter;
import org.talend.dataprep.transformation.cache.CacheKeyGenerator;
import org.talend.dataprep.transformation.cache.InitialTransformationMetadataCacheKey;
import org.talend.dataprep.transformation.cache.TransformationCacheKey;
import org.talend.dataprep.transformation.format.WriterRegistrationService;
import org.talend.dataprep.transformation.pipeline.Pipeline;
import org.talend.dataprep.transformation.pipeline.builder.NodeBuilder;
import org.talend.dataprep.transformation.pipeline.model.WriterNode;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.talend.dataprep.transformation.service.TransformationRowMetadataUtils;

@Component
public class MetadataGenerator {

    /**
     * This class' logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataGenerator.class);

    @Autowired
    private CommandUtil commandUtil;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private PipelineGenerator pipelineGenerator;

    @Autowired
    protected ContentCache contentCache;

    @Autowired
    protected CacheKeyGenerator cacheKeyGenerator;

    @Autowired
    private WriterRegistrationService writerRegistrationService;

    @Autowired
    private TransformationRowMetadataUtils transformationRowMetadataUtils;

    /** The transformer factory. */
    @Autowired
    protected TransformerFactory factory;

    public void generateMetadataForPreparation(String preparationId, String stepId, ContentCacheKey metadataCacheKey) {

        PreparationMessage preparation = commandUtil.getPreparation(preparationId);

        // get content cache key
        final TransformationCacheKey contentCacheKey = cacheKeyGenerator.generateContentKey(preparation.getDataSetId(),
                preparationId, stepId, "JSON", ExportParameters.SourceType.HEAD, null);

        // get metadata cache key
        final InitialTransformationMetadataCacheKey initialMetadataCacheKey =
                cacheKeyGenerator.generateInitialMetadataKey(preparationId, stepId, null);

        // get last
        DataSet dataSet = null;
        try {
            dataSet = getDataSetFromCache(contentCacheKey, initialMetadataCacheKey);
        } catch (IOException e) {
            LOGGER.warn("Cannot get initial data from cache. We relaunch the pipeline to generate it");
            dataSet = getDataSetFromPipeline(preparation, contentCacheKey, initialMetadataCacheKey);
        }

        try {

            // initialize the writer. We only want to write metadata on the cache with this method
            final TransformerWriter writer =
                    writerRegistrationService.getWriter("JSON", new NullOutputStream(), Collections.emptyMap());
            final ConfiguredCacheWriter metadataWriter = new ConfiguredCacheWriter(contentCache, ContentCache.TimeToLive.DEFAULT);

            // prepare the fallback row metadata
            RowMetadata fallBackRowMetadata = transformationRowMetadataUtils.getMatchingEmptyRowMetadata(preparation.getRowMetadata());

            final WriterNode writerNode = new WriterNode(writer, metadataWriter, metadataCacheKey, fallBackRowMetadata);

            Pipeline pipeline = new Pipeline();

            NodeBuilder mainPipeline = pipelineGenerator.buildPipelineToCalculateMetadata(pipeline, writerNode);

            pipeline.setNode(mainPipeline.build());
            pipeline.execute(dataSet);

        } catch (Throwable e) { // NOSONAR
            contentCache.evict(metadataCacheKey);
            throw e;
        }
    }

    private DataSet getDataSetFromPipeline(PreparationMessage preparation, TransformationCacheKey contentCacheKey, InitialTransformationMetadataCacheKey initialMetadataCacheKey) {
        try {
            this.launchPreparationPipeline(preparation, contentCacheKey, initialMetadataCacheKey);
            return getDataSetFromCache(contentCacheKey, initialMetadataCacheKey);
        } catch (IOException e) {
            LOGGER.error("Cannot get dataset from cache but we just relaunch the pipeline.", e);
            // Not expected: We've just ran a transformation, yet no cached?
             throw new TDPException(TransformationErrorCodes.UNABLE_TO_COMPUTE_DATASET_ACTIONS);
        }
    }

    private DataSet getDataSetFromCache(ContentCacheKey contentCacheKey, ContentCacheKey metadataCacheKey) throws IOException {
        // get dataset
        JsonParser parser = mapper.getFactory().createParser(contentCache.get(contentCacheKey));
        DataSet dataSet = mapper.readerFor(DataSet.class).readValue(parser);

        // get metadata
        parser = mapper.getFactory().createParser(contentCache.get(metadataCacheKey));
        DataSetMetadata metadata = mapper.readerFor(DataSetMetadata.class).readValue(parser);

        // link and return
        dataSet.setMetadata(metadata);
        return dataSet;
    }

    private void launchPreparationPipeline(PreparationMessage preparation, ContentCacheKey contentCacheKey, ContentCacheKey metadataCacheKey) {
        Pipeline pipeline = new Pipeline();

        // initialize the writer. We want to write both transformation and metadata into the cache
        final TransformerWriter writer =
                writerRegistrationService.getWriter("JSON", contentCache.put(contentCacheKey, ContentCache.TimeToLive.DEFAULT), Collections.emptyMap());
        final ConfiguredCacheWriter metadataWriter = new ConfiguredCacheWriter(contentCache, ContentCache.TimeToLive.DEFAULT);

        // prepare the fallback row metadata
        RowMetadata fallBackRowMetadata = transformationRowMetadataUtils.getMatchingEmptyRowMetadata(preparation.getRowMetadata());

        final WriterNode writerNode = new WriterNode(writer, metadataWriter, metadataCacheKey, fallBackRowMetadata);

        //TODO: Arreter ici: le pb est que le contenu n'est pas généré ds le cache

        NodeBuilder prepPipeline = pipelineGenerator.buildPipelineToApplyActionsFromPreparation(pipeline, preparation, r -> true,
                preparation.getRowMetadata(), false, false, false, writerNode);

        // get the initial dataset
        DataSet dataset = commandUtil.getDataSet(preparation.getDataSetId());

        pipeline.setNode(prepPipeline.build());
        pipeline.execute(dataset);
    }
}
