package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.TextUtil;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 Tests for ElementList.

 @author Jonathan Hedley, jonathan@hedley.net */
public class ElementsTest {
    @Test public void filter() {
        String h = "<p>Excl</p><div class=headline><p>Hello</p><p>There</p></div><div class=headline><h1>Headline</h1></div>";
        Document doc = Jsoup.parse(h);
        Elements els = doc.select(".headline").select("p");
        assertEquals(2, els.size());
        assertEquals("Hello", els.get(0).text());
        assertEquals("There", els.get(1).text());
    }

    @Test public void attributes() {
        String h = "<p title=foo><p title=bar><p class=foo><p class=bar>";
        Document doc = Jsoup.parse(h);
        Elements withTitle = doc.select("p[title]");
        assertEquals(2, withTitle.size());
        assertTrue(withTitle.hasAttr("title"));
        assertFalse(withTitle.hasAttr("class"));
        assertEquals("foo", withTitle.attr("title"));

        withTitle.removeAttr("title");
        assertEquals(2, withTitle.size()); // existing Elements are not reevaluated
        assertEquals(0, doc.select("p[title]").size());

        Elements ps = doc.select("p").attr("style", "classy");
        assertEquals(4, ps.size());
        assertEquals("classy", ps.last().attr("style"));
        assertEquals("bar", ps.last().attr("class"));
    }

    @Test public void hasAttr() {
        Document doc = Jsoup.parse("<p title=foo><p title=bar><p class=foo><p class=bar>");
        Elements ps = doc.select("p");
        assertTrue(ps.hasAttr("class"));
        assertFalse(ps.hasAttr("style"));
    }

    @Test public void hasAbsAttr() {
        Document doc = Jsoup.parse("<a id=1 href='/foo'>One</a> <a id=2 href='https://jsoup.org'>Two</a>");
        Elements one = doc.select("#1");
        Elements two = doc.select("#2");
        Elements both = doc.select("a");
        assertFalse(one.hasAttr("abs:href"));
        assertTrue(two.hasAttr("abs:href"));
        assertTrue(both.hasAttr("abs:href")); // hits on #2
    }

    @Test public void attr() {
        Document doc = Jsoup.parse("<p title=foo><p title=bar><p class=foo><p class=bar>");
        String classVal = doc.select("p").attr("class");
        assertEquals("foo", classVal);
    }

    @Test public void absAttr() {
        Document doc = Jsoup.parse("<a id=1 href='/foo'>One</a> <a id=2 href='https://jsoup.org'>Two</a>");
        Elements one = doc.select("#1");
        Elements two = doc.select("#2");
        Elements both = doc.select("a");

        assertEquals("", one.attr("abs:href"));
        assertEquals("https://jsoup.org", two.attr("abs:href"));
        assertEquals("https://jsoup.org", both.attr("abs:href"));
    }

    @Test public void classes() {
        Document doc = Jsoup.parse("<div><p class='mellow yellow'></p><p class='red green'></p>");

        Elements els = doc.select("p");
        assertTrue(els.hasClass("red"));
        assertFalse(els.hasClass("blue"));
        els.addClass("blue");
        els.removeClass("yellow");
        els.toggleClass("mellow");

        assertEquals("blue", els.get(0).className());
        assertEquals("red green blue mellow", els.get(1).className());
    }

    @Test public void hasClassCaseInsensitive() {
        Elements els = Jsoup.parse("<p Class=One>One <p class=Two>Two <p CLASS=THREE>THREE").select("p");
        Element one = els.get(0);
        Element two = els.get(1);
        Element thr = els.get(2);

        assertTrue(one.hasClass("One"));
        assertTrue(one.hasClass("ONE"));

        assertTrue(two.hasClass("TWO"));
        assertTrue(two.hasClass("Two"));

        assertTrue(thr.hasClass("ThreE"));
        assertTrue(thr.hasClass("three"));
    }

    @Test public void text() {
        String h = "<div><p>Hello<p>there<p>world</div>";
        Document doc = Jsoup.parse(h);
        assertEquals("Hello there world", doc.select("div > *").text());
    }

    @Test public void hasText() {
        Document doc = Jsoup.parse("<div><p>Hello</p></div><div><p></p></div>");
        Elements divs = doc.select("div");
        assertTrue(divs.hasText());
        assertFalse(doc.select("div + div").hasText());
    }

    @Test public void html() {
        Document doc = Jsoup.parse("<div><p>Hello</p></div><div><p>There</p></div>");
        Elements divs = doc.select("div");
        assertEquals("<p>Hello</p>\n<p>There</p>", divs.html());
    }

    @Test public void outerHtml() {
        Document doc = Jsoup.parse("<div><p>Hello</p></div><div><p>There</p></div>");
        Elements divs = doc.select("div");
        assertEquals("<div><p>Hello</p></div><div><p>There</p></div>", TextUtil.stripNewlines(divs.outerHtml()));
    }

    @Test public void setHtml() {
        Document doc = Jsoup.parse("<p>One</p><p>Two</p><p>Three</p>");
        Elements ps = doc.select("p");

        ps.prepend("<b>Bold</b>").append("<i>Ital</i>");
        assertEquals("<p><b>Bold</b>Two<i>Ital</i></p>", TextUtil.stripNewlines(ps.get(1).outerHtml()));

        ps.html("<span>Gone</span>");
        assertEquals("<p><span>Gone</span></p>", TextUtil.stripNewlines(ps.get(1).outerHtml()));
    }

    @Test public void val() {
        Document doc = Jsoup.parse("<input value='one' /><textarea>two</textarea>");
        Elements els = doc.select("input, textarea");
        assertEquals(2, els.size());
        assertEquals("one", els.val());
        assertEquals("two", els.last().val());

        els.val("three");
        assertEquals("three", els.first().val());
        assertEquals("three", els.last().val());
        assertEquals("<textarea>three</textarea>", els.last().outerHtml());
    }

    @Test public void before() {
        Document doc = Jsoup.parse("<p>This <a>is</a> <a>jsoup</a>.</p>");
        doc.select("a").before("<span>foo</span>");
        assertEquals("<p>This <span>foo</span><a>is</a> <span>foo</span><a>jsoup</a>.</p>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void after() {
        Document doc = Jsoup.parse("<p>This <a>is</a> <a>jsoup</a>.</p>");
        doc.select("a").after("<span>foo</span>");
        assertEquals("<p>This <a>is</a><span>foo</span> <a>jsoup</a><span>foo</span>.</p>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void wrap() {
        String h = "<p><b>This</b> is <b>jsoup</b></p>";
        Document doc = Jsoup.parse(h);
        doc.select("b").wrap("<i></i>");
        assertEquals("<p><i><b>This</b></i> is <i><b>jsoup</b></i></p>", doc.body().html());
    }

    @Test public void wrapDiv() {
        String h = "<p><b>This</b> is <b>jsoup</b>.</p> <p>How do you like it?</p>";
        Document doc = Jsoup.parse(h);
        doc.select("p").wrap("<div></div>");
        assertEquals(
            "<div>\n <p><b>This</b> is <b>jsoup</b>.</p>\n</div>\n<div>\n <p>How do you like it?</p>\n</div>",
            doc.body().html());
    }

    @Test public void unwrap() {
        String h = "<div><font>One</font> <font><a href=\"/\">Two</a></font></div";
        Document doc = Jsoup.parse(h);
        doc.select("font").unwrap();
        assertEquals("<div>\n" +
            " One <a href=\"/\">Two</a>\n" +
            "</div>", doc.body().html());
    }

    @Test public void unwrapP() {
        String h = "<p><a>One</a> Two</p> Three <i>Four</i> <p>Fix <i>Six</i></p>";
        Document doc = Jsoup.parse(h);
        doc.select("p").unwrap();
        assertEquals("<a>One</a> Two Three <i>Four</i> Fix <i>Six</i>", TextUtil.stripNewlines(doc.body().html()));
    }

    @Test public void unwrapKeepsSpace() {
        String h = "<p>One <span>two</span> <span>three</span> four</p>";
        Document doc = Jsoup.parse(h);
        doc.select("span").unwrap();
        assertEquals("<p>One two three four</p>", doc.body().html());
    }

    @Test public void empty() {
        Document doc = Jsoup.parse("<div><p>Hello <b>there</b></p> <p>now!</p></div>");
        doc.outputSettings().prettyPrint(false);

        doc.select("p").empty();
        assertEquals("<div><p></p> <p></p></div>", doc.body().html());
    }

    @Test public void remove() {
        Document doc = Jsoup.parse("<div><p>Hello <b>there</b></p> jsoup <p>now!</p></div>");
        doc.outputSettings().prettyPrint(false);

        doc.select("p").remove();
        assertEquals("<div> jsoup </div>", doc.body().html());
    }

    @Test public void eq() {
        String h = "<p>Hello<p>there<p>world";
        Document doc = Jsoup.parse(h);
        assertEquals("there", doc.select("p").eq(1).text());
        assertEquals("there", doc.select("p").get(1).text());
    }

    @Test public void is() {
        String h = "<p>Hello<p title=foo>there<p>world";
        Document doc = Jsoup.parse(h);
        Elements ps = doc.select("p");
        assertTrue(ps.is("[title=foo]"));
        assertFalse(ps.is("[title=bar]"));
    }

    @Test public void parents() {
        Document doc = Jsoup.parse("<div><p>Hello</p></div><p>There</p>");
        Elements parents = doc.select("p").parents();

        assertEquals(3, parents.size());
        assertEquals("div", parents.get(0).tagName());
        assertEquals("body", parents.get(1).tagName());
        assertEquals("html", parents.get(2).tagName());
    }

    @Test public void not() {
        Document doc = Jsoup.parse("<div id=1><p>One</p></div> <div id=2><p><span>Two</span></p></div>");

        Elements div1 = doc.select("div").not(":has(p > span)");
        assertEquals(1, div1.size());
        assertEquals("1", div1.first().id());

        Elements div2 = doc.select("div").not("#1");
        assertEquals(1, div2.size());
        assertEquals("2", div2.first().id());
    }

    @Test public void tagNameSet() {
        Document doc = Jsoup.parse("<p>Hello <i>there</i> <i>now</i></p>");
        doc.select("i").tagName("em");

        assertEquals("<p>Hello <em>there</em> <em>now</em></p>", doc.body().html());
    }

    @Test public void traverse() {
        Document doc = Jsoup.parse("<div><p>Hello</p></div><div>There</div>");
        final StringBuilder accum = new StringBuilder();
        doc.select("div").traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                accum.append("<").append(node.nodeName()).append(">");
            }

            @Override
            public void tail(Node node, int depth) {
                accum.append("</").append(node.nodeName()).append(">");
            }
        });
        assertEquals("<div><p><#text></#text></p></div><div><#text></#text></div>", accum.toString());
    }

    @Test public void forms() {
        Document doc = Jsoup.parse("<form id=1><input name=q></form><div /><form id=2><input name=f></form>");
        Elements els = doc.select("form, div");
        assertEquals(3, els.size());

        List<FormElement> forms = els.forms();
        assertEquals(2, forms.size());
        assertNotNull(forms.get(0));
        assertNotNull(forms.get(1));
        assertEquals("1", forms.get(0).id());
        assertEquals("2", forms.get(1).id());
    }

    @Test public void comments() {
        Document doc = Jsoup.parse("<!-- comment1 --><p><!-- comment2 --><p class=two><!-- comment3 -->");
        List<Comment> comments = doc.select("p").comments();
        assertEquals(2, comments.size());
        assertEquals(" comment2 ", comments.get(0).getData());
        assertEquals(" comment3 ", comments.get(1).getData());

        List<Comment> comments1 = doc.select("p.two").comments();
        assertEquals(1, comments1.size());
        assertEquals(" comment3 ", comments1.get(0).getData());
    }

    @Test public void textNodes() {
        Document doc = Jsoup.parse("One<p>Two<a>Three</a><p>Four</p>Five");
        List<TextNode> textNodes = doc.select("p").textNodes();
        assertEquals(2, textNodes.size());
        assertEquals("Two", textNodes.get(0).text());
        assertEquals("Four", textNodes.get(1).text());
    }

    @Test public void dataNodes() {
        Document doc = Jsoup.parse("<p>One</p><script>Two</script><style>Three</style>");
        List<DataNode> dataNodes = doc.select("p, script, style").dataNodes();
        assertEquals(2, dataNodes.size());
        assertEquals("Two", dataNodes.get(0).getWholeData());
        assertEquals("Three", dataNodes.get(1).getWholeData());

        doc = Jsoup.parse("<head><script type=application/json><crux></script><script src=foo>Blah</script>");
        Elements script = doc.select("script[type=application/json]");
        List<DataNode> scriptNode = script.dataNodes();
        assertEquals(1, scriptNode.size());
        DataNode dataNode = scriptNode.get(0);
        assertEquals("<crux>", dataNode.getWholeData());

        // check if they're live
        dataNode.setWholeData("<cromulent>");
        assertEquals("<script type=\"application/json\"><cromulent></script>", script.outerHtml());
    }

    @Test public void nodesEmpty() {
        Document doc = Jsoup.parse("<p>");
        assertEquals(0, doc.select("form").textNodes().size());
    }

    @Test public void classWithHyphen() {
        Document doc = Jsoup.parse("<p class='tab-nav'>Check</p>");
        Elements els = doc.getElementsByClass("tab-nav");
        assertEquals(1, els.size());
        assertEquals("Check", els.text());
    }

    @Test public void siblings() {
        Document doc = Jsoup.parse("<div><p>1<p>2<p>3<p>4<p>5<p>6</div><div><p>7<p>8<p>9<p>10<p>11<p>12</div>");

        Elements els = doc.select("p:eq(3)"); // gets p4 and p10
        assertEquals(2, els.size());

        Elements next = els.next();
        assertEquals(2, next.size());
        assertEquals("5", next.first().text());
        assertEquals("11", next.last().text());

        assertEquals(0, els.next("p:contains(6)").size());
        final Elements nextF = els.next("p:contains(5)");
        assertEquals(1, nextF.size());
        assertEquals("5", nextF.first().text());

        Elements nextA = els.nextAll();
        assertEquals(4, nextA.size());
        assertEquals("5", nextA.first().text());
        assertEquals("12", nextA.last().text());

        Elements nextAF = els.nextAll("p:contains(6)");
        assertEquals(1, nextAF.size());
        assertEquals("6", nextAF.first().text());

        Elements prev = els.prev();
        assertEquals(2, prev.size());
        assertEquals("3", prev.first().text());
        assertEquals("9", prev.last().text());

        assertEquals(0, els.prev("p:contains(1)").size());
        final Elements prevF = els.prev("p:contains(3)");
        assertEquals(1, prevF.size());
        assertEquals("3", prevF.first().text());

        Elements prevA = els.prevAll();
        assertEquals(6, prevA.size());
        assertEquals("3", prevA.first().text());
        assertEquals("7", prevA.last().text());

        Elements prevAF = els.prevAll("p:contains(1)");
        assertEquals(1, prevAF.size());
        assertEquals("1", prevAF.first().text());
    }

    @Test public void eachText() {
        Document doc = Jsoup.parse("<div><p>1<p>2<p>3<p>4<p>5<p>6</div><div><p>7<p>8<p>9<p>10<p>11<p>12<p></p></div>");
        List<String> divText = doc.select("div").eachText();
        assertEquals(2, divText.size());
        assertEquals("1 2 3 4 5 6", divText.get(0));
        assertEquals("7 8 9 10 11 12", divText.get(1));

        List<String> pText = doc.select("p").eachText();
        Elements ps = doc.select("p");
        assertEquals(13, ps.size());
        assertEquals(12, pText.size()); // not 13, as last doesn't have text
        assertEquals("1", pText.get(0));
        assertEquals("2", pText.get(1));
        assertEquals("5", pText.get(4));
        assertEquals("7", pText.get(6));
        assertEquals("12", pText.get(11));
    }

    @Test public void eachAttr() {
        Document doc = Jsoup.parse(
            "<div><a href='/foo'>1</a><a href='http://example.com/bar'>2</a><a href=''>3</a><a>4</a>",
            "http://example.com");

        List<String> hrefAttrs = doc.select("a").eachAttr("href");
        assertEquals(3, hrefAttrs.size());
        assertEquals("/foo", hrefAttrs.get(0));
        assertEquals("http://example.com/bar", hrefAttrs.get(1));
        assertEquals("", hrefAttrs.get(2));
        assertEquals(4, doc.select("a").size());

        List<String> absAttrs = doc.select("a").eachAttr("abs:href");
        assertEquals(3, absAttrs.size());
        assertEquals(3, absAttrs.size());
        assertEquals("http://example.com/foo", absAttrs.get(0));
        assertEquals("http://example.com/bar", absAttrs.get(1));
        assertEquals("http://example.com", absAttrs.get(2));
    }

    @Test public void setElementByIndex() {
        Document doc = Jsoup.parse("<p>One<p>Two<p>Three");
        Element newP = doc.createElement("p").text("New").attr("id", "new");

        Elements ps = doc.select("p");
        Element two = ps.get(1);
        Element old = ps.set(1, newP);
        assertSame(old, two);
        assertSame(newP, ps.get(1)); // replaced in list
        assertEquals("<p>One</p>\n<p id=\"new\">New</p>\n<p>Three</p>", doc.body().html()); // replaced in dom
    }

    @Test public void removeElementByIndex() {
        Document doc = Jsoup.parse("<p>One<p>Two<p>Three");

        Elements ps = doc.select("p");
        Element two = ps.get(1);
        assertTrue(ps.contains(two));
        Element old = ps.remove(1);
        assertSame(old, two);

        assertEquals(2, ps.size()); // removed from list
        assertFalse(ps.contains(old));
        assertEquals("<p>One</p>\n<p>Three</p>", doc.body().html()); // removed from dom
    }

    @Test public void removeElementByObject() {
        Document doc = Jsoup.parse("<p>One<p>Two<p>Three");

        Elements ps = doc.select("p");
        Element two = ps.get(1);
        assertTrue(ps.contains(two));
        boolean removed = ps.remove(two);
        assertTrue(removed);

        assertEquals(2, ps.size()); // removed from list
        assertFalse(ps.contains(two));
        assertEquals("<p>One</p>\n<p>Three</p>", doc.body().html()); // removed from dom
    }

    @Test public void removeElementObjectNoops() {
        Document doc = Jsoup.parse("<p>One<p>Two<p>Three");
        String origHtml = doc.html();
        Element newP = doc.createElement("p").text("New");

        Elements ps = doc.select("p");
        int size = ps.size();
        assertFalse(ps.remove(newP));
        assertFalse(ps.remove(newP.childNodes()));
        assertEquals(origHtml, doc.html());
        assertEquals(size, ps.size());
    }

    @Test public void clear() {
        Document doc = Jsoup.parse("<p>One</p><p>Two</p><div>Three</div>");
        Elements ps = doc.select("p");
        assertEquals(2, ps.size());
        ps.clear();
        assertEquals(0, ps.size());

        assertEquals(0, doc.select("p").size());
    }

    @Test public void removeAll() {
        Document doc = Jsoup.parse("<p>One<p>Two<p>Three<p>Four</p><div>Div");
        Elements ps = doc.select("p");
        assertEquals(4, ps.size());
        Elements midPs = doc.select("p:gt(0):lt(3)"); //Two and Three
        assertEquals(2, midPs.size());

        boolean removed = ps.removeAll(midPs);
        assertEquals(2, ps.size());
        assertTrue(removed);
        assertEquals(2, midPs.size());

        Elements divs = doc.select("div");
        assertEquals(1, divs.size());
        assertFalse(ps.removeAll(divs));
        assertEquals(2, ps.size());

        assertEquals("<p>One</p>\n<p>Four</p>\n<div>Div</div>", doc.body().html());
    }

    @Test public void retainAll() {
        Document doc = Jsoup.parse("<p>One<p>Two<p>Three<p>Four</p><div>Div");
        Elements ps = doc.select("p");
        assertEquals(4, ps.size());
        Elements midPs = doc.select("p:gt(0):lt(3)"); //Two and Three
        assertEquals(2, midPs.size());

        boolean removed = ps.retainAll(midPs);
        assertEquals(2, ps.size());
        assertTrue(removed);
        assertEquals(2, midPs.size());

        assertEquals("<p>Two</p>\n<p>Three</p>\n<div>Div</div>", doc.body().html());

        Elements psAgain = doc.select("p");
        assertFalse(midPs.retainAll(psAgain));

        assertEquals("<p>Two</p>\n<p>Three</p>\n<div>Div</div>", doc.body().html());
    }

    @Test public void iteratorRemovesFromDom() {
        Document doc = Jsoup.parse("<p>One<p>Two<p>Three<p>Four");
        Elements ps = doc.select("p");

        assertEquals(4, ps.size());
        for (Iterator<Element> it = ps.iterator(); it.hasNext(); ) {
            Element el = it.next();
            if (el.text().contains("Two"))
                it.remove();
        }
        assertEquals(3, ps.size());
        assertEquals("<p>One</p>\n<p>Three</p>\n<p>Four</p>", doc.body().html());
    }

    @Test public void removeIf() {
        Document doc = Jsoup.parse("<p>One<p>Two<p>Three<p>Four");
        Elements ps = doc.select("p");

        assertEquals(4, ps.size());
        boolean removed = ps.removeIf(el -> el.text().contains("Two"));
        assertTrue(removed);
        assertEquals(3, ps.size());
        assertEquals("<p>One</p>\n<p>Three</p>\n<p>Four</p>", doc.body().html());

        assertFalse(ps.removeIf(el -> el.text().contains("Five")));
        assertEquals("<p>One</p>\n<p>Three</p>\n<p>Four</p>", doc.body().html());
    }

    @Test public void removeIfSupportsConcurrentRead() {
        Document doc = Jsoup.parse("<p>One<p>Two<p>Three<p>Four");
        Elements ps = doc.select("p");
        assertEquals(4, ps.size());

        boolean removed = ps.removeIf(el -> ps.contains(el));
        assertTrue(removed);
        assertEquals(0, ps.size());
        assertEquals("", doc.body().html());
    }

    @Test public void replaceAll() {
        Document doc = Jsoup.parse("<p>One<p>Two<p>Three<p>Four");
        Elements ps = doc.select("p");
        assertEquals(4, ps.size());

        ps.replaceAll(el -> {
            Element div = doc.createElement("div");
            div.text(el.text());
            return div;
        });

        // Check Elements
        for (Element p : ps) {
            assertEquals("div", p.tagName());
        }

        // check dom
        assertEquals("<div>One</div><div>Two</div><div>Three</div><div>Four</div>", TextUtil.normalizeSpaces(doc.body().html()));
    }

    @Test void selectFirst() {
        Document doc = Jsoup.parse("<p>One</p><p>Two <span>Jsoup</span></p><p><span>Three</span></p>");
        Element span = doc.children().selectFirst("span");
        assertNotNull(span);
        assertEquals("Jsoup", span.text());
    }

    @Test void selectFirstNullOnNoMatch() {
        Document doc = Jsoup.parse("<p>One</p><p>Two</p><p>Three</p>");
        Element span = doc.children().selectFirst("span");
        assertNull(span);
    }

    @Test void expectFirst() {
        Document doc = Jsoup.parse("<p>One</p><p>Two <span>Jsoup</span></p><p><span>Three</span></p>");
        Element span = doc.children().expectFirst("span");
        assertNotNull(span);
        assertEquals("Jsoup", span.text());
    }

    @Test void expectFirstThrowsOnNoMatch() {
        Document doc = Jsoup.parse("<p>One</p><p>Two</p><p>Three</p>");

        boolean threw = false;
        try {
            Element span = doc.children().expectFirst("span");
        } catch (IllegalArgumentException e) {
            threw = true;
            assertEquals("No elements matched the query 'span' in the elements.", e.getMessage());
        }
        assertTrue(threw);
    }

    @Test void selectFirstFromPreviousSelect() {
        Document doc = Jsoup.parse("<div><p>One</p></div><div><p><span>Two</span></p></div><div><p><span>Three</span></p></div>");
        Elements divs = doc.select("div");
        assertEquals(3, divs.size());

        Element span = divs.selectFirst("p span");
        assertNotNull(span);
        assertEquals("Two", span.text());

        // test roots
        assertNotNull(span.selectFirst("span")); // reselect self
        assertNull(span.selectFirst(">span")); // no span>span

        assertNotNull(divs.selectFirst("div")); // reselect self, similar to element.select
        assertNull(divs.selectFirst(">div")); // no div>div
    }
}
