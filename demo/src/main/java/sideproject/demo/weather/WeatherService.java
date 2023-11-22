package sideproject.demo.weather;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    private final WeatherRepository weatherRepository;
    private final JsonFile jsonFile;
    private final Secret secret;
    private final Mailer mailer;
    private final String cwbToken;

    @Autowired
    public WeatherService(WeatherRepository weatherRepository, JsonFile jsonFile, Secret secret, Mailer gmailer, String cwbToken) {
        this.weatherRepository = weatherRepository;
        this.jsonFile = jsonFile;
        this.secret = secret;
        this.mailer = gmailer;
        this.cwbToken = secret.getAuthorizationCode();
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    public User getUser(String name) {
        Optional<User> optionalUser = weatherRepository.findByName(name);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            return user;
        } else {
            throw new IllegalStateException("User id is not exist");
        }

    }

    public String generateVerificationCode(int length) {
        int min = (int) Math.pow(10, length - 1);
        int max = (int) Math.pow(10, length) - 1;
        Random random = new Random();
        return String.valueOf(random.nextInt(max - min + 1) + min);
    }

    public String register(String email, String name, String password) throws Exception {

        String verificationCode = generateVerificationCode(4);
        if (weatherRepository.findByEmail(email).isPresent()
                || weatherRepository.findByName(name).isPresent()) {
            throw new IllegalStateException("the email or username is already exist");
        } else {
            mailer.sendEmail(email, "weather.me 註冊驗證碼", name + " 您好,您的驗證碼是:" + verificationCode);
            redisTemplate.opsForValue().set(email, verificationCode);
            return "OK";
        }

    }

    public boolean verifyUser(User user) {
        if (redisTemplate.opsForValue().get(user.getEmail()).equals(user.getVerificationCode())) {
            return true;
        }
        return false;
    }

    public String saveUser(User user) {
        redisTemplate.delete(user.getEmail());
        User.initNewUser(user);
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        weatherRepository.save(user);
        return "OK";
    }

    public boolean authentication(User user) {
        User db_user = getUser(user.getName());
        if (BCrypt.checkpw(user.getPassword(), db_user.getPassword())) {
            return true;
        }
        return false;
    }

    public String getTwoDaysWeatherLocationID(String location) {
        Object document = jsonFile.getTwoDaysWeatherIdSurface();
        return JsonPath.read(document, location);
    }

    public String getWeeklyWeatherLocationID(String town) {
        Object document = jsonFile.getWeeklyWeatherIdSurface();
        return JsonPath.read(document, town);
    }

    public String getTownsID(String town) {
        Object document = jsonFile.getTownIdSurface();
        List<String> values = JsonPath.read(document, "$.." + town);
        if (!values.isEmpty()) {
            return values.get(0);
        } else {
            throw new IllegalStateException("Town id is not exist");
        }
    }

    public TimlyWeather getWeatherTimely(String name) throws Exception {

        Optional<User> optionalUser = weatherRepository.findByName(name);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            String obsId = user.getObsId();

//            String url = "https://opendata.cwb.gov.tw/api/v1/rest/datastore/O-A0001-001?Authorization=" + cwbToken + "&stationId="
//                    + obsId + "&elementName=TEMP,WDSD,H_24R,Weather&parameterName=CITY,TOWN";
            String url = "https://opendata.cwb.gov.tw/api/v1/rest/datastore/O-A0001-001?Authorization="+cwbToken+  "&StationId="
                    +obsId+"&WeatherElement=Weather,Now,WindDirection,WindSpeed,AirTemperature,RelativeHumidity&GeoInfo=CountyName,TownName";

            /*HttpClient 用法 速度較慢所以先不採用*/
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(new URI(url))
//                    .GET()
//                    .build();
//
//            HttpResponse<String> response = HttpClient.newBuilder()
//                    .build()
//                    .send(request, HttpResponse.BodyHandlers.ofString());

            /*HttpUrlConnection 用法*/
            URL obj = new URL(url);

            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
//
            con.setRequestMethod("GET");
//
            Scanner scanner = new Scanner(con.getInputStream(), "UTF-8");

            while (scanner.hasNextLine()) {

                TimlyWeather timlyWeather = new TimlyWeather();
                String line = scanner.nextLine();
                Object document = Configuration.defaultConfiguration().jsonProvider().parse(line);

                String time = JsonPath.read(document, "$['records']['Station'][0]['ObsTime']['DateTime']");
                String temp = JsonPath.read(document,
                        "$['records']['Station'][0]['WeatherElement']['AirTemperature']").toString();
                String wdsd = JsonPath.read(document,
                        "$['records']['Station'][0]['WeatherElement']['WindSpeed']").toString();
                String h_24r = JsonPath.read(document,
                        "$['records']['Station'][0]['WeatherElement']['Now']['Precipitation']").toString();
                String weather = JsonPath.read(document,
                        "$['records']['Station'][0]['WeatherElement']['Weather']");
                String location = JsonPath.read(document,
                                "$['records']['Station'][0]['GeoInfo']['CountyName']")
                        .toString()
                        + JsonPath.read(document,
                                "$['records']['Station'][0]['GeoInfo']['TownName']")
                        .toString();
                // System.out.printf("%s %s %s %s %s ", time, temp, wdsd, h_24r, weather);

                String[] weatherList = {time, temp, wdsd, h_24r, weather, location};

                for (int x = 0; x < weatherList.length; x++) {
                    if (weatherList[x].equals("-99") || weatherList[x].equals("")) {
                        weatherList[x] = "—";
                    }
                }

                timlyWeather.setTitle("即時天氣狀況");
                timlyWeather.setStartTime(weatherList[0]);
                timlyWeather.setTemp(weatherList[1]);
                timlyWeather.setWindLevel(weatherList[2]);
                timlyWeather.setAccumulatedRainfall(weatherList[3]);
                timlyWeather.setWeather(weatherList[4]);
                timlyWeather.setLocation(weatherList[5]);

                scanner.close();
                return timlyWeather;
            }
        }
        throw new IllegalStateException("error token");
    }

    public ArrayList<TwoDaysWeather> getTwoDaysWeather(String name) throws Exception {

        Optional<User> optionalUser = weatherRepository.findByName(name);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            String locationId = getTwoDaysWeatherLocationID(user.getCity());
            String town = user.getTown();

            String url = "https://opendata.cwb.gov.tw/api/v1/rest/datastore/" + locationId
                    + "?Authorization=" + cwbToken +  "&locationName="
                    + URLEncoder.encode(town, "UTF-8");
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("GET");

            Scanner scanner = new Scanner(con.getInputStream(), "UTF-8");

            ArrayList<TwoDaysWeather> listForTwoDaysWeather = new ArrayList<>();

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                Object document = Configuration.defaultConfiguration().jsonProvider().parse(line);

                for (int i = 0; i < 32; i++) {
                    TwoDaysWeather twoDaysWeather = new TwoDaysWeather();
                    twoDaysWeather.setStartTime(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][1]['time']["
                                    + i
                                    + "]['startTime']"));
                    twoDaysWeather.setWeather(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][1]['time']["
                                    + i
                                    + "]['elementValue'][0]['value']"));
                    twoDaysWeather.setTemp(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][3]['time']["
                                    + i
                                    + "]['elementValue'][0]['value']"));
                    twoDaysWeather.setBodyTemp(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][2]['time']["
                                    + i
                                    + "]['elementValue'][0]['value']"));
                    twoDaysWeather.setConfort(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][5]['time']["
                                    + i
                                    + "]['elementValue'][1]['value']"));
                    twoDaysWeather.setHumidity(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][4]['time']["
                                    + i
                                    + "]['elementValue'][0]['value']")
                            + "%");
                    twoDaysWeather.setWindDirection(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][9]['time']["
                                    + i
                                    + "]['elementValue'][0]['value']"));
                    twoDaysWeather.setWindLevel(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][8]['time']["
                                    + i
                                    + "]['elementValue'][0]['value']"));
                    twoDaysWeather.setPop6h(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][7]['time']["
                                    + i / 2
                                    + "]['elementValue'][0]['value']")
                            + "%");
                    twoDaysWeather.setPop12h(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][0]['time']["
                                    + i / 4
                                    + "]['elementValue'][0]['value']")
                            + "%");

                    listForTwoDaysWeather.add(twoDaysWeather);
                }
            }
            scanner.close();
            return listForTwoDaysWeather;
        }
        throw new IllegalStateException("error to get datas");
    }

    public ArrayList<WeeklyWeather> getWeeklyWeather(String name) throws Exception {

        Optional<User> optionalUser = weatherRepository.findByName(name);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            String locationId = getWeeklyWeatherLocationID(user.getCity());
            String town = user.getTown();

            String url = "https://opendata.cwb.gov.tw/api/v1/rest/datastore/" + locationId
                    + "?Authorization=" + cwbToken +  "&locationName="
                    + URLEncoder.encode(town, "UTF-8");
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("GET");

            Scanner scanner = new Scanner(con.getInputStream(), "UTF-8");

            ArrayList<WeeklyWeather> listForWeeklyWeather = new ArrayList<>();

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                Object document = Configuration.defaultConfiguration().jsonProvider().parse(line);

                for (int i = 0; i < 14; i++) {
                    WeeklyWeather weeklyWeather = new WeeklyWeather();
                    weeklyWeather.setStartTime(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][0]['time']["
                                    + i
                                    + "]['startTime']"));
                    weeklyWeather.setWeather(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][6]['time']["
                                    + i
                                    + "]['elementValue'][0]['value']"));
                    weeklyWeather.setMaxTemp(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][12]['time']["
                                    + i
                                    + "]['elementValue'][0]['value']"));
                    weeklyWeather.setMinTemp(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][8]['time']["
                                    + i
                                    + "]['elementValue'][0]['value']"));
                    weeklyWeather.setMaxBodyTemp(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][5]['time']["
                                    + i
                                    + "]['elementValue'][0]['value']"));
                    weeklyWeather.setMinBodyTemp(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][11]['time']["
                                    + i
                                    + "]['elementValue'][0]['value']"));
                    weeklyWeather.setHumidity(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][2]['time']["
                                    + i
                                    + "]['elementValue'][0]['value']")
                            + "%");
                    weeklyWeather.setWindDirection(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][13]['time']["
                                    + i
                                    + "]['elementValue'][0]['value']"));
                    weeklyWeather.setWindLevel(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][4]['time']["
                                    + i
                                    + "]['elementValue'][0]['value']"));
                    weeklyWeather.setPop12h(JsonPath.read(document,
                            "$['records']['locations'][0]['location'][0]['weatherElement'][0]['time']["
                                    + i
                                    + "]['elementValue'][0]['value']")
                            + "%");

                    if (!weeklyWeather.getStartTime().endsWith("18:00:00")) { // 有太陽時
                        weeklyWeather.setUvi(JsonPath.read(document,
                                "$['records']['locations'][0]['location'][0]['weatherElement'][9]['time']["
                                        + i / 2
                                        + "]['elementValue'][0]['value']"));
                    } else {
                        weeklyWeather.setUvi("null");
                    }

                    listForWeeklyWeather.add(weeklyWeather);
                }
            }
            scanner.close();
            return listForWeeklyWeather;
        }
        throw new IllegalStateException("error to get datas");
    }

    public void updateUser(User newUser) {
        Optional<User> optionalUser = weatherRepository.findByName(newUser.getName());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (!user.equals(newUser)) {
                user.setCity(newUser.getCity());
                user.setTown(newUser.getTown());
                user.setObsId(newUser.getObsId());
                user.setNotificationTime(newUser.getNotificationTime());
                weatherRepository.save(user);
            }
        }
    }

    public void updateLineId(User userWithLindId) {
        Optional<User> optionalUser = weatherRepository.findByName(userWithLindId.getName());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getLineId() == null || !user.getLineId().equals(userWithLindId.getLineId())){
                user.setLineId(userWithLindId.getLineId());
                weatherRepository.save(user);
            }
        }
    }

    public String getUsersLindId(String code) throws Exception {
        // Connect to the API, and get the authorization token
        URL obj = new URL("https://api.line.me/oauth2/v2.1/token");
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");

        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36 Edg/110.0.1587.78");

        con.setDoOutput(true);

        String parameters = "grant_type=authorization_code&code=" + code
                + "&redirect_uri=https://weatherme-3vsl.onrender.com/line/connect&client_id=2000634910&client_secret=74db32fa5721796f13c8c082c96895f3";

        byte[] postData = parameters.getBytes(StandardCharsets.UTF_8);

        OutputStream outputStream = con.getOutputStream();

        outputStream.write(postData);
        outputStream.flush();
        outputStream.close();

        int responseCode = con.getResponseCode();
        try{
                Scanner scanner = new Scanner(con.getInputStream(), "UTF-8");
                String accessToken="";
                while(scanner.hasNextLine()){
                    String line = scanner.nextLine();
                    Object document = Configuration.defaultConfiguration().jsonProvider().parse(line);
                    accessToken = JsonPath.read(document, "$['access_token']");
                }
                scanner.close();
                con.disconnect();
    
                // Connect to the API, and get the userId
                URL obj2 = new URL("https://api.line.me/oauth2/v2.1/userinfo");
                HttpURLConnection idConnection = (HttpURLConnection) obj2.openConnection();
                idConnection.setRequestMethod("GET");
                idConnection.setRequestProperty("authorization", "Bearer " + accessToken);
                
                String userLineId="";
                Scanner scanner2 = new Scanner(idConnection.getInputStream(), "UTF-8");
                while(scanner2.hasNextLine()){
                    String line = scanner2.nextLine();
                    Object document = Configuration.defaultConfiguration().jsonProvider().parse(line);
                    userLineId = JsonPath.read(document, "$['sub']");
                }
                scanner2.close();
                idConnection.disconnect();
    
                return userLineId;
                
            }catch(Exception e){
            throw new Exception(e.getMessage());
        }
    }
}
