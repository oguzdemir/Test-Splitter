# Test-Splitter

This project aims to split complex JUnit tests into parts for various purposes. 

[Current Status Report](status.pdf)

## How to Run


| Option                  	| Functionality                                                                                                      	|
|-------------------------	|--------------------------------------------------------------------------------------------------------------------	|
| -p                      	| Path to test file                                                                                                  	|
| -c (optional)           	| Target class name that includes the methods to be splitted. All the test class files will be processed by default. 	|
| -t (optional, repeated) 	| Target method name(s) to split. All methods with (@Test) annotation will be processed by default.                  	|
| -s (optional, repeated) 	| Split points (method names). All method calls in the test function will be considered as split point.              	|
| -a                      	| Enabling splitting in assertions rather than method calls.                                                         	|                                                        |



``` Shell
mvn compile exec:java -Dexec.mainClass="org.od.TestSplitter.TestParser" -Dexec.args="<arguments>"
```

_Path should be given in Unix style in all platforms_
&nbsp;\
&nbsp;

__BankAccount Example__

_The original tests will be `@Before` function of the generated test class._

* Splitting the test from specific methods
``` Shell
 mvn compile exec:java -Dexec.mainClass="org.od.TestSplitter.TestParser" \
     -Dexec.args="-p ./src/main/java/org/od/TestSplitter/Samples/ \
     -c BankAccountTest -t testS0 -s withdrawMoney -s addMoney"
```

* Splitting the test from all assertions
``` Shell
 mvn compile exec:java -Dexec.mainClass="org.od.TestSplitter.TestParser" \
     -Dexec.args="-p ./src/main/java/org/od/TestSplitter/Samples/ -c BankAccountTest -t testS0 -a"
```

* Splitting the test from all methods
``` Shell
 mvn compile exec:java -Dexec.mainClass="org.od.TestSplitter.TestParser" \
     -Dexec.args="-p ./src/main/java/org/od/TestSplitter/Samples/ -c BankAccountTest -t testS0"
```

