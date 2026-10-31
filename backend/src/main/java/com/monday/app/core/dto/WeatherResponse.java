package com.monday.app.core.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WeatherResponse {
    private Double temperature;
    private Double windspeed;
    private Integer weathercode;

    @JsonProperty("isDay")
    @JsonAlias({"is_day", "day"})
    private Integer isDay;

    public WeatherResponse() {}

    public WeatherResponse(Double temperature, Double windspeed, Integer weathercode, Integer isDay) {
        this.temperature = temperature;
        this.windspeed = windspeed;
        this.weathercode = weathercode;
        this.isDay = isDay;
    }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Double getWindspeed() { return windspeed; }
    public void setWindspeed(Double windspeed) { this.windspeed = windspeed; }

    public Integer getWeathercode() { return weathercode; }
    public void setWeathercode(Integer weathercode) { this.weathercode = weathercode; }

    public Integer getIsDay() { return isDay; }
    public void setIsDay(Integer isDay) { this.isDay = isDay; }
}
