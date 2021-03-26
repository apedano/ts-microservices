package com.apedano.ts.shipmentservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class HelloController {

    @Value("${HELLO_MESSAGE}")
    private String message;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${welcome:default_value}")
    private String welcome;

    @GetMapping
    public OutputMessage sayHello() {
        return new OutputMessage(appName, message, welcome);
    }

    private class OutputMessage {
        String applicationName;
        String helloMessage;
        String welcome;

        public OutputMessage(String applicationName, String helloMessage, String welcome) {
            this.applicationName = applicationName;
            this.helloMessage = helloMessage;
            this.welcome = welcome;
        }

        public String getApplicationName() {
            return applicationName;
        }

        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }

        public String getHelloMessage() {
            return helloMessage;
        }

        public void setHelloMessage(String helloMessage) {
            this.helloMessage = helloMessage;
        }

        public String getWelcome() {
            return welcome;
        }

        public void setWelcome(String welcome) {
            this.welcome = welcome;
        }
    }
}