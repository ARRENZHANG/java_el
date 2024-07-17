a simple Java-Expression parser,
related to Java Expression Language.


[Summary]
=======================================================
1: provide you the possibility to configure your programs with Dynamic parameters,
for example, a configure-file looks like :

    some.a=1
    some.property=${Math.max(256,Runtime.getRuntime().availableProcessors()*8)}
    some.b=2

the properties can be distributed by a configure-server or a disk file,
and can be used as :

    tomcat.setMaxThreads(EL.eval(config.get("some.property")));

will make your tomcat service run with correct threads number based on the machine
it is running in.
this makes you use proper CPU/Memory resources for you application when it is running in 
different server, not a static value for all different servers in a cluster.

2: a command line program EL.main() can be used to parse user input, and test the logical
before you write them into your code. 
for example:
    java -cp az-el.jar az.el.EL -e a=1 System.out.println(a)
will evaluate those 2 expressions.
and:
    java -cp az-el.jar az.el.EL -f /home/user/file.txt
will evaluate expressions in the file, line by line.
and:
    java -cp az-el.jar az.el.EL -i
will evaluate expressions you entered on console. this can be used as a java-shell.


[Data Types]
=======================================================
data types supported:

bool/boolean --> true|false.
int/integer  --> like 1, 100 etc.
long         --> like 2L, 100L etc.
float        --> like 2.0f etc.
string       --> like 'abc' etc.

to supply a string in expression, please use a pair of single quotation marks,
for example:
    EL.eval(new String[]{"System.out.print('some string here\n');"})
.


[Special Variables]
=======================================================
? : the result of previous expression, any type.
$ : the current element when iterating through a collection.
context : the execution context, which contains all variables.

other variables which defined by user can be referenced by its name,
for example:
    EL.eval("a=1;System.out.println(a*2)") 
will print out "2" on console.

strings should be enclosed with a pair of '', 
for example:
    EL.eval("a='xyz';print(a)")
will print out a string "xyz" on console.

and do not name your variables with a start character "$".


[Embedded Functions]
=======================================================
1: map()
helps creating a Map object, for example:
    EL.eval("result=map('a',1,'b','2')") 
will create a Map<Object,Object> named "result" with two entries of {"a"->1,"b"->"2"}.

2: list()
to create a List object, for example: 
    EL.eval("list(1,'a',2,3)")
will create a List<Object> collection object with 4 items in it.

3: array()
create an array, for example : array(1,2,'3').

4: each()/every()/foreach()
can be used to iterate over a collection, like list/array. 
for example:
    EL.eval("each(a_list,'System.out.println($)')"),
will print out each element on console. the collection can be an object
of type Iterator/Iterable/List/Set/Map.
if you just want to print out each element of a collection,
use "each(a_list_object)", the same as "each(a_list_object,'println($)')".

5: iif()
used to check given condition and then return corresponding value.
for example:
    x=iif(a>1, obj.doSomething(a), obj.doOther(a))
will evaluate the sub-expression "obj.doSomething(a)" only if "a>1", and never
evaluate "obj.doOther(a)" anyway. we delayed the expression-evaluation.

6: print(), println(), printf().
used to print something out on console, 
similar method-signature as System.out.print*() series.


[Supported operators]
=======================================================
+ - * / % & | ! ^ ~ >> << =
!= == > < >= <= ++ --
a+=1 a-=1 a*=1 a/=1 a%=2 a&=1 a|=1 a^=1


[Special characters]
=======================================================
;  <semicolon>
\n <line-feed>
you can't feed expressions to EL.eval(String exp) method,
if the string contains (;\n), use EL.eval(String[] exp) instead.


[Method invoking]
=======================================================
for example :
    Math.max#int(a,b) 
            for --> Math.max(int a, int b).

    Math.max(long_var_1,long_var_2).

    var.add#int#int(1,2) 
            for --> Some.add(int a, int b).

you can specify a method what is matching the parameters-list you given, 
to overcome the method-overloading problem.
for example:
    System.out.println#int(...)
            for --> print out a integer value.
    System.out.println#float(...)
            for --> print out a float value.


[Black-list / White-list classes]
=======================================================
it's possible to provide a list of classes which shouldn't be used in code.
    context.put(EL.VAR_NAME_CLASS_BLACKLIST, "a.SomeClass,b.*")
to disable some classes.

and you can setup a white-list
    context.put(EL.VAR_NAME_CLASS_WHITELIST, "a.SomeClass,b.*")
to narrow down what class can be used in expression.


[Expression Examples]
=======================================================
"java.lang.System.out.print#string('hello');System.out.println('world')"
"java.lang.Math.max#int(1,2)"
"com.some.Class.prop='word'"
"com.some.Class.method#string#int('a',1,?)"
"var=com.SomeClass.new#string('jacky');var.sayHello('iAmLucy')"
"2*Math.max#int(5*Runtime.getRuntime().availableProcessors(),8*3)+3*4-1"
"iif(a>b,c,com.SomeClass.doSomething('x'));"
"var=array('a','b','c');each(var,'System.out.println#string($)');"
"foreach(map('a',1,'b',2))"

[Found any Issues ?]
you can contact me through email : arrenzhang@hotmail.com .