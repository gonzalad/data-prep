package org.talend.dataprep.transformation.service;

import com.fasterxml.jackson.core.JsonParser;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.org.talend.dataprep.api.export.ExportParameters;
import org.talend.dataprep.api.preparation.Preparation;
import org.talend.dataprep.cache.ContentCache;
import org.talend.dataprep.command.dataset.DataSetSampleGet;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.TransformationErrorCodes;
import org.talend.dataprep.format.export.ExportFormat;
import org.talend.dataprep.transformation.api.transformer.configuration.Configuration;
import org.talend.dataprep.transformation.cache.TransformationCacheKey;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A {@link ExportStrategy strategy} to apply a preparation on a different dataset (different from the one initially
 * in the preparation).
 */
@Component
public class ApplyPreparationExportStrategy extends StandardExportStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplyPreparationExportStrategy.class);

    @Override
    public int order() {
        return 3;
    }

    @Override
    public boolean accept(ExportParameters parameters) {
        if (parameters == null) {
            return false;
        }
        // Valid if both data set and preparation are set.
        return parameters.getContent() == null //
                && !StringUtils.isEmpty(parameters.getDatasetId()) //
                && !StringUtils.isEmpty(parameters.getPreparationId());
    }

    @Override
    public StreamingResponseBody execute(ExportParameters parameters) {
        final String formatName = parameters.getExportType();
        final ExportFormat format = getFormat(formatName);
        ExportUtils.setExportHeaders(parameters.getExportName(), format);

        return outputStream -> executeApplyPreparation(parameters, outputStream);
    }

    private void executeApplyPreparation(ExportParameters parameters, OutputStream outputStream) {
        final String stepId = parameters.getStepId();
        final String preparationId = parameters.getPreparationId();
        final String formatName = parameters.getExportType();
        final Preparation preparation = getPreparation(preparationId);
        final String dataSetId = parameters.getDatasetId();
        final ExportFormat format = getFormat(parameters.getExportType());

        // get the dataset content (in an auto-closable block to make sure it is properly closed)
        final DataSetSampleGet dataSetGet = applicationContext.getBean(DataSetSampleGet.class, dataSetId);
        try (InputStream datasetContent = dataSetGet.execute()) {
            try (JsonParser parser = mapper.getFactory().createParser(datasetContent)) {
                // head is not allowed as step id
                String version = stepId;
                if (StringUtils.equals("head", stepId) || StringUtils.isEmpty(stepId)) {
                    version = preparation.getSteps().get(preparation.getSteps().size() - 1);
                }
                // Create dataset
                final DataSet dataSet = mapper.readerFor(DataSet.class).readValue(parser);
                // get the actions to apply (no preparation ==> dataset export ==> no actions)
                String actions = getActions(preparationId, version);

                final TransformationCacheKey key = new TransformationCacheKey(preparationId, dataSetId, formatName, version);
                LOGGER.debug("Cache key: " + key.getKey());
                LOGGER.debug("Cache key details: " + key.toString());
                TeeOutputStream tee = new TeeOutputStream(outputStream, contentCache.put(key, ContentCache.TimeToLive.DEFAULT));
                try {
                    Configuration configuration = Configuration.builder() //
                            .args(parameters.getArguments()) //
                            .outFilter(rm -> filterService.build(parameters.getFilter(), rm)) //
                            .format(format.getName()) //
                            .actions(actions) //
                            .preparationId(preparationId) //
                            .stepId(version) //
                            .volume(Configuration.Volume.SMALL) //
                            .output(tee) //
                            .build();
                    factory.get(configuration).transform(dataSet, configuration);
                    tee.flush();
                } catch (Throwable e) { // NOSONAR
                    contentCache.evict(key);
                    throw e;
                }
            }
        } catch (TDPException e) {
            throw e;
        } catch (Exception e) {
            throw new TDPException(TransformationErrorCodes.UNABLE_TO_TRANSFORM_DATASET, e);
        }
    }
}