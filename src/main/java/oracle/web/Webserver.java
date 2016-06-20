package oracle.web;


import http.ResponseBuilder;
import http.SimpleHTTPServer;

import oracle.Oracle;
import oracle.Settings;
import processing.core.PApplet;
import processing.data.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;


/**
 * Created by raminsoleymani on 09/04/16.
 */
public class Webserver {

    SimpleHTTPServer server;
    OracleWebsocketServer websocketServer;
    Oracle Pparent;


    public Webserver(Oracle parent) {
        server = new SimpleHTTPServer(parent);
        this.Pparent = parent;
    }

    public void sendTexts(String input,String result,int timout) {
        websocketServer.sendTexts(input,result,timout);
    }


    class SessionLog extends ResponseBuilder {

        public  String getResponse(String requestBody) {
            JSONObject json = new JSONObject();
            StringBuilder sb = new StringBuilder();
            try (Stream<String> stream = Files.lines(Paths.get(Pparent.sketchPath()+"/session.log"))) {
                stream.forEach(sb::append);
            } catch (IOException e) {
                e.printStackTrace();
            }
            json.setString("text", sb.toString());
            return json.toString();
        }
    }


    public void webSocketServerEvent(String msg){
        websocketServer.webSocketServerEvent(msg);
    }
}
