package com.novaordis.gc.parser.linear;

import com.novaordis.gc.model.FieldType;
import com.novaordis.gc.model.Timestamp;
import com.novaordis.gc.model.event.NewGenerationCollection;
import com.novaordis.gc.parser.ParserException;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:ovidiu@novaordis.com">Ovidiu Feodorov</a>
 *
 * Copyright 2013 Nova Ordis LLC
 */
public class NewGenerationCollectionParserTest extends Assert
{
    // Constants ---------------------------------------------------------------------------------------------------------------------------

    private static final Logger log = Logger.getLogger(NewGenerationCollectionParserTest.class);

    // Static ------------------------------------------------------------------------------------------------------------------------------

    // Attributes --------------------------------------------------------------------------------------------------------------------------

    // Constructors ------------------------------------------------------------------------------------------------------------------------

    // Public ------------------------------------------------------------------------------------------------------------------------------

    @Test
    public void testUnrecognized() throws Exception
    {
        String line = "[GC [PSYoungGen: this is something we did not see so far]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        try
        {
            p.parse(null, line, 78, null);
            fail("should have failed with unrecognized format");
        }
        catch(ParserException e)
        {
            assertEquals(78, e.getLineNumber());
            log.info(e.getMessage());
        }
    }

    // && !line.startsWith("[GC-- [PSYoungGen")
    @Test
    public void testValidSample() throws Exception
    {
        String line = "[GC [PSYoungGen: 1890405K->109278K(1933696K)] 2815732K->1034613K(6128000K), 0.0342390 secs] [Times: user=0.24 sys=0.00, real=0.04 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(1000L).applyTimeOrigin(0L);

        NewGenerationCollection e = (NewGenerationCollection)p.parse(ts, line, -1, null);

        assertNotNull(e);

        assertEquals(1000L, e.getTime().longValue());
        assertEquals(1000L, e.getOffset().longValue());

        assertEquals(1890405L * 1024, e.get(FieldType.NG_BEFORE).getValue());
        assertEquals(109278L * 1024, e.get(FieldType.NG_AFTER).getValue());
        assertEquals(1933696L * 1024, e.get(FieldType.NG_CAPACITY).getValue());

        assertEquals(2815732L * 1024, e.get(FieldType.HEAP_BEFORE).getValue());
        assertEquals(1034613L * 1024, e.get(FieldType.HEAP_AFTER).getValue());
        assertEquals(6128000L * 1024, e.get(FieldType.HEAP_CAPACITY).getValue());

        assertEquals(34, e.getDuration());
    }

    @Test
    public void testValidSample2() throws Exception
    {
        String line = "[GC-- [PSYoungGen: 1139286K->1139286K(1398144K)] 4848062K->5170981K(5592448K), 0.0536370 secs] [Times: user=0.31 sys=0.00, real=0.05 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(1000L).applyTimeOrigin(0L);

        NewGenerationCollection e = (NewGenerationCollection)p.parse(ts, line, -1, null);

        assertNotNull(e);

        assertEquals(1000L, e.getTime().longValue());
        assertEquals(1000L, e.getOffset().longValue());

        assertEquals(1139286L * 1024, e.get(FieldType.NG_BEFORE).getValue());
        assertEquals(1139286L * 1024, e.get(FieldType.NG_AFTER).getValue());
        assertEquals(1398144L * 1024, e.get(FieldType.NG_CAPACITY).getValue());

        assertEquals(4848062L * 1024, e.get(FieldType.HEAP_BEFORE).getValue());
        assertEquals(5170981L * 1024, e.get(FieldType.HEAP_AFTER).getValue());
        assertEquals(5592448L * 1024, e.get(FieldType.HEAP_CAPACITY).getValue());

        assertEquals(54, e.getDuration(), 0.0001);
    }

    //
    // CMS new generation collections
    //
    // [GC 1.985: [ParNew: 136320K->6357K(153344K), 0.0083580 secs] 136320K->6357K(4177280K), 0.0085020 secs] [Times: user=0.05 sys=0.01, real=0.01 secs]
    //

    @Test
    public void testCMS_EmbeddedOffsetPrecedesThanLineStartOffset() throws Exception
    {
        String line = "[GC 1.985: [ParNew: 136320K->6357K(153344K), 0.0083580 secs] 136320K->6357K(4177280K), 0.0085020 secs] [Times: user=0.05 sys=0.01, real=0.01 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(1986L);

        try
        {
            p.parse(ts, line, 10, null);
            fail("should have thrown exception, mismatching offset");
        }
        catch(ParserException e)
        {
            log.info(e.getMessage());
            assertEquals(10, e.getLineNumber());
        }
    }

    @Test
    public void testCMS_EmbeddedOffsetBiggerThanLineStartOffset() throws Exception
    {
        String line = "[GC 1.986: [ParNew: 136320K->6357K(153344K), 0.0083580 secs] 136320K->6357K(4177280K), 0.0085020 secs] [Times: user=0.05 sys=0.01, real=0.01 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(1985L).applyTimeOrigin(0);

        NewGenerationCollection e = (NewGenerationCollection)p.parse(ts, line, -1, null);

        assertNotNull(e);

        assertEquals(1985L, e.getTime().longValue());
        assertEquals(1985L, e.getOffset().longValue());

        assertEquals(136320L * 1024, e.get(FieldType.NG_BEFORE).getValue());
        assertEquals(6357L * 1024, e.get(FieldType.NG_AFTER).getValue());
        assertEquals(153344L * 1024, e.get(FieldType.NG_CAPACITY).getValue());

        assertEquals(136320L * 1024, e.get(FieldType.HEAP_BEFORE).getValue());
        assertEquals(6357L * 1024, e.get(FieldType.HEAP_AFTER).getValue());
        assertEquals(4177280L * 1024, e.get(FieldType.HEAP_CAPACITY).getValue());

        assertEquals(9, e.getDuration(), 0.0001);
    }

    @Test
    public void testCMS() throws Exception
    {
        String line = "[GC 1.985: [ParNew: 136320K->6357K(153344K), 0.0083580 secs] 136320K->6357K(4177280K), 0.0085020 secs] [Times: user=0.05 sys=0.01, real=0.01 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(1985L).applyTimeOrigin(0);

        NewGenerationCollection e = (NewGenerationCollection)p.parse(ts, line, -1, null);

        assertNotNull(e);

        assertEquals(1985L, e.getTime().longValue());
        assertEquals(1985L, e.getOffset().longValue());

        assertEquals(136320L * 1024, e.get(FieldType.NG_BEFORE).getValue());
        assertEquals(6357L * 1024, e.get(FieldType.NG_AFTER).getValue());
        assertEquals(153344L * 1024, e.get(FieldType.NG_CAPACITY).getValue());

        assertEquals(136320L * 1024, e.get(FieldType.HEAP_BEFORE).getValue());
        assertEquals(6357L * 1024, e.get(FieldType.HEAP_AFTER).getValue());
        assertEquals(4177280L * 1024, e.get(FieldType.HEAP_CAPACITY).getValue());

        assertEquals(9, e.getDuration(), 0.0001);
    }

    @Test
    public void testCMSWithPrintGCDateStamps() throws Exception
    {
        String line = "[GC2014-08-14T08:38:27.033-0700: 53795.248: [ParNew: 451130K->52416K(471872K), 0.0293380 secs] 1011202K->614147K(4141888K) icms_dc=0 , 0.0294790 secs] [Times: user=0.14 sys=0.00, real=0.03 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(53795248L).applyTimeOrigin(0);

        NewGenerationCollection e = (NewGenerationCollection)p.parse(ts, line, -1, null);

        assertNotNull(e);

        assertEquals(53795248L, e.getTime().longValue());
        assertEquals(53795248L, e.getOffset().longValue());

        assertEquals(451130L * 1024, e.get(FieldType.NG_BEFORE).getValue());
        assertEquals(52416L * 1024, e.get(FieldType.NG_AFTER).getValue());
        assertEquals(471872L * 1024, e.get(FieldType.NG_CAPACITY).getValue());

        assertEquals(1011202L * 1024, e.get(FieldType.HEAP_BEFORE).getValue());
        assertEquals(614147L * 1024, e.get(FieldType.HEAP_AFTER).getValue());
        assertEquals(4141888L * 1024, e.get(FieldType.HEAP_CAPACITY).getValue());

        assertEquals(29, e.getDuration(), 0.0001);
    }

    @Test
    public void testCMS_PromotionFailed_degenerateCase() throws Exception
    {
        // we should never get in this situation, this is a remnant test from before splitting the
        // line by the timestamps

        String line = "[GC2014-08-14T01:12:28.622-0700: 27036.837: [ParNew (promotion failed): 471872K->471872K(471872K), 0.3931530 secs]2014-08-14T01:12:29.015-0700: 27037.231: [CMS2014-08-14T01:12:29.867-0700: 27038.083: [CMS-concurrent-preclean: 4.167/17.484 secs] [Times: user=21.55 sys=2.82, real=17.48 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(27036837L);

        try
        {
            p.parse(ts, line, 77L, null);
            fail("should fail with ParserException as we're trying to parse a timestamp as heap info");
        }
        catch(ParserException e)
        {
            log.info(e.getMessage());
            assertEquals(77L, e.getLineNumber());
        }
    }

    @Test
    public void testCMS_GCAndCMSEventsOnTheSameLine_degenerateCase() throws Exception
    {
        // we should never get in this situation, this is a remnant test from before splitting the
        // line by the timestamps

        String line = "[GC2014-08-14T01:53:16.892-0700: 29485.108: [ParNew: 471872K->471872K(471872K), 0.0000420 secs]2014-08-14T01:53:16.892-0700: 29485.108: [CMS2014-08-14T01:53:17.076-0700: 29485.292: [CMS-concurrent-mark: 4.337/12.788 secs] [Times: user=19.68 sys=0.31, real=12.79 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(29485108L);

        try
        {
            p.parse(ts, line, 77L, null);
            fail("should fail with ParserException as we're trying to parse a timestamp as heap info");
        }
        catch(ParserException e)
        {
            log.info(e.getMessage());
            assertEquals(77L, e.getLineNumber());
        }
    }

    //
    // [GC 53233.950: [ParNew: 4070928K->986133K(4373760K), 0.0997910 secs] 12266198K->9181403K(16039168K), 0.1002900 secs] [Times: user=0.76 sys=0.00, real=0.10 secs]
    //

    @Test
    public void duplicateTimestamp_TimestampsMatch() throws Exception
    {
        String line = "[GC 53233.950: [ParNew: 4070928K->986133K(4373760K), 0.0997910 secs] 12266198K->9181403K(16039168K), 0.1002900 secs] [Times: user=0.76 sys=0.00, real=0.10 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(53233950L).applyTimeOrigin(0);

        NewGenerationCollection e = (NewGenerationCollection)p.parse(ts, line, -1, null);

        assertNotNull(e);

        assertEquals(53233950L, e.getTime().longValue());
        assertEquals(53233950L, e.getOffset().longValue());

        assertEquals(4070928L * 1024, e.get(FieldType.NG_BEFORE).getValue());
        assertEquals(986133L * 1024, e.get(FieldType.NG_AFTER).getValue());
        assertEquals(4373760L * 1024, e.get(FieldType.NG_CAPACITY).getValue());

        assertEquals(12266198L * 1024, e.get(FieldType.HEAP_BEFORE).getValue());
        assertEquals(9181403L * 1024, e.get(FieldType.HEAP_AFTER).getValue());
        assertEquals(16039168L * 1024, e.get(FieldType.HEAP_CAPACITY).getValue());

        assertEquals(100, e.getDuration(), 0.01);
    }

    @Test
    public void duplicateTimestamp_TimestampsDoNotMatch() throws Exception
    {
        String line = "[GC 53233.950: [ParNew: 4070928K->986133K(4373760K), 0.0997910 secs] 12266198K->9181403K(16039168K), 0.1002900 secs] [Times: user=0.76 sys=0.00, real=0.10 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(53233951L);

        try
        {
            p.parse(ts, line, 77L, null);
            fail("should have failed with duplicated timestamp mismatch");
        }
        catch(ParserException e)
        {
            log.info(e.getMessage());
            assertEquals(77L, e.getLineNumber());
        }
    }

    // Package protected -------------------------------------------------------------------------------------------------------------------

    // Protected ---------------------------------------------------------------------------------------------------------------------------

    // Private -----------------------------------------------------------------------------------------------------------------------------

    // Inner classes -----------------------------------------------------------------------------------------------------------------------
}



