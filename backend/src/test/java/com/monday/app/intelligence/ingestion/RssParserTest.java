package com.monday.app.intelligence.ingestion;

import com.monday.app.intelligence.dto.RawRssFeed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RssParserTest {

    private RssParser parser;

    @BeforeEach
    void setUp() {
        parser = new RssParser();
    }

    @Test
    void parse_parsesRss2FeedSuccessfully() throws Exception {
        String rssXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
                <channel>
                    <title>Tech Insights</title>
                    <link>https://techinsights.org</link>
                    <description>Cutting edge tech news</description>
                    <language>en-us</language>
                    <item>
                        <title>Autonomous Systems in 2026</title>
                        <link>https://techinsights.org/posts/auto-sys-2026</link>
                        <description>In-depth coverage of autonomous agent orchestration.</description>
                        <author>Alice Developer</author>
                        <pubDate>Mon, 27 Oct 2026 12:00:00 GMT</pubDate>
                        <guid>https://techinsights.org/posts/auto-sys-2026</guid>
                    </item>
                </channel>
            </rss>
            """;

        RawRssFeed feed = parser.parse(rssXml);
        assertNotNull(feed);
        assertEquals("Tech Insights", feed.title());
        assertEquals("https://techinsights.org", feed.link());
        assertEquals(1, feed.items().size());
        assertEquals("Autonomous Systems in 2026", feed.items().get(0).title());
        assertEquals("Alice Developer", feed.items().get(0).author());
    }

    @Test
    void parse_parsesAtomFeedSuccessfully() throws Exception {
        String atomXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
                <title>Atom Architecture Digest</title>
                <link href="https://atomdigest.dev" rel="alternate"/>
                <subtitle>Distributed systems briefings</subtitle>
                <entry>
                    <title>Scaling Embedded SQLite</title>
                    <link href="https://atomdigest.dev/sqlite-scale" rel="alternate"/>
                    <id>urn:uuid:12345-67890</id>
                    <updated>2026-10-28T09:30:00Z</updated>
                    <summary>Optimizing I/O performance on NVMe drives.</summary>
                    <author><name>Bob Engineer</name></author>
                </entry>
            </feed>
            """;

        RawRssFeed feed = parser.parse(atomXml);
        assertNotNull(feed);
        assertEquals("Atom Architecture Digest", feed.title());
        assertEquals(1, feed.items().size());
        assertEquals("Scaling Embedded SQLite", feed.items().get(0).title());
        assertEquals("https://atomdigest.dev/sqlite-scale", feed.items().get(0).link());
        assertEquals("Bob Engineer", feed.items().get(0).author());
    }

    @Test
    void parse_throwsOnInvalidXml() {
        assertThrows(Exception.class, () -> parser.parse("Not XML at all"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(""));
    }
}
