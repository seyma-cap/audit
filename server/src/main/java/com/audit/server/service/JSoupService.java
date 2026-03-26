package com.audit.server.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * This class extracts elements from the DOM based on the successcriteria that needs examination, meaning eacht methode
 * belongs to one single criterium
 *
 * @author Seyma Kaya
 */
@Service
public class JSoupService {

    public void getData(String url){
        try {
            Document webPage = Jsoup.connect(url).get();
            System.out.println(webPage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks criterium 1.1.1
     */
    public Elements getAltText(String url){
        try {
            Document webPage = Jsoup.connect(url).get();

            // extracts every element with <img> tag
            Elements x = webPage.getElementsByTag("img");

            return x;
            // loop to check for alt text
//            for (Element el : x){
//                if (el.attr("alt").isEmpty()) {
//                    System.out.println(el.attr("src") + " is empty");
//                    System.out.println(el);
//                } else {
//                    System.out.println(el.attr("src") + " is had alt text " + el.attr("alt"));
//                }
//
//            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
