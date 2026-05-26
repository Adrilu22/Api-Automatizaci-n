package com.tiendamoda.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsController {

    @GetMapping("/metrics")
    public void metrics(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request.getRequestDispatcher("/actuator/prometheus").forward(request, response);
    }
}
