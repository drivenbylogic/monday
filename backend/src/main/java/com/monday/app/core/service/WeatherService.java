package com.monday.app.core.service;

import com.monday.app.core.dto.WeatherResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@Service
public class WeatherService {
    
    private final RestTemplate restTemplate;

    public WeatherService() {
        this.restTemplate = new RestTemplate();
    }

    public WeatherResponse getCurrentWeather() {
        // Coimbatore coordinates
        String url = "https://api.open-meteo.com/v1/forecast?latitude=11.0168&longitude=76.9558&current_weather=true";
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Map<String, Object> currentWeather = (Map<String, Object>) body.get("current_weather");
                
                WeatherResponse weatherResponse = new WeatherResponse();
                if (currentWeather != null) {
                    if (currentWeather.get("temperature") instanceof Number) {
                        weatherResponse.setTemperature(((Number) currentWeather.get("temperature")).doubleValue());
                    }
                    if (currentWeather.get("windspeed") instanceof Number) {
                        weatherResponse.setWindspeed(((Number) currentWeather.get("windspeed")).doubleValue());
                    }
                    if (currentWeather.get("weathercode") instanceof Number) {
                        weatherResponse.setWeathercode(((Number) currentWeather.get("weathercode")).intValue());
                    }
                    if (currentWeather.get("is_day") instanceof Number) {
                        weatherResponse.setIsDay(((Number) currentWeather.get("is_day")).intValue());
                    }
                }
                return weatherResponse;
            }
        } catch (Exception e) {
            // Log error, fallback to empty response
        }
        return new WeatherResponse();
    }
}
