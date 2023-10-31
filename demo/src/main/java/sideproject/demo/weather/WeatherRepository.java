package sideproject.demo.weather;

import com.google.auth.http.HttpCredentialsAdapter;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.util.ResourceUtils;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Authentication;
import org.springframework.core.io.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.query.Param;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.internal.Path;
//Gmail API
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.auth.Credentials;
import com.google.auth.http.HttpTransportFactory;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.auth.oauth2.TokenVerifier;
import com.google.auth.oauth2.TokenVerifier.Builder;
import com.google.gson.Gson;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.Instant;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Message.RecipientType;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.Message.RecipientType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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