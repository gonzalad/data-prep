package org.talend.dataprep.api.filter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.talend.dataprep.api.dataset.DataSetRow;

public class FilterServiceTest {

    FilterService service;

    DataSetRow row;

    @Before
    public void setUp() throws Exception {
        service = new SimpleFilterService();
        Map<String, String> values = new HashMap<String, String>() {
            {
                put("0000", "value");
                put("0001", "value with spaces");
                put("0002", "2");
                put("0003", "");
                put("0004", "10.5");
                put("0005", "abcde/XxXx-ZZZZ"); // Match "aaaaa/AaAa-AAAA"
                put("0006", "1234@55 678"); // Match "9999@99 999"
                put("0007", "14/12/2015 12:00:00"); // Match "d/m/yyyy h:m:s"
            }
        };
        row = new DataSetRow(values);
    }

    @Test
    public void testNullFilter() throws Exception {
        final Predicate<DataSetRow> predicate = service.build(null);
        assertThat(predicate.test(row), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMalformedFilter() throws Exception {
        service.build("{}");
    }

    @Test
    public void testEmptyFilter() throws Exception {
        final Predicate<DataSetRow> predicate = service.build("");
        assertThat(predicate.test(row), is(true));
    }

    @Test
    public void testEquals() throws Exception {
        // Test match on "0000 = value"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"eq\": {\"field\": \"0000\",\"value\": \"value\"}}");
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "0000 = value1"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"eq\": {\"field\": \"0000\",\"value\": \"value1\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testGreaterThan() throws Exception {
        // Test match on "0002 > 1"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"gt\": {\"field\": \"0002\",\"value\": \"1\"}}");
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "0002 > 3"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"gt\": {\"field\": \"0002\",\"value\": \"3\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testGreaterThanOnString() throws Exception {
        // Test match on "0001 > 3"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"gt\": {\"field\": \"0001\",\"value\": \"3\"}}");
        assertThat(matchPredicate.test(row), is(false));
    }

    @Test
    public void testGreaterThanEquals() throws Exception {
        // Test match on "0002 >= 2"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"gte\": {\"field\": \"0002\",\"value\": \"2\"}}");
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "0002 >= 3"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"gte\": {\"field\": \"0002\",\"value\": \"3\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testGreaterThanEqualsOnString() throws Exception {
        // Test match on "0001 >= 2"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"gte\": {\"field\": \"0001\",\"value\": \"2\"}}");
        assertThat(matchPredicate.test(row), is(false));
    }

    @Test
    public void testLessThan() throws Exception {
        // Test match on "0002 < 3"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"lt\": {\"field\": \"0002\",\"value\": \"3\"}}");
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "0002 < 1"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"lt\": {\"field\": \"0002\",\"value\": \"1\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testLessThanOnString() throws Exception {
        // Test non match on "0002 < 1"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"lt\": {\"field\": \"0001\",\"value\": \"1\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testLessThanEquals() throws Exception {
        // Test match on "0002 <= 2"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"lte\": {\"field\": \"0002\",\"value\": \"2\"}}");
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "0002 <= 1"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"lte\": {\"field\": \"0002\",\"value\": \"1\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testLessThanEqualsOnString() throws Exception {
        // Test non match on "0001 <= 2"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"lte\": {\"field\": \"0001\",\"value\": \"2\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testRange() throws Exception {
        // Test match on "0 <= 0002 < 3"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"range\": {\"field\": \"0002\",\"start\": \"0\",\"end\": \"3\"}}");
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "0 <= 0002 < 1"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"range\": {\"field\": \"0002\",\"start\": \"0\",\"end\": \"1\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testRangeOnDouble() throws Exception {
        // Test match on "10.4 <= 0004 < 10.6"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"range\": {\"field\": \"0004\",\"start\": \"10.4\",\"end\": \"10.6\"}}");
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "10.1 <= 0004 < 10.4"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"range\": {\"field\": \"0004\",\"start\": \"10.1\",\"end\": \"10.4\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testRangeOnDoubleInMax() throws Exception {
        // Test non match on "10.1 <= 0004 < 10.5"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"range\": {\"field\": \"0004\",\"start\": \"10.1\",\"end\": \"10.5\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testRangeMinEqualsMax() throws Exception {
        // Test non match on "10.5 <= 0004 < 10.5" -> should be equivalent to "0004 = 10.5"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"range\": {\"field\": \"0004\",\"start\": \"10.5\",\"end\": \"10.5\"}}");
        assertThat(nonMatchPredicate.test(row), is(true));
    }

    @Test
    public void testRangeOnString() throws Exception {
        // Test match on "0 <= 0001 <= 2"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"range\": {\"field\": \"0001\",\"start\": \"0\",\"end\": \"2\"}}");
        assertThat(matchPredicate.test(row), is(false));
    }

    @Test
    public void testContains() throws Exception {
        // Test match on "0000 contains alu"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"contains\": {\"field\": \"0000\",\"value\": \"alu\"}}");
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "0000 contains text"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"contains\": {\"field\": \"0000\",\"value\": \"text\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testContainsCaseNotSensitive() throws Exception {
        // Test match on "0000 contains alu"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"contains\": {\"field\": \"0000\",\"value\": \"ALU\"}}");
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "0000 contains text"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"contains\": {\"field\": \"0000\",\"value\": \"TEXT\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testSimpleAndEquals() throws Exception {
        // Test match on "0000 = value && 0002 = 2"
        final Predicate<DataSetRow> matchPredicate = service.build(IOUtils.toString(FilterServiceTest.class.getResourceAsStream("simpleAnd_match.json")));
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "0000 = value && 0001 = 2"
        final Predicate<DataSetRow> nonMatchPredicate = service.build(IOUtils.toString(FilterServiceTest.class.getResourceAsStream("simpleAnd_non_match.json")));
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testComplexAndEquals() throws Exception {
        // Test match on "0000 = value && 0001 = value with spaces && 0002 = 2"
        final Predicate<DataSetRow> matchPredicate = service.build(IOUtils.toString(FilterServiceTest.class.getResourceAsStream("simpleAnd_match.json")));
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "0000 = value && 0001 = value with spaces && 0002 = 3"
        final Predicate<DataSetRow> nonMatchPredicate = service.build(IOUtils.toString(FilterServiceTest.class.getResourceAsStream("simpleAnd_non_match.json")));
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMalformedAnd() throws Exception {
        service.build(IOUtils.toString(FilterServiceTest.class.getResourceAsStream("malformedAnd.json")));
    }

    @Test
    public void testNot() throws Exception {
        // Test match on "not(0000 = value1)"
        final Predicate<DataSetRow> matchPredicate = service.build(IOUtils.toString(FilterServiceTest.class.getResourceAsStream("not.json")));
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "not(0000 = value)"
        final Predicate<DataSetRow> nonMatchPredicate = service.build(IOUtils.toString(FilterServiceTest.class.getResourceAsStream("not_non_match.json")));
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testAndNot() throws Exception {
        // Test match on "not(0000 = value1) && not(0002 = 3)"
        final Predicate<DataSetRow> matchPredicate = service.build(IOUtils.toString(FilterServiceTest.class.getResourceAsStream("simpleAnd_not_match.json")));
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "not(0000 = value) && not(0001 = 2)"
        final Predicate<DataSetRow> nonMatchPredicate = service.build(IOUtils.toString(FilterServiceTest.class.getResourceAsStream("simpleAnd_not_non_match.json")));
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMalformedNotWithArray() throws Exception {
        service.build(IOUtils.toString(FilterServiceTest.class.getResourceAsStream("malformed_not_array.json")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMalformedNotWithNull() throws Exception {
        service.build(IOUtils.toString(FilterServiceTest.class.getResourceAsStream("malformed_not_null.json")));
    }

    @Test
    public void testInvalid() throws Exception {
        row.getRowMetadata().getById("0000").getQuality().getInvalidValues().add("value");
        // Test match on "invalid(0000)"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"invalid\": {\"field\": \"0000\"}}");
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "invalid(0001)"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"invalid\": {\"field\": \"0001\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testValid() throws Exception {
        row.getRowMetadata().getById("0000").getQuality().getInvalidValues().add("value");
        // Test match on "valid(0001)"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"valid\": {\"field\": \"0001\"}}");
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "valid(0000)"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"valid\": {\"field\": \"0000\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testEmpty() throws Exception {
        // Test match on "empty(0003)"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"empty\": {\"field\": \"0003\"}}");
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "empty(0000)"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"empty\": {\"field\": \"0000\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testStringMatches() throws Exception {
        // Test match on "matches(0005, aaaaa/AaAa-AAAA)"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"matches\": {\"field\": \"0005\", \"value\": \"aaaaa/AaAa-AAAA\"}}");
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "matches(0005, aaaaa_Aa_)"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"matches\": {\"field\": \"0005\", \"value\": \"aaaaa_Aa_\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    @Test
    public void testNumberMatches() throws Exception {
        // Test match on "matches(0006, 9999@99 999)"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"matches\": {\"field\": \"0006\", \"value\": \"9999@99 999\"}}");
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "matches(0006, 9999_99-999)"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"matches\": {\"field\": \"0006\", \"value\": \"9999_99-999\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

    // d/m/yyyy h:m:s
    @Test
    public void testDateMatches() throws Exception {
        // Test match on "matches(0007, d/m/yyyy h:m:s)"
        final Predicate<DataSetRow> matchPredicate = service.build("{\"matches\": {\"field\": \"0007\", \"value\": \"d/M/yyyy h:m:s\"}}");
        assertThat(matchPredicate.test(row), is(true));
        // Test non match on "matches(0007, d/m/yyyy)"
        final Predicate<DataSetRow> nonMatchPredicate = service.build("{\"matches\": {\"field\": \"0007\", \"value\": \"d/M/yyyy\"}}");
        assertThat(nonMatchPredicate.test(row), is(false));
    }

}