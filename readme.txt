a simple Java-Expression parser, related to Java Expression Language.

Useful
----------------------------------
1, provide you the possibility to configure your programs with Dynamic parameters,
for example, a configure item like :

    some.property=${Math.max(256,Runtime.getRuntime().availableProcessors()*8)}
then :
    tomcat.setMaxThreads(EL.eval(config.get("some.property")));

will make your tomcat service run with correct threads number.

this makes you use proper CPU resources for you application when it is running in 
different server, not a static value for all different servers in a cluster.

2, a command line program EL.main() can be used to parse user input, and test the logical
before you write them into your code.


Data Types 
----------------------------------
supported data types :
boolean, int/integer, long, float, string.

Special variables
----------------------------------
? : the result of previous expression, any type.
$ : the current element when interating a collection.
context : the execution context, which contains all variables.
other user defined variables can be referenced by its name,
use
    EL.eval("a=1;System.out.println(a*2)") 
will print out "2" on console.
strings should be enclosed with a pair of '', for example "a='xyz';print(a);".

Embedded Functions
----------------------------------
1, map()
helps creating a Map object, like :
    EL.eval("m=map('a',1,'b',2)") 
will create a map with two entries, of ["a"->1,"b"->2].
2, list()
will create a List object, like "list(1,'a',2,3)", a list with 4 items.
3, array()
create an array, like "array(1,2,3)".
4, each(), every(), foreach()
can be used to iterate a collection, like 
    EL.eval("each(a_list,'System.out.println($)')"),
will print each elements in console.
5, iif()
used to check conditions, like "x=iif(a>1, obj.doSomething(a), obj.doOther(a))",
will give you the result for different conditions.
6, print(), println(), printf();


Supported operators
----------------------------------
+ - * / % ! & | ^ ~ >> << =
!= == > < >= <= ++ --
a+=1 a-=1 a*=1 a/=1

Method invoking
----------------------------------
for example :
    Math.max#int(a,b) 
            -> for Math.max(int a, int b).

    Math.max(long_var_1,long_var2).

    var.add#int#int(1,2) 
            -> for Some.add(int a, int b).
so you can specify more exact method to use.


Blacklist classes
----------------------------------
it's possible to provide a list of classes which shouldn't be used in code.
    context.put(EL.VAR_NAME_CLASS_BLACKLIST, "a.SomeClass,b.*")
to disable some classes.


Expression Examples
----------------------------------
"java.lang.System.out;?.print#String('hello');?.print(' ');?.print('world')"
"java.lang.Math.max#int(1,2)"
"com.some.Class.prop='word'"
"com.some.Class.method#string#int('a',1,?)"
"5*java.lang.Runtime.getRuntime().availableProcessors()"
"Math.max#int(5*Runtime.getRuntime().availableProcessors(),8*3)"
"EL.iif(a>b,c,d);"
"list=array('a','b','c');func='System.out.println#string($);EL.each(list,func,context);"