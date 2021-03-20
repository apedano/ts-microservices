package com.apedano.ts.shipmentservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class HelloController {

    @Value("${hello.message}")
    private String message;

    @GetMapping
    public String sayHello() {
        return message;
    }
}