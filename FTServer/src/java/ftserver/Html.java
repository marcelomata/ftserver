package ftserver;

import java.util.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class Html {

    public static Page get(String url, HashSet<String> subUrls) {
        try {
            if (url == null || url.length() > Page.MAX_URL_LENGTH || url.length() < 8) {
                return null;
            }
            Document doc = Jsoup.connect(url).timeout(15 * 1000).get();
            if (!doc.hasText()) {
                return null;
            }

            fixSpan(doc);

            Page page = new Page();
            page.url = url;
            //page.html = doc.html();
            page.text = replace(doc.body().text());

            if (page.text.length() < 10) {
                return null;
            }

            if (subUrls != null) {
                String host = getHost(url);
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String ss = link.attr("abs:href");
                    if (ss != null && ss.length() > 8) {
                        ss = getUrl(ss);
                        String h = getHost(ss);
                        if (host.equals(h)) {
                            subUrls.add(ss);
                        }
                    }
                }
            }

            String title = null;
            String keywords = null;
            String description = null;

            try {
                title = doc.title();
            } catch (Throwable e) {

            }
            if (title == null) {
                title = "";
            }
            if (title.length() < 1) {
                title = url;
            }
            title = replace(title);
            if (title.length() > 100) {
                title = title.substring(0, 100);
            }

            keywords = getMetaContentByName(doc, "keywords");
            for (char c : splitWords.toCharArray()) {
                keywords = keywords.replaceAll("\\" + c, " ");
            }
            if (keywords.length() > 50) {
                keywords = keywords.substring(0, 50);
            }

            description = getMetaContentByName(doc, "description");
            if (description.length() > 300) {
                description = description.substring(0, 300);
            }

            page.title = title;
            page.keywords = keywords;
            page.description = description;

            return page;
        } catch (Throwable e) {
            //e.printStackTrace();
            return null;
        }
    }

    private static String getMetaContentByName(Document doc, String name) {
        String description = null;
        try {
            description = doc.selectFirst("meta[name='" + name + "']").attr("content");
        } catch (Throwable e) {

        }

        try {
            if (description == null) {
                description = doc.selectFirst("meta[property='og:" + name + "']").attr("content");
            }
        } catch (Throwable e) {

        }

        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        try {
            if (description == null) {
                description = doc.selectFirst("meta[name='" + name + "']").attr("content");
            }
        } catch (Throwable e) {

        }

        if (description == null) {
            description = "";
        }

        return replace(description);
    }

    static String splitWords = " ,.　，。";

    public static ArrayList<PageText> getDefaultTexts(Page page) {
        if (page.textOrder < 1) {
            //no id;
            return null;
        }

        ArrayList<PageText> result = new ArrayList<PageText>();

        String title = page.title;
        String keywords = page.keywords;

        String url = page.url;
        long textOrder = page.textOrder;

        PageText description = new PageText();

        description.textOrder = textOrder;
        description.url = url;
        description.title = title;
        description.keywords = keywords;

        description.text = page.description;

        description.priority = PageText.descriptionPriority;
        if (page.isKeyPage) {
            description.priority = PageText.descriptionKeyPriority;
        }
        result.add(description);

        String content = page.text.trim() + "..";
        int maxLength = PageText.max_text_length;

        int wordCount = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c < 256) {
                wordCount++;
            } else {
                boolean isword = IndexAPI.ENGINE.sUtil.isWord(c);
                if (isword) {
                    wordCount++;
                }
            }
        }
        if (((double) wordCount / (double) content.length()) > 0.8) {
            maxLength *= 4;
        }

        long startPriority = PageText.descriptionPriority - 1;
        while (startPriority > 0 && content.length() > 0) {

            PageText text = new PageText();
            text.textOrder = textOrder;
            text.url = url;
            text.title = title;
            text.keywords = "";

            text.text = null;
            StringBuilder texttext = new StringBuilder(maxLength + 100);

            int last = Math.min(maxLength, content.length() - 1);
            int p1 = 0;
            for (char c : splitWords.toCharArray()) {
                int t = content.lastIndexOf(c, last);
                if (t >= 0) {
                    p1 = Math.max(p1, t);
                }
            }
            if (p1 == 0) {
                p1 = last;
            }

            texttext.append(content.substring(0, p1 + 1));

            content = content.substring(p1 + 1);

            if (content.length() > 0 && content.length() < 100) {
                texttext.append(" ").append(content);
                content = "";
            }

            text.text = texttext.toString();
            text.priority = startPriority;
            result.add(text);
            startPriority--;
        }

        return result;

    }

    private static String replace(String content) {
        return content.replaceAll(Character.toString((char) 8203), "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&gt;", " ")
                .replaceAll("&lt;", " ")
                .replaceAll("\t|\r|\n|�|<|>|�|\\$|\\|", " ")
                .replaceAll("　", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String getHost(String url) {
        url = getUrl(url);
        int p = url.indexOf("//");
        if (p > 0) {
            url = url.substring(p + 2);

            p = url.indexOf("/");
            if (p > 0) {
                url = url.substring(0, p);
            }
            return url;
        }
        return url;
    }

    public static String getUrl(String name) {

        int p = name.indexOf("http://");
        if (p < 0) {
            p = name.indexOf("https://");
        }
        if (p >= 0) {
            name = name.substring(p).trim();
            int t = name.indexOf("#");
            if (t > 0) {
                name = name.substring(0, t);
            }
            t = name.indexOf(" ");
            if (t > 0) {
                name = name.substring(0, t);
            }
            return name;
        }
        return "";
    }

    private static void fixSpan(Document doc) {
        for (Element e : doc.getElementsByTag("span")) {
            e.text(" " + e.text() + " ");
        }
    }

}
