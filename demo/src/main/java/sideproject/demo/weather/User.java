package sideproject.demo.weather;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {
    
    @Id
    private String id;

    private String email;

    private String name;

    private String password;

    private String city;

    private String town;

    private String obsId;

    private String lineId;

    private String notificationTime;

    private String verificationCode;

    public User(){
        
    }

    public User(String name, String city, String town, String lineId, String notificationTime){
        this.name = name;
        this.city = city;
        this.town = town;
        this.lineId = lineId;
        this.notificationTime = notificationTime;
    }

    public static void initNewUser(User newUser){
        newUser.setCity("臺北市");
        newUser.setTown("中正區");
        newUser.setNotificationTime("22:00");
        newUser.setObsId("C0A770");
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCity() {
        return this.city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getTown() {
        return this.town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getObsId() {
        return this.obsId;
    }

    public void setObsId(String obsId) {
        this.obsId = obsId;
    }

    public String getLineId() {
        return this.lineId;
    }

    public void setLineId(String lineId) {
        this.lineId = lineId;
    }

    public String getNotificationTime() {
        return this.notificationTime;
    }

    public void setNotificationTime(String notificationTime) {
        this.notificationTime = notificationTime;
    }

    public String getVerificationCode() {
        return this.verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

}

