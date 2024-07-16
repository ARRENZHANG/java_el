package az.el;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class JavaEL_Test {
    
    public JavaEL_Test() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void test_00() throws ReflectiveOperationException, IOException, ParseException
    {        
        EL.eval("a=1;b='2';each(context)");
        EL.eval("list=array('a','b','c');func='System.out.println#string($)';each(list,func);");
        EL.eval("list=array('a','b','c');each(list)");
        
        Object b = EL.eval(""
                + "a=(1>2) || (3>2);"
                + "");
        Assert.assertTrue( Boolean.TRUE.equals(b) );
        
        Object a = EL.eval(""
                + "1+(2*3-4)*1+1;"
                + "");
        Assert.assertTrue(Integer.compare(4,Integer.class.cast(a))==0);
    }
    @Test
    public void test_0b() throws ReflectiveOperationException, IOException, ParseException
    {        
        Integer a = EL.eval(""
                + "1+2;"
                + "System.out.println#string(?)"
                + "");
        Assert.assertTrue(Integer.compare(3,Integer.class.cast(a))==0);
    }
    @Test
    public void test_0b_01() throws ReflectiveOperationException, IOException, ParseException
    {        
        EL.eval("System.out.println(Math.max(2*3+1,Runtime.getRuntime().availableProcessors()*2))");
    }
    
    
    @Test
    public void test_0f() throws ReflectiveOperationException, IOException, ParseException
    {
        Integer a = EL.eval(""
                + "Math.max#int(Runtime.getRuntime().availableProcessors(),1);"
                + "Math.max#int(1,?*3/5)"
                + "");
        Assert.assertTrue(Integer.compare(4,Integer.class.cast(a))==0);
    }
    @Test
    public void test_0g() throws ReflectiveOperationException, IOException, ParseException
    {
        Integer a = EL.eval(""
                + "i=1;"
                + "iif(?i>0,1+2,java.lang.Math.max#int(5,?));"
                + "");
        Assert.assertTrue(Integer.compare(3,Integer.class.cast(a))==0);
        Integer b = EL.eval(""
                + "i=1;"
                + "iif(?i>2,1+2,java.lang.Math.max#int(5,?));"
                + "");
        Assert.assertTrue(Integer.compare(5,Integer.class.cast(b))==0);
    }
    @Test
    public void test_0h() throws ReflectiveOperationException, IOException, ParseException
    {
        int n = EL.eval(""
                + "Runtime.getRuntime().availableProcessors()*2;"
                + "3*Runtime.getRuntime().availableProcessors()+3"
                + "");
        Assert.assertTrue(n==3*java.lang.Runtime.getRuntime().availableProcessors()+3);
    }
    @Test
    public void test_0i() throws ReflectiveOperationException, IOException, ParseException
    {
        int n = EL.eval(""
                + "a=1;"
                + "java.lang.Math.max#int(?a*2+2-1,5*java.lang.Runtime.getRuntime().availableProcessors())"
                + "");
        Assert.assertTrue(n==5*java.lang.Runtime.getRuntime().availableProcessors());
    }

    @Test
    public void test_0e() throws ReflectiveOperationException, IOException, ParseException
    {
        boolean yes  = EL.eval(""
                + "!(2<1);"
                + "",
                Boolean.class );
        Assert.assertTrue( yes );
        
        boolean sure  = EL.eval(""
                + "3<2*2;"
                + "",
                Boolean.class );
        Assert.assertTrue( sure );
    }
    
    @Test
    public void test_0d() throws ReflectiveOperationException, IOException, ParseException
    {        
        Map<String,Object> context = new HashMap<>(9);
        
        int a = EL.eval(context, ""
                + "a=3;1+2*++a"
                + "");
        Assert.assertTrue(a==9 && context.get("a").equals(4));
        
        int b = EL.eval(context, ""
                + "a=3;1+2*a++"
                + "");
        Assert.assertTrue(b==7 && context.get("a").equals(4));
        
        int c = EL.eval(context, ""
                + "a=3;1+2*--a"
                + "");
        Assert.assertTrue(c==5 && context.get("a").equals(2));
        
        int d = EL.eval(context, ""
                + "a=3;1+2*a--"
                + "");
        Assert.assertTrue(d==7 && context.get("a").equals(2));
    }
    @Test
    public void test_0d_0() throws ReflectiveOperationException, IOException, ParseException
    {        
        int a = EL.eval(""
                + "a=2;a*=2"
                + "");
        Assert.assertTrue(a==4);
        
        int b = EL.eval(""
                + "a=9;a/=3;"
                + "");
        Assert.assertTrue(b==3);
        
        int c = EL.eval(""
                + "a=3;a-=2"
                + "");
        Assert.assertTrue(c==1);
        
        int d = EL.eval(""
                + "a=0;a+=2"
                + "");
        Assert.assertTrue(d==2);
    }
    
    
    @Test
    public void test_0d_2() throws ReflectiveOperationException, IOException, ParseException
    {        
        int a = EL.eval(""
                + "a=2;a|=1"
                + "");
        Assert.assertTrue(a==3);
        
        int b = EL.eval(""
                + "a=1;a&=3;"
                + "");
        Assert.assertTrue(b==1);
        
        int c = EL.eval(""
                + "a=3;a^=2"
                + "");
        Assert.assertTrue(c==1);
    }
    
    
    @Test
    public void test_0d_3() throws ReflectiveOperationException, IOException, ParseException
    {  
        int b = EL.eval(""
                + "1+2*3+4*1-5;"
                + "");
        Assert.assertTrue(b==6);
    }
    @Test
    public void test_0c() throws ReflectiveOperationException, IOException, ParseException
    {        
        String a = EL.eval(""
                + "1+2;"
                + "?+''"
                + "");
        Assert.assertTrue(a.equals("3"));
    }
    @Test
    public void test_0a() throws ReflectiveOperationException, IOException, ParseException
    {        
        Object a = EL.eval(""
                + "4;"
                + "x=java.lang.String.valueOf#long(?);"
                + "java.lang.Integer.valueOf#int(?x)"
                + "");
        Assert.assertTrue(Integer.compare(4,Integer.class.cast(a))==0);
    }
        
    @Test
    public void test_01() throws ReflectiveOperationException, IOException, ParseException
    {        
        Object a = EL.eval(""
                + "az.el.JavaEL_Test$T.new();"
                + "?.add(?.add(1,1),3)"
                + "");
        Assert.assertTrue(Integer.compare(5,Integer.class.cast(a))==0);
    }
         
    @Test
    public void test_02() throws ReflectiveOperationException, IOException, ParseException
    {     
        Object b = EL.eval(""
                + "az.el.JavaEL_Test$T.new(1,2);"
                + "?.add(2,'3.0')"
                + "");
        Assert.assertTrue(Integer.compare(8,Integer.class.cast(b))==0);
    }
         
    @Test
    public void test_03() throws ReflectiveOperationException, IOException, ParseException
    {     
        Object x = EL.eval(""
                + "o=java.lang.System.out;"
                + "a='this-line-from-el';"
                + "o.println#String(?a);"
                + "x=1;"
                + "o.println#String(?x);"
                + "o.println#String('iLoveYou');"
                + "o;"
                + "");
        Assert.assertTrue(java.io.PrintStream.class.isAssignableFrom(x.getClass()));
    }
         
    @Test
    public void test_04() throws ReflectiveOperationException, IOException, ParseException
    {     
        Assert.assertTrue(java.io.File.separator.equals(EL.eval("java.io.File.separator")));
    }
      
    @Test
    public void test_05() throws ReflectiveOperationException, IOException, ParseException
    {     
        Map<String,Object> context = new HashMap<>(24);
        
        EL.eval(context, ""
                + "user=System.getProperty('user.home');"
                + "System.out.println#string(?);"
                + "1+(2+3)*4+5*Math.max#int(System.getProperties().size(),1);"
                + "tt=?;"
                + "System.out.println#string('start test...');"
                + "cores=Runtime.getRuntime().availableProcessors()*2;"
                + "freem=Runtime.getRuntime().freeMemory();"
                + "");
        Assert.assertTrue(context.get("cores").equals(Runtime.getRuntime().availableProcessors()*2));
        Assert.assertTrue(context.get("freem").equals(Runtime.getRuntime().freeMemory()));
        Assert.assertTrue(context.get("user").equals(System.getProperty("user.home")));
        Assert.assertTrue(context.get("tt").equals(System.getProperties().size()*5+4*(2+3)+1));        
    }
    
    @Test
    public void test_06() throws ReflectiveOperationException, IOException, ParseException
    {     
        Map<String,Object> context = new HashMap<>(24);
        context.put("array",new String[]{"hello","world"});
        
        EL.eval(context, ""
                + "list=list('yes','my','lord');"
                + "System.out.println(?list[2]);"
                + "friends=list( 'a','1' );"
                + "book=map('one','LucyReading','two','JackBook');"
                + "System.out.println#string(book.get('one'));"
                + "friends[1]='jacky';"
                + "System.out.println#string(?friends[0]+'@'+?friends[1]);"
                + "array[1]='china';"
                + "System.out.println#string(?array[1])"
                + "");
        EL.eval(context, ""
                + "each='System.out.println#string(?$)';"
                + "EL.each(list,each,context);"
                + "EL.each(?friends,?each,?context);"
                + "EL.each(?array,?each,?context);"
                + "");
    }
    
    public static class T
    {    
        final int init_value;

        public T(){
            this.init_value = 0;
        }
        public T(int a, int b){
            this.init_value = a+b;
        }
        public int add(int a, int b){
            return a+b+init_value;
        }
    }
    
}