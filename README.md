# Test-Splitter

This project aims to split complex JUnit tests into parts for various purposes. 

## How to Run

Project includes a main class to provide the functionality. The generated test file should be executed once before 
executing the generated methods. 

``` Shell
mvn exec:java -Dexec.mainClass="TestParser" -Dexec.args="path-to-file JUnitClassName MethodName"
```

_Path should be given in Unix style in all platforms_
&nbsp;\
&nbsp;


__To run the the examples, first run the program to instrument the code.__  


__1. SingleLinkedList Example__
``` Shell
mvn exec:java -Dexec.mainClass="TestParser" \
   -Dexec.args="./src/main/java/Samples/Sample1/SingleLinkedListTest.java SingleLinkedListTest testS0"
```

* Run the instrumented test
``` Shell
mvn -Dtest=Samples.Sample1.GeneratedTest#testS0 test
```

* You can verify the generated tests
``` Shell
mvn -Dtest=Samples.Sample1.GeneratedTest#generatedU0 test
mvn -Dtest=Samples.Sample1.GeneratedTest#generatedU1 test
```
&nbsp;

__2. BankAccount Example__
``` Shell
mvn exec:java -Dexec.mainClass="TestParser" \
   -Dexec.args="./src/main/java/Samples/Sample2/BankAccountTest.java BankAccountTest testS0"
```



* Run the instrumented test
``` Shell
mvn -Dtest=Samples.Sample2.GeneratedTest#testS0 test
```

* You can verify the generated tests
``` Shell
mvn -Dtest=Samples.Sample2.GeneratedTest#generatedU0 test
mvn -Dtest=Samples.Sample2.GeneratedTest#generatedU1 test
```

