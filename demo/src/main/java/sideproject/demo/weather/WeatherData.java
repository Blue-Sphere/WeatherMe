package sideproject.demo.weather;

// public class WeatherData {

//     private String title;
//     private Map<String, List<Optional<String>>> datas;

//     public String getTitle() {
//         return this.title;
//     }

//     public void setTitle(String title) {
//         this.title = title;
//     }

//     public Map<String,List<Optional<String>>> getDatas() {
//         return this.datas;
//     }

//     public void setDatas(Map<String,List<Optional<String>>> datas) {
//         this.datas = datas;
//     }
// }

public class WeatherData{

    private String title;
    private String startTime;
    private String weather;
    private String humidity;
    private String windLevel;
    private String windDirection;

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStartTime() {
        return this.startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getWeather() {
        return this.weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public String getHumidity() {
        return this.humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getWindLevel() {
        return this.windLevel;
    }

    public void setWindLevel(String windLevel) {
        this.windLevel = windLevel;
    }

    public String getWindDirection() {
        return this.windDirection;
    }

    public void setWindDirection(String windDirection) {
        this.windDirection = windDirection;
    }
}

class TimlyWeather extends WeatherData{

    private String accumulatedRainfall;
    private String temp;
    private String location;

    public String getLocation() {
        return this.location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAccumulatedRainfall() {
        return this.accumulatedRainfall;
    }

    public void setAccumulatedRainfall(String accumulatedRainfall) {
        this.accumulatedRainfall = accumulatedRainfall;
    }

    public String getTemp() {
        return this.temp;
    }

    public void setTemp(String temp) {
        this.temp = temp;
    }
    

}

class TwoDaysWeather extends WeatherData{

    private String bodyTemp;
    private String pop6h;
    private String temp;
    private String pop12h;
    private String confort;

    public String getBodyTemp() {
        return this.bodyTemp;
    }

    public void setBodyTemp(String bodyTemp) {
        this.bodyTemp = bodyTemp;
    }

    public String getPop6h() {
        return this.pop6h;
    }

    public void setPop6h(String pop6h) {
        this.pop6h = pop6h;
    }

    public String getTemp() {
        return this.temp;
    }

    public void setTemp(String temp) {
        this.temp = temp;
    }

    public String getPop12h() {
        return this.pop12h;
    }

    public void setPop12h(String pop12h) {
        this.pop12h = pop12h;
    }

    public String getConfort() {
        return this.confort;
    }

    public void setConfort(String confort) {
        this.confort = confort;
    }

}

class WeeklyWeather extends WeatherData{

    private String maxTemp;
    private String minTemp;
    private String pop12h;
    private String maxBodyTemp;
    private String minBodyTemp;
    private String uvi;

    public String getMaxTemp() {
        return this.maxTemp;
    }

    public void setMaxTemp(String maxTemp) {
        this.maxTemp = maxTemp;
    }

    public String getMinTemp() {
        return this.minTemp;
    }

    public void setMinTemp(String minTemp) {
        this.minTemp = minTemp;
    }

    public String getPop12h() {
        return this.pop12h;
    }

    public void setPop12h(String pop12h) {
        this.pop12h = pop12h;
    }

    public String getMaxBodyTemp() {
        return this.maxBodyTemp;
    }

    public void setMaxBodyTemp(String maxBodyTemp) {
        this.maxBodyTemp = maxBodyTemp;
    }

    public String getMinBodyTemp() {
        return this.minBodyTemp;
    }

    public void setMinBodyTemp(String minBodyTemp) {
        this.minBodyTemp = minBodyTemp;
    }

    public String getUvi() {
        return this.uvi;
    }

    public void setUvi(String uvi) {
        this.uvi = uvi;
    }

}