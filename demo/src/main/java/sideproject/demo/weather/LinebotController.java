package sideproject.demo.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.LineMessagingClientBuilder;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.flex.container.FlexContainer;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

@SpringBootApplication
@LineMessageHandler
public class LinebotController {

    private final WeatherRepository weatherRepository;
    private final WeatherService weatherService;
    private final JsonFile jsonFile;

    @Autowired
    public LinebotController(WeatherRepository weatherRepository, WeatherService weatherService , JsonFile jsonFile){
        this.weatherRepository = weatherRepository;
        this.weatherService = weatherService;
        this.jsonFile = jsonFile;
    }

    @Autowired
    private LineMessagingClient lineMessagingClient;

    @Value("${lineMessageBotToken}")
    private String messageBotToken;

    @EventMapping()
    @GetMapping("/callback")
    public TextMessage handleTextMessageEvent(MessageEvent<TextMessageContent> event) throws Exception {

        LineMessagingClientBuilder lineMessagingClientBuilder = LineMessagingClient.builder(messageBotToken);
        lineMessagingClientBuilder.build();

        final String message = event.getMessage().getText();

        if (!message.startsWith("/")) {
            return new TextMessage("目前還無法提指令以外的功能，請透過圖文選單選擇功能，感謝您！");
        } else {
            /* 進入指令區塊 */
            final String lineId = event.getSource().getUserId();
            final String replyToken = event.getReplyToken();

            final Optional<User> optionalUser = weatherRepository.findByLineId(lineId);

            if (optionalUser.isPresent()) {
                final User user = optionalUser.get();

                if (message.startsWith("/showTwoDaysWeather")) {
                    ArrayList<TwoDaysWeather> twoDaysWeathers = weatherService.getTwoDaysWeather(user.getName());

                    Object pushMessageTemplate = jsonFile.getLineBotPushMessageTemplate();
                    DocumentContext context = JsonPath.parse(pushMessageTemplate);

                    /*確認推送訊息不會是過去資料*/
                    int pushcount = 10;
                    int count = 0;
                    Date currentDate = new Date();
                    for(;count<pushcount;count++){
                        int templateYear = Integer.parseInt(twoDaysWeathers.get(count).getStartTime().substring(0,4));
                        int templateMonth = Integer.parseInt(twoDaysWeathers.get(count).getStartTime().substring(5,7));
                        int templateDay = Integer.parseInt(twoDaysWeathers.get(count).getStartTime().substring(8,10));
                        int templateHour = Integer.parseInt(twoDaysWeathers.get(count).getStartTime().substring(11,13));

                        if (currentDate.after(new Date(templateYear, templateMonth, templateDay, templateHour, 0))){
                            pushcount +=1;
                            continue;
                        }
                        break;
                    }

                    for (;count<pushcount;count++){

                        TwoDaysWeather twoDaysWeather = twoDaysWeathers.get(count);
                        
                        // 修改標題、時間
                        context.set("$['contents'][" + count + "]['body']['contents'][0]['text']", "氣象預報：" + twoDaysWeather.getStartTime());// 修改城市、鄉鎮
                        // 修改城市、鄉鎮
                        context.set("$['contents'][" + count + "]['body']['contents'][1]['text']", user.getCity() + " - " + user.getTown());// 修改城市、鄉鎮
                        // 修改天氣狀況
                        context.set("$['contents'][" + count + "]['body']['contents'][2]['text']", "天氣狀況：" + twoDaysWeather.getWeather());
                        // 修改溫度
                        context.set("$['contents'][" + count + "]['body']['contents'][4]['contents'][0]['contents'][1]['text']", twoDaysWeather.getTemp()+"°C");
                        // 修改降雨機率(6h)
                        context.set("$['contents'][" + count + "]['body']['contents'][4]['contents'][1]['contents'][1]['text']", twoDaysWeather.getPop6h());
                        // 修改降雨機率(12h)
                        context.set("$['contents'][" + count + "]['body']['contents'][4]['contents'][2]['contents'][1]['text']", twoDaysWeather.getPop12h());
                        // 修改風級
                        context.set("$['contents'][" + count + "]['body']['contents'][4]['contents'][4]['contents'][1]['text']", twoDaysWeather.getWindLevel());
                        // 修改風向
                        context.set("$['contents'][" + count + "]['body']['contents'][4]['contents'][5]['contents'][1]['text']", twoDaysWeather.getWindDirection());
                        // 修改濕度
                        context.set("$['contents'][" + count + "]['body']['contents'][4]['contents'][6]['contents'][1]['text']", twoDaysWeather.getHumidity());
                        // 修改體感舒適度
                        context.set("$['contents'][" + count + "]['body']['contents'][4]['contents'][7]['contents'][1]['text']", twoDaysWeather.getConfort());

                    }
                    ObjectMapper objectMapper = new ObjectMapper();
                    ObjectReader objectReader = objectMapper.readerFor(FlexContainer.class);
                    String jsonString = objectMapper.writeValueAsString(context.json());

                    FlexContainer flexContainer = objectReader.readValue(jsonString);
                    FlexMessage flexMessage = new FlexMessage("近兩日天氣預報", flexContainer);
                    ReplyMessage replyMessage = new ReplyMessage(replyToken, flexMessage);
                    lineMessagingClient.replyMessage(replyMessage);

                }else if(message.startsWith("/showWeeklyWeather")){
                    ArrayList<WeeklyWeather> weeklyWeathers = weatherService.getWeeklyWeather(user.getName());

                    Object weeklyMessageTemplate = jsonFile.getLineBotWeeklyMessageTemplate();
                    DocumentContext context = JsonPath.parse(weeklyMessageTemplate);

                    /* 當天不計算 所以去除0、1索引值 */
                    for (int count=2; count<14; count++){

                        WeeklyWeather weeklyWeather = weeklyWeathers.get(count);

                        // 修改標題、時間
                        context.set("$['contents'][" + (count-2) + "]['body']['contents'][0]['text']", "氣象預報：" + weeklyWeather.getStartTime());// 修改城市、鄉鎮
                        // 修改城市、鄉鎮
                        context.set("$['contents'][" + (count-2) + "]['body']['contents'][1]['text']", user.getCity() + " - " + user.getTown());// 修改城市、鄉鎮
                        // 修改天氣狀況
                        context.set("$['contents'][" + (count-2) + "]['body']['contents'][2]['text']", "天氣狀況：" + weeklyWeather.getWeather());
                        // 修改最高溫度
                        context.set("$['contents'][" + (count-2) + "]['body']['contents'][4]['contents'][0]['contents'][1]['text']", weeklyWeather.getMaxTemp()+"°C");
                        // 修改最低溫度
                        context.set("$['contents'][" + (count-2) + "]['body']['contents'][4]['contents'][1]['contents'][1]['text']", weeklyWeather.getMinTemp()+"°C");
                        // 修改最高體感溫度
                        context.set("$['contents'][" + (count-2) + "]['body']['contents'][4]['contents'][2]['contents'][1]['text']", weeklyWeather.getMaxBodyTemp()+"°C");
                        // 修改最低體感溫度
                        context.set("$['contents'][" + (count-2) + "]['body']['contents'][4]['contents'][3]['contents'][1]['text']", weeklyWeather.getMinBodyTemp()+"°C");
                        // 修改降雨機率(12h)
                        context.set("$['contents'][" + (count-2) + "]['body']['contents'][4]['contents'][4]['contents'][1]['text']", weeklyWeather.getPop12h());
                        // 修改風級
                        context.set("$['contents'][" + (count-2) + "]['body']['contents'][4]['contents'][6]['contents'][1]['text']", weeklyWeather.getWindLevel());
                        // 修改風向
                        context.set("$['contents'][" + (count-2) + "]['body']['contents'][4]['contents'][7]['contents'][1]['text']", weeklyWeather.getWindDirection());
                        // 修改濕度
                        context.set("$['contents'][" + (count-2) + "]['body']['contents'][4]['contents'][8]['contents'][1]['text']", weeklyWeather.getHumidity());

                    }
                    ObjectMapper objectMapper = new ObjectMapper();
                    ObjectReader objectReader = objectMapper.readerFor(FlexContainer.class);
                    String jsonString = objectMapper.writeValueAsString(context.json());

                    FlexContainer flexContainer = objectReader.readValue(jsonString);
                    FlexMessage flexMessage = new FlexMessage("近一週天氣預報", flexContainer);
                    ReplyMessage replyMessage = new ReplyMessage(replyToken, flexMessage);
                    lineMessagingClient.replyMessage(replyMessage);

                }else if(message.startsWith("/report")){
                    return new TextMessage("您好，目前尚未開發紀錄問題回報與建議的功能，敬請期待");
                }
            } else {
                return new TextMessage("LineId 無法對應至資料庫的 UserId ！");
            }
        }
        return null;
    }
}
