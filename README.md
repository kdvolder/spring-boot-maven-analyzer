Spring Boot Maven Analyzer
==========================

Quick and dirty implementation of a standalone Java app that
analyzes a sample spring boot app using maven.

To run this code
 - import into Eclipse as an 'existing maven project'
 - Select class BootDependencyAnalyzer and 'Run as Java Application'.
 
The program will analyze the 'sample' app that is embedded in the project
in the 'sample' directory.

It will output an xml file at
boot-completion-data.txt in the project root dir.

Note:
  - using .txt extension instead of xml otherwise Eclipse will 
    croak/hang trying to validate the large xml file.
