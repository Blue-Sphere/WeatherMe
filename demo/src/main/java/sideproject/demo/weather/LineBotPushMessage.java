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
import org.springframework.beans.factory.annotation.Autowired;
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

    final private WeatherRepository weatherRepository;

    final private WeatherService weatherService;

    final private JsonFile jsonFile;

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

            LineMessagingClientBuilder lineMessagingClientBuilder = LineMessagingClient.builder("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Ik1UaEVOVUpHTkVNMVFURTRNMEZCTWpkQ05UZzVNRFUxUlRVd1FVSkRNRU13UmtGRVFrRXpSZyJ9.eyJodHRwczovL2FwaS5vcGVuYWkuY29tL3Byb2ZpbGUiOnsiZW1haWwiOiJhNjEyMzQ4QHlhaG9vLmNvbS50dyIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlfSwiaHR0cHM6Ly9hcGkub3BlbmFpLmNvbS9hdXRoIjp7InVzZXJfaWQiOiJ1c2VyLXhsMmVWd25UMnBYUGRSMVgzdkhEQVFVWiJ9LCJpc3MiOiJodHRwczovL2F1dGgwLm9wZW5haS5jb20vIiwic3ViIjoiYXV0aDB8NjNmMGFiODAwN2FkZTNmMDdhZDVmN2ZkIiwiYXVkIjpbImh0dHBzOi8vYXBpLm9wZW5haS5jb20vdjEiLCJodHRwczovL29wZW5haS5vcGVuYWkuYXV0aDBhcHAuY29tL3VzZXJpbmZvIl0sImlhdCI6MTY5NDE1NDI5NiwiZXhwIjoxNjk1MzYzODk2LCJhenAiOiJUZEpJY2JlMTZXb1RIdE45NW55eXdoNUU0eU9vNkl0RyIsInNjb3BlIjoib3BlbmlkIGVtYWlsIHByb2ZpbGUgbW9kZWwucmVhZCBtb2RlbC5yZXF1ZXN0IG9yZ2FuaXphdGlvbi5yZWFkIG9yZ2FuaXphdGlvbi53cml0ZSBvZmZsaW5lX2FjY2VzcyJ9.suR6V9xX-CM1Ctv0oP0FozoqsmYMZzEDQ4UNRCVZP3yK5dPRs3z2vScfj8zRm2ljY5atEuBIJgoJxP-MpGe3aUi9I4UsFw7s2-sJMr1x1r--QicWaAMuI5s9JhkhgKXMfIl9DaarvTv_Us1_3omn21GvW9AzNbqRYDb3zxkTvTpzJI2C-FhHAJ2-T15KNVgyHtZ4CCoGktDL0CbOjZw96Lgo4VMI52uYY3qcE3SYCImjxwuX0MOmaP2evcvvn_sJMvet9sjf-M5F0jf6aMmTnL0ulYEbrbCIg88KEbrwx9eoYfpVuJMIo5M2N3KyXqE4_x-_LlgZtNQkuU2f_Qqijg");
            lineMessagingClientBuilder.build();

            Object pushMessageTemplate = jsonFile.getLineBotPushMessageTemplate();


            Date currentDate = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(currentDate);

            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            String notificationTime = hour + ":" + minute;

            List<User> theUsersWhoNeedToBeNotified = weatherRepository.getUsersWhoNeedToBeNotified(notificationTime);
//            // 測試PushMessage
//            List<User> theUsersWhoNeedToBeNotified = weatherRepository.getUsersWhoNeedToBeNotified("22:00");

            for(int i=0; i<theUsersWhoNeedToBeNotified.size(); i++){
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

