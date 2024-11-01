# Generating Random Quizzes From Templates
Basic Question Template Format (ignore parenthesis): <br>

```
Question #1: (needed to start the question block)

#R1#: int, random, 1, 10
#R2#: int, random, 1, 10
#R3#: int, random, 1, 10
#S1#: {quantity, amount, total, result}
#N1#: from #S1#

:Text:
What are variables' values after the following code segment is executed?
long #N1# = #R1# + #R2# / #R3#;
:EndText:

Solution: #R1# + #R2# / #R3#
SolutionType: long
Unit: {L ,l}
```


If the question requires code to be compiled and run, the format is slightly different.

Compile Question Template Format (ignore parenthesis):

```java
Question #1:

#R1#: double, random, 40, 60
#S1#: {message}
#S2#: {score, points, amount}
#S3#: {Linda, John, Dan, Joe}
#N1#: from #S1#
#N2#: from #S2#
#N3#: from #S3#

:Code:
// the class in the Code block must be named DynamicCode
public class DynamicCode
{
  private static String result = "";

  public static void talk()
  {
    String #N1# = "hello";
    result += #N1#;
  }

  public static void run()
  {
    double #N2# = #R1#;
    result += String.valueOf(#N2#);
  }

  public static void main(String[] args)
  {
    String name = "#N3#";
    talk();
    run();
    result += name;

    // the code needs to print out the final result once executed
    // this can be done simply by appending to a string variable where
    // the code would usually call System.out.print() and then printing
    // the result
    System.out.println(String.format("Output of the program: [%s]", result));
  }
}

:EndCode:

:Text:
Type: FMB
Specify the output of the program.

(this code will not be run, but will display as the question text unlike the code block)
public class TalkRun
{
  String result = "";

  public static void talk()
  {
    String #N1# = "hello";
    System.out.print(#N1#);
  }

  public static void run()
  {
    double #N2# = #R1#;
    System.out.print(#N2#);
  }

  public static void main(String[] args)
  {
    String name = "#N3#";
    talk();
    run();
    System.out.print(name);
  }
}

Note that you will need to pay attention to how a double value will be printed out.
:EndText:

Solution: compile
```
