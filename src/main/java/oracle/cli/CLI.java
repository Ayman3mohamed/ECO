package oracle.cli;


import oracle.Oracle;
import oracle.Settings;
import oracle.ostern.Jesus;
import oracle.web.OracleWebsocketServer;
import processing.core.PApplet;
import processing.core.PFont;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by mrzl on 31.03.2016.
 */
public class CLI{
    public static final String LINE_PREFIX_CHARS = "[ ] ";
    private static final int PREF_WORD_LEN = LINE_PREFIX_CHARS.split( " " ).length;
    private float LINE_PREFIX_WIDTH;

    private ArrayList< Line > lines = new ArrayList<>();
    private ArrayList< Line > hiddenLines = new ArrayList<>(); // for edge case of endless writer...
    private Oracle parent;

    private PFont font;

    public enum CliState{
        USER_INPUT,
        ORACLE_THINKING,
        ORACLE_TYPING
    }

    public CliState state = CliState.USER_INPUT;

    private Jesus jesus;
    private BlinkingRectangle blinker;
    private DelayedTyper delayedTyper;

    private int currentY;

    BoxValues margin,padding;
    protected int textSize;
    protected int lineHeight;
    int maximumNumberOfLines;

    public CLI( Oracle p ) {

        this.parent = p;
        this.font = p.createFont( "data" + File.separator + "Glass_TTY_VT220.ttf", Settings.CLI_TEXT_SIZE );
        this.parent.textFont( this.font );


        this.margin = new BoxValues(Settings.CLI_MARGIN_TOP,Settings.CLI_MARGIN_BOTTOM,
                Settings.CLI_MARGIN_LEFT,Settings.CLI_MARGIN_RIGHT, p.width, p.height);
        this.padding = new BoxValues(Settings.CLI_PADDING_TOP,Settings.CLI_PADDING_BOTTOM,
                Settings.CLI_PADDING_LEFT,Settings.CLI_PADDING_RIGHT, margin.width, margin.height);

        jesus = new Jesus( p );
        blinker = new BlinkingRectangle( p, this );
        delayedTyper = new DelayedTyper( this );

        lineHeight = Settings.CLI_LINE_HEIGTH;
        textSize = Settings.CLI_TEXT_SIZE;
        parent.textSize(textSize);

        LINE_PREFIX_WIDTH = getTextWidth(LINE_PREFIX_CHARS);
        setupWidth();
        maximumNumberOfLines = (int)padding.height / Settings.CLI_LINE_HEIGTH;
        reset();
    }

    public void setupWidth() {
        // limit for the input line
        String widthTestString = "";
        do {
            widthTestString += "a";
        } while ( parent.textWidth( widthTestString ) < padding.width);
        Line.CHAR_LIMIT = widthTestString.length();
       // System.out.println("debug: Line Limit: "+Line.CHAR_LIMIT);
    }


    public String getUserInput() {
        int lineIndex = lines.size() - 1;
        Line line = lines.get(lineIndex);
        String userInput = "";
        while(line.lineType == Line.LineType.USER_LINE) {
            userInput = line.getText() + userInput;
            lineIndex--;
            if(lineIndex >= 0) {
                line = lines.get(0);
            }
            else { // only for the edges case of an endless writer...
                line = hiddenLines.get(hiddenLines.size() + lineIndex);
            }
        }
        // USER_START line
        userInput = line.getText() + userInput;
        return userInput;
    }


    public void draw() {
        parent.fill( 0, 255, 0 );
        parent.pushMatrix();
        parent.translate(margin.left,margin.top);

        // BORDER
        parent.pushStyle();
        parent.noFill();
        parent.stroke( 0, 255, 0 );
        parent.strokeWeight( 5 );
        parent.rect( 0,0, margin.width,margin.height );
        parent.popStyle();

        jesus.drawBeforeEaster();

        pushLinesUp();
        parent.pushMatrix();
        parent.translate(padding.left, padding.top);

        // test draw of the padding
        //parent.pushStyle();
        //parent.noFill();
        //parent.stroke( 255, 0 , 0 );
        //parent.strokeWeight( 1 );
        //parent.rect( 0,0, padding.width, padding.height );
        //parent.popStyle();

        for(int li = 0; li < lines.size(); li++){
            parent.pushMatrix();
            parent.translate(0,(li + 1) * lineHeight);
            Line line = lines.get(li);
            if(line.lineType != Line.LineType.USER_START)
                parent.translate(LINE_PREFIX_WIDTH,0);
            line.draw();
            if(li == lines.size() - 1)
                blinker.draw( delayedTyper.isWaiting() );
            parent.popMatrix();
        }

        // types the text in delayed manner
        try {
            if( delayedTyper.update() ){
                newLine( Line.LineType.USER_START );
            }
            state = delayedTyper.getState();
        } catch (NullPointerException e ){
            // hacky, but i dont understand the problem.
            delayedTyper = new DelayedTyper( this );
            reset();
        }

        //System.out.println(state.name());

        parent.popMatrix();
        parent.popMatrix();
        jesus.drawAfterEaster();
    }

    private void pushLinesUp() {
        if(lines.size() >= maximumNumberOfLines) {
            Line removed = lines.remove(0);
            if(state == CliState.USER_INPUT) {
                hiddenLines.add(removed);
            }
            return;
        }
    }


    public void backspace() {
        if( state == CliState.USER_INPUT ) {
            // should be true anyway, since cli.isActive() is in the oracle keypressed
            if(getLastLine().backspace()) {
               if(getLastLine().lineType == Line.LineType.USER_LINE) {
                   lines.remove(lines.size() - 1);
               }
            }
            //wordLineSplit();  didnt work really... yet
            OracleWebsocketServer.sendTypingBackspace();
        }
    }

    public long finish( String answer ) {
        newLine(Line.LineType.BOT_LINE);
        delayedTyper.addText( answer );
        int words = answer.split( " " ).length;
        long delayMillis = calculateDelayByResponseWordCount( words );
        delayedTyper.startTimout( delayMillis );
        state = CliState.ORACLE_THINKING;
        return delayMillis;
    }

    public void finishHack(){
        newLine(Line.LineType.BOT_LINE);
        delayedTyper.addText( "*yawn*" );
        delayedTyper.startTimout( 15000 );
        state = CliState.ORACLE_THINKING;
    }

    public long calculateDelayByResponseWordCount( int length ) {
        int inputDelayMaxWords = 30;
        return ( int ) PApplet.map( PApplet.min( length, inputDelayMaxWords ), 1, inputDelayMaxWords, Settings.MIN_ANSWER_DELAY_COUNT, Settings.MAX_ANSWER_DELAY_COUNT );
    }

    public void type( char c ) {
        if( state == CliState.USER_INPUT ){
            // should be true anyway, since cli.isActive() is in the oracle keypressed
            delayedTyper.type( c );
            OracleWebsocketServer.sendTyping( c );
        }
    }

    public void newLine() {
        if(state == CliState.USER_INPUT)
            newLine(Line.LineType.USER_LINE);
        else
            newLine(Line.LineType.BOT_LINE);
    }


    public void newLine( Line.LineType type ) {
        //currentY += Settings.CLI_LINE_HEIGTH;
        Line newLine = new Line( this.parent, type );
        lines.add( newLine );
        if( type == Line.LineType.USER_START){
            for(Character c : LINE_PREFIX_CHARS.toCharArray())
                delayedTyper.type(c);
        }
        if(type == Line.LineType.USER_START) {
            hiddenLines.clear();
        }
    }

    public Line getLastLine() {
        return lines.get( lines.size() - 1 );
    }

    public void reset() {
        lines.clear();
        newLine(Line.LineType.USER_START);
    }

    public boolean available() {
        return getUserInput().split( " " ).length >
                PREF_WORD_LEN && delayedTyper.isEmpty();
    }

    public void startEmojiEasterEgg() {
        jesus.start( Jesus.EASTEREGG_TYPE.EMOJI, 10 );
    }

    public int getTextWidth( String text ) {
        return ( int ) parent.textWidth( text );
    }

    public int getTextSize() {
        return Settings.CLI_TEXT_SIZE;
    }

    public boolean isActive() {
        return !delayedTyper.isEmpty();
    }

    public boolean isReadyForInput() {
        return state == CliState.USER_INPUT;
    }

    public void suspendTyper( int millis ) {
        delayedTyper.addDelay( millis );
        System.out.println( "cli.suspendTyper: " + millis );
    }

    public boolean interceptTypeNow( String content ) {
        if( state == CliState.ORACLE_THINKING ){
            delayedTyper.typeNow( content );
            return true;
        } else {
            return false;
        }
    }

    public void wordLineSplit() {
        if(lines.size() == 1)
            return;
        Line last = lines.get(lines.size() - 1);
        Line sndLast = lines.get(lines.size() - 2);
        String text = sndLast.getText() + last.getText() ;
        String[] words = text.split(" ");
        //System.out.println(Arrays.toString(words));
        String txtFor2ndLast = "";
        String txtForLastLine = "";
        int w = 0;
        for(; w < words.length; w++) {
            if(txtFor2ndLast.length() + words[w].length() + 1 < sndLast.limit()){
                txtFor2ndLast += words[w] + " ";
            } else {
                // might be the bizarre edge case that somebody wants to be funny
                // not typing any spaces...
                if(w == 0 || w == 2 && sndLast.lineType == Line.LineType.USER_START) {
                    int firstPartLength = parent.min(words[w].length(),sndLast.limit());
                    String wordSplit1 = words[w].substring(0,firstPartLength);
                    String wordSplit2 = words[w].substring(firstPartLength);
                    txtFor2ndLast += wordSplit1;
                    txtForLastLine = wordSplit2;
                } else
                    break;
            }

        }
        for(; w < words.length; w++) {
            txtForLastLine += words[w] + (w < words.length - 1 ? " " : "");
        }
        sndLast.setText(txtFor2ndLast);
        last.setText(txtForLastLine);
    }

    public void typeNow() {
        delayedTyper.typeNow();
    }
}

