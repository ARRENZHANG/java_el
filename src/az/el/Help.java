package az.el;

/**
 *
 * @author arren
 */
final class Help 
{
    private Help()
    {}
    
    public static String fullClassName(String[] dots, int offset, int[] end)
    {
        String prefix;
        
        switch( dots[offset] )
        {
        case "java":
        case "javax":
        case "jdk":
        case "sun":
        case "oracle":
        case "com":
        case "org":
        case "az": /* ^-^ */
        default:
            prefix = null;
            break;
        case "System":
        case "Runtime":
        case "Thread":
        case "Math":
        case "String":
        case "Byte":
        case "Character":
        case "Short":
        case "Integer":
        case "Long":
        case "Float":
        case "Double":
        case "Number":
        case "Boolean":
            prefix = "java.lang.";
            break;
        case "Random":
        case "Calendar":
        case "Arrays":
        case "ArrayList":
        case "HashMap":
        case "Properties":
        case "Objects":
        case "Collections":
            prefix = "java.util.";
            break;
        case "Stream":
            prefix = "java.util.stream.";
            break;
        case "ThreadLocalRandom":
        case "Executors":
            prefix = "java.util.concurrent.";
            break;
        case "Pattern":
            prefix = "java.util.regex.";
            break;
        case "URL":
            prefix = "java.net.";
            break;
        case "HTTP":
            prefix = "az.net.";
            break;
        case "EL":
            prefix = "az.el.";
            break;
        }
        if( prefix!=null ){
            dots[offset] = prefix+dots[offset];
            end[0] = offset+1;
        }
        return 
            end[0]<=offset+1 /* not need to join elements ? */
                ? dots[offset]  
                : String.join(".",java.util.Arrays.copyOfRange(dots, offset, end[0]));
    }
    
    
    public static String trim(String expression){
        String s;
        return
            (s=expression.trim()).isEmpty() ||
            (s.charAt(s.length()-1)==';' && (s=s.substring(0,s.length()-1).trim()).isEmpty())
            ? s : s;
    }
    
}