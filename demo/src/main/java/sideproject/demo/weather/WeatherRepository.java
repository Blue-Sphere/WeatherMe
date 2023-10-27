package sideproject.demo.weather;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.util.ResourceUtils;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Optional;
import java.util.Set;
import java.util.Base64;

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
import com.google.api.services.gmail.model.Message;
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
import java.util.Collections;
import java.util.List;

import java.util.Properties;
import javax.mail.Session;
import javax.mail.Message.RecipientType;
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
class GMailer {

    private static final String SENDER = "a6109z@gmail.com";

    private final Gmail service;

    private NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

    private JsonFactory jsonFactory = GsonFactory.getDefaultInstance();


    public boolean credentialsExpired(AccessToken accessToken) {
        Instant expirationTime = accessToken.getExpirationTime().toInstant();
        long expirationTimeMillis = expirationTime.toEpochMilli();
        long currrentTimeMillis = System.currentTimeMillis();
        return expirationTimeMillis < currrentTimeMillis;
    }

    @Autowired
    public GMailer() throws Exception {
        service = new Gmail.Builder(httpTransport, jsonFactory, getCredential())
                .setApplicationName("Mailer")
                .build();
    }
    private Credential getCredential() throws IOException{ //Files.readAllBytes(Paths.get(jsonFile.getAbsolutePath()     

        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:secrets/client_secret.json");
        GoogleClientSecrets clientSecret = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(resource.getInputStream()));

        // final List<String> scopes = Collections.singletonList(GmailScopes.GMAIL_LABELS);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            httpTransport, jsonFactory, clientSecret, Set.of(GmailScopes.GMAIL_SEND))
            .setDataStoreFactory(new FileDataStoreFactory(Paths.get("tokens").toFile()))
            .setAccessType("offline")
            .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        // Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        // if(credential.getExpiresInSeconds() <= System.currentTimeMillis() / 1000){
        //     credential.refreshToken();    
        // }

        // return credential;
    }
    

    public void sendMail(String reciver, String subject, String message) throws Exception {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(SENDER));
        email.setRecipient(RecipientType.TO, new InternetAddress(reciver));
        email.setSubject(subject);
        email.setText(message);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] rawMessageBytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().withoutPadding().encodeToString(rawMessageBytes);
        Message msg = new Message();
        msg.setRaw(encodedEmail);

        try {
            msg = service.users().messages().send("me", msg).execute();
            System.out.println("Message id: " + msg.getId());
            System.out.println(msg.toPrettyString());
        } catch (GoogleJsonResponseException e) {
            GoogleJsonError error = e.getDetails();
            if (error.getCode() == 403) {
                System.err.println("Unable to send message: " + e.getDetails());
            } else {
                throw e;
            }
        }
    }

}