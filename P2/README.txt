Test procedure and expectation:
===============================
0. setup jdk-1.8 to change the Java version and 
    cd P2/bin
1. Run the storage process on 129.210.16.82, 129.210.16.83, and 129.210.16.84:
 - "java chunk.CServer"

2. Run the proxy process on or 129.210.16.80 or linux60810:
 - "Server 16 129.210.16.83:xxxx linux60814:yyyy" 

3. Run the user client on or 129.210.16.81 or linux60811:
 - "Client 129.210.16.81 zzzz" 

4. Commands order and expectations:
 - upload mwang2/client/testFile:
   
 - list mwang2
   
 - test download
   - "download mwang2/testFile”, the file testFile should be
         restored back under P2 or correct testFile content displayed.
         
 - test consistency
   nothing is changed here
 - add 129.210.16.82:xxxx, where xxxx is the port reported in step 1
   - Expect to see message displaying some files moved from 83/84 move to 82,
     otherwise -10 points
   - Expect to see some files moved from under /tmp on 83/84 to /tmp under 82,
     otherwise -10 points
 - remove 129.210.16.83:xxxx
   - Expect to see message displaying files moved from 83 move to 82/84,
     otherwise -10 points
   - Expect to see all files moved from under /tmp on 83 to /tmp under 82/84,
     and now no _mwang2_keys* file under /tmp on 83. Otherwise -10 points
 - delete mwang2/testFile
   - Expect to see message says keys1x4w deleted, and no _mwang2/keys1x4w
     under 82/83/84. Otherwise -10 points
   - do the same step above for keys4096x32w
 - list mwang2
   - All files (i.e., only keys2x4w) should displayed in "ls -lrt" format

The package of OpenStackStorage includes Client, Server, Chunk and util four packages. 

For the server package, I include Server.java, Node.java, Disk.java, CClient.java  and UserFile.java. Server.java is mainly for running server side application. CClient is used to communicate with hard drive CServers. Disk and UserFile class are the child class of Node class so I can use polymorphism for better and easier input argument use. 

For the client package, I include Client.java and one testFile.txt. Client.java is mainly used for running client side application and includes functions that can communicate with server and show input and output on the screen. testFile.txt is for testing upload command which can be ignored. 

In terms of Chunk package, there is a CServer.java which is similar to Server.java but designed for the hard drives to communicate with master server/client. It can receive command from master and display the output on the screen and send to client at the same time. 

The util package includes StringChecking.java which has a function called isNullorEmpty which can return a boolean and tell if the string is null or empty and FileOperation class which is used for upload, delete, download file.

Execution:

0. After connection to DC linux server and before compiling any code, please check the version of Java on the linux server using $ java -version.
If the version shows java 1.7, please enter $ setup jdk-1.8 to change the default java version to Java8.
** You don’t need to compile any file since I have already include the .class file in the uploaded file folder under /bin.

1. run CServer
a. go to the folder of bin $ cd P2/bin/
b. connect to 3 or 4 linux server in DC using $ java chunk.CServer and remember the return port number and corresponding IP address.

2. run Server
a. go in the folder of bin $ cd P2/bin/
b. input: $ java server.Server
c. input: $ server 16 129.210.16.80:portNumber1 129.210.16.81:portNumber2 129.210.16.83:portNumber3 129.210.16.86:portNumber4
d. wait for server return portNumber for client.

3. run Client
a. go in the folder of bin $ cd P2/bin/
b.$ java client.Client
c.$ client localhost portNumber
d.Now you can input command as you wish such as:
$ upload cgao/client/testFile
$ list cgao
$ download cage/testFile
$ remove DiskIP
$ add DiskIP
e. testFile can be replaced. /client/testFile is the absolute path of the file.

Module Design:
Server:
1. Space:
The total space is divided into Math.pow(2, partition number) parts. Each disk take one of them. The partitions of this disk start from previous disk in Consistent hash circle, end with current disk.
Partition is named by the its order(first partition is partition 1).

2. Consistent Hash
Use 2 treeMap to do the consistent Hash. One is for file, the Other is for disk. The two treeMap share same hash value space. Hence we can use the file hash to search the next disk in disk tree map.

3.use hash map:partitionToObject to recode partition to object. key is partition number, value is object hashing value.
use hashMap:objectToPartition to recode object to All partitions. the first list is origin , the second list is copy. 

4. Disk and UserFile are the subclass of Node. Disk has beginIndex and endIndex. From beginIndex to end Index contains all the partition number (we design each disk take over the continuous partitions)

5. Config module
a. read input disk
b. create new Server instance
c. call add disk to add each input disk

6. Start Module
while true to read request of client then execute them.

7. Add Disk module
a. add node to disk of treeMap:
b. shiftDisksSpaceAndUpdateTable by update the begin index and end index of relevant disk
c. moveFileBetweenDisks move files to the new disk according to the partition number.

8. delete disk
a. delete node to disk of treeMap:
b. shiftDisksSpaceAndUpdateTable by update the begin index and end index of relevant disk

9. upload and download file:
a. create a CClient object and display content through sendToCServer
b. update two hash map.


Client:
1. Client config is used at first when user on the client side input the connection request and port number he wants to connect to. It will analyze the input and extract the ip address and port number.

2. socket function enable the client to accept the server connection and create InputStream and OutputStream to send out/take in chat to communicate with sever and display the input and output at the same time on both side. 

3. getSocket module returns the Socket object

4. setSocket take a socket as input and assign to make the socket I want.

CServer:
The major functional difference between Server and CServer is CServer has checkSum to hash the value of file name and content and is called when user inputs download user/onjectName. 

1. CServer saves a HashMap fileToChecksum in the memory to store the hash value of file name and file content so when user download the file, the system hash the file replica it finds and compare the hash value of the replica and the value it has to determine if the file is corrupted or not.

CClient:
sendToCServer function is called by Server and display the content the file to client screen.
