package edu.buffalo.cse.cse486586.groupmessenger1;

import java.io.Serializable;
import java.security.MessageDigestSpi;

/**
 * Created by ramesh on 2/19/16.
 */
public class Message implements Serializable {

    public String message;
    public String avd;
    public String fromPort;

    public Message(String message){
        this.message = message;
    }
    public Message(String message, String avd){
        this.message = message;
        this.avd = avd;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAvd() {
        return avd;
    }

    public void setAvd(String avd) {
        this.avd = avd;
    }

    public String getFromPort() {
        return fromPort;
    }

    public void setFromPort(String fromPort) {
        this.fromPort = fromPort;
    }
}
