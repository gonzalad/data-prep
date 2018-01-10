package org.talend.dataprep.services.api;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import org.springframework.web.bind.annotation.RequestMapping;
import org.talend.daikon.annotation.Service;
import org.talend.dataprep.transformation.aggregation.api.AggregationResult;

/**
 * High level Aggregation API.
 */
@Service(name = "dataprep.api.aggregation")
public interface AggregationAPI {

    /**
     * Compute an aggregation according to the given parameters.
     *
     * @param input The aggregation parameters.
     */
    @RequestMapping(value = "/api/aggregate", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    AggregationResult compute(AggregationParameters input);
}

