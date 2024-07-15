package az.el;

import java.io.IOException;
import java.text.ParseException;
import java.util.Comparator;
import java.util.Map;

/***
 * scanning a complex expression, and evaluate each part, to make
 * final expression simple to be processed by {@link az.el.Formula Formula} .
 * 
 * @author arren
 */
final class Scanner 
{
    private Scanner()
    {}    
        
    private static void setPartialString(StringBuilder builder, int offset, int end, String value){
        int i = 0;
        for(;i<value.length();i++){
            builder.setCharAt(offset+i, value.charAt(i));
        }
        for(int j=offset+i;j<end;j++){
            builder.setCharAt(j, ' ');
        }
    }
    
    final static String vName(int index, int depth){
        return "$"+Integer.toString(index+depth*EL.MAX_VARS_IN_DEPTH);
    }
    
 
    public static Object scan(final Map<String,Object> context, int depth,
        StringBuilder expr,
        int[] brackets, int[] commas, int[] ops, Job[][] oplist, int[] funcs
        )
        throws ReflectiveOperationException, IOException, ParseException
    {
        @SuppressWarnings("unused")
        int modified = 0;
        int i, m, x, vari = 0, stack = 0; brackets[0] = 0; commas[0] = 0; ops[0] = 0; funcs[0] = 0;
        
        String varn; boolean b;
        
        for(i=0; i<expr.length(); i++)
        {
        final char  c = expr.charAt(i); 
            
            switch( c )
            {
            case '+':
                switch(expr.charAt(i+1)){
                case '+':
                    b = i==0 || commas[stack]==i-1 || brackets[stack]==i-1 || (ops[stack]>0 && oplist[stack][ops[stack]-1].end==i);
                    Job.set(oplist[stack], ops[stack]++, i, "++", b ? OP.SELF_ADD_left : OP.SELF_ADD_right );
                    i++;
                    break;
                case '=':
                    Job.set(oplist[stack], ops[stack]++, i, "+=", OP.ADD_TO);
                    i++;
                    break;
                default:
                    Job.set(oplist[stack], ops[stack]++, i, "+", OP.ADD);
                    break;
                }
                funcs[stack] = 0; break;
            case '-':
                switch(expr.charAt(i+1)){
                case '-':
                    b = i==0 || commas[stack]==i-1 || brackets[stack]==i-1 || (ops[stack]>0 && oplist[stack][ops[stack]-1].end==i);
                    Job.set(oplist[stack], ops[stack]++, i, "--", b ? OP.SELF_MINUS_left : OP.SELF_MINUS_right );
                    i++;
                    break;
                case '=':
                    Job.set(oplist[stack], ops[stack]++, i, "-=", OP.MINUS_TO);
                    i++;
                    break;
                default:
                    Job.set(oplist[stack], ops[stack]++, i, "-", OP.MINUS);
                    break;
                }
                funcs[stack] = 0; break;
            case '*':
                switch(expr.charAt(i+1)){
                case '=':
                    Job.set(oplist[stack], ops[stack]++, i, "*=", OP.MULTIPLY_TO);
                    i++;
                    break;
                default:
                    Job.set(oplist[stack], ops[stack]++, i, "*", OP.MULTIPLY);
                    break;
                }
                funcs[stack] = 0; break;
            case '/':
                switch(expr.charAt(i+1)){
                case '=':
                    Job.set(oplist[stack], ops[stack]++, i, "/=", OP.DIVIDE_TO);
                    i++;
                    break;
                default:
                    Job.set(oplist[stack], ops[stack]++, i, "/", OP.DIVIDE);
                    break;
                }
                funcs[stack] = 0; break;
            case '%':
                switch(expr.charAt(i+1)){
                case '=':
                    Job.set(oplist[stack], ops[stack]++, i, "%=", OP.MOD_TO);
                    i++;
                    break;
                default:
                    Job.set(oplist[stack], ops[stack]++, i, "%", OP.MOD);
                    break;
                }
                funcs[stack] = 0; break;
            case '>': // >, >=, >>
                switch(expr.charAt(i+1)){
                case '=':
                    Job.set(oplist[stack], ops[stack]++, i, ">=", OP.GTE);
                    i++;
                    break;
                case '>':
                    Job.set(oplist[stack], ops[stack]++, i, ">>", OP.BIT_MOVE_R);
                    i++;
                    break;
                default:
                    Job.set(oplist[stack], ops[stack]++, i, ">", OP.GT);
                    break;
                }
                funcs[stack] = 0; break;
            case '<': // <, <=, <<
                switch(expr.charAt(i+1)){
                case '=':
                    Job.set(oplist[stack], ops[stack]++, i, "<=", OP.LTE);
                    i++;
                    break;
                case '<':
                    Job.set(oplist[stack], ops[stack]++, i, "<<", OP.BIT_MOVE_L);
                    i++;
                    break;
                case '>':
                    Job.set(oplist[stack], ops[stack]++, i, "!=", OP.NEQ);
                    i++;
                    break;
                default:
                    Job.set(oplist[stack], ops[stack]++, i, "<", OP.LT);
                    break;
                }
                funcs[stack] = 0; break;
            case '=': // =, ==
                switch(expr.charAt(i+1)){
                case '=':
                    Job.set(oplist[stack], ops[stack]++, i, "==", OP.EQ);
                    i++;
                    break;
                default:
                    Job.set(oplist[stack], ops[stack]++, i, "=", OP.SET);
                    break;
                }
                funcs[stack] = 0; break;
            case '!': // !, !=
                switch(expr.charAt(i+1)){
                case '=':
                    Job.set(oplist[stack], ops[stack]++, i, "!=", OP.NEQ);
                    i++;
                    break;
                default:
                    Job.set(oplist[stack], ops[stack]++, i, "!", OP.NOT);
                    break;
                }
                funcs[stack] = 0; break;
            case '&': // &, &&
                switch(expr.charAt(i+1)){
                case '=':
                    Job.set(oplist[stack], ops[stack]++, i, "&=", OP.AND_TO);
                    i++;
                    break;
                case '&':
                    Job.set(oplist[stack], ops[stack]++, i, "&&", OP.AND);
                    i++;
                    break;
                default:
                    Job.set(oplist[stack], ops[stack]++, i, "&", OP.BIT_AND);
                    break;
                }
                funcs[stack] = 0; break;
            case '|': // |, ||
                switch(expr.charAt(i+1)){
                case '=':
                    Job.set(oplist[stack], ops[stack]++, i, "|=", OP.OR_TO);
                    i++;
                    break;
                case '|':
                    Job.set(oplist[stack], ops[stack]++, i, "||", OP.OR);
                    i++;
                    break;
                default:
                    Job.set(oplist[stack], ops[stack]++, i, "|", OP.BIT_OR);
                    break;
                }
                funcs[stack] = 0; break;
            case '^': 
                switch(expr.charAt(i+1)){
                case '=':
                    Job.set(oplist[stack], ops[stack]++, i, "^=", OP.XOR_TO);
                    i++;
                    break;
                default:
                    Job.set(oplist[stack], ops[stack]++, i, "^", OP.BIT_XOR);
                    break;
                }
                funcs[stack] = 0; break;
            case '~':
                switch(expr.charAt(i+1)){
                default:
                    Job.set(oplist[stack], ops[stack]++, i, "~", OP.BIT_NOT);
                    break;
                }
                funcs[stack] = 0; break;
                
            case '(':
                stack++; brackets[stack] = i; commas[stack] = 0; ops[stack] = 0; funcs[stack] = 0;
                break;
                
            case ',':
                if( ops[stack]>0 || funcs[stack]>0 )
                {/* 
                  * we detcted some operations ? like : 2*Math.min(x*3,2*i) 
                  ****/
                    m = commas[stack]>0 ? commas[stack]+1 : brackets[stack]+1;
                    varn = vName( vari++, depth );
                    context.put( varn, Formula.calculate(expr, m, i, ops[stack], oplist[stack], oplist[oplist.length-1], context) );
                    setPartialString( expr, m, i, varn );
                    modified++;
                }
                commas[stack] = i; ops[stack] = 0; funcs[stack] = 0;
                break;
                
            case ')': 
                if( ops[stack]>0 || funcs[stack]>0 ) /* has operators ? or has function-calling ? */
                {/*
                  * like "Math.min(Math.max(5,i*2)*4,3*Math.max(1+2,3+4))", here we calculate "i*2" and "3+4" .
                  ****/ 
                    m = commas[stack]>0 ? commas[stack]+1 : brackets[stack]+1;
                    varn = vName( vari++, depth );
                    context.put( varn, Formula.calculate(expr, m, i, ops[stack], oplist[stack], oplist[oplist.length-1], context) );
                    setPartialString( expr, m, i, varn );
                    modified++;
                }
                stack--; funcs[stack] += 1; /* a method was found ? */
                break;
                
            case '.': 
                if( funcs[stack]>0 ) /* has a function-calling ? */
                {/* 
                  * like : Runtime.getRuntime().availableProcessor()
                  ****/
                    m = ops[stack]>0 ? oplist[stack][ops[stack]-1].end : commas[stack]>0 ? commas[stack]+1 : brackets[stack]>0?brackets[stack]+1:0;
                    varn = vName( vari++, depth );
                    context.put( varn, Formula.calculate(expr, m, i, 0, oplist[stack], oplist[oplist.length-1], context) );
                    setPartialString( expr, m, i, varn );
                    modified++; funcs[stack] = 0;
                }
                break;
                
            case '\'': /* to prevent the evaluation being interfered by a string which contains special characters */
                x = searchNextSingleQuote(expr,i+1);
                m = 0x03ffff & (x);
                x = 0x003fff & (x>>18); /* special chars */
                
                if( m>expr.length() )
                    throw new java.lang.IllegalArgumentException("InvalidStringInExpression");
                else if( m>i+48 || x>0 )
                {
                    varn = vName( vari++, depth );
                    context.put( varn, expr.substring(i+1,m) ); /* define the string as a variable */
                    setPartialString( expr, i, m+1, varn );
                    modified++;
                }
                i = m; /* move forward */
                break;
            }
        }
        if( stack!=0 )
            throw new java.lang.IllegalArgumentException("InvalidJavaExpression");
        return
            Formula.calculate( expr, 0, i, ops[stack], oplist[stack], oplist[oplist.length-1], context );
    }
    
    private static int searchNextSingleQuote(CharSequence s, int from)
    {
        int f = s.length()+1, special = 0;
        
        for(int i=from, k=0; i<s.length() && k<1; i++){
            switch(s.charAt(i))
            {
            case '+':
            case '-':
            case '*':
            case '/':
            case '%':
            case '>': // >, >=, >>
            case '<': // <, <=, <<
            case '=': // =, ==
            case '!': // !, !=
            case '&': // &, &&
            case '|': // |, ||
            case '^': //
            case '~':
            case ',':
            case '(':
            case ')':
                special++;
                break;
            case '\'':
                f = i; k++;
                break;
            }
        }
        return f | (special<<18);
    }
    
    
    static class Job
    {
        public int index, position, end;
        public String operator;
        public OP op;
        
        public void set(int index, int position, String operator, OP op){
            this.index = index;
            this.position = position;
            this.operator = operator;
            this.op = op;
            this.end = position+operator.length();
        }        
        
        public static void set(Job[] jobs, int index, int position, String operator, OP op){
            if( jobs[index]==null ){
                jobs[index]= new Job();
            }
            jobs[index].set(index, position,operator,op);
        }
        
        public static Job[][] newArray(int size, int elements){
            Job[][] jobs = new Job[size][];
            for(int i=0;i<size;i++) jobs[i]=new Job[elements];
            return jobs;
        }
        
        static final Comparator<Job> comparator = (a,b)->Integer.compare(a.op.priority,b.op.priority);
    }
    /* end : class Job */
}