package com.monday.app.core.dto;

public class WeatherResponse {
    private Double temperature;
    private Double windspeed;
    private Integer weathercode;
    private Integer isDay;

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Double getWindspeed() { return windspeed; }
    public void setWindspeed(Double windspeed) { this.windspeed = windspeed; }

    public Integer getWeathercode() { return weathercode; }
    public void setWeathercode(Integer weathercode) { this.weathercode = weathercode; }

    public Integer getIsDay() { return isDay; }
    public void setIsDay(Integer isDay) { this.isDay = isDay; }
}
