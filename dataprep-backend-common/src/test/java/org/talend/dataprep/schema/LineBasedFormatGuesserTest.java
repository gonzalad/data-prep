package org.talend.dataprep.schema;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Unit test for the LineBasedFormatGuesser.
 * 
 * @see LineBasedFormatGuesser
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = LineBasedFormatGuesserTest.class)
@Configuration
@ComponentScan(basePackages = "org.talend.dataprep")
@EnableAutoConfiguration
public class LineBasedFormatGuesserTest {


    /** The format guesser to test. */
    @Autowired
    LineBasedFormatGuesser guesser;

    /**
     * Text file
     */
    @Test
    public void should_not_guess() throws IOException {
        FormatGuesser.Result actual = guesser.guess(new ByteArrayInputStream(new byte[0]), "UTF-8");

        Assert.assertNotNull(actual);
        Assert.assertTrue(actual.getFormatGuess() instanceof UnsupportedFormatGuess);

    }

    /**
     * Standard csv file.
     */
    @Test
    public void should_guess_CSV() throws IOException {

        String fileName = "org/talend/dataprep/schema/standard.csv";
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            FormatGuesser.Result actual = guesser.guess(inputStream, "UTF-8");

            Assert.assertNotNull(actual);
            Assert.assertTrue(actual.getFormatGuess() instanceof CSVFormatGuess);
        }
    }

    /**
     * csv file with 2 possible separators : ';' or '/', ';' should be selected
     */
    @Test
    public void should_guess_best_separator() throws IOException {
        String fileName = "org/talend/dataprep/schema/mixed_separators.csv";
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            FormatGuesser.Result actual = guesser.guess(inputStream, "UTF-8");

            Assert.assertNotNull(actual);
            Assert.assertTrue(actual.getFormatGuess() instanceof CSVFormatGuess);
            char separator = actual.getParameters().get(CSVFormatGuess.SEPARATOR_PARAMETER).charAt(0);
            Assert.assertEquals(separator, ';');
        }
    }

    /**
     * Have a look at https://jira.talendforge.org/browse/TDP-181
     */
    @Test
    public void should_guess_best_separator_out_of_two() throws IOException {
        String fileName = "org/talend/dataprep/schema/tdp-181.csv";
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            FormatGuesser.Result actual = guesser.guess(inputStream, "UTF-8");

            Assert.assertNotNull(actual);
            Assert.assertTrue(actual.getFormatGuess() instanceof CSVFormatGuess);
            char separator = actual.getParameters().get(CSVFormatGuess.SEPARATOR_PARAMETER).charAt(0);
            Assert.assertEquals(separator, ';');
        }
    }

    /**
     * Have a look at https://jira.talendforge.org/browse/TDP-258
     */
    @Test
    public void should_guess_separator_with_ISO_8859_1_encoded_file() throws IOException {
        String fileName = "org/talend/dataprep/schema/iso-8859-1.csv";
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            FormatGuesser.Result actual = guesser.guess(inputStream, "UTF-8");

            Assert.assertNotNull(actual);
            Assert.assertTrue(actual.getFormatGuess() instanceof CSVFormatGuess);
            char separator = actual.getParameters().get(CSVFormatGuess.SEPARATOR_PARAMETER).charAt(0);
            Assert.assertEquals(separator, ';');
        }
    }
}