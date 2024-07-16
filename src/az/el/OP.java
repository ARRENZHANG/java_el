package az.el;

interface Calculator
{
    Object calculate(Object left, Object right);
    
    public final static Calculator DUMMY = (a,b)->null;
} 

enum OP
{
    SET(1,"=", Calculator.DUMMY), 
    AND(8,"&&",
        (a,b)->Boolean.class.cast(a) && Boolean.class.cast(b)),
    OR(8,"||",
        (a,b)->Boolean.class.cast(a) || Boolean.class.cast(b)),
    NOT(8,"!",(a,b)->!Boolean.class.cast(b)),
    
    @SuppressWarnings("unchecked")
    EQ(20,"==",
        (a,b)->a==b || Comparable.class.cast(a).compareTo(Comparable.class.cast(b))==0),
    @SuppressWarnings("unchecked")
    NEQ(20,"!=",
        (a,b)->Comparable.class.cast(a).compareTo(Comparable.class.cast(b))!=0),
    @SuppressWarnings("unchecked")
    LT(20,"<",
        (a,b)->Comparable.class.cast(a).compareTo(Comparable.class.cast(b))<0),
    @SuppressWarnings("unchecked")
    LTE(20,"<=",
        (a,b)->Comparable.class.cast(a).compareTo(Comparable.class.cast(b))<=0),
    @SuppressWarnings("unchecked")
    GT(20,">",
        (a,b)->Comparable.class.cast(a).compareTo(Comparable.class.cast(b))>0),
    @SuppressWarnings("unchecked")
    GTE(20,">=",
        (a,b)->Comparable.class.cast(a).compareTo(Comparable.class.cast(b))>=0),

    
    BIT_AND(60,"&",
        (a,b)->Integer.parseInt(a.toString()) & Integer.parseInt(b.toString())),
    BIT_OR(60,"|",
        (a,b)->Integer.parseInt(a.toString()) | Integer.parseInt(b.toString())),
    BIT_XOR(60,"^",
        (a,b)->Integer.parseInt(a.toString()) ^ Integer.parseInt(b.toString())),
    BIT_NOT(65,"~",
        (a,b)->~Integer.valueOf(b.toString())),

    BIT_MOVE_R(70,">>",
        (a,b)->Integer.parseInt(a.toString())>>Integer.parseInt(b.toString())),
    BIT_MOVE_L(70,"<<",
        (a,b)->Integer.parseInt(a.toString())<<Integer.parseInt(b.toString())),

    ADD(81,"+",(a,b)->{ /* !!! don't use "?:" operator to do this */
        if( a instanceof String || b instanceof String ){
            return (a!=null?a.toString():null)+(b!=null?b.toString():null);
        }
        else if( a instanceof Float || b instanceof Float ){
            return Float.parseFloat(a.toString()) + Float.parseFloat(b.toString());
        }
        else if( a instanceof Long || b instanceof Long ){
            return Long.parseLong(a.toString()) + Long.parseLong(b.toString());
        }
        else{
            return Integer.parseInt(a.toString()) + Integer.parseInt(b.toString());
        }
    }),
    MINUS(81,"-",(a,b)->{ /* !!! don't use "?:" operator to do this */
        if( a instanceof Float || b instanceof Float ){
            return Float.parseFloat(a.toString()) - Float.parseFloat(b.toString());
        }
        else if( a instanceof Long || b instanceof Long ){
            return Long.parseLong(a.toString()) - Long.parseLong(b.toString());
        }
        else{
            return Integer.parseInt(a.toString()) - Integer.parseInt(b.toString());
        }
    }),
    MULTIPLY(83,"*",(a,b)->{
        if( a instanceof Float || b instanceof Float ){
            return Float.parseFloat(a.toString()) * Float.parseFloat(b.toString());
        }
        else if( a instanceof Long || b instanceof Long ){
            return Long.parseLong(a.toString()) * Long.parseLong(b.toString());
        }
        else{
            return Integer.parseInt(a.toString()) * Integer.parseInt(b.toString());
        }
    }),
    DIVIDE(82,"/",(a,b)->{
        if( a instanceof Float || b instanceof Float ){
            return Float.parseFloat(a.toString()) / Float.parseFloat(b.toString());
        }
        else if( a instanceof Long || b instanceof Long ){
            return Long.parseLong(a.toString()) / Long.parseLong(b.toString());
        }
        else{
            return Integer.parseInt(a.toString()) / Integer.parseInt(b.toString());
        }
    }),
    MOD(82,"%",(a,b)->{
        if( a instanceof Float || b instanceof Float ){
            return Float.parseFloat(a.toString()) % Float.parseFloat(b.toString());
        }
        else if( a instanceof Long || b instanceof Long ){
            return Long.parseLong(a.toString()) % Long.parseLong(b.toString());
        }
        else{
            return Integer.parseInt(a.toString()) % Integer.parseInt(b.toString());
        }
    }),
    
    SELF_ADD_left(85,"++", (a,b)->{
        if( b instanceof Long ){
            return Long.parseLong(b.toString())+1;
        }
        else{
            return Integer.parseInt(b.toString())+1;
        }
    }),
    SELF_ADD_right(85,"++", (a,b)->{
        if( a instanceof Long ){
            return Long.parseLong(a.toString())+1;
        }
        else{
            return Integer.parseInt(a.toString())+1;
        }
    }),
    
    SELF_MINUS_left(85,"--",(a,b)->{
        if( b instanceof Long ){
            return Long.parseLong(b.toString())-1;
        }
        else{
            return Integer.parseInt(b.toString())-1;
        }
    }),
    SELF_MINUS_right(85,"--", (a,b)->{
        if( a instanceof Long ){
            return Long.parseLong(a.toString())-1;
        }
        else{
            return Integer.parseInt(a.toString())-1;
        }
    }),
    
    
    ADD_TO(2,"+=", ADD.calculator),
    MINUS_TO(2,"-=",MINUS.calculator),
    MULTIPLY_TO(4,"*=",MULTIPLY.calculator),
    DIVIDE_TO(4,"/=",DIVIDE.calculator),
    MOD_TO(4,"%=",MOD.calculator),
    
    OR_TO(4,"|=",(a,b)->{
        if( a instanceof Boolean && b instanceof Boolean){
            return Boolean.class.cast(a) || Boolean.class.cast(b);
        }
        else{
            return Integer.parseInt(a.toString()) | Integer.parseInt(b.toString());
        }
    }),
    AND_TO(4,"&=",(a,b)->{
        if( a instanceof Boolean && b instanceof Boolean){
            return Boolean.class.cast(a) && Boolean.class.cast(b);
        }
        else{
            return Integer.parseInt(a.toString()) & Integer.parseInt(b.toString());
        }
    }),
    XOR_TO(4,"^=",(a,b)->{
        if( a instanceof Boolean && b instanceof Boolean){
            return Boolean.class.cast(a) ^ Boolean.class.cast(b);
        }
        else{
            return Integer.parseInt(a.toString()) ^ Integer.parseInt(b.toString());
        }
    }),
    
    NONE(0," ", Calculator.DUMMY);

    public final int priority;
    public final String operator;
    public final Calculator calculator;

    private OP(int priority, String operator, Calculator func){
        this.priority = priority;
        this.operator = operator;
        this.calculator = func;
    }
   
}