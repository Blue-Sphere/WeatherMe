package sideproject.demo.weather;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.view.RedirectView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class WeatherController {
    
    private static Logger logger = LoggerFactory.getLogger(WeatherController.class);

    private final WeatherService weatherService;

    private final WeatherSecurity weatherSecurity;

    @Autowired
    public WeatherController(WeatherService weatherService, WeatherSecurity weatherSecurity){
        this.weatherSecurity = weatherSecurity;
        this.weatherService = weatherService;
    }

    @GetMapping(path = "/")
    public RedirectView redirectLoginPage(){
        return new RedirectView("/login");
    }

    @GetMapping(path = "/sign-up")
    public String signUpPage(){
        return "sign-up.html";
    }

    @GetMapping(path = "/login")
    public String loginPage(){
        return "login.html";
    }

    @GetMapping(path = "/line/connect")
    public String lineConnectingPage(){
        return "connecting.html";
    }

    @GetMapping(path = "/index")
    public String index(@RequestHeader("Authorization") String bearer, Model model) throws Exception{
        String token = bearer.replace("Bearer ", "");
        if(weatherSecurity.validateToken(token)){

            model.addAttribute("obsId", weatherService.getUser(weatherSecurity.getUserNameFromToken(token)).getObsId());
            model.addAttribute("city", weatherService.getUser(weatherSecurity.getUserNameFromToken(token)).getCity());
            model.addAttribute("town", weatherService.getUser(weatherSecurity.getUserNameFromToken(token)).getTown());
            model.addAttribute("notificationTime", weatherService.getUser(weatherSecurity.getUserNameFromToken(token)).getNotificationTime());
            if(weatherService.getUser(weatherSecurity.getUserNameFromToken(token)).getLineId() == null){
                model.addAttribute("lineConnectionSuccess",false);
            }else{
                model.addAttribute("lineConnectionSuccess",true);
            }
            
            TimlyWeather timlyWeather = weatherService.getWeatherTimely(weatherSecurity.getUserNameFromToken(token));
            model.addAttribute("timlyLocation",timlyWeather.getLocation());
            model.addAttribute("timlyTime",timlyWeather.getStartTime());
            model.addAttribute("timlyWeather",timlyWeather.getWeather());
            model.addAttribute("timlyTemp",timlyWeather.getTemp());
            model.addAttribute("timlyAccumulatedRainfall",timlyWeather.getAccumulatedRainfall());
            model.addAttribute("timlyWindLevel",timlyWeather.getWindLevel());
            model.addAttribute("timlyWeatherUrl", "https://www.cwb.gov.tw/V8/C/W/OBS_Station.html?ID=" + weatherService.getUser(weatherSecurity.getUserNameFromToken(token)).getObsId().replaceFirst(".$", ""));

            ArrayList<TwoDaysWeather> listForTwoDaysWeather = weatherService.getTwoDaysWeather(weatherSecurity.getUserNameFromToken(token));
            model.addAttribute("listForTwoDaysWeather", listForTwoDaysWeather);
            model.addAttribute("twoDaysWeatherUrl", "https://www.cwb.gov.tw/V8/C/W/Town/Town.html?TID=" + weatherService.getTownsID(weatherService.getUser(weatherSecurity.getUserNameFromToken(token)).getTown()));

            ArrayList<WeeklyWeather> listForWeeklyWeather = weatherService.getWeeklyWeather(weatherSecurity.getUserNameFromToken(token));
            model.addAttribute("listForWeeklyWeather", listForWeeklyWeather);
            model.addAttribute("weeklyWeatherUrl", "https://www.cwb.gov.tw/V8/C/W/Town/Town.html?TID=" + weatherService.getTownsID(weatherService.getUser(weatherSecurity.getUserNameFromToken(token)).getTown()));
            
            return "index.html";
        }
        throw new IllegalStateException("invalid token");
    }

    @RestController
    public class DataController{
        
        @PostMapping(path = "/signUp")
        public ResponseEntity<String> SignUpPage(@RequestBody User newUser) throws Exception{
            try{
                String result = weatherService.register(newUser.getEmail(), newUser.getName(), newUser.getPassword());
                return ResponseEntity.ok(result);
            }catch (Exception e){
                return ResponseEntity.badRequest().body(e.getMessage());
            }


        }

        @PostMapping(path = "/login")
        public ResponseEntity<String> indexPage(@ModelAttribute User user){

            if(weatherService.authentication(user)){
                String token = weatherSecurity.generateToken(user.getName());
                return ResponseEntity.ok(token);
            }else{
                throw new IllegalStateException("Invaild username or password");
            }
        }

        @PostMapping(path = "/verifyUser")
        public ResponseEntity<String> verifyUser(@RequestBody User user) throws Exception{
            if(weatherService.verifyUser(user)){
                return ResponseEntity.ok(weatherService.saveUser(user));
            }
            throw new IllegalStateException("verification code is not correct");
        }

        @PostMapping(path = "/changeSetting")
        public String changeSetting(@RequestHeader("Authorization") String bearer, @RequestBody User user){
            String token = bearer.replace("Bearer ", "");
            if(weatherSecurity.validateToken(token)){
                String userName = weatherSecurity.getUserNameFromToken(token);
                user.setName(userName);
                weatherService.updateUser(user);
                return "success";
            }else{
                throw new IllegalStateException("error");
            }
        }
        
        @PostMapping(path = "/line/connect")
        public String lineConnect(@RequestHeader("Authorization") String bearer,@RequestBody Map<String, String> requestBodyMap) throws Exception{
            String token = bearer.replace("Bearer ", "");
            if(weatherSecurity.validateToken(token)){
                String lineId = weatherService.getUsersLindId(requestBodyMap.get("code"));
                User user = weatherService.getUser(weatherSecurity.getUserNameFromToken(token));
                user.setLineId(lineId);
                weatherService.updateLineId(user);

                return "connect success";
            }else{
                return "error";
            }
        }

    }
}
