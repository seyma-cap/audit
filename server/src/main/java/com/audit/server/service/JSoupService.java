package com.audit.server.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class JSoupService {

    public void getData(){
        try {
            Document webPage = Jsoup.connect("https://en.wikipedia.org/wiki/STAYC").get();
            System.out.println(webPage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
