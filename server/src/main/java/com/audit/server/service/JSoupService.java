package com.audit.server.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * This class extracts elements from the DOM based on the success criteria that needs examination, meaning each methode
 * belongs to one single criterion
 *
 * @author Seyma Kaya
 */
@Service
public class JSoupService {

    public void getData(String url) {
        try {
            Document webPage = Jsoup.connect(url).get();
            System.out.println(webPage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks criterion 1.1.1
     */
    public Elements getAltText(String url) {
        try {
            Document webPage = Jsoup.connect(url).get();

            // extracts every element with <img> tag
            Elements x = webPage.getElementsByTag("img");

            return x;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *  Checks criterion 1.3.5
     */
    public String getLabelsAndInput(String url){
        try {
            Document webPage = Jsoup.connect(url).get();

            // extracts every element with <img> tag
            Elements x = webPage.getElementsByTag("label");
            Elements y = webPage.getElementsByTag("input");

            return x + y.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks criterion 1.4.2
     */
    public Elements getAudioElements(String url){
        try {
            Document webPage = Jsoup.connect(url).get();

            Elements x = webPage.getElementsByTag("audio");

            return x;
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks criterion 2.4.2
     */
    public Elements getTitle(String url){
        try {
            Document webPage = Jsoup.connect(url).get();

            Elements x = webPage.getElementsByTag("title");

            return x;
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks criterion 2.4.4
     */
    public Elements getLinks(String url){
        try {
            Document webPage = Jsoup.connect(url).get();

            Elements x = webPage.getElementsByTag("a");

            return x;
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks criterion 2.4.6
     */
    public Elements getLabelsHeading(String url){
        try {
            Document webPage = Jsoup.connect(url).get();

            Elements x = webPage.select("h1, h2, h3, h4, h5, h6, label");

            return x;
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks criterion 2.5.3
     */
    public Elements getLabels(String url){
        try {
            Document webPage = Jsoup.connect(url).get();

            Elements x = webPage.select("label");

            System.out.println(x);

            return x;
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks criterion 3.1.1
     */
    public boolean getLangElement(String url) {
        try {
            Document webPage = Jsoup.connect(url).get();

            // checks if HTML element has lang attribute
            boolean x = webPage.getElementsByTag("html").hasAttr("lang");
            System.out.println(x);
            return x;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks criterion 3.1.2
     */
    public String getAllLangElements(String url) {
        StringBuilder sb = new StringBuilder();
        try {
            Document webPage = Jsoup.connect(url).get();

            for (Element el : webPage.getElementsByAttribute("lang")) {
                sb.append(el.tagName())
                        .append(" lang=").append(el.attr("lang"));

                if (el.hasAttr("id")) sb.append(" id=").append(el.attr("id"));
                if (el.hasAttr("class")) sb.append(" class=").append(el.attr("class"));

                // Small text preview only if it's an inline element (span, em, etc.)
                if (!el.tagName().equals("html") && !el.tagName().equals("body")) {
                    String preview = el.text();
                    if (preview.length() > 80) preview = preview.substring(0, 80) + "...";
                    sb.append(" text=\"").append(preview).append("\"");
                }

                sb.append("\n");
            }

            System.out.println(sb);

            return sb.toString();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks criterion 3.3.1
     */
    public Elements getFormElements(String url) {
        try {
            System.out.println(url);
            Document webPage = Jsoup.connect(url).get();

            // extracts every element with <img> tag
            Elements x = webPage.getElementsByTag("form");
            System.out.println(x);

            return x;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks criterion 4.1.2
     */
    public String getCustomElements(String url) {
        try {
            Document webPage = Jsoup.connect(url).get();
            StringBuilder sb = new StringBuilder();

            // Elements with role attribute
            Elements roleElements = webPage.select("[role]");
            for (Element el : roleElements) {
                sb.append(el.tagName())
                        .append(" role=").append(el.attr("role"));
                if (el.hasAttr("aria-label")) sb.append(" aria-label=").append(el.attr("aria-label"));
                if (el.hasAttr("aria-labelledby")) sb.append(" aria-labelledby=").append(el.attr("aria-labelledby"));
                if (el.hasAttr("aria-describedby")) sb.append(" aria-describedby=").append(el.attr("aria-describedby"));
                if (el.hasAttr("aria-hidden")) sb.append(" aria-hidden=").append(el.attr("aria-hidden"));
                if (el.hasAttr("tabindex")) sb.append(" tabindex=").append(el.attr("tabindex"));
                sb.append("\n");
            }

            // Custom web components (tag names containing a dash)
            webPage.getAllElements().stream()
                    .filter(el -> el.tagName().contains("-"))
                    .forEach(el -> {
                        sb.append("custom-element: ").append(el.tagName());
                        el.attributes().forEach(attr ->
                                sb.append(" ").append(attr.getKey()).append("=").append(attr.getValue())
                        );
                        sb.append("\n");
                    });

            // Interactive elements missing accessible name
            Elements interactive = webPage.select("button, a, input, select, textarea");
            for (Element el : interactive) {
                boolean hasName = el.hasAttr("aria-label")
                        || el.hasAttr("aria-labelledby")
                        || el.hasAttr("title")
                        || !el.text().isBlank()
                        || el.hasAttr("alt");
                if (!hasName) {
                    sb.append("MISSING NAME: ").append(el.tagName());
                    if (el.hasAttr("id")) sb.append(" id=").append(el.attr("id"));
                    if (el.hasAttr("type")) sb.append(" type=").append(el.attr("type"));
                    sb.append("\n");
                }
            }

            System.out.println(sb);

            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}