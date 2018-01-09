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

package org.talend.dataprep.transformation.util;

import static org.talend.daikon.exception.ExceptionContext.build;
import static org.talend.dataprep.exception.error.DataSetErrorCodes.UNABLE_TO_READ_DATASET_CONTENT;
import static org.talend.dataprep.exception.error.PreparationErrorCodes.UNABLE_TO_READ_PREPARATION;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.preparation.PreparationMessage;
import org.talend.dataprep.command.dataset.DataSetGet;
import org.talend.dataprep.command.dataset.DataSetGetMetadata;
import org.talend.dataprep.command.preparation.PreparationDetailsGet;
import org.talend.dataprep.exception.TDPException;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CommandUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandUtil.class);

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected ObjectMapper mapper;

    /**
     * @param preparationId the wanted preparation id.
     * @return the preparation out of its id.
     */
    public PreparationMessage getPreparation(String preparationId) {
        return getPreparation(preparationId, null);
    }

    /**
     * @param preparationId the wanted preparation id.
     * @param stepId the preparation step (might be different from head's to navigate through versions).
     * @return the preparation out of its id.
     */
    public PreparationMessage getPreparation(String preparationId, String stepId) {
        final PreparationDetailsGet preparationDetailsGet = applicationContext.getBean(PreparationDetailsGet.class,
                preparationId, stepId);
        try (InputStream details = preparationDetailsGet.execute()) {
            return mapper.readerFor(PreparationMessage.class).readValue(details);
        } catch (Exception e) {
            LOGGER.error("Unable to read preparation {}", preparationId, e);
            throw new TDPException(UNABLE_TO_READ_PREPARATION, e, build().put("id", preparationId));
        }
    }

    public DataSet getDataSet(String dataSetId) {
        final DataSetGet dataSetGet = applicationContext.getBean(DataSetGet.class, dataSetId, false, true);
        try (InputStream details = dataSetGet.execute()) {
            return mapper.readerFor(DataSet.class).readValue(details);
        } catch (Exception e) {
            LOGGER.error("Unable to read dataset {}", dataSetId, e);
            throw new TDPException(UNABLE_TO_READ_DATASET_CONTENT, e, build().put("id", dataSetId));
        }

    }


//    final DataSetGetMetadata dataSetGetMetadata = applicationContext.getBean(DataSetGetMetadata.class, dataSetId);
}
