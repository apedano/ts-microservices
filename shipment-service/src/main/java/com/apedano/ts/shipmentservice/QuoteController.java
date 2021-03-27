package com.apedano.ts.shipmentservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quote")
public class QuoteController {


    @Autowired
    private QuoteService quoteService;

    @GetMapping
    public String getQuote(@RequestParam(required = false) String languageCode) {
        return quoteService.getRandomQuote(languageCode);
    }
}
