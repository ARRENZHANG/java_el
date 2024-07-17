package az.el;

import java.io.IOException;
import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * evaluate a simple <b>expression</b> and export the result to caller.
 * <br>
 * the <b>expressions</b> can be specified like this, for example:
 * <pre>
        "java.lang.System.out.print#string('hello');System.out.println('world')"
        "java.lang.Math.max#int(1,2)"
        "com.some.Class.prop='word'"
        "com.some.Class.method#string#int('a',1,?)"
        "var=com.SomeClass.new#string('jacky');var.sayHello('iAmLucy')"
        "2*Math.max#int(5*Runtime.getRuntime().availableProcessors(),8*3)+3*4-1"
        "iif(a>b,c,com.SomeClass.doSomething('x'));"
        "var=array('a','b','c');each(var,'System.out.println#string($)');"
        "foreach(map('a',1,'b',2))"
 * </pre>
 * you can invoke the class like : <br><code>
 *      int n = EL.eval("var=1;java.lang.Math.max#int(var,5*java.lang.Runtime.getRuntime().availableProcessors())");
 * </code>
 * @author arren
 */
public class EL 
{
    public static interface LineReader{
        int size();
        String readLine() throws IOException;
    }
    
    private EL()
    {}    
    
    public static int MAX_SENTENCE_LENGTH    = 168;
    public static int MAX_BRACKETS_DEPTH     = 5;
    public static int MAX_OP_COUNT           = 9;
    public static int MAX_VARS_IN_DEPTH      = 9;
    
    public static final String VAR_NAME_CONTEXT = "context";
    
    /***
     * to set classes-blacklist in context, set value for this variable.
     * value can be : "com.x.Class,com.y.Class" or "com.x.*,com.y.*".
     */
    public static final String VAR_NAME_CLASS_BLACKLIST = "$class_blacklist";
    public static final String VAR_NAME_CLASS_WHITELIST = "$class_whitelist";
    
    
    public static final Object NULL = new Object();
    public static final Object VOID = new Object();
    
    static final Pattern PATTERN_LINE_SPLITER = Pattern.compile("[\n;]");
    
    
    @SuppressWarnings("unchecked")
    public static <T> T evalNoError( final String el, T def, Class<T> required )
    {
        return (T)Formula.convert(evalNoError(el,def), required);
    }
    public static <T> T evalNoError( final String el, T def )
    {
        try{
            return eval( null, PATTERN_LINE_SPLITER.split(el) );
        }
        catch(ReflectiveOperationException|IOException|ParseException|RuntimeException ex){
            return def;
        }
    }
    
    
    @SuppressWarnings("unchecked")
    public static <T> T eval( final String el, Class<T> required )
        throws ReflectiveOperationException, IOException, ParseException
    {
        return (T)Formula.convert(eval(null,PATTERN_LINE_SPLITER.split(el)), required);
    }
    public static <T> T eval( final String el )
        throws ReflectiveOperationException, IOException, ParseException
    {
        return eval( null, PATTERN_LINE_SPLITER.split(el) );
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T eval( final String[] els, Class<T> required )
        throws ReflectiveOperationException, IOException, ParseException
    {
        return (T)Formula.convert(eval(null,els), required);
    }
    public static <T> T eval( final String[] els )
        throws ReflectiveOperationException, IOException, ParseException
    {
        return eval( null, els );
    }
    
    
    @SuppressWarnings("unchecked")
    public static <T> T eval( final Map<String,Object> context, final String el, Class<T> required )
        throws ReflectiveOperationException, IOException, ParseException
    {
        return (T)Formula.convert(eval(context,PATTERN_LINE_SPLITER.split(el)), required);
    }
    public static <T> T eval( final Map<String,Object> context, final String el )
        throws ReflectiveOperationException, IOException, ParseException
    {
        return eval( context, PATTERN_LINE_SPLITER.split(el) );
    }
    

    @SuppressWarnings("unchecked")
    public static <T> T eval( final Map<String,Object> context, final String[] els, Class<T> required)
        throws ReflectiveOperationException, IOException, ParseException
    {
        return (T)Formula.convert(eval(context, els), required);
    }

    
    public static <T> T eval( final Map<String,Object> eval_context, final String[] exps)
        throws ReflectiveOperationException, IOException, ParseException
    {
        return eval( eval_context, 0, new ExpressionArrayReader(exps) );
    }
    
    
    static class ExpressionArrayReader implements LineReader
    {
        final String[] items;
        volatile int i = 0;
        
        ExpressionArrayReader(String[] array){
            this.items = array;
        }
        ExpressionArrayReader(String array){
            this.items = PATTERN_LINE_SPLITER.split(array);
        }
        
        public ExpressionArrayReader reset(){
            i = 0;
            return this;
        }

        @Override
        public int size(){
            return items.length;
        }

        @Override
        public String readLine() throws IOException {
            int k = i++;
            return k>=items.length ? null : items[k];
        }
    }
    /* end : class StringArrayReader */
    
    public static <T> T eval( final Map<String,Object> eval_context, int depth, final LineReader reader)
        throws ReflectiveOperationException, IOException, ParseException
    {
        return eval( eval_context, depth, reader, false );
    }

    
    @SuppressWarnings("unchecked")
    public static <T> T eval( final Map<String,Object> eval_context, int depth, final LineReader reader, boolean suppress_errors)
        throws ReflectiveOperationException, IOException, ParseException
    {
        Map<String,Object> context = eval_context!=null ? eval_context : new HashMap<>(reader.size()*4*4/3);
             
        int[] 
                brackets     = new int[MAX_BRACKETS_DEPTH], 
                commas       = new int[MAX_BRACKETS_DEPTH],
                ops          = new int[MAX_BRACKETS_DEPTH],
                funcs        = new int[MAX_BRACKETS_DEPTH]
                ;
        Scanner.Job[][] jobs = Scanner.Job.newArray(MAX_BRACKETS_DEPTH+1,MAX_OP_COUNT);
                
        StringBuilder builder = new StringBuilder( MAX_SENTENCE_LENGTH );        
        Object o, result = null;
        
        for(String item = reader.readLine(); item != null; item = reader.readLine())
        {
            if((item=Help.trim(item)).isEmpty() || Formula.PATTERN_NULL.matcher(item).matches() )
                continue;   
            
        // just defined a complex string;
        // if a string contains special characters, like : +-*/%&|^!<>=
        // you should defined it alone; and then reference it with another variable, using "string=?" will
        // make you access the string, like "System.out.println#string(?string)" .
        //
            if( item.length()>1 && item.charAt(0)=='\'' && item.charAt(item.length()-1)=='\'' )
            {/*
              * just defined a complex string ?
              ****/
                context.put( "?", item.substring(1,item.length()-1) );
                continue;
            }
        // a complex expression, need to be processed.
        //
            builder.ensureCapacity( item.length()+8 );
            builder.setLength( 0 );
            builder.append( item );
            
            try{
                o = Scanner.scan(context,depth,builder,brackets,commas,ops,jobs,funcs);
            }
            catch(ReflectiveOperationException|IOException|ParseException|RuntimeException ex){
                if(!suppress_errors ) throw ex; else
                {
                    o = VOID;
                    System.err.println(ex.getMessage());
                }
            }
            if( VOID!=o ){
                result = o;
                context.put( "?", o );
            }
        }
        return (T)result;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T convert(Object obj, Class<T> need){
        return (T)Formula.convert(obj, need);
    }
    
    
    ///-----------------------------------------------------------------------------
    ///
    /// method can be used in java-expression
    ///
    ///-----------------------------------------------------------------------------
    
    /***
     * can be used in an expression.<br>
     * for example, an expression like <code>"iif(?v>?x*2,?v,?x)"</code>
     * will return you a value like : <code>v>x*2 ? v : x</code>.
     * 
     * @param condition true or false.
     * @param if_true be returned if condition is true.
     * @param or_else be returned when condition is false.
     * @param context
     * @return the result.
     * @throws java.lang.ReflectiveOperationException
     * @throws java.io.IOException
     * @throws java.text.ParseException
     */
    public static Object iif(Object condition, Object if_true, Object or_else, Map<String,Object> context)
        throws ReflectiveOperationException, IOException, ParseException
    {
        boolean yes = condition==null 
                ? false
                :!Routine.class.isInstance(condition)
                ? Boolean.parseBoolean(condition.toString())
                : Boolean.parseBoolean(Routine.class.cast(condition).run(context).toString())
                ;
        if( yes ){
            return if_true instanceof Routine ? Routine.class.cast(if_true).run(context) : if_true;
        }
        else{
            return or_else instanceof Routine ? Routine.class.cast(or_else).run(context) : or_else;
        }
    }
    
    public static Object each(Object items, String exp, Map<String,Object> context) 
        throws ReflectiveOperationException, IOException, ParseException
    {
        if( items==null )
            throw new java.lang.IllegalArgumentException("items");
        if( exp==null || exp.isEmpty() )
            exp= items instanceof Map ? "System.out.println($.getKey()+'='+$.getValue())" : "System.out.println($)";
        
        Object result = null;
        ExpressionArrayReader processor = new ExpressionArrayReader(exp);
        
        if( items instanceof Iterable )
            result = forEach_iterator( context, Iterable.class.cast(items).iterator(), processor );
        else if( items instanceof Iterator )
            result = forEach_iterator( context, Iterator.class.cast(items), processor );
        else if( items.getClass().isArray() )
            result = forEach_iterator( context, new ArrayIterator(items), processor );
        else if( items instanceof Map )
            result = forEach_iterator( context, Map.class.cast(items).entrySet().iterator(), processor );
        else{
            throw new java.lang.IllegalArgumentException("NotArrayListObject_"+items.getClass());
        }
        return result;
    }
    
    private static Object forEach_iterator(Map<String,Object> context, Iterator<?> iter, ExpressionArrayReader reader) 
        throws ReflectiveOperationException, IOException, ParseException
    {        
        Object result = null, o;
        
        while( iter.hasNext() ){
            context.put("$", iter.next()); 
            if( NULL!=(o=EL.eval(context, 1, reader.reset())) ) result = o; else break;
        }
        context.remove( "$" );
        return result;
    }
    
    static class ArrayIterator implements Iterator<Object>{
        final Object array;
        final int length; int i = 0;
        
        ArrayIterator(Object array){
            this.array = array;
            this.length = Array.getLength(array);
        }
        @Override
        public boolean hasNext() {
            return i<length;
        }

        @Override
        public Object next() {
            return Array.get(array, i++);
        }
    }
    /* end : class ArrayIterator */
    
    /***
     * make it possible to delay the evaluation of an expression,
     * especially for "iif()" method.
     */
    static class Routine
    {
        final String exp;
        
        public Routine(String exp){
            this.exp = exp;
        }
        public Object run(Map<String,Object> context) 
            throws ReflectiveOperationException, IOException, ParseException
        {
            ExpressionArrayReader processor = new ExpressionArrayReader(exp);
            return eval(context,2,processor.reset());
        }
    }
    /* end : class Routine */



    public static void main(String[] args){
        try{
            Main.entry( args );
        }
        catch(ReflectiveOperationException| IOException|ParseException|RuntimeException ex)
        {
            System.err.println("error :\n");
            ex.printStackTrace( System.err );
        }
    }
}