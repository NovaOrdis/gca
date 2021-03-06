package com.novaordis.gc.parser.linear;

import com.novaordis.gc.model.Field;
import com.novaordis.gc.model.FieldType;
import com.novaordis.gc.model.Timestamp;
import com.novaordis.gc.model.event.GCEvent;
import com.novaordis.gc.model.event.NewGenerationCollection;
import com.novaordis.gc.parser.GCLogParser;
import com.novaordis.gc.parser.GCLogParserFactory;
import com.novaordis.gc.parser.TimeOrigin;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

/**
 * @author <a href="mailto:ovidiu@novaordis.com">Ovidiu Feodorov</a>
 *
 * Copyright 2013 Nova Ordis LLC
 */
public class NewGenerationCollectionParserTest extends Assert
{
    // Constants -------------------------------------------------------------------------------------------------------

    private static final Logger log = Logger.getLogger(NewGenerationCollectionParserTest.class);

    // Static ----------------------------------------------------------------------------------------------------------

    // Attributes ------------------------------------------------------------------------------------------------------

    // Constructors ----------------------------------------------------------------------------------------------------

    // Public ----------------------------------------------------------------------------------------------------------

    @Test
    public void testUnrecognized() throws Exception
    {
        String line = "[GC [PSYoungGen: this is something we did not see so far]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        GCEvent event = p.parse(null, line, 78, null);

        // temporarily dropping this line as unparseable
        assertNull(event);
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

    // embedded timestamp tests ----------------------------------------------------------------------------------------

    // These are NG tests because I've seen the condition only showing up in case of NG collections, but we need to use
    // the entire parser to catch it, as it involves leading timestamp processing.

    @Test
    public void embedded_timestamp_leadingTimestampBeforeEmbeddedTimestamp() throws Exception
    {
        String s = "598272.974: [GC 598272.975: [ParNew: 4374049K->1247393K(4518144K), 0.3814120 secs] 13519475K->10668769K(16567552K), 0.3819210 secs] [Times: user=1.27 sys=0.00, real=0.38 secs]";

        Reader r = new InputStreamReader(new ByteArrayInputStream(s.getBytes()));
        GCLogParser p = GCLogParserFactory.getParser(r);
        assertTrue(p instanceof LinearScanParser);

        List<GCEvent> events = p.parse(new TimeOrigin(0L));

        r.close();

        assertEquals(1, events.size());

        NewGenerationCollection ng = (NewGenerationCollection)events.get(0);

        Field field = ng.get(FieldType.EMBEDDED_TIMESTAMP_LITERAL);
        assertNotNull(field);
        String value = (String)field.getValue();

        assertEquals("598272.975", value);
        assertEquals(598272974L, ng.getTime().longValue());
    }

    @Test
    public void embedded_timestamp_leadingTimestampAfterEmbeddedTimestamp() throws Exception
    {
        String s = "598272.976: [GC 598272.975: [ParNew: 4374049K->1247393K(4518144K), 0.3814120 secs] 13519475K->10668769K(16567552K), 0.3819210 secs] [Times: user=1.27 sys=0.00, real=0.38 secs]";

        Reader r = new InputStreamReader(new ByteArrayInputStream(s.getBytes()));

        GCLogParser p = GCLogParserFactory.getParser(r);
        assertTrue(p instanceof LinearScanParser);

        try
        {
            p.parse(new TimeOrigin(0L));
            fail("should fail with IllegalStateException because 598272.976 does not precede or equal with 598272.975");
        }
        catch(IllegalStateException e)
        {
            log.info(e.getMessage());
        }

        r.close();
    }

    @Test
    public void duplicateTimestamp_TimestampsDoNotMatch() throws Exception
    {
        String line = "[GC 53233.950: [ParNew: 4070928K->986133K(4373760K), 0.0997910 secs] 12266198K->9181403K(16039168K), 0.1002900 secs] [Times: user=0.76 sys=0.00, real=0.10 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(53233949L).applyTimeOrigin(0L);

        GCEvent event = p.parse(ts, line, 77L, null);

        NewGenerationCollection ng = (NewGenerationCollection)event;

        assertEquals(53233949L, ng.getTime().longValue());
        assertEquals("53233.950", ng.get(FieldType.EMBEDDED_TIMESTAMP_LITERAL).getValue());
    }

    @Test
    public void embeddedOffsetPrecedesThanLineStartOffset() throws Exception
    {
        String line = "[GC 1.985: [ParNew: 136320K->6357K(153344K), 0.0083580 secs] 136320K->6357K(4177280K), 0.0085020 secs] [Times: user=0.05 sys=0.01, real=0.01 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(1986L);

        try
        {
            p.parse(ts, line, 10, null);
            fail("should fail with IllegalStateException e");
        }
        catch(IllegalStateException e)
        {
            log.info(e.getMessage());
        }
    }

    @Test
    public void embeddedOffsetBiggerThanLineStartOffset() throws Exception
    {
        String line = "[GC 1.986: [ParNew: 136320K->6357K(153344K), 0.0083580 secs] 136320K->6357K(4177280K), 0.0085020 secs] [Times: user=0.05 sys=0.01, real=0.01 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(1985L).applyTimeOrigin(0L);

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

    // ParNew ----------------------------------------------------------------------------------------------------------

    @Test
    public void testParNew() throws Exception
    {
        String line = "[GC 1.985: [ParNew: 136320K->6357K(153344K), 0.0083580 secs] 136320K->6357K(4177280K), 0.0085020 secs] [Times: user=0.05 sys=0.01, real=0.01 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(1985L).applyTimeOrigin(0L);

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
    public void testParNewWithPrintGCDateStamps() throws Exception
    {
        String line = "[GC2014-08-14T08:38:27.033-0700: 53795.248: [ParNew: 451130K->52416K(471872K), 0.0293380 secs] 1011202K->614147K(4141888K) icms_dc=0 , 0.0294790 secs] [Times: user=0.14 sys=0.00, real=0.03 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(53795248L).applyTimeOrigin(0L);

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
    public void testPromotionFailed_degenerateCase() throws Exception
    {
        // we should never get in this situation, this is a remnant test from before splitting the
        // line by the timestamps

        String line = "[GC2014-08-14T01:12:28.622-0700: 27036.837: [ParNew (promotion failed): 471872K->471872K(471872K), 0.3931530 secs]2014-08-14T01:12:29.015-0700: 27037.231: [CMS2014-08-14T01:12:29.867-0700: 27038.083: [CMS-concurrent-preclean: 4.167/17.484 secs] [Times: user=21.55 sys=2.82, real=17.48 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(27036837L);

        GCEvent event = p.parse(ts, line, 77L, null);

        assertNull("should bail on the event as we're trying to parse a timestamp as heap info", event);
    }

    @Test
    public void testGCAndCMSEventsOnTheSameLine_degenerateCase() throws Exception
    {
        // we should never get in this situation, this is a remnant test from before splitting the
        // line by the timestamps

        String line = "[GC2014-08-14T01:53:16.892-0700: 29485.108: [ParNew: 471872K->471872K(471872K), 0.0000420 secs]2014-08-14T01:53:16.892-0700: 29485.108: [CMS2014-08-14T01:53:17.076-0700: 29485.292: [CMS-concurrent-mark: 4.337/12.788 secs] [Times: user=19.68 sys=0.31, real=12.79 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(29485108L);

        GCEvent event = p.parse(ts, line, 77L, null);

        assertNull("should bail on the event as we're trying to parse a timestamp as heap info", event);
    }

    @Test
    public void duplicateTimestamp_TimestampsMatch() throws Exception
    {
        String line = "[GC 53233.950: [ParNew: 4070928K->986133K(4373760K), 0.0997910 secs] 12266198K->9181403K(16039168K), 0.1002900 secs] [Times: user=0.76 sys=0.00, real=0.10 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(53233950L).applyTimeOrigin(0L);

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
    public void testDefNew() throws Exception
    {
        String line = "[GC 4.993: [DefNew: 204800K->20403K(307200K), 0.0417850 secs] 204800K->20403K(1126400K), 0.0418540 secs] [Times: user=0.02 sys=0.02, real=0.04 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(4993L);

        NewGenerationCollection e = (NewGenerationCollection)p.parse(ts, line, 7L, null);

        assertNotNull(e);

        assertEquals(-1L, e.getTime().longValue());
        assertEquals(4993L, e.getOffset().longValue());

        assertEquals(204800L * 1024, e.get(FieldType.NG_BEFORE).getValue());
        assertEquals(20403L * 1024, e.get(FieldType.NG_AFTER).getValue());
        assertEquals(307200L * 1024, e.get(FieldType.NG_CAPACITY).getValue());

        assertEquals(204800L * 1024, e.get(FieldType.HEAP_BEFORE).getValue());
        assertEquals(20403L * 1024, e.get(FieldType.HEAP_AFTER).getValue());
        assertEquals(1126400L * 1024, e.get(FieldType.HEAP_CAPACITY).getValue());

        assertEquals(42, e.getDuration(), 0.01);
    }

    @Test
    public void testDefNew_PromotionFailed() throws Exception
    {
        String line = "[GC 1080.181: [DefNew (promotion failed) : 256020K->243246K(307200K), 0.0986650 secs]";

        NewGenerationCollectionParser p = new NewGenerationCollectionParser();

        Timestamp ts = new Timestamp(1080181L);

        NewGenerationCollection e = (NewGenerationCollection)p.parse(ts, line, 7L, null);

        assertNotNull(e);

        assertEquals(-1L, e.getTime().longValue());
        assertEquals(1080181L, e.getOffset().longValue());

        assertEquals("promotion failed", e.get(FieldType.NOTES).getValue());

        assertEquals(256020L * 1024, e.get(FieldType.NG_BEFORE).getValue());
        assertEquals(243246L * 1024, e.get(FieldType.NG_AFTER).getValue());
        assertEquals(307200L * 1024, e.get(FieldType.NG_CAPACITY).getValue());

        assertNull(e.get(FieldType.HEAP_CAPACITY));
        assertEquals(99, e.getDuration(), 0.01);
    }

    // Package protected -------------------------------------------------------------------------------------------------------------------

    // Protected ---------------------------------------------------------------------------------------------------------------------------

    // Private -----------------------------------------------------------------------------------------------------------------------------

    // Inner classes -----------------------------------------------------------------------------------------------------------------------
}



