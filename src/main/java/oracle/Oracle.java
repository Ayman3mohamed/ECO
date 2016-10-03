package oracle;

import gifAnimation.Gif;
import oracle.cli.CLI;
import oracle.gif.GifDisplayer;
import oracle.lyrik.Lyrik;
import oracle.markov.Markov;
import oracle.web.Webserver;
import processing.core.PApplet;


import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mrzl on 31.03.2016.
 * <p>
 * This is the main starting point for the Electronic Chaos Oracle (ECO)
 */

public class Oracle extends PApplet {
    public CLI cli;
    Webserver server;
    OracleLogger logger;
    public Settings settings;

    long millisLastInteraction; // this timer should be part of a egg

    Lyrik lyrik;
    Markov markov;
    GifDisplayer gif;
    boolean useLyrik = true; // use lyrik in general
    boolean askMarkov = !useLyrik; // switched on when input arrived. set false after asking
    boolean startWebserver = true;


    List<Gif> testGifs = new ArrayList<Gif>();
    private String inputText;

    String lastInputText;
    String[] lastResults;

    //GifDisplayer gifDisplayer;

    public static void main(String[] args) {
        PApplet.main( "oracle.Oracle" );
    }

    public void settings() {
        size(640 * 2, 480, P3D);
        logger = new OracleLogger();

        settings = new Settings();

        if( Settings.FULLSCREEN ){
            fullScreen( SPAN );
        }

        useLyrik = Settings.USE_LYRIK;
        // TODO connectivity check

        startWebserver = Settings.START_WEBSERVER;
        if (startWebserver) {
            server = new Webserver(this);
            Settings.printIps();
        }

        millisLastInteraction = System.currentTimeMillis();

    }

    public void setup() {
        cli = new CLI(this);
        lyrik = new Lyrik();
        markov = new Markov(this);
        startWebserver(Settings.START_WEBSERVER);
        useLyrik = Settings.USE_LYRIK; // TODO connectivity check
        gif = new GifDisplayer(this);

        //gifDisplayer.getGiyGifsAsnyc(new String[]{"dog","king"},4);


        // load anyway. switch off if memory is a problem
        // otherwise load async on demand for fast response
        //if (!useLyrik) {

        //}
        noCursor();
        imageMode(CENTER);
        millisLastInteraction = System.currentTimeMillis();
    }


    public void startWebserver(boolean startWS){
        if (startWS) {
            server = new Webserver(this);
            Settings.printIps();
        }
    }

    public void draw() {
        background( 0 );
        cli.draw();
        //println(frameCount);

        Optional<String[]> results = Optional.empty();
        if (useLyrik) {
            Lyrik.LyrikState lyrikState = lyrik.getState();
            if (lyrikState == Lyrik.LyrikState.DONE) {
                results = lyrik.getNewAnswer();
                lyrik.setState(Lyrik.LyrikState.IDLE);
                if (!results.isPresent()) {
                    askMarkov = true;
                    useLyrik = false; // switch lyrik off for now
                    System.out.println("Switching Lyrik off for now...");
                }
            }
        }
        if (askMarkov) {
            results = markov.askLocalMarkov(inputText);
            askMarkov = false;
            if(!results.isPresent()){
                System.err.println("even good old markov fail...");
            }
        }
        if(results.isPresent()){
            lastResults = results.get();
            processAnswer();
        }


        // TODO just a test. not loaded in setup...
        if (gif.getAsyncGifysAvailable()) {
            testGifs = gif.getAsyncGifys();
            testGifs.stream().forEach(Gif::play);
        }
        if (testGifs.size() > 0){
            int x = Settings.GIFY_X;
            int y = Settings.GIFY_Y;
            int w = Settings.GIFY_W;
            int h = Settings.GIFY_H;
            image( testGifs.get( ( frameCount / 5 ) % testGifs.size() ), x, y, w, h );
        }
        if (System.currentTimeMillis() > millisLastInteraction + Settings.CLI_RESET_DELAY_MILLIS) {
            cli.reset();
        }
    }


    public void keyPressed() {
        millisLastInteraction = System.currentTimeMillis();

        if (cli.isActive() || !cli.isReadyForInput()) {
            key = 0;
            return;
        }

        if (key == CODED) {
            switch (keyCode) {
                case KeyEvent.VK_F1:
                    cli.reset();
                    break;
            }
        } else {
            switch (key) {
                case BACKSPACE:
                    cli.backspace();
                    break;
                case ENTER:
                    if (!cli.available()) {
                        return;
                    }
                    processInput();
                    break;
                case TAB:
                case DELETE:
                    break;
                case ESC:
                    key = 0;
                    cli.reset();
                    break;
                default:
                    if (!cli.isActive()) {
                        cli.type(key);
                    }
                    break;
            }
        }
    }



    private void processInput() {
        inputText = cli.getUserInput().trim();
        cli.setState(CLI.CliState.ORACLE_WAITING);
        lastInputText = removeSpecialCharacters(inputText);
        println( lastInputText );

        String[] results;

        String[] textSplit = lastInputText.split("\\s+");
        gif.getGiyGifsAsnyc(textSplit,1);

        if (useLyrik) {
            lyrik.askLyrikAsync(lastInputText);
        } else { // good old local markov chain
            // just set the flag here so we gonna have it in the next draw
            askMarkov = true;
        }

        if (startWebserver) {
            //server.sendTexts(inputText, result, -1); // TODO why commented out. remove 3. parameter
        }

    }

    private void processAnswer() {

        String result = lastResults[0];
        String logResult = lastResults[1];

        logger.log(logger.USER, inputText);
        logger.log(logger.ORACLE, logResult);

        System.out.println("u:::" + inputText);
        System.out.println("o:::" + logResult);


        //long delayMillis = cli.finish(result);
        if (startWebserver) { // TODO that should come back...
            //server.sendTexts(inputText, result, delayMillis);
        }
        cli.finish(result);

        // jesus easter egg....

        //if( result.contains( "lacuna" ) ){
        //    cli.startEmojiEasterEgg();
        //}
        //} catch ( Exception e ) {
        //    e.printStackTrace();
        //    cli.finish( "oh", calculateDelayByResponseWordCount( inputWordsString.split( " " ).length ) );
        //}

    }

    /*

            cli.startThinkingState();
        String[] textSplit = text.split("\\s+");
        gif.getGiyGifsAsnyc(textSplit,1);

     */




    private String removeSpecialCharacters(String input) {
        Pattern p = Pattern.compile("\\W*");
        Matcher matcher = p.matcher(input);
        matcher.find();
        if (matcher.start() == 0) {
            return matcher.replaceFirst("");
        } else
            return input;
    }

    public void webSocketServerEvent(String msg) {
        server.webSocketServerEvent(msg);
    }

    public void intercept(String message) {
        if (cli.interceptTypeNow(message)) {
            logger.log(logger.INTERCEPTION, message);
        }
    }

}
