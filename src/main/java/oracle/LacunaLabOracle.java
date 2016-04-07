package oracle;

import oracle.cli.CLI;
import processing.core.PApplet;

import java.awt.event.KeyEvent;
import java.util.logging.Logger;

/**
 * Created by mrzl on 31.03.2016.
 */
public class LacunaLabOracle extends PApplet {

    public static String EXPORT_FILENAME_PREFIX = "lacuna_markov_export-order";
    public static int MAX_INPUT_WORDS = 3;
    private CLI cli;
    private MarkovManager markov;

    Logger allnowingLogger = Logger.getLogger( "input" );

    public void settings () {
        size( 640, 480 );
        new OracleLogger();
        allnowingLogger.setUseParentHandlers( false );
        //fullScreen( );
    }

    public void setup () {
        cli = new CLI( this );
        cli.addEmojiEasterEgg( );

        markov = new MarkovManager( );

        //markov.save();
        markov.load( );

        noCursor( );
    }

    public void draw () {
        background( 0 );
        cli.draw( );
    }

    public void keyPressed () {
        if ( key == CODED ) {
            switch ( keyCode ) {
                case KeyEvent.VK_F1:
                    cli.reset( );
                    break;
            }
        } else {
            switch ( key ) {
                case BACKSPACE:
                    cli.backspace( );
                    break;
                case ENTER:
                    if ( !cli.available( ) ) {
                        cli.emptyInput();
                        return;
                    }
                    String inputWordsString = cli.getLastLine( ).getText( true );
                    String result = markov.getAnswer( inputWordsString );

                    allnowingLogger.severe( "u:::" + inputWordsString);
                    allnowingLogger.severe( "o:::" + result );

                    cli.finish( result );
                    break;
                case TAB:
                case DELETE:
                    break;
                case ESC:
                    cli.reset( );
                    break;
                default:
                    cli.type( key );
                    break;
            }
        }
    }
}
