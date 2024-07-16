package az.el;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * a formula is used to evaluate an expression, to give you the result of the expression.<br>
 * for example, you submit a expression like "1+2*3" and the result will be "7".
 *
 * @author arren
 */
final class Formula
{
    private final String expr;
    private Formula left, right;
    public  OP op = OP.NONE;
    private Object resolved;

    public Formula( int depth, String el ){
        this.expr = trim_brackets(el.trim());
    }
    
    public Formula( int depth, OP op, Formula left, Formula right ){
        this.expr = null;
        this.op = op;
        this.left = left;
        this.right = right;
    }
    
    
    @Override
    public String toString(){
        return (expr!=null ? expr : "") + (left!=null?left.toString():"") + op.operator + (right!=null?right.toString():"");
    }
    

    static final Pattern
            PATTERN_INT_LONG    = Pattern.compile("^[\\d]+[Ll]?$"),
            PATTERN_FLOAT_DBL   = Pattern.compile("^[\\d]+([\\.][\\d]*)?[Ff]?$"),
            PATTERN_BOOL        = Pattern.compile("^true|false$",Pattern.CASE_INSENSITIVE),
            PATTERN_NULL        = Pattern.compile("^null|undefined$",Pattern.CASE_INSENSITIVE)
            ;
    
    
    public Object process(final Map<String,Object> context)
    {
        Object o;
        
        switch( op )
        {
        case AND:
        case OR:
        case NOT:
        case GT:
        case GTE:
        case LT:
        case LTE:
        case EQ:
        case NEQ:
        case BIT_MOVE_R:
        case BIT_MOVE_L:
        case BIT_AND:
        case BIT_OR:
        case BIT_XOR:
        case BIT_NOT:
        case ADD:
        case MINUS:
        case MULTIPLY:
        case DIVIDE:
        case MOD:
            resolved = op.calculator.calculate( left.process(context), right.process(context) );
            break;
            
        case SELF_ADD_left: /* no left expression */
        case SELF_MINUS_left: /* no left expression */
            resolved = op.calculator.calculate( null, right.process(context) );
            evaluate( right.expr, true, resolved, context);
            break;
        case SELF_ADD_right: /* no right expression */
        case SELF_MINUS_right: /* no right expression */
            o = op.calculator.calculate( resolved = left.process(context), null );
            evaluate( left.expr, true, o, context);
            break;
            
        case ADD_TO:
        case MINUS_TO:
        case MULTIPLY_TO:
        case DIVIDE_TO:
        case MOD_TO:
        case OR_TO:
        case AND_TO:
        case XOR_TO:
            resolved = op.calculator.calculate( left.process(context), right.process(context) );
            evaluate( left.expr, true, resolved, context);
            break;

        case SET: /* set variable value, set field value */
            evaluate( left.expr, true, resolved = right.process(context), context );
            break;
            
        default: /* a simple value, or a field get, a method invoking */
            resolved = expr==null || expr.isEmpty() ? EL.VOID : evaluate( expr, false, null, context );
            break;
        }
        return resolved;
    }
    
    private Object evaluate(String el, boolean set_get, Object param, final Map<String,Object> context)
    {
        Object o;
        if( null!=(o=checkAndUseSimpleExpression(el))){
            return o;
        }
        try
        {
            int i = 0;
            boolean m = el.charAt(el.length()-1)==')' && (i=el.indexOf('('))>0; /* like : a.b.c.func(1,2) or a.b.c.field */
            String v, 
                    dots[] = (m ? el.substring(0,i) : el).split("\\."),
                    args[] = !m ? null : (v=el.substring(i+1,el.length()-1).trim()).isEmpty() ? null : v.split("[,]");
            Object[] objs = purify_args(args,context);
        return
            m && dots.length==1 
            ? parse_internal_functions(dots[0],objs,context)
            : evaluate( m,
                    dots, /* elements/dots, like : a.b.c.func or a.b.c.field */
                    objs, /* args, like : [1,2] ; or null for non-method */
                    set_get, param, context );
        }
        catch(ReflectiveOperationException|IOException|ParseException|RuntimeException ex){
            throw new IllegalArgumentException( toString(),ex );
        }
    }

    private static boolean is_string(String exp){
        return exp.length()>1 && exp.charAt(0)=='\'' && exp.charAt(exp.length()-1)=='\'';
    }

    
    private static Object checkAndUseSimpleExpression( String exp )
    {        
        if( is_string(exp) ){
            return exp.substring(1,exp.length()-1); 
        }
        if( PATTERN_NULL.matcher(exp).matches() )
            return null;
        if( PATTERN_BOOL.matcher(exp).matches() )
            return Boolean.valueOf(exp);
        if( PATTERN_INT_LONG.matcher(exp).matches() )
        {
            if( exp.charAt(exp.length()-1)=='L' || exp.charAt(exp.length()-1)=='l' )
                return Long.valueOf(exp.substring(0,exp.length()-1));
            else 
                return Integer.valueOf(exp);
        }
        if( PATTERN_FLOAT_DBL.matcher(exp).matches() )
        {
            if( exp.charAt(exp.length()-1)=='F' || exp.charAt(exp.length()-1)=='f' )
                return Float.valueOf(exp.substring(0,exp.length()-1));
            else
                return Float.valueOf(exp);
        }
        return null;
    }
    
    
    private static String trim_brackets(String s){
        return s!=null && s.length()>1 
            && s.charAt(0)=='(' && s.charAt(s.length()-1)==')' && s.indexOf('(',1)<0
            ?  s.substring(1,s.length()-1).trim() 
            :  s;
    }
    private static String trim_number_dot(String s){ /* "1.0" -> "1" */
        int i;
        return (i=s.indexOf('.'))>0 ? s.substring(0,i) : s;
    }

    private static Object[] purify_args(String[] args, final Map<String,Object> context)
    {
        if( args==null || args.length<1 || args[0].isEmpty() )
            return null;

        Object[] objs = new Object[args.length];
        Object o;
        
        for(int i=0;i<args.length;i++)
        {
            args[i] = args[i].trim();
            objs[i] = null!=(o=checkAndUseSimpleExpression(args[i])) ? o : get_variable(context,args[i]);
        }
        return objs;
    }
    
    @SuppressWarnings("unchecked")
    private static Object parse_internal_functions(String func, Object[] args, final Map<String,Object> context) 
            throws ReflectiveOperationException, IOException, ParseException
    {
        switch(func.toLowerCase()){
        case "array":
            return args;
        case "list":
            return Arrays.asList(args);
        case "map":
            return newHashMap(args);
        case "each":
        case "every":
        case "foreach":
            return EL.each(args[0], args[1].toString(), 
                args.length>2 && Map.class.isInstance(args[2]) ? (Map<String,Object>)args[2] : context);
        case "iif":
            return EL.iif(args[0], args[1], args[2], 
                args.length>3 && Map.class.isInstance(args[3]) ? (Map<String,Object>)args[3] : context);
        default:
            throw new IllegalArgumentException("InvalidInternalMethod_"+func);
        }
    }
    private static HashMap<Object,Object> newHashMap(Object[] args)
    {
        HashMap<Object,Object> map = new HashMap<>(args.length/2*4/3);
        
        for(int i=0;i<args.length-1;i+=2){
            map.put( args[i], args[i+1] );
        }
        return map;
    }
    
    /**
     * query the value of the specified variable.
     * 
     * @param context a collection which contains all variables.
     * @param var a string like "?a" or "?".
     * @return the variable value, or null.
     **/
    private static Object get_variable( final Map<String,Object> context, String var ){
        String name = var.charAt(0)=='?' && var.length()>1 ? var.substring(1) : var;
        return getv( context, name );
    }
    
    private static Object getv(final Map<String,Object> context, String refn)
    {
        int i; Object o;
        
        if( refn.charAt(refn.length()-1)==']' && (i=refn.indexOf('['))>0 )
        {
            o = context.get(refn.substring(0,i));
            i = Integer.parseInt(refn.substring(i+1,refn.length()-1));

            return o==null
                ? null
                : o.getClass().isArray() 
                ? Array.get(o,i)
                : o instanceof List ? List.class.cast(o).get(i) : throwError("InvalidArrayListIndex_"+refn)
                ;
        }
        return refn.equals(EL.VAR_NAME_CONTEXT) ? context : context.get(refn);
    }
    @SuppressWarnings("unchecked")
    private static Object setv(final Map<String,Object> context, String refn, Object param)
    {
        int i; Object o;
        if( refn.charAt(refn.length()-1)==']' && (i=refn.indexOf('['))>0 )
        {
            o = context.get(refn.substring(0,i));
            i = Integer.parseInt(refn.substring(i+1,refn.length()-1));

            if( o==null )
                throwError("InvalidArrayListReference_"+refn);
            else if( o.getClass().isArray() )
                Array.set(o,i,param);
            else if( o instanceof List )
                List.class.cast(o).set(i, param); /* type problem ? it was List<String,String> !!! */
            else
                throwError("InvalidArrayListIndex_"+refn);
        }
        else if(!refn.equals(EL.VAR_NAME_CONTEXT) ){
            context.put( refn, param );
        }
        return param;
    }

    private static <T> T throwError(String error){
        throw new IllegalArgumentException(error);
    }
    
    public final static Object convert(Object obj, Class<?> need)
    {
        switch( need.getSimpleName().toLowerCase())
        {
        case "string":
            return obj==null ? null : obj.toString();
        case "byte":
            return Byte.valueOf(obj==null ? "0" : trim_number_dot(obj.toString()));
        case "short":
            return Short.valueOf(obj==null ? "0" : trim_number_dot(obj.toString()));
        case "int":
        case "integer":
            return Integer.valueOf(obj==null ? "0" : trim_number_dot(obj.toString()));
        case "long":
            return Long.valueOf(obj==null ? "0" : trim_number_dot(obj.toString()));
        case "float":
            return Float.valueOf(obj==null ? "0" : obj.toString());
        case "double":
            return Double.valueOf(obj==null ? "0" : obj.toString());
        case "boolean":
            return Boolean.valueOf(obj==null ? "false" : obj.toString());
        default:
            return obj;
        }
    }
    
    private Object evaluate(boolean method, String[] dots, Object[] args, boolean set_get, Object param,
        final Map<String,Object> context)
        throws ReflectiveOperationException,IOException,ParseException
    {
        final String refn = (dots[0].length()>1 && dots[0].charAt(0)=='?' ? dots[0].substring(1) : dots[0]).trim();
        
        if( dots.length==1 ) /* set value as : "a=1", or get value as : "a" */
        {        
            return !set_get ? get_variable(context,refn) : EL.VAR_NAME_CONTEXT.equals(refn) ? param : setv(context,refn,param);
        }
        else if( method )
        {
            return eval_method( dots, get_variable(context,refn), dots[dots.length-1], args, context );
        }
        else{
            return eval_variable_field( dots, get_variable(context,refn), set_get, param, context );
        }
    }
    
    private static Class<?> findClassAndUseIt(String[] dots, int[] end, Map<String,Object> context)
    {
        int offset = 1; Class<?> c = null; String nm; Object o;
        
        String blacklist = null!=(o=context.get(EL.VAR_NAME_CLASS_BLACKLIST)) ? o.toString() : "";
        do{
            end[0] = dots.length-offset;
            nm = Help.fullClassName(dots,0,end);
            
    // we don't use a class if it is in the black-list !
    //
            if( blacklist!=null && (
                blacklist.contains(nm) || blacklist.contains(nm.substring(0,nm.lastIndexOf('.')+1)+"*")) )
            {
                throw new java.lang.SecurityException("ClassInBlacklist_"+nm);
            }
    // then we load the class, and use it as we expected.
            try{
                c = Class.forName( nm );
            }
            catch(ClassNotFoundException ex)
            {
                if( offset<dots.length-1 )
                    offset++;
                else 
                    throw new IllegalArgumentException("NoClass_"+dots[0],ex);
            }
        }
        while( c==null ); return c;
    }
    
    
    // set : x.Attr=b
    //
    private Object eval_variable_field(String[] dots, Object objo, boolean set_get, Object param,
        final Map<String,Object> context)
        throws ReflectiveOperationException
    {
        Class<?> claz; int prop; Field field;
        
        if( objo!=null )
        {
            prop = 1;
            claz = objo.getClass();
        }
        else /* no object in context ? might be using static field of a class */
        {
            int[] end = {1};
            claz = Objects.requireNonNull(
                findClassAndUseIt(dots,end,context), "InvalidClassName_"+dots[0] );
            prop = end[0];
        }
        
        for(;prop<dots.length-1;prop++)
        {
            field = claz.getField( dots[prop] );
            field.setAccessible( true );            
            claz = field.getType();
            objo = field.get( objo );
        }
        {
            field = claz.getField( dots[prop] );
            field.setAccessible( true );
        }
        if(!set_get )
            return field.get( objo );
        else
        {
            field.set( objo, param );
            return param;
        }
    }
    
    
    // invoke : x.Func(1,2,'a',?x,?y)
    //
    private Object eval_method(String[] dots, Object objo, String name, Object[] args, final Map<String,Object> context)
        throws ReflectiveOperationException,IOException,ParseException
    {
        Class<?> claz; int prop; Field field;
        
        if( objo!=null )
        {
            prop = 1;
            claz = objo.getClass();
        }
        else /* no object in context ? might be using static method of a class */
        {
            int[] end = {1};
            claz = Objects.requireNonNull(
                findClassAndUseIt(dots,end,context), "InvalidClassName_"+dots[0] );
            prop = end[0];
        }
        
        for(;prop<dots.length-1;prop++)
        {
            field = claz.getField( dots[prop] );
            field.setAccessible( true );
            claz = field.getType();
            objo = field.get( objo );
        }
        
        final RefBoolean void_return = new RefBoolean(false);
        
    // find out correct method with given name, and invoke it.
    //
        final Object o = el_search_method_and_exec(claz, objo, name, args, (Class<?>[] types, Object[] params)->
            {
                for(int i=0; i<params.length; i++){
                    params[i] = types[i].isInstance(args[i]) ? args[i] : convert(args[i],types[i]);
                }
                return params;
            }, 
            void_return, context )
            ; 
        return 
            void_return.value ? EL.VOID : o;
    }
        
    
    static final Pattern PATTERN_NEW_CTOR = Pattern.compile("new|ctor",Pattern.CASE_INSENSITIVE);
    
    static final String[] EMPTY_ARRAY = new String[0];
    
    
    @FunctionalInterface
    static interface BiFunction<T, U, R>
    {
        R apply(T t, U u) throws ReflectiveOperationException, IOException, ParseException;
    }


    private static String[] check_args_types( Object[] args )
    {
    final String[] types = new String[args.length];

        for(int i=0; i<args.length; i++)
        {
            types[i] = args[i]==null 
                ? String.class.getSimpleName().toLowerCase() 
                : args[i].getClass().getSimpleName().toLowerCase();
        }
        return types;
    }
    
    private static Object el_search_method_and_exec(Class<?> clz, Object ref, String name, Object[] args,
            final BiFunction<Class<?>[],Object[],Object[]> params,
            final RefBoolean void_return,
            final Map<String,Object> cache ) throws ReflectiveOperationException, IOException, ParseException
    {
        final String[] exact = name.split("#");
            for(int i=1; i<exact.length; i++) exact[i] = exact[i].toLowerCase();
            
        final int args_count = args!=null ? args.length : 0;
            
        final String[] types = exact.length>1 
                ? Arrays.copyOfRange(exact,1,exact.length) 
                : args_count>0 ? check_args_types(args) : EMPTY_ARRAY;
    
        if( PATTERN_NEW_CTOR.matcher(exact[0]).matches() )
        {
            final Constructor<?> method = el_search_constructor(clz, name, types, args_count, exact, cache);
        
            return Objects.requireNonNull(method,"NoMatchedConstructor")
                .newInstance(
                        method.getParameterCount()>0
                            ? params.apply(method.getParameterTypes(), new Object[method.getParameterCount()])
                            : null
                ); 
        }
        else
        {
            final Method method = el_search_method(clz, name, types, args_count, exact, void_return, cache);
        
            return Objects.requireNonNull(method,"NoMatchedMethod")
                .invoke( 
                        0<(method.getModifiers() & Modifier.STATIC) ? null : ref,
                        method.getParameterCount()>0
                            ? params.apply(method.getParameterTypes(), new Object[method.getParameterCount()])
                            : null
                ); 
        }
    }
    
    private static Constructor<?> el_search_constructor(Class<?> clz, String name, final String[] types, 
        final int args_count, final String[] exact, 
        final Map<String,Object> cache ) throws ReflectiveOperationException
    {
        String label = clz.getName()+"."+name;
        Constructor<?> method = Constructor.class.cast(cache.get(label));
        
        if( method==null ){
            method = (Constructor)el_search_method_in_list(types,args_count,exact,clz.getConstructors(),label,cache);
        }    
        return method;
    }
    private static Method el_search_method(Class<?> clz, String name, final String[] types, 
        final int args_count, final String[] exact, final RefBoolean void_return,
        final Map<String,Object> cache ) throws ReflectiveOperationException
    {
        String label = clz.getName()+"."+name;
        Method method = Method.class.cast(cache.get(label));
        
        if( method==null ){
            final String mName = exact[0];
            final Method[] list = Arrays.stream(clz.getMethods())
                    .filter(m->mName.equals(m.getName()))
                    .toArray( Method[]::new );
            method = (Method)el_search_method_in_list(types,args_count,exact,list,label,cache);
        }
        void_return.value
            = "void".equalsIgnoreCase(Objects.requireNonNull(method,"NoMethod_"+name).getReturnType().getTypeName());
        return method;
    }
    
    
    private static Executable el_search_method_in_list( final String[] types, 
        final int args_count, final String[] exact, Executable[] list, String label,
        final Map<String,Object> cache ) throws ReflectiveOperationException
    {
        Executable method = null;
        final int[] matched = {0,0,0}; int i = 0, n = 0;

        for(Executable m : list )
        {
            if((list.length<2 && exact.length<2)
            || (m.getParameterCount()>=args_count && el_search_method_match(m.getParameters(),types)) )
            {                          
                method = m; method.setAccessible( true ); break;
            }
            else if( m.getParameterCount()==types.length ){
                matched[n++ % matched.length] = i;
            }
            i++;
        }
        if( method==null && n==1 ){
            method = list[matched[(n-1) % matched.length]];
            method.setAccessible( true );
        }
        if( method!=null && (n==1 || exact.length>1) ){
            cache.put( label, method );
        }
        return method;
    }
    
    private static boolean el_search_method_match(Parameter[] params, final String[] types){
        int i = 0;
        for(;i<params.length && i<types.length;){
            String cnm = params[i].getType().getName();
            if( cnm.toLowerCase().endsWith(types[i])) i++; else break;
        }
        return i==types.length;
    }
    
    
    
    public static Formula complexFormula(int depth, StringBuilder el, int offset, int to,
        int ops, Scanner.Job[] oplist, int from, int end, Scanner.Job[] queue)
    {
        final int count = end-from;
        System.arraycopy( oplist, from, queue, 0, count );
        Arrays.sort( queue, 0, count, Scanner.Job.comparator );
        
        final Scanner.Job job = queue[0]; /* the job with lowest priority */
        final int array_index = job.index;
        
        final Formula formula = new Formula( depth, job.op,
            /* left formula */
            array_index<from+1
                    ? new Formula(depth+1, el.substring(array_index>0 ? oplist[array_index-1].end : offset, job.position))
                    : complexFormula(depth+1, el,offset,to,ops,oplist,from,array_index,queue) 
            ,
            /* right formula */
            array_index>end-2
                    ? new Formula(depth+1, el.substring(job.end, array_index<ops-1 ? oplist[array_index+1].position : to))
                    : complexFormula(depth+1, el,offset,to,ops,oplist,array_index+1,end,queue) 
            );
        return formula;
    }
        
 
    public static Object calculate(StringBuilder el, int offset, int to,
            int ops, Scanner.Job[] oplist, Scanner.Job[] queue, final Map<String,Object> context)
    {
        return ops<1
                /* a function-calling ? like : Math.min(1,2) */
            ? new Formula( 0, el.substring(offset,to) ).process( context )
                /* a much more complex sentence, need to be parsed and processed */
            : complexFormula( 0, el, offset, to, ops, oplist, 0, ops, queue ).process( context );
    }
    
}