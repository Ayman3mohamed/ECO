package oracle;

import oracle.markov.MarkovChain;
import oracle.markov.MarkovQueue;
import processing.core.PApplet;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by mrzl on 06.04.2016.
 */
public class MarkovManager extends ArrayList< MarkovChain > {

    private int maxAnswerLength = 150;

    public MarkovManager () {
    }

    public String getAnswer ( String input ) {
        String answer = "";

        input.replace( "?", "" )
                .replace( "!", "" )
                .replace( ".", "" )
                .replace( ",", "" )
                .replace( "#", "" )
                .replace( "�", "" )
                .replace( "$", "" )
                .replace( "%", "" )
                .replace( "&", "" )
                .replace( "/", "" )
                .replace( "(", "" )
                .replace( ")", "" )
                .replace( "=", "" )
                .replace( ";", "" )
                .replace( ":", "" )
                .replace( "_", "" )
                .toLowerCase( );
        String[] inputWords = input.split( " " );
        answer = check( inputWords );

        if ( answer.equals( "nothing" ) ) {
            String noAnswer = "we don't care about " + input;
            answer = noAnswer;
        }

        if ( answer.length( ) > maxAnswerLength ) {
            System.out.println( "Cropping text" );
            answer = answer.substring( 0, maxAnswerLength );
        }

        return answer;
    }

    String check ( String[] input ) {
        MarkovQueue queue = new MarkovQueue( input.length );
        if ( queue.getOrder( ) - 1 >= size( ) ) {
            input = Arrays.copyOfRange( input, 0, size( ) - 1 );
            queue = new MarkovQueue( input.length );
        }

        for ( String s : input ) {
            queue.addLast( s );
        }
        String result = get( queue.getOrder( ) - 1 ).generateSentence( queue );
        if ( result.equals( "nothing" ) ) {
            if ( input.length < 2 ) {
                return "nothing";
            }

            String[] subarray = Arrays.copyOfRange( input, 0, input.length - 1 );
            result = check( subarray );
        }
        return result;
    }

    public void save () {
        for ( int i = 1; i < LacunaLabOracle.MAX_INPUT_WORDS + 1; i++ ) {
            MarkovChain chain = new MarkovChain( i );
            chain.train( loadText( "lacuna_lab_texts.txt" ) );
            add( chain );
            System.out.println( "Training chain with order " + i );
        }

        for ( MarkovChain chain : this ) {
            String fileName = "lacuna_chain-" + chain.getOrder( ) + ".data";
            try {
                ObjectOutputStream obj_out = new ObjectOutputStream(
                        new FileOutputStream( fileName )
                );
                obj_out.writeObject( chain );
            } catch ( FileNotFoundException e ) {
                e.printStackTrace( );
            } catch ( IOException e ) {
                e.printStackTrace( );
            }
            System.out.println( "Saved markov chain to " + fileName );
        }
    }

    public void load () {
        try {
            for ( int i = 1; i < LacunaLabOracle.MAX_INPUT_WORDS + 1; i++ ) {
                String fileName = "lacuna_chain-" + i + ".data";
                FileInputStream f_in = new FileInputStream( fileName );
                ObjectInputStream obj_in = new ObjectInputStream( f_in );
                Object obj = obj_in.readObject( );

                if ( obj instanceof MarkovChain ) {
                    add( ( MarkovChain ) obj );
                }

                System.out.println( "Loaded from " + fileName + ". With order " + get( size( ) - 1 ).getOrder( ) );
            }
        } catch ( FileNotFoundException e ) {
            e.printStackTrace( );
        } catch ( ClassNotFoundException e ) {
            e.printStackTrace( );
        } catch ( IOException e ) {
            e.printStackTrace( );
        }
        System.out.println( "Loaded markov chain from " + LacunaLabOracle.EXPORT_FILENAME_PREFIX );
    }

    private String loadText ( String fileName ) {
        String completeText = "";
        try ( Stream< String > stream = Files.lines( Paths.get( fileName ) ) ) {

            String s = stream.collect( Collectors.joining( ) );
            completeText += s;

        } catch ( IOException e ) {
            e.printStackTrace( );
        }

        return completeText.toLowerCase( );
    }
}