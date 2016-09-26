# FacebookSocialSNAP
The SNAP Data and it's Fiddlings!

1. The Code is Written in Java. The directory Neo4jGraphs contains all the codes and the resources need to exectute the program. The main source code file is located in "himangshu_p2_csc555_report_code/Neo4JGraphs/src/main/java/FacebookGraphs.java"

2. You have to download and integrate Neo4J to run this project. The maven dependencies are alreay in the POM file, so it should download the necesssary Neo4J packages. If there's any issue, kindly consult the officical Neo4j website.
http://docs.neo4j.org/chunked/stable/tutorials-java-embedded-setup.html

3. The Code takes all the Data from the edgelist, so it has two different relationships for every bidirectional relationship.

4. It uses the apache math and Statistics library, they are included in the project. If any issues please refer to
http://commons.apache.org/proper/commons-math/userguide/stat.html

5. The Graph is loaded form the Edgelists in the runtime and is kept in the main memory for fatser processing.

6. There are two modes of operation.

-> Batch :: Type ALL in the beginning when it asks input, it will load the ego nets for the 10 users one by one and calculate everything for each customer. It will take up some time.

-> Single :: Enter the node ID from the lost 0, 107, 348, 414, 686, 698, 1684, 1912, 3437, 3980 to get the results for only those nodes. Takes about 15 seconds for the processing of one user, may change depending on the system.

7. There are cases where we have features in the FEAT files for Nodes not even present in the EGONET of the user, same happended for the CIRCLEs too.All those cases are reported to the Standard output.

8. All the errors and regular outputs are printed to the standard console.

9. The project was developed in IntelliJIdea as a Maven project, please use the same IDE for best results.

10. Finally, please contact me in hborah@ncsu.edu or 9197858515 for any trouble running or understanding the code.
