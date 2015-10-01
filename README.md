# self-replicate
A program in Java that Self Replicates

## About

This is a team assignment for a security class at EWU. Requirements are to have program replicate itself using identical code each time. One method, that is more common, is to have string variables containing code to write to file and compile and run. This program uses a less common approach. This program will decompile the Jar file used to execute it down to source in a new directory, compile that new source back to a Jar file, and execute new Jar file through use of command line tools. Starting at the second generation, the code that is decompiled and re-compiled reaches a stable state and is identical from this point on for each generation thereafter.

## Supported platforms

Currently, the program has succesfully run on Mac OSX Yosemite and Debian 8. At the moment, it is assumed to work on Unix based OSes until proven otherwise. Code for Windows support exists, but currently does not work. We are working on having it fully supported on the Windows platform.

## How to use

1. cd to location of .java
2. create output directory of compiling files `mkdir classes`
3. compile Java files `javac -d classes Main.java Replicator.java UnixBasedReplicator.java WindowsBasedReplicator.java`
4. create Jar file `jar -cfm ../../self-replicate.jar ../META-INF/MANIFEST.MF Main.class Replicator.class UnixBasedReplicator.class WindowsBasedReplicator.class`
5. cd to Jar file
6. execute jar file `java -jar self-replicate.jar [number] -{xspdci}`

number: specifies number of generations to create, optional

-x: specifies that each generation should cleanup any files and folders required to create the next generation

-s: specifies that std out and std err outputs should be tunneled back to console

-p: specifies that a generation should delete its parent and pass its location to its child

-d: specifies program should halt after decompiling itself, use for debugging

-c: specifies program should halt after compiling source for next generation, use for debugging

-i: specifies that program should run indefinitely, may result in computer crash due to either no storage or RAM space

more functionality to come...

## Note

This program has malicious potential and should be used with extreme caution. This project is under the MIT License. We share no liability of any harm that comes from the usage of this code. You are to use this at your own risk. 
