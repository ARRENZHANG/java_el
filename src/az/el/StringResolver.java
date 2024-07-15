package az.el;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * helps formatting a string by resolving variables embedded in the string.
 * 
 * <p> 
 * for example, a string like "${user.home}/folder" will be resolved as "/home/user/folder".<br>
 * if a <b>java-expression</b> embedded in the string, 
 * it will be evaluated as well using <b>{@link az.el.EL}</b>,
 * for example,
 * <code>
 *    "${a=256;System.setProperty('tomcat.queue',a*2)}
 * </code>
 * will give you "tomcat.queue"==256*2==512.
 * </p>
 * @author arren
 */
public class StringResolver implements BiFunction<String,Object,String>
{
public static final StringResolver EMPTY = new StringResolver(new HashMap<>(1));


    protected final Map<String,String> map;

    public StringResolver(final Map<String,String> entries){
        this.map = entries;
    }

    @Override
    public String apply(String name, Object null_)
    {
        String s;
        return null!=(s=System.getProperty(name)) 
            ? s : null!=(s=System.getenv(name)) 
            ? s : null!=(s=map.get(name)) 
            ? s : az.el.EL.evalNoError(name,null,String.class);
    }

    public static void resolve(final Map<String,String> map, final StringResolver resolver ){
        map.entrySet().forEach(m->m.setValue(format(m.getValue(),resolver)));
    }
    public static String resolve(final String value, final StringResolver resolver ){
        return format( value, resolver );
    }
    public static String resolve(final String value, final Map<String,String> map ){
        return resolve(value, new StringResolver(map) );
    }

    
    public static final Pattern PATTERN_VARIABLES = Pattern.compile("\\$\\{([^}]+)\\}");
    
    public static String format(String s, final Properties data)
    {
        return format(s,(n,o)->data.getProperty(n),null,null);
    } 
    public static String format(String s, final Map<String,String> data)
    {
        return format(s,(n,o)->data.get(n),null,null);
    } 
    public static String format(String s, final BiFunction<String,Object,String> replace)
    {
        return format(s,replace,null,null);
    } 
    public static String format(String s, final BiFunction<String,Object,String> replace, final Object object)
    {
        return format(s,replace,object,null);
    } 
    
    public static String format(String s, final BiFunction<String,Object,String> replace, 
        final Object args, final StringBuilder output )
    {
        StringBuilder builder = output!=null && output.capacity()>=s.length()
                ? output
                : new StringBuilder(s.length()*4/3)
                ;        
        final Matcher m = PATTERN_VARIABLES.matcher(s);
        
        int i = 0;        
        while( m.find() )
        {
            int start = m.start(), end = m.end(); 
            String word = m.group(1); /* the variable which need to be replaced ! */
            String to = replace.apply(word,args);
            if( to!=null ){
                builder.append( s.substring(i,start) ).append( to );
            }
            else{
                builder.append( s.substring(i,end+1) );
            }
            i = end;
        }
        if( i<s.length()-1 ){
            builder.append( s.substring(i)); /* sub-string after the last "}" if any*/
        }
        
        return
            builder.toString();
    }    

}