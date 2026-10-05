package com.monday.app.core.controller;

import com.monday.app.core.dto.WeatherResponse;
import com.monday.app.core.service.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/core")
public class CoreController {

    private final WeatherService weatherService;

    public CoreController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/weather")
    public WeatherResponse getWeather() {
        return weatherService.getCurrentWeather();
    }
}
