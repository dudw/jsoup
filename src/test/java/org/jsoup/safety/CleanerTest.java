package org.jsoup.safety;

import org.jsoup.Jsoup;
import org.jsoup.MultiLocaleExtension.MultiLocaleTest;
import org.jsoup.TextUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Range;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.parser.TagSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 Tests for the cleaner.

 @author Jonathan Hedley, jonathan@hedley.net */
public class CleanerTest {
    @Test public void simpleBehaviourTest() {
        String h = "<div><p class=foo><a href='http://evil.com'>Hello <b id=bar>there</b>!</a></div>";
        String cleanHtml = Jsoup.clean(h, Safelist.simpleText());

        assertEquals("Hello <b>there</b>!", TextUtil.stripNewlines(cleanHtml));
    }

    @Test public void simpleBehaviourTest2() {
        String h = "Hello <b>there</b>!";
        String cleanHtml = Jsoup.clean(h, Safelist.simpleText());

        assertEquals("Hello <b>there</b>!", TextUtil.stripNewlines(cleanHtml));
    }

    @Test public void basicBehaviourTest() {
        String h = "<div><p><a href='javascript:sendAllMoney()'>Dodgy</a> <A HREF='HTTP://nice.com'>Nice</a></p><blockquote>Hello</blockquote>";
        String cleanHtml = Jsoup.clean(h, Safelist.basic());

        assertEquals("<p><a rel=\"nofollow\">Dodgy</a> <a href=\"http://nice.com\" rel=\"nofollow\">Nice</a></p><blockquote>Hello</blockquote>",
                TextUtil.stripNewlines(cleanHtml));
    }

    @Test public void basicWithImagesTest() {
        String h = "<div><p><img src='http://example.com/' alt=Image></p><p><img src='ftp://ftp.example.com'></p></div>";
        String cleanHtml = Jsoup.clean(h, Safelist.basicWithImages());
        assertEquals("<p><img src=\"http://example.com/\" alt=\"Image\"></p><p><img></p>", TextUtil.stripNewlines(cleanHtml));
    }

    @Test public void testRelaxed() {
        String h = "<h1>Head</h1><table><tr><td>One<td>Two</td></tr></table>";
        String cleanHtml = Jsoup.clean(h, Safelist.relaxed());
        assertEquals("<h1>Head</h1><table><tbody><tr><td>One</td><td>Two</td></tr></tbody></table>", TextUtil.stripNewlines(cleanHtml));
    }

    @Test public void testRemoveTags() {
        String h = "<div><p><A HREF='HTTP://nice.com'>Nice</a></p><blockquote>Hello</blockquote>";
        String cleanHtml = Jsoup.clean(h, Safelist.basic().removeTags("a"));

        assertEquals("<p>Nice</p><blockquote>Hello</blockquote>", TextUtil.stripNewlines(cleanHtml));
    }

    @Test public void testRemoveAttributes() {
        String h = "<div><p>Nice</p><blockquote cite='http://example.com/quotations'>Hello</blockquote>";
        String cleanHtml = Jsoup.clean(h, Safelist.basic().removeAttributes("blockquote", "cite"));

        assertEquals("<p>Nice</p><blockquote>Hello</blockquote>", TextUtil.stripNewlines(cleanHtml));
    }

    @Test void allAttributes() {
        String h = "<div class=foo data=true><p class=bar>Text</p></div><blockquote cite='https://example.com'>Foo";
        Safelist safelist = Safelist.relaxed();
        safelist.addAttributes(":all", "class");
        safelist.addAttributes("div", "data");

        String clean1 = Jsoup.clean(h, safelist);
        assertEquals("<div class=\"foo\" data=\"true\"><p class=\"bar\">Text</p></div><blockquote cite=\"https://example.com\">Foo</blockquote>", TextUtil.stripNewlines(clean1));

        safelist.removeAttributes(":all", "class", "cite");

        String clean2 = Jsoup.clean(h, safelist);
        assertEquals("<div data=\"true\"><p>Text</p></div><blockquote>Foo</blockquote>", TextUtil.stripNewlines(clean2));
    }

    @Test void removeProtocols() {
        String h = "<a href='any://example.com'>Link</a>";
        Safelist safelist = Safelist.relaxed();
        String clean1 = Jsoup.clean(h, safelist);
        assertEquals("<a>Link</a>", clean1);

        safelist.removeProtocols("a", "href", "ftp", "http", "https", "mailto");
        String clean2 = Jsoup.clean(h, safelist); // all removed means any will work
        assertEquals("<a href=\"any://example.com\">Link</a>", clean2);
    }

    @Test public void testRemoveEnforcedAttributes() {
        String h = "<div><p><A HREF='HTTP://nice.com'>Nice</a></p><blockquote>Hello</blockquote>";
        String cleanHtml = Jsoup.clean(h, Safelist.basic().removeEnforcedAttribute("a", "rel"));

        assertEquals("<p><a href=\"http://nice.com\">Nice</a></p><blockquote>Hello</blockquote>",
                TextUtil.stripNewlines(cleanHtml));
    }

    @Test public void testRemoveProtocols() {
        String h = "<p>Contact me <a href='mailto:info@example.com'>here</a></p>";
        String cleanHtml = Jsoup.clean(h, Safelist.basic().removeProtocols("a", "href", "ftp", "mailto"));

        assertEquals("<p>Contact me <a rel=\"nofollow\">here</a></p>",
                TextUtil.stripNewlines(cleanHtml));
    }

    @MultiLocaleTest
    public void safeListedProtocolShouldBeRetained(Locale locale) {
        Locale.setDefault(locale);

        Safelist safelist = Safelist.none()
                .addTags("a")
                .addAttributes("a", "href")
                .addProtocols("a", "href", "something");

        String cleanHtml = Jsoup.clean("<a href=\"SOMETHING://x\"></a>", safelist);

        assertEquals("<a href=\"SOMETHING://x\"></a>", TextUtil.stripNewlines(cleanHtml));
    }

    @Test public void testDropComments() {
        String h = "<p>Hello<!-- no --></p>";
        String cleanHtml = Jsoup.clean(h, Safelist.relaxed());
        assertEquals("<p>Hello</p>", cleanHtml);
    }

    @Test public void testDropXmlProc() {
        String h = "<?import namespace=\"xss\"><p>Hello</p>";
        String cleanHtml = Jsoup.clean(h, Safelist.relaxed());
        assertEquals("<p>Hello</p>", cleanHtml);
    }

    @Test public void testDropScript() {
        String h = "<SCRIPT SRC=//ha.ckers.org/.j><SCRIPT>alert(/XSS/.source)</SCRIPT>";
        String cleanHtml = Jsoup.clean(h, Safelist.relaxed());
        assertEquals("", cleanHtml);
    }

    @Test public void testDropImageScript() {
        String h = "<IMG SRC=\"javascript:alert('XSS')\">";
        String cleanHtml = Jsoup.clean(h, Safelist.relaxed());
        assertEquals("<img>", cleanHtml);
    }

    @Test public void testCleanJavascriptHref() {
        String h = "<A HREF=\"javascript:document.location='http://www.google.com/'\">XSS</A>";
        String cleanHtml = Jsoup.clean(h, Safelist.relaxed());
        assertEquals("<a>XSS</a>", cleanHtml);
    }

    @Test public void testCleanAnchorProtocol() {
        String validAnchor = "<a href=\"#valid\">Valid anchor</a>";
        String invalidAnchor = "<a href=\"#anchor with spaces\">Invalid anchor</a>";

        // A Safelist that does not allow anchors will strip them out.
        String cleanHtml = Jsoup.clean(validAnchor, Safelist.relaxed());
        assertEquals("<a>Valid anchor</a>", cleanHtml);

        cleanHtml = Jsoup.clean(invalidAnchor, Safelist.relaxed());
        assertEquals("<a>Invalid anchor</a>", cleanHtml);

        // A Safelist that allows them will keep them.
        Safelist relaxedWithAnchor = Safelist.relaxed().addProtocols("a", "href", "#");

        cleanHtml = Jsoup.clean(validAnchor, relaxedWithAnchor);
        assertEquals(validAnchor, cleanHtml);

        // An invalid anchor is never valid.
        cleanHtml = Jsoup.clean(invalidAnchor, relaxedWithAnchor);
        assertEquals("<a>Invalid anchor</a>", cleanHtml);
    }

    @Test public void testDropsUnknownTags() {
        String h = "<p><custom foo=true>Test</custom></p>";
        String cleanHtml = Jsoup.clean(h, Safelist.relaxed());
        assertEquals("<p>Test</p>", cleanHtml);
    }

    @Test public void testHandlesEmptyAttributes() {
        String h = "<img alt=\"\" src= unknown=''>";
        String cleanHtml = Jsoup.clean(h, Safelist.basicWithImages());
        assertEquals("<img alt=\"\">", cleanHtml);
    }

    @Test public void testIsValidBodyHtml() {
        String ok = "<p>Test <b><a href='http://example.com/' rel='nofollow'>OK</a></b></p>";
        String ok1 = "<p>Test <b><a href='http://example.com/'>OK</a></b></p>"; // missing enforced is OK because still needs run thru cleaner
        String nok1 = "<p><script></script>Not <b>OK</b></p>";
        String nok2 = "<p align=right>Test Not <b>OK</b></p>";
        String nok3 = "<!-- comment --><p>Not OK</p>"; // comments and the like will be cleaned
        String nok4 = "<html><head>Foo</head><body><b>OK</b></body></html>"; // not body html
        String nok5 = "<p>Test <b><a href='http://example.com/' rel='nofollowme'>OK</a></b></p>";
        String nok6 = "<p>Test <b><a href='http://example.com/'>OK</b></p>"; // missing close tag
        String nok7 = "</div>What";
        assertTrue(Jsoup.isValid(ok, Safelist.basic()));
        assertTrue(Jsoup.isValid(ok1, Safelist.basic()));
        assertFalse(Jsoup.isValid(nok1, Safelist.basic()));
        assertFalse(Jsoup.isValid(nok2, Safelist.basic()));
        assertFalse(Jsoup.isValid(nok3, Safelist.basic()));
        assertFalse(Jsoup.isValid(nok4, Safelist.basic()));
        assertFalse(Jsoup.isValid(nok5, Safelist.basic()));
        assertFalse(Jsoup.isValid(nok6, Safelist.basic()));
        assertFalse(Jsoup.isValid(ok, Safelist.none()));
        assertFalse(Jsoup.isValid(nok7, Safelist.basic()));
    }

    @Test public void testIsValidDocument() {
        String ok = "<html><head></head><body><p>Hello</p></body><html>";
        String nok = "<html><head><script>woops</script><title>Hello</title></head><body><p>Hello</p></body><html>";

        Safelist relaxed = Safelist.relaxed();
        Cleaner cleaner = new Cleaner(relaxed);
        Document okDoc = Jsoup.parse(ok);
        assertTrue(cleaner.isValid(okDoc));
        assertFalse(cleaner.isValid(Jsoup.parse(nok)));
        assertFalse(new Cleaner(Safelist.none()).isValid(okDoc));
    }

    @Test public void resolvesRelativeLinks() {
        String html = "<a href='/foo'>Link</a><img src='/bar'>";
        String clean = Jsoup.clean(html, "http://example.com/", Safelist.basicWithImages());
        assertEquals("<a href=\"http://example.com/foo\">Link</a><img src=\"http://example.com/bar\">", clean);
    }

    @Test public void preservesRelativeLinksIfConfigured() {
        String html = "<a href='/foo'>Link</a><img src='/bar'> <img src='javascript:alert()'>";
        String clean = Jsoup.clean(html, "http://example.com/", Safelist.basicWithImages().preserveRelativeLinks(true));
        assertEquals("<a href=\"/foo\">Link</a><img src=\"/bar\"> <img>", clean);
    }

    @Test public void dropsUnresolvableRelativeLinks() { // when not preserving
        String html = "<a href='/foo'>Link</a>";
        String clean = Jsoup.clean(html, Safelist.basic());
        assertEquals("<a rel=\"nofollow\">Link</a>", clean);
    }

    @Test void dropsJavascriptWhenRelativeLinks() {
        String html ="<a href='javascript:alert()'>One</a>";
        Safelist safelist = Safelist.basic().preserveRelativeLinks(true);
        assertEquals("<a rel=\"nofollow\">One</a>", Jsoup.clean(html, safelist));
        assertFalse(Jsoup.isValid(html, safelist));
    }

    @Test void dropsConcealedJavascriptProtocolWhenRelativesLinksEnabled() {
        Safelist safelist = Safelist.basic().preserveRelativeLinks(true);
        String html = "<a href=\"&#0013;ja&Tab;va&Tab;script&#0010;:alert(1)\">Link</a>";
        String clean = Jsoup.clean(html, "https://", safelist);
        assertEquals("<a rel=\"nofollow\">Link</a>", clean);
        assertFalse(Jsoup.isValid(html, safelist));

        String colon = "<a href=\"ja&Tab;va&Tab;script&colon;alert(1)\">Link</a>";
        String cleanColon = Jsoup.clean(colon, "https://", safelist);
        assertEquals("<a rel=\"nofollow\">Link</a>", cleanColon);
        assertFalse(Jsoup.isValid(colon, safelist));
    }

    @Test void dropsConcealedJavascriptProtocolWhenRelativesLinksDisabled() {
        Safelist safelist = Safelist.basic().preserveRelativeLinks(false);
        String html = "<a href=\"ja&Tab;vas&#0013;cript:alert(1)\">Link</a>";
        String clean = Jsoup.clean(html, "https://", safelist);
        assertEquals("<a rel=\"nofollow\">Link</a>", clean);
        assertFalse(Jsoup.isValid(html, safelist));
    }

    @Test public void handlesCustomProtocols() {
        String html = "<img src='cid:12345' /> <img src='data:gzzt' />";
        String dropped = Jsoup.clean(html, Safelist.basicWithImages());
        assertEquals("<img> <img>", dropped);

        String preserved = Jsoup.clean(html, Safelist.basicWithImages().addProtocols("img", "src", "cid", "data"));
        assertEquals("<img src=\"cid:12345\"> <img src=\"data:gzzt\">", preserved);
    }

    @Test public void handlesAllPseudoTag() {
        String html = "<p class='foo' src='bar'><a class='qux'>link</a></p>";
        Safelist safelist = new Safelist()
                .addAttributes(":all", "class")
                .addAttributes("p", "style")
                .addTags("p", "a");

        String clean = Jsoup.clean(html, safelist);
        assertEquals("<p class=\"foo\"><a class=\"qux\">link</a></p>", clean);
    }

    @Test public void addsTagOnAttributesIfNotSet() {
        String html = "<p class='foo' src='bar'>One</p>";
        Safelist safelist = new Safelist()
            .addAttributes("p", "class");
        // ^^ safelist does not have explicit tag add for p, inferred from add attributes.
        String clean = Jsoup.clean(html, safelist);
        assertEquals("<p class=\"foo\">One</p>", clean);
    }

    @Test public void supplyOutputSettings() {
        // test that one can override the default document output settings
        Document.OutputSettings os = new Document.OutputSettings();
        os.prettyPrint(false);
        os.escapeMode(Entities.EscapeMode.extended);
        os.charset("ascii");

        String html = "<div><p>&bernou;</p></div>";
        String customOut = Jsoup.clean(html, "http://foo.com/", Safelist.relaxed(), os);
        String defaultOut = Jsoup.clean(html, "http://foo.com/", Safelist.relaxed());
        assertNotSame(defaultOut, customOut);

        assertEquals("<div><p>&Bscr;</p></div>", customOut); // entities now prefers shorted names if aliased
        assertEquals("<div>\n" +
            " <p>ℬ</p>\n" +
            "</div>", defaultOut);

        os.charset("ASCII");
        os.escapeMode(Entities.EscapeMode.base);
        String customOut2 = Jsoup.clean(html, "http://foo.com/", Safelist.relaxed(), os);
        assertEquals("<div><p>&#x212c;</p></div>", customOut2);
    }

    @Test public void handlesFramesets() {
        String dirty = "<html><head><script></script><noscript></noscript></head><frameset><frame src=\"foo\" /><frame src=\"foo\" /></frameset></html>";
        String clean = Jsoup.clean(dirty, Safelist.basic());
        assertEquals("", clean); // nothing good can come out of that

        Document dirtyDoc = Jsoup.parse(dirty);
        Document cleanDoc = new Cleaner(Safelist.basic()).clean(dirtyDoc);
        assertNotNull(cleanDoc);
        assertEquals(0, cleanDoc.body().childNodeSize());
    }

    @Test public void cleansInternationalText() {
        assertEquals("привет", Jsoup.clean("привет", Safelist.none()));
    }

    @Test
    public void testScriptTagInSafeList() {
        Safelist safelist = Safelist.relaxed();
        safelist.addTags( "script" );
        assertTrue( Jsoup.isValid("Hello<script>alert('Doh')</script>World !", safelist) );
    }

    @Test
    public void bailsIfRemovingProtocolThatsNotSet() {
        assertThrows(IllegalArgumentException.class, () -> {
            // a case that came up on the email list
            Safelist w = Safelist.none();

            // note no add tag, and removing protocol without adding first
            w.addAttributes("a", "href");
            w.removeProtocols("a", "href", "javascript"); // with no protocols enforced, this was a noop. Now validates.
        });
    }

    @Test public void handlesControlCharactersAfterTagName() {
        String html = "<a/\06>";
        String clean = Jsoup.clean(html, Safelist.basic());
        assertEquals("<a rel=\"nofollow\"></a>", clean);
    }

    @Test public void handlesAttributesWithNoValue() {
        // https://github.com/jhy/jsoup/issues/973
        String clean = Jsoup.clean("<a href>Clean</a>", Safelist.basic());

        assertEquals("<a rel=\"nofollow\">Clean</a>", clean);
    }

    @Test public void handlesNoHrefAttribute() {
        String dirty = "<a>One</a> <a href>Two</a>";
        Safelist relaxedWithAnchor = Safelist.relaxed().addProtocols("a", "href", "#");
        String clean = Jsoup.clean(dirty, relaxedWithAnchor);
        assertEquals("<a>One</a> <a>Two</a>", clean);
    }

    @Test public void handlesNestedQuotesInAttribute() {
        // https://github.com/jhy/jsoup/issues/1243 - no repro
        String orig = "<div style=\"font-family: 'Calibri'\">Will (not) fail</div>";
        Safelist allow = Safelist.relaxed()
            .addAttributes("div", "style");

        String clean = Jsoup.clean(orig, allow);
        boolean isValid = Jsoup.isValid(orig, allow);

        assertEquals(orig, TextUtil.stripNewlines(clean)); // only difference is pretty print wrap & indent
        assertTrue(isValid);
    }

    @Test public void copiesOutputSettings() {
        Document orig = Jsoup.parse("<p>test<br></p>");
        orig.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        orig.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
        Safelist safelist = Safelist.none().addTags("p", "br");

        Document result = new Cleaner(safelist).clean(orig);
        assertEquals(Document.OutputSettings.Syntax.xml, result.outputSettings().syntax());
        assertEquals("<p>test\n <br /></p>", result.body().html());
    }

    @Test void preservesSourcePositionViaUserData() {
        Document orig = Jsoup.parse("<script>xss</script>\n <p id=1>Hello</p>", Parser.htmlParser().setTrackPosition(true));
        Element p = orig.expectFirst("p");
        Range origRange = p.sourceRange();
        assertEquals("2,2:22-2,10:30", origRange.toString());
        assertEquals("1,1:0-1,1:0", orig.sourceRange().toString());
        assertEquals("2,19:39-2,19:39", orig.endSourceRange().toString());

        Range.AttributeRange attributeRange = p.attributes().sourceRange("id");
        assertEquals("2,5:25-2,7:27=2,8:28-2,9:29", attributeRange.toString());

        Document clean = new Cleaner(Safelist.relaxed().addAttributes("p", "id")).clean(orig);
        Element cleanP = clean.expectFirst("p");
        assertEquals("1", cleanP.id());
        Range cleanRange = cleanP.sourceRange();
        assertEquals(origRange, cleanRange);
        assertEquals(p.endSourceRange(), cleanP.endSourceRange());
        assertEquals(attributeRange, cleanP.attributes().sourceRange("id"));
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void cleansCaseSensitiveElements(boolean preserveCase) {
        // https://github.com/jhy/jsoup/issues/2049
        String html = "<svg><feMerge baseFrequency=2><feMergeNode kernelMatrix=1 /><feMergeNode><clipPath /></feMergeNode><feMergeNode />";
        String[] tags = {"svg", "feMerge", "feMergeNode", "clipPath"};
        String[] attrs = {"kernelMatrix", "baseFrequency"};

        if (!preserveCase) {
            tags = Arrays.stream(tags).map(String::toLowerCase).toArray(String[]::new);
            attrs = Arrays.stream(attrs).map(String::toLowerCase).toArray(String[]::new);
        }

        Safelist safelist = Safelist.none().addTags(tags).addAttributes(":all", attrs);
        String clean = Jsoup.clean(html, safelist);
        String expected = "<svg>\n" +
            " <feMerge baseFrequency=\"2\">\n" +
            "  <feMergeNode kernelMatrix=\"1\" />\n" +
            "  <feMergeNode>\n" +
            "   <clipPath />\n" +
            "  </feMergeNode>\n" +
            "  <feMergeNode />\n" +
            " </feMerge>\n" +
            "</svg>";
        assertEquals(expected, clean);
    }

    @Test void nofollowOnlyOnExternalLinks() {
        // We want to add nofollow to external links, but not to for relative links or those on the same site
        String html = "<a href='http://external.com/'>One</a> <a href='/relative/'>Two</a> <a href='../other/'>Three</a> <a href='http://example.com/bar'>Four</a>";

        Safelist basic = Safelist.basic().preserveRelativeLinks(true);
        String clean = Jsoup.clean(html, "http://example.com/", basic);
        assertEquals("<a href=\"http://external.com/\" rel=\"nofollow\">One</a> <a href=\"/relative/\">Two</a> <a href=\"../other/\">Three</a> <a href=\"http://example.com/bar\">Four</a>", clean);

        // If we don't pass in a base URI, still want to preserve the relative links.
        String clean2 = Jsoup.clean(html, basic);
        assertEquals("<a href=\"http://external.com/\" rel=\"nofollow\">One</a> <a href=\"/relative/\">Two</a> <a href=\"../other/\">Three</a> <a href=\"http://example.com/bar\" rel=\"nofollow\">Four</a>", clean2);
        // Four gets nofollowed because we didn't specify the base URI, so must assume it is external

        // Want it to be valid with relative links (and no base uri required / provided):
        assertTrue(Jsoup.isValid(html, basic));

        // test that it works in safelist.relaxed as well, which doesn't by default have rel=nofollow
        Safelist relaxed = Safelist.relaxed().preserveRelativeLinks(true).addEnforcedAttribute("a", "rel", "nofollow");
        String clean3 = Jsoup.clean(html, "http://example.com/", relaxed);
        assertEquals("<a href=\"http://external.com/\" rel=\"nofollow\">One</a> <a href=\"/relative/\">Two</a> <a href=\"../other/\">Three</a> <a href=\"http://example.com/bar\">Four</a>", clean3);
        assertTrue(Jsoup.isValid(html, relaxed));

        String clean4 = Jsoup.clean(html, relaxed);
        assertEquals("<a href=\"http://external.com/\" rel=\"nofollow\">One</a> <a href=\"/relative/\">Two</a> <a href=\"../other/\">Three</a> <a href=\"http://example.com/bar\" rel=\"nofollow\">Four</a>", clean4);
    }

    @Test void discardsSvgScriptData() {
        // https://github.com/jhy/jsoup/issues/2320
        Safelist svgOk = Safelist.none().addTags("svg");
        String cleaned = Jsoup.clean("<svg><script> a < b </script></svg>", svgOk);
        assertEquals("<svg></svg>", cleaned);
    }

    @Test void canSupplyConfiguredTagset() {
        // https://github.com/jhy/jsoup/issues/2326

        // by default, iframe is data
        String input = "<iframe>content is <data></iframe>";
        Safelist safelist = Safelist.relaxed().addTags("iframe");
        String clean = Jsoup.clean(input, safelist);
        assertEquals("<iframe>content is <data></iframe>", clean);

        Document doc = Jsoup.parse(input);
        assertEquals("", doc.text()); // data is not text

        // can change to text
        TagSet tags = TagSet.Html();
        Tag iframe = tags.valueOf("iframe", Parser.NamespaceHtml);
        iframe.clear(Tag.Data).set(Tag.RcData);
        Document doc2 = Jsoup.parse(input, Parser.htmlParser().tagSet(tags));
        assertEquals("content is <data>", doc2.text());
        assertEquals("<iframe>content is &lt;data&gt;</iframe>", doc2.body().html());

        // text nodes are escaped
        assertEquals("<iframe>content is &lt;data&gt;</iframe>", doc2.body().html());

        // can use cleaner with updated tagset
        Cleaner cleaner = new Cleaner(Safelist.relaxed());
        String clean2 = cleaner.clean(doc2).body().html();
        assertEquals("content is &lt;data&gt;", clean2);
    }
}
