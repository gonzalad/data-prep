package org.talend.dataprep.transformation.service;

import static com.jayway.restassured.RestAssured.given;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.talend.dataprep.transformation.preview.api.PreviewParameters;

import com.jayway.restassured.http.ContentType;

/**
 * Diff integration tests.
 */
public class DiffTests extends TransformationServiceBaseTests {

    /** The dataprep ready to use jackson object builder. */
    @Autowired
    protected Jackson2ObjectMapperBuilder builder;

    @Test
    public void should_return_preview() throws Exception {
        // given
        PreviewParameters input = new PreviewParameters( //
                getSingleTransformation(), //
                getMultipleTransformation(), //
                createDataset("../preview/input.csv", "input4preview", "text/csv"), //
                "[2,4,6]");

        // when
        final String response = given() //
                .contentType(ContentType.JSON) //
                .body(builder.build().writer().writeValueAsString(input)) //
                .when().expect().statusCode(200).log().ifError() //
                .post("/transform/preview") //
                .asString();

        // then
        final String expected = IOUtils.toString(this.getClass().getResourceAsStream("../preview/expected_output.json"));
        assertEquals(expected, response, false);
    }

    /**
     * After TDP-1184 fix, there is a problem on preview (regression?).
     * Use case is:
     * - delete a column (lastname here)
     * - add new columns (with split on city here)
     * - preview an action on the first new column (uppercase on 0000 here)
     *
     * -> lastname is still on the preview data for lines 4 & 6. it is absent (which is what we expect) only for the first line!
     */
    @Test
    public void test_TDP_1184() throws Exception {
        // given
        final String datasetContent = IOUtils.toString(this.getClass().getResourceAsStream("../preview/input.json"));
        final String expected = IOUtils.toString(this.getClass().getResourceAsStream("../preview/expected_output_TDP_1184.json"));

        final String oldActions = getTransformation_TDP_1184_step_1();
        final String newActions = getTransformation_TDP_1184_step_2();
        final String indexes = "[1,4,6]";

        // when
        final String response = given() //
                .multiPart("oldActions", oldActions) //
                .multiPart("newActions", newActions) //
                .multiPart("indexes", indexes) //
                .multiPart("content", datasetContent) //
                .when() //
                .post("/transform/preview")
                .asString();

        // then
        assertEquals(expected, response, true);
    }

    @Test
    public void should_return_created_columns() throws Exception {
        // given
        PreviewParameters input = new PreviewParameters( //
                getSingleTransformation(), //
                getMultipleTransformationWithNewColumn(), //
                createDataset("../preview/input.csv", "input4preview", "text/csv"), //
                null);

        // when
        final String response = given() //
                .contentType(ContentType.JSON) //
                .body(builder.build().writer().writeValueAsString(input)) //
                .when().expect().statusCode(200).log().ifError() //
                .post("/transform/diff/metadata")
                .asString();

        // then
        assertEquals("{\"createdColumns\":[\"0009\"]}", response, false);
    }


    @Test
    public void should_return_empty_array_when_step_does_not_create_columns() throws Exception {
        // given
        PreviewParameters input = new PreviewParameters( //
                getSingleTransformation(), //
                getMultipleTransformationWithoutNewColumn(), //
                createDataset("../preview/input.csv", "input4preview", "text/csv"), //
                null);

        // when
        final String response = given() //
                .contentType(ContentType.JSON) //
                .body(builder.build().writer().writeValueAsString(input)) //
                .when().expect().statusCode(200).log().ifError() //
                .post("/transform/diff/metadata")
                .asString();

        // then
        assertEquals("{\"createdColumns\":[]}", response, false);
    }

    private String getSingleTransformation() throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream("../preview/uppercase.json"));
    }

    private String getMultipleTransformationWithNewColumn() throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream("../preview/uppercase_copy.json"));
    }

    private String getTransformation_TDP_1184_step_1() {
        return "{\"actions\": [ { \"action\": \"delete_column\", \"parameters\":{ \"column_id\": \"lastname\", \"scope\": \"column\" } }, { \"action\": \"split\", \"parameters\":{ \"column_id\": \"city\", \"scope\": \"column\", \"separator\":\" \", \"limit\":\"2\" } } ]}";
    }

    private String getTransformation_TDP_1184_step_2() {
        return "{\"actions\": [ { \"action\": \"delete_column\", \"parameters\":{ \"column_id\": \"lastname\", \"scope\": \"column\" } }, { \"action\": \"split\", \"parameters\":{ \"column_id\": \"city\", \"scope\": \"column\", \"separator\":\" \", \"limit\":\"2\" } }, { \"action\": \"uppercase\",\"parameters\":{ \"column_id\": \"0000\", \"scope\": \"column\" } } ]}";
    }

    private String getMultipleTransformationWithoutNewColumn() throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream("../preview/uppercase_lowercase.json"));
    }

    private String getMultipleTransformation() throws IOException {
        return IOUtils.toString(this.getClass().getResourceAsStream("../preview/various_actions.json"));
    }

}
