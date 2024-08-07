# Table of Contents
- [Concurrent Server with Sockets](#concurrent-server-with-sockets)
- [How to run the project](#how-to-run-the-project)
- [Dependencies](#dependencies)
- [Assumptions and Decisions](#assumptions-and-decisions)
- [Maximizing Throughput](#maximizing-throughput)


# Concurrent Server with Sockets 
Using any of the following programming language (taking performance into consideration):
Java, Kotlin, Python, Go, write a server ("Application") that opens a socket
and restricts input to at most 5 concurrent clients. Clients will connect to the Application and write one or more numbers of 9 digit numbers, each number followed by a server-native newline sequence, and then close the connection. The Application must write a de- duplicated list of these numbers to a log file in no particular order.

#### Primary Considerations

- The Application should work correctly as defined below in Requirements.
- The overall structure of the Application should be simple.
- The code of the Application should be descriptive and easy to read, and the build method and runtime parameters must be well-described and work.
- The design should be resilient with regard to data loss.
- The Application should be optimized for maximum throughput, weighed along with the other Primary Considerations and the Requirements below.

#### Requirements

- The Application must accept input from at most 5 concurrent clients on TCP/IP port 4000.
- Input lines presented to the Application via its socket must either be composed of exactly nine decimal digits (e.g.: 314159265 or 007007009) immediately followed by a
server-native newline sequence; or a termination sequence as detailed in #9, below.
- Numbers presented to the Application must include leading zeros as necessary to ensure they are each 9 decimal digits.
- The log file, to be named "numbers.log”, must be created anew and/or cleared when the Application starts.
- Only numbers may be written to the log file. Each number must be followed by a server-native newline sequence.
- No duplicate numbers may be written to the log file.
- Any data that does not conform to a valid line of input should be discarded and the client connection terminated immediately and without comment.
- Every 10 seconds, the Application must print a report to standard output:

  - The difference since the last report of the count of new unique numbers that have been received.

  - The difference since the last report of the count of new duplicate numbers that have been received.
  - The total number of unique numbers received for this run of the Application. Example text for #8: Received 50 unique numbers, 2 duplicates. Unique total: 567231

- If any connected client writes a single line with only the word "terminate" followed by a server-native newline sequence, the Application must disconnect all clients and perform a clean shutdown as quickly as possible.
- Clearly state all of the assumptions you made in completing the Application.

#### Notes

- You may write tests at your own discretion. 
- Tests are useful to ensure your Application passes Primary Consideration A.
- You may use common libraries in your project such as Apache Commons and Google Guava, particularly if their use helps improve Application simplicity and readability. However the use of large frameworks, such as Akka, is prohibited.
- Your Application may not for any part of its operation use or require the use of external systems, for example Apache Kafka or Redis.
- At your discretion, leading zeroes present in the input may be stripped—or not used—when writing output to the log or console.
- Robust implementations of the Application typically handle more than 2M numbers per 10-second reporting period on a modern MacBook Pro laptop (e.g.: 16 GiB of RAM and a 2.5 GHz Intel i7 processor).
- To test if your application is working as expected, you can try to telnet to it through the port 4000 by executing:
    > telnet localhost 4000 

    And manually type in the numbers sequentially followed by a newline (enter).

# How to run the project

A java jar file with the last version of the project has been added in the path: 

`out/artifacts/Concurrent_Server_Sockets_jar/Concurrent_Server_Sockets.jar `

It is possible to run it with arguments or without them. The available arguments are the port number and the maximum of concurrent clients:

### No args

It will set port 4000 and 5 concurrent clients by default.  

    java -jar .\out\artifacts\Concurrent_Server_Sockets_jar\Concurrent_Server_Sockets.jar


### Using args

This modality has been implemented to make it easy to set up in a real environment. The first argument refers to the port
and the second to clients

    java -jar .\out\artifacts\Concurrent_Server_Sockets_jar\Concurrent_Server_Sockets.jar 4000 5


# Dependencies

Two libraries from Apache Commons and JUnit have been used. 

        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-text</artifactId>
            <version>1.9</version>
        </dependency>

        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>3.12.0</version>
        </dependency>

        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>RELEASE</version>
            <scope>test</scope>
        </dependency>


# Assumptions and Decisions

- Since the test doesn't set a limit to the number of requests that the server might receive, it has been preferred to
implement an unbounded queue (LinkedBlockingQueue) for communicating Producers with Consumers. However, in a real environment, I
believe it should be monitored to have an approximate maximum limit and set the necessary limits to ensure that the server
doesn't go down due to lack of resources.
- The keyword 'terminate' is only checked as the test states. Alternatives like 'Terminate', 'tErminate'... will not be valid.
- Numbers are stored without leading zeros to make the log file more readable.
- Port number and maximum number of clients have been implemented as arguments to ensure an easy set up in a real environment.
- Although using a Logger is really useful, System.out.println has been used since it provides a better performance as 
shown in the following paragraph. 


# Maximizing Throughput

JProfile tool has been used to monitor threads and CPU performance and enhance the server throughput as much as possible.
Therefore, the server has been tested by sending 2M numbers as a lower limit to see its performance, and varying the number
of Producers and Consumers.

### First test:

- 2 Producers
- 4 Consumers

It took around 1.20 min to process 2M numbers. There were many blocked threads (red part shown below) that did not allow a faster execution.

<p align="left">
  <img alt="first-test" src="src/main/resources/readme-resources/first_test.png">
</p>

### Second test:

- 1 Producer
- 2 Consumers

As expected, the number of blocked threads were reduced, but the execution took around 2.20 min to finish. It was necessary to 
find the bottleneck in the application, the part of code where threads got stuck waiting for another thread to release a lock.

<p align="left">
  <img alt="first-test" src="src/main/resources/readme-resources/second_test.png">
</p>

### Third test:

I found out that most of the threads got stuck when they needed to Log something on the console. This is because of 
most of them works with a synchronous fragment of code to write data. Also, the method
`available()` from `BufferedInputStream` class was running for a long time, as I was using a buffer and checking every time
if something was written by the client.

<p align="left">
  <img alt="first-test" src="src/main/resources/readme-resources/third_test.png">
</p>

At this point, I made some changes. First, I set 1 Producer and 1 Consumer and decided to remove both the logger and the buffered reading method.
I tried to use a blocking reading method to avoid checking that something has been written every time.

The performance increased significantly, there weren't almost blocked threads and the application was able to process 2M numbers
in 31 seconds.

<p align="left">
  <img alt="first-test" src="src/main/resources/readme-resources/third_test_2.png">
</p>

### Fourth test:

I consider logs to be really important to monitor an application and, from my point of view, it's a big mistake to remove them just for improving the performance,
so I decided to test the app with simple `System.out.println` methods. 

<p align="left">
  <img alt="first-test" src="src/main/resources/readme-resources/fourth_test.png">
</p>

The overall throughput was better by using these methods, since it run 3 times faster than logger. Although there were some blocked threads, it took 28 seconds to process everything. 

<p align="left">
  <img alt="first-test" src="src/main/resources/readme-resources/fourth_test_2.png">
</p>

Probably the best solution would be to research how to log things asynchronously, but I just didn't have time and decided to use this option.

### Fifth test:

Finally, in this fifth test, I removed some unnecessary logs, and used again the buffered reading method, as the blocking method affected when
a client typed 'terminate' and most of the client threads weren't able to finish. They got stuck on that reading until they read something, causing me 
to need the sockets to close abruptly.

<p align="left">
  <img alt="first-test" src="src/main/resources/readme-resources/fifth_test.png">
</p>

At the end, I was able to improve the overall throughput in this way, processing 2M numbers in approximately 25 seconds.


### Developed and tested with a PC with the following specifications:

- Processor: Intel(R) Core(TM) i5-7600K CPU @ 3.80GHz (4 CPUs), ~3.8GHz
- Memory: 8192MB RAM

 
