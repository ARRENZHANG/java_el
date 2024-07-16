package az.el;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Arrays;


final class Main
{

    public static Object entry(String[] args)
        throws ReflectiveOperationException, IOException, ParseException
    {
        boolean use_args = false; String [] the_args = null;
        boolean use_file = false; String the_file = null;

        for(int n = args!=null ? args.length : 0, i=0; i<n; i++){
            switch(args[i])
            {
            case "-f":
            case "-file":
                use_file = true;
                the_file = i+1<n ? args[++i] : null;
                break;
            case "-s":
            case "-stream":
            case "-":
            case "-i":
            case "-in":
                break;
            case "-e":
            case "-eval":
            case "-c":
                use_args = true;
                the_args = i+1<n
                    ? Arrays.stream( Arrays.copyOfRange(args,i+1,n))
                        .flatMap(a->Arrays.stream(EL.PATTERN_LINE_SPLITER.split(a)))
                        .toArray( String[]::new )
                    : null
                    ;
                i = n;
                break;
            }
        }
        if( use_args ){
            return the_args==null ? null : az.el.EL.eval( null, the_args );
        }
        else if( use_file )
        {
            java.nio.file.Path path;
            if( the_file==null || !Files.exists(path=Paths.get(the_file)) || Files.size(path)<1 )
                return null;
            try(BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)){
                return az.el.EL.eval(null, 0, new FileLinesReader(reader));
            }
        }
        else{            
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8))){
                return az.el.EL.eval(null, 0, new FileLinesReader(reader), true);
            }
        }
    }

    static class FileLinesReader implements az.el.EL.LineReader
    {
        final BufferedReader reader;

        FileLinesReader(BufferedReader reader){
            this.reader = reader;
        }
        @Override
        public String readLine() throws IOException 
        {
            System.out.print(">");
            String line = reader.readLine();

            if( line!=null ){
                switch(line)
                {
                case "exit":
                case "quit":
                case "\\q":
                    line = null;
                    break;
                default:
                    break;
                }
            }
            return line;
        }

        @Override
        public int size() {
            return 256;
        }
    }
    /* end : class FileLinesReader */
}