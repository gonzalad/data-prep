package org.talend.dataprep.preparation.store;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.talend.dataprep.api.preparation.Preparation;
import org.talend.dataprep.api.preparation.Step;
import org.talend.dataprep.preparation.store.inmemory.InMemoryPreparationRepository;

/**
 * Unit test for the InMemoryPreparationRepository.
 * 
 * @see InMemoryPreparationRepository
 */
public class InMemoryPreparationRepositoryTest {

    /** The preparation repository to test. */
    private PreparationRepository repository;

    /**
     * Default constructor.
     */
    public InMemoryPreparationRepositoryTest() {
        repository = new InMemoryPreparationRepository();
    }

    /**
     *
     */
    @Before
    public void before() {
        repository.clear();
    }

    /**
     * Test the getByDataSet method.
     * 
     * @see PreparationRepository#getByDataSet(String)
     */
    @Test
    public void getByDataSetTest() {

        // populate repository with noise (steps and preparations to be ignored)
        repository.add(getStep("s1"));
        repository.add(getStep("s2"));
        repository.add(getStep("s3"));
        repository.add(getStep("s4"));
        repository.add(getStep("s5"));

        repository.add(getPreparation("p1"));
        repository.add(getPreparation("p2"));
        repository.add(getPreparation("p3"));
        repository.add(getPreparation("p4"));
        repository.add(getPreparation("p5"));

        // add relevant data
        String dataSetId = "wantedId";
        Collection<Preparation> expected = Arrays.asList(getPreparation(dataSetId, "10"), getPreparation(dataSetId, "11"),
                getPreparation(dataSetId, "12"));
        for (Preparation preparation : expected) {
            repository.add(preparation);
        }

        // run the test
        Collection<Preparation> actual = repository.getByDataSet(dataSetId);

        // check the result
        Assert.assertEquals(actual.size(), 3);
        for (Preparation preparation : expected) {
            Assert.assertTrue(actual.contains(preparation));
        }
    }

    /**
     * Helper method that only generates a step but simplify code.
     */
    private Step getStep(String rootName) {
        return new Step(rootName + "_parent", rootName + "_content");
    }

    /**
     * Helper method that only generates a preparation but simplify code.
     * 
     * @param rootName root name for all the preparation attributes.
     * @return a new Preparation.
     */
    private Preparation getPreparation(String rootName) {
        return getPreparation(rootName + "_setId", rootName);
    }

    /**
     * Helper method that only generates a preparation but simplify code.
     * 
     * @param datasetId the preparation dataset id.
     * @param rootName root name for all the preparation attributes.
     * @return a new Preparation.
     */
    private Preparation getPreparation(String datasetId, String rootName) {
        Preparation preparation = new Preparation(datasetId, getStep(rootName));
        preparation.setName(rootName + "_name");
        return preparation;
    }
}