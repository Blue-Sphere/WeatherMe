package sideproject.demo.weather;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

// import org.springframework.data.jpa.repository.Query;

@Repository
public interface WeatherRepository extends MongoRepository<User, String> {

    Optional<User> findByName(String name);

    Optional<User> findByEmail(String email);

    @Query("{ 'lineId' : { $ne : null }, 'notificationTime' : ?0 }")
    List<User> getUsersWhoNeedToBeNotified (String timeNow);

}

@org.springframework.context.annotation.Configuration
class JsonFile {
    @Bean(name = "getTwoDaysWeatherIdSurface")
    public Object getTwoDaysWeatherIdSurface() {
        try {
            InputStream jsonFile = new ClassPathResource("json/TwoDaysWeatherIdComparison.json").getInputStream();
            String jsonString = IOUtils.toString(jsonFile, StandardCharsets.UTF_8);

            Object document = Configuration.defaultConfiguration().jsonProvider().parse(jsonString);
            return document;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Bean(name = "getWeeklyWeatherIdSurface")
    public Object getWeeklyWeatherIdSurface() {
        try {
            InputStream jsonFile = new ClassPathResource("json/WeeklyWeatherIdComparison.json").getInputStream();
            String jsonString = IOUtils.toString(jsonFile, StandardCharsets.UTF_8);

            Object document = Configuration.defaultConfiguration().jsonProvider().parse(jsonString);
            return document;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Bean(name = "getTownIdSurface")
    public Object getTownIdSurface() {
        try {

            InputStream jsonFile = new ClassPathResource("json/TownIdComparison.json").getInputStream();
            String jsonString = IOUtils.toString(jsonFile, StandardCharsets.UTF_8);
            Object document = Configuration.defaultConfiguration().jsonProvider().parse(jsonString);
            return document;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Bean(name = "getLineBotPushMessageTemplate")
    public Object getLineBotPushMessageTemplate() {
        try {
            InputStream jsonFile = new ClassPathResource("json/LineBotPushMessageTemplate.json").getInputStream();
            String jsonString = IOUtils.toString(jsonFile, StandardCharsets.UTF_8);

            Object document = Configuration.defaultConfiguration().jsonProvider().parse(jsonString);
            return document;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}

@org.springframework.context.annotation.Configuration
class Secret{

    @Bean(name = "getCwbAuthorizationCode")
    public String getAuthorizationCode(){
        try{
            InputStream jsonFile = new ClassPathResource("secrets/CwbAuthorization.json").getInputStream();
            String document = IOUtils.toString(jsonFile, StandardCharsets.UTF_8);
            return JsonPath.read(document, "$.authorizationCode");
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

}

@Component
class Mailer {

    private final String sender, password;

    @Autowired
    public Mailer() throws Exception {
        InputStream jsonFile = new ClassPathResource("secrets/GmailAccount.json").getInputStream();
        String document = IOUtils.toString(jsonFile, StandardCharsets.UTF_8);
        this.sender = JsonPath.read(document, "$.account");
        this.password = JsonPath.read(document, "$.password");
    }

    public void sendEmail(String receiver, String sub, String content){

        String host = "smtp.gmail.com";
        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.port", "587");
        properties.setProperty("mail.smtp.starttls.enable", "true");

        Session session = Session.getDefaultInstance(properties);

        try{
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(sender));
            message.addRecipient(RecipientType.TO, new InternetAddress(receiver));
            message.setSubject(sub);
            message.setText(content);

            Transport transport = session.getTransport("smtp");
            transport.connect(host, sender, password);

            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        }catch (MessagingException e){
            e.printStackTrace();
        }

    }
}