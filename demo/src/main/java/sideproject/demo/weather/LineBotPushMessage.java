package sideproject.demo.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.LineMessagingClientBuilder;
import com.linecorp.bot.model.PushMessage;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.flex.container.FlexContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class LineBotPushMessage {

    private static final Logger logger = LoggerFactory.getLogger(LineBotPushMessage.class);

    final private WeatherRepository weatherRepository;

    final private WeatherService weatherService;

    final private JsonFile jsonFile;

    @Value("${lineMessageBotToken}")
    private String messageBotToken;

    @Autowired
    public LineBotPushMessage(WeatherRepository weatherRepository, WeatherService weatherService, JsonFile jsonFile) {
        this.weatherRepository = weatherRepository;
        this.weatherService= weatherService;
        this.jsonFile = jsonFile;
    }

    @Autowired
    private LineMessagingClient lineMessagingClient;

    @Scheduled(cron = "0 0,30 * * * *")
    public void checkToPushMessage() throws Exception {

        logger.info("執行Scheduled - PushMessage偵測並推送 - 每30分鐘運行一次");

        LineMessagingClientBuilder lineMessagingClientBuilder = LineMessagingClient.builder(messageBotToken);
        lineMessagingClientBuilder.build();

        Object pushMessageTemplate = jsonFile.getLineBotPushMessageTemplate();

        Date currentDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        String notificationTime = hour + ":" + minute;

        List<User> theUsersWhoNeedToBeNotified = weatherRepository.getUsersWhoNeedToBeNotified(notificationTime);

//        測試PushMessage
//        List<User> theUsersWhoNeedToBeNotified = weatherRepository.getUsersWhoNeedToBeNotified("22:00");

        for(int i=0; i<theUsersWhoNeedToBeNotified.size(); i++){

            logger.info(theUsersWhoNeedToBeNotified.get(i).getName() + " 用戶推送中");

            User target = theUsersWhoNeedToBeNotified.get(i);

            ArrayList<TwoDaysWeather> twoDaysWeather = weatherService.getTwoDaysWeather(target.getName());

            DocumentContext context = JsonPath.parse(pushMessageTemplate);

            for(int count=0; count<10;count++){
                TwoDaysWeather predictWeather = twoDaysWeather.get(count);

                // 修改標題、時間
                context.set("$['contents'][" + count + "]['body']['contents'][0]['text']", "氣象預報：" + predictWeather.getStartTime());// 修改城市、鄉鎮
                // 修改城市、鄉鎮
                context.set("$['contents'][" + count + "]['body']['contents'][1]['text']", target.getCity() + " - " + target.getTown());// 修改城市、鄉鎮
                // 修改天氣狀況
                context.set("$['contents'][" + count + "]['body']['contents'][2]['text']", "天氣狀況：" + predictWeather.getWeather());
                // 修改溫度
                context.set("$['contents'][" + count + "]['body']['contents'][4]['contents'][0]['contents'][1]['text']", predictWeather.getTemp()+"°C");
                // 修改降雨機率(6h)
                context.set("$['contents'][" + count + "]['body']['contents'][4]['contents'][1]['contents'][1]['text']", predictWeather.getPop6h());
                // 修改降雨機率(12h)
                context.set("$['contents'][" + count + "]['body']['contents'][4]['contents'][2]['contents'][1]['text']", predictWeather.getPop12h());
                // 修改風級
                context.set("$['contents'][" + count + "]['body']['contents'][4]['contents'][4]['contents'][1]['text']", predictWeather.getWindLevel());
                // 修改風向
                context.set("$['contents'][" + count + "]['body']['contents'][4]['contents'][5]['contents'][1]['text']", predictWeather.getWindDirection());
                // 修改濕度
                context.set("$['contents'][" + count + "]['body']['contents'][4]['contents'][6]['contents'][1]['text']", predictWeather.getHumidity());
                // 修改體感舒適度
                context.set("$['contents'][" + count + "]['body']['contents'][4]['contents'][7]['contents'][1]['text']", predictWeather.getConfort());
            }

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectReader objectReader = objectMapper.readerFor(FlexContainer.class);
            String jsonString = objectMapper.writeValueAsString(context.json());

            FlexContainer flexContainer = objectReader.readValue(jsonString);
            FlexMessage flexMessage = new FlexMessage("天氣預報通知", flexContainer);
            PushMessage pushMessage = new PushMessage(target.getLineId(), flexMessage);
            lineMessagingClient.pushMessage(pushMessage);
        }
    }
}

