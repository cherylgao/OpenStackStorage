package server;

import static util.StringChecking.getCommandFromInput;
import static util.StringChecking.getFilePathFromUsrAndFile;
import static util.StringChecking.getUsrAndFilePathFromInput;
import static util.FileOperation.writeToFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import util.Constants;

import static util.StringChecking.isNullOrEmpty;

public class Server {
	private static final int HASH_CHUNK_SIZE = 4;
	private static final int NUMBER_OF_REPLICA = 2;
	private static final int HEX_LENGTH = 8;
	private static final int BINARY_LENGTH = 32;
	private static final long MAX_HASHVALUE = (long) Math.pow(2, 32);
	private int portNumber;
	private final ArrayList<Disk> disks;
	private final HashMap<String, HashSet<String>> usrObjectLookUpTable;
	// private final Long[][] partitionDiskTable;
	private final int POWER_NUMBER;
	private long totalSpace;
	private final TreeMap<Long, Node> circleFileMap;
	private final TreeMap<Long, Node> circleDiskMap;
	private final HashMap<Integer, Long> partitionToObject;
	private final HashMap<Long, ArrayList<Integer>[]> objectToPartition;
	private ServerSocket serverSocket;
	private final String diskShift = "";

	public static void main(String[] args) {
		// Server server = new Server(0, null);
		// server.hashValue("Listen to the music");
		Server server = Server.config();
		server.start();
	}

	public Server(int powerNumber, ArrayList<Disk> disks) {
		this.POWER_NUMBER = powerNumber;
		totalSpace = 0;
		this.disks = disks;
		circleFileMap = new TreeMap<>();
		circleDiskMap = new TreeMap<>();
		usrObjectLookUpTable = new HashMap<>();
		// partitionDiskTable = new
		// Long[NUMBER_OF_REPLICA][getPartitionNumber()];
		partitionToObject = new HashMap<>();
		objectToPartition = new HashMap<>();
	}

	public static Server config() {
		System.out.println(
				"please input command to config server, eg: server 16 129.210.16.80 129.210.16.81 129.210.16.83 129.210.16.86");
		Scanner scan = new Scanner(System.in);
		String input = scan.nextLine();
		input = input.trim();
		String[] inputs = input.split(" ");
		int powerNumber = Integer.parseInt(inputs[1]);
		ArrayList<Disk> disksTmp = new ArrayList<Disk>();
		for (int i = 2; i < inputs.length; i++) {
			if (inputs[i].length() > 0) {
				String ip = inputs[i];
				disksTmp.add(new Disk(getHostAddress(ip)));
			}
		}
		Server server = new Server(powerNumber, new ArrayList<>());
		for (int i = 0; i < disksTmp.size(); i++) {
			Disk disk = disksTmp.get(i);
			server.addNodeToCircleDiskMap(disk);
		}
		try {
			server.setServerSocket(getAvailableServerSocket());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return server;
	}

	public static ServerSocket getAvailableServerSocket() throws Exception {
		for (int i = 1024; i <= 65535; i++) {
			try {
				return new ServerSocket(i);
			} catch (Exception e) {
				continue;
			}
		}
		throw new Exception("no available port number");
	}

	public void start() {
		portNumber = serverSocket.getLocalPort();
		System.out.println(
				"server is started, portNumber is " + serverSocket.getLocalPort() + ", wait for client request.");
		try {
			Socket client = serverSocket.accept();
			PrintWriter out = new PrintWriter(client.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			while (true) {
				String commandInput = in.readLine();
				System.out.println(commandInput);
				String command = getCommandFromInput(commandInput);
				String content = null;
				if (command.equals(Constants.UPLOAD)) {

					StringBuffer file = new StringBuffer();
					while (in.ready()) {
						String line = in.readLine();
						file.append(line);
					}
					// System.out.println(file.toString());
					String usrAndFile = getUsrAndFilePathFromInput(commandInput);
					String fileName = getFilePathFromUsrAndFile(usrAndFile);
					content = file.toString();
					writeToFile(fileName, file.toString());
				}
				String res = executeCommand(commandInput, content);
				out.println(res);
				out.flush();
				if (commandInput.equals("bye")) {
					break;
				}
				// CompletableFuture.supplyAsync(()->executeCommand(command));
			}
			client.close();
			// serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String executeCommand(String command, String content) {
		if (isNullOrEmpty(command)) {
			return "invalid command";

		}
		String[] args = command.trim().split(" ");
		if (args == null || args.length == 0) {
			return "invalid command";

		}
		String res = null;
		try {
			switch (args[0]) {
			case Constants.DOWNLOAD:
				res = downloadFile(args);
				break;
			case Constants.LIST:
				res = list(args);
				break;
			case Constants.UPLOAD:
				res = uploadFile(args, content);
				break;
			case Constants.DELETE:
				res = deleteFile(args);
				break;
			case Constants.ADD:
				res = addDisk(args);
				break;
			case Constants.REMOVE:
				res = removeDisk(args);
				break;
			default:
				res = "Unknown command, please try again";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(res);
		showDisk();
		return res;
	}

	public String addDisk(String[] args) {
		String diskName = args[1];

		Disk disk = new Disk(getHostAddress(diskName));		
		addNodeToCircleMap(disk);
		Optional<Long> diskHash = hashValue(diskName + diskShift);
		int index = disk.getBeginPartitionIndex();
		ArrayList<Long> files = new ArrayList<Long>();
		Disk old = getNextDiskByHash(diskHash.get());
		while(index != disk.getEndPartitionIndex()){
			if(partitionToObject.containsKey(index)){
				long fileKey = partitionToObject.get(index);
				files.add(fileKey);
			}
		}
		moveFilesBetweenDisks(files,old);
		return showTable((Disk) circleDiskMap.get(diskHash.get()), " is added which");
	}
	
	public static String getHostAddress(String ip) {
		InetAddress addr;
		try {
			String[] ips = ip.split(":");
			addr = InetAddress.getByName(ips[0]);
			String host = addr.getHostAddress();
			System.out.println("host Value:" + host);
			return host+":"+ips[1];
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static boolean isValidIpAddress(String ip) {
		if (isNullOrEmpty(ip)) {
			return false;
		}
		String[] parts = ip.split("\\.");
		if (parts.length != 4)
			return false;
		for (int i = 0; i < parts.length; i++) {
			try {
				int v = Integer.parseInt(parts[i]);
				if (v < 0 || v > 255) {
					return false;
				}
			} catch (Exception e) {
				return false;
			}
		}

		return true;
	}

	public String removeDisk(String[] args) {
		String diskName = getHostAddress(args[1]);
		Disk disk = new Disk(getHostAddress(diskName));
		StringBuffer res = new StringBuffer();

		Optional<Long> diskHash = hashValue(diskName + diskShift);
		disk = (Disk) circleDiskMap.get(diskHash.get());
		deleteNode(disk);
		Disk nextDisk = getNextDiskByHash(diskHash.get());
		Disk preDisk = getPrevDiskByHash(diskHash.get());

		res.append(showTable(disk, " is deleted which"));
		res.append("////////////");

		res.append(showTable(nextDisk, " is changed which"));
		res.append("////////////");
		res.append(showTable(preDisk, " is changed which"));
		return res.toString();
	}

	private String showTable(Disk disk, String message) {
		StringBuffer res = new StringBuffer("The disk:" + disk.getName() + message + " contains partition number from "
				+ disk.getBeginPartitionIndex() + " to " + disk.getEndPartitionIndex() + ".");
		res.append("partition used with usr/object as follows:");
		int index = disk.getBeginPartitionIndex();
		while (index != disk.getEndPartitionIndex()) {
			if (partitionToObject.containsKey(index)) {
				Long fileKey = partitionToObject.get(index);
				UserFile userFile = (UserFile) circleFileMap.get(fileKey);
				res.append("partition " + index + "size from" + index * getEachPartitionSpaceSize() + " to "
						+ ((index + 1) * getEachPartitionSpaceSize() - 1) + ":");
				res.append(userFile.getUser() + "/" + userFile.getName());
			}
			index++;
			index %= getPartitionNumber();
		}
		return res.toString();
	}

	public String downloadFile(String[] args) throws Exception {
		String[] userAndFile = args[1].split("/");
		String user = userAndFile[0];
		String fileName = userAndFile[1];
		if (userAndFile.length > 2) {
			for (int i = 2; i < userAndFile.length; i++) {
				fileName += "/" + userAndFile[i];
			}
		}
		UserFile userFile = new UserFile(user, fileName);
		StringBuffer res = new StringBuffer();
		ArrayList<Disk> savedDisk = getDiskFromFile(userFile);
		Disk moved = null;
		boolean needMove = false;
		for(int i = 0; i < savedDisk.size();i++){
			Disk disk = savedDisk.get(i);
			try{
				CClient cClient = new CClient(disk);
				String response = cClient.sendToCServer(args[0]+" "+args[1], disk, null);
				if(response != null){
					if(res.length() == 0){
						res.append(response);
					}
				}else{
					needMove = true;
					moved = disk;
				}
			}catch(Exception e){
				System.err.println(disk.getName()+"is down");
				deleteDisk(disk);
			}
		}
		if(needMove){
			CClient cClient = new CClient(moved);
			String response = cClient.sendToCServer(Constants.UPLOAD+" "+args[1], moved, res.toString());
		}
		return res.toString();
	}

	public String uploadFile(String[] args, String content) throws Exception {
		String[] userAndFile = args[1].split("/");
		String res = null;
		String user = userAndFile[0];
		String fileName = userAndFile[1];
		if (userAndFile.length > 2) {
			for (int i = 2; i < userAndFile.length; i++) {
				fileName += "/" + userAndFile[i];
			}
		}
		File file = new File(fileName);
		UserFile userFile = new UserFile(fileName, file.length());
		userFile.setUser(user);
		addNodeToCircleMap(userFile);

		List<Disk> savedDisk = getDiskFromFile(userFile);
		savedDisk.parallelStream().forEach((disk) -> {
			try {
				CClient cClient = new CClient(disk);
				cClient.sendToCServer(args[0] +" "+ args[1], disk, content);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.err.println(disk.getName()+"is down");
				e.printStackTrace();
				try {
					deleteDisk(disk);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

		});
		return getInformationByObjectName(args[1]);
	}

	private String getInformationByObjectName(String usrAndFile) {
		long fileHash = hashValue(usrAndFile).get();
		String res = null;
		UserFile userFile = (UserFile) circleFileMap.get(fileHash);
		if (userFile != null) {
			ArrayList<Integer>[] allPartitions = objectToPartition.get(fileHash);
			ArrayList<Disk> disklist = new ArrayList<Disk>();
			circleDiskMap.values().stream().forEachOrdered((v) -> {
				if (isPartitionInDisk(allPartitions[0].get(0), (Disk) v)) {
					disklist.add((Disk) v);
				}
				if (isPartitionInDisk(allPartitions[1].get(0), (Disk) v)) {
					disklist.add((Disk) v);
				}
			});

			// getNextDiskByHash(userFile);
			res = "file:" + usrAndFile + "is located in disk:" + disklist.get(0).getName() + "and disk:"
					+ disklist.get(1).getName() + ", partitionNumbers :";
			String[] resArr = { res };
			allPartitions[0].stream().forEachOrdered((v) -> {
				resArr[0] += v + ",";
			});
			allPartitions[1].stream().forEachOrdered((v) -> {
				resArr[0] += v + ",";
			});
			return resArr[0];
		} else {
			return "file is not exist";
		}
	}

	public String deleteFile(String[] args) throws Exception {
		String[] userAndFile = args[1].split("/");
		String res = "delete successfully";
		String user = userAndFile[0];
		String fileName = userAndFile[1];
		if (userAndFile.length > 2) {
			for (int i = 2; i < userAndFile.length; i++) {
				fileName += "/" + userAndFile[i];
			}
		}
		UserFile userFile = new UserFile(user, fileName);
		List<Disk> savedDisks = getDiskFromFile(userFile);
		savedDisks.parallelStream().forEach((d) -> {
			CClient cClient;
			try {
				cClient = new CClient(d);
				cClient.sendToCServer(args[0] + " "+args[1], d, null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		});
		deleteNode(userFile);

		return res;
	}

	public String list(String[] args) {
		String user = args[1];
		StringBuffer res = new StringBuffer();
		usrObjectLookUpTable.get(user).stream().forEach((s) -> {
			res.append(s + " ");
		});
		return res.toString().trim();
	}

	public String deleteNode(Node node) {
		try {
			if (node instanceof UserFile) {
				return deleteUserFile((UserFile) node);
			} else if (node instanceof Disk) {
				return deleteDisk((Disk) node);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String deleteUserFile(UserFile file) throws Exception {
		Optional<Long> fileHashValueOptional = hashValue(file.getUser() + "/" + file.getName());
		if (!usrObjectLookUpTable.containsKey(file.getUser())
				|| !(usrObjectLookUpTable.get(file.getUser()).contains(file.getName()))) {
			throw new Exception("user:" + file.getUser() + ",or file:" + file.getName() + " doesn't exist");
		}
		usrObjectLookUpTable.get(file.getUser()).remove(file.getName());
		ArrayList<Integer>[] partitions = objectToPartition.get(fileHashValueOptional.get());
		objectToPartition.remove(fileHashValueOptional.get());
		partitions[0].stream().forEach((v) -> {
			partitionToObject.remove(v);
		});
		partitions[1].stream().forEach((v) -> {
			partitionToObject.remove(v);
		});
		// int partitionNumber = partitions[0].get(0);
		Disk disk = getNextDiskByHash(fileHashValueOptional.get());
		Optional<Long> diskHash = hashValue(disk.getName());
		Disk copyDisk = getNextDiskByHash(diskHash.get() + 1);
		circleFileMap.remove(fileHashValueOptional.get());
		String res = "Detete file:" + file.getName() + "for user " + file.getUser() + "disk:" + disk.getName()
				+ " and disk:" + copyDisk.getName();
		return res;
	}

	public String deleteDisk(Disk disk) throws Exception {
		Optional<Long> curDiskHash = hashValue(disk.getName() + diskShift);
		if (!circleDiskMap.containsKey(curDiskHash.get())) {
			throw new Exception("disk is not exist");
		}
		Disk curDisk = (Disk) circleDiskMap.get(curDiskHash.get());
		Disk nextDisk = getNextDiskByHash(curDiskHash.get());
		Disk preDisk = getPrevDiskByHash(curDiskHash.get());
		nextDisk.setBeginPartitionIndex(curDisk.getBeginPartitionIndex());
		circleDiskMap.remove(curDiskHash.get());
		int oldBegin = curDisk.getBeginPartitionIndex();
		int oldEnd = curDisk.getEndPartitionIndex();
		int index = oldBegin;
		List<Long> fileList = new ArrayList<Long>();
		while (index != oldEnd) {
			if (partitionToObject.containsKey(index)) {
				Long fileKey = partitionToObject.get(index);
				ArrayList<Integer>[] partitions = objectToPartition.get(fileKey);
				if (isPartitionInDisk(partitions[0].get(0), nextDisk)
						&& isPartitionInDisk(partitions[1].get(0), nextDisk)) {
					ArrayList<Integer> movedPartitions = new ArrayList<Integer>();
					for (int i = 0; i < partitions[1].size(); i++) {
						Optional<Integer> movedNumber = findNextAvailablePartitionInDisk(
								preDisk.getBeginPartitionIndex(), preDisk);
						movedPartitions.add(movedNumber.get());
						partitionToObject.remove(partitions[1].get(i));
						partitionToObject.put(movedNumber.get(), fileKey);
					}
					partitions[1] = movedPartitions;
					objectToPartition.put(fileKey, partitions);
				}
				fileList.add(fileKey);
			}
			index++;
			index = index % getPartitionNumber();
		}
		moveFilesBetweenDisks(fileList,disk);
		String res = "";
		return res;
	}

	public boolean isPartitionInDisk(int partitionNum, Disk disk) {
		if (disk.getBeginPartitionIndex() <= disk.getEndPartitionIndex()) {
			return partitionNum >= disk.getBeginPartitionIndex() && partitionNum <= disk.getEndPartitionIndex();
		} else {
			return partitionNum <= disk.getEndPartitionIndex() || partitionNum >= disk.getBeginPartitionIndex();
		}

	}

	public void addNodeToCircleMap(Node node) {
		if (node instanceof Disk) {
			// Optional<Integer>
			addNodeToCircleDiskMap((Disk) node);
		} else if (node instanceof UserFile) {
			addNodeToCircleFileMap((UserFile) node);
		}
	}

	private void addNodeToCircleFileMap(UserFile file) {
		Optional<Long> fileHashValueOptional = hashValue(file.getUser() + "/" + file.getName());
		try {
			if (!fileHashValueOptional.isPresent()) {
				throw new Exception("fileName is invalid");
			}
			long fileHashValue = fileHashValueOptional.get();

			if (circleFileMap.containsKey(fileHashValue)) {
				return;
			}
			usrObjectLookUpTable.computeIfAbsent(file.getUser(), (k) -> new HashSet<>());
			usrObjectLookUpTable.get(file.getUser()).add(file.getName());

			Optional<Integer> curPartition = getPartitionLocation(fileHashValueOptional);
			Disk curDisk = getNextDiskByHash(fileHashValueOptional.get());
			Optional<Integer> validPartition = findNextAvailablePartitionInDisk(curPartition.get(), curDisk);
			if (!validPartition.isPresent()) {
				throw new Exception("number of partitions is not enough");
			}
			saveFileToPartitions(fileHashValue, file, validPartition.get(), curDisk);
			circleFileMap.put(fileHashValue, file);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Disk getNextDiskByHash(Long fileHashValue) {
		Long higherKey = circleDiskMap.higherKey(fileHashValue);
		if (higherKey == null) {
			higherKey = circleDiskMap.firstKey();
		}
		return (Disk) circleDiskMap.get(higherKey);
	}

	private Disk getPrevDiskByHash(Long fileHashValue) {
		Long lowerKey = circleDiskMap.lowerKey(fileHashValue);
		if (lowerKey == null) {
			lowerKey = circleDiskMap.lastKey();
		}
		return (Disk) circleDiskMap.get(lowerKey);
	}

	private Long getDiskHashByFileHash(Long fileHashValue) {
		Long higherKey = circleDiskMap.higherKey(fileHashValue);
		if (higherKey == null) {
			higherKey = circleDiskMap.firstKey();
		}
		return higherKey;
	}

	private void saveFileToPartitions(Long fileHashValue, UserFile file, int cur, Disk disk) {
		Long diskHash = getDiskHashByFileHash(fileHashValue);
		long partitionSize = getEachPartitionSpaceSize();
		long fileSize = file.getSize();
		ArrayList<Integer> partitions = new ArrayList<Integer>();
		ArrayList<Integer> copies = new ArrayList<Integer>();
		ArrayList<Integer>[] allPartitions = new ArrayList[NUMBER_OF_REPLICA];
		Disk nextDisk = getNextDiskByHash(diskHash);
		while (fileSize > partitionSize) {
			Optional<Integer> partitionNumber = findNextAvailablePartitionInDisk(cur, disk);
			partitions.add(partitionNumber.get());
			Optional<Integer> copyPartitionNumber = findNextAvailablePartitionInDisk(nextDisk.getBeginPartitionIndex(),
					nextDisk);
			copies.add(copyPartitionNumber.get());
			partitionToObject.put(partitionNumber.get(), fileHashValue);
			partitionToObject.put(copyPartitionNumber.get(), fileHashValue);
			cur = partitionNumber.get() + 1;
			fileSize -= partitionSize;
		}
		if (fileSize <= partitionSize) {
			Optional<Integer> partitionNumber = findNextAvailablePartitionInDisk(cur, disk);
			partitions.add(partitionNumber.get());
			Optional<Integer> copyPartitionNumber = findNextAvailablePartitionInDisk(nextDisk.getBeginPartitionIndex(),
					nextDisk);
			copies.add(copyPartitionNumber.get());
			partitionToObject.put(partitionNumber.get(), fileHashValue);
			partitionToObject.put(copyPartitionNumber.get(), fileHashValue);
		}
		allPartitions[0] = partitions;
		allPartitions[1] = copies;
		objectToPartition.put(fileHashValue, allPartitions);
	}

	private Optional<Integer> findNextAvailablePartitionInDisk(int cur, Disk disk) {
		if (!partitionToObject.containsKey(cur)) {
			return Optional.of(cur);
		}

		int end = cur != disk.getBeginPartitionIndex() ? cur - 1 : disk.getEndPartitionIndex();
		do {
			if (!partitionToObject.containsKey(cur)) {
				return Optional.of(cur);
			}
			cur++;
			if (cur > disk.getEndPartitionIndex()) {
				cur = disk.getBeginPartitionIndex();
			}
		} while (cur != end);
		return Optional.empty();
	}

	private void addNodeToCircleDiskMap(Disk disk) {
		Optional<Long> diskHashValueOptional = hashValue(disk.getName() + diskShift);
		try {
			if (!diskHashValueOptional.isPresent()) {
				throw new Exception("disk address is empty");
			}
			long diskHashValue = diskHashValueOptional.get();
			if (circleDiskMap.containsKey(diskHashValue)) {
				return;
			}
			if (circleDiskMap.isEmpty()) {
				disk = shiftDisksSpaceAndUpdateTable(null, diskHashValueOptional, disk);
			} else {
				Long lowerDiskKey = circleDiskMap.lowerKey(diskHashValue);
				if (lowerDiskKey == null) {
					Long highestDiskKey = circleDiskMap.lastKey();
					disk = shiftDisksSpaceAndUpdateTable(highestDiskKey, diskHashValueOptional, disk);
				} else {
					disk = shiftDisksSpaceAndUpdateTable(lowerDiskKey, diskHashValueOptional, disk);
				}
			}
			// Long higherDiskKey = circleDiskMap.higherKey(diskHashValue);

			/*
			 * if (higherDiskKey != null && lowerDiskKey == null) { Long
			 * highestDiskKey = circleDiskMap.lastKey();
			 * shiftDisksSpaceAndUpdateTable(highestDiskKey, higherDiskKey,
			 * diskHashValueOptional); }
			 */
			circleDiskMap.put(diskHashValue, disk);
			disks.add(disk);
			totalSpace += disk.getSize();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Disk shiftDisksSpaceAndUpdateTable(Long lowerDiskKey, Optional<Long> currentKey, Disk curDisk) {
		if (circleDiskMap.isEmpty()) {
			// int index = 0;
			int max = getPartitionNumber();
			/*
			 * while (index < max) { partitionDiskTable[0][index] =
			 * currentKey.get(); index++; }
			 */
			int startPartition = (getPartitionLocation(currentKey).get() + 1) % max;
			// int endIndex = currentPartition == 0?max:currentPartition-1;
			curDisk.setBeginPartitionIndex(startPartition);
			curDisk.setEndPartitionIndex(getPartitionLocation(currentKey).get());
			return curDisk;
		}
		int lower = (getPartitionLocation(Optional.of(lowerDiskKey)).get() + 1) % getPartitionNumber();
		Long higherDiskKey = circleDiskMap.higherKey(currentKey.get());
		if (higherDiskKey == null) {
			higherDiskKey = circleDiskMap.firstKey();
		}
		Disk higherDisk = (Disk) circleDiskMap.get(higherDiskKey);

		int cur = getPartitionLocation(currentKey).get();
		higherDisk.setBeginPartitionIndex((cur + 1) % getPartitionNumber());
		circleDiskMap.put(higherDiskKey, higherDisk);
		curDisk.setBeginPartitionIndex(lower);
		curDisk.setEndPartitionIndex(cur);
		/*
		 * while (lower <= cur) { partitionDiskTable[0][lower] =
		 * currentKey.get(); Optional<Integer> copy = getCopyNumber(lower);
		 * copy.ifPresent((v) -> { partitionDiskTable[1][v] = currentKey.get();
		 * }); lower++; lower %= getPartitionNumber(); }
		 */
		return curDisk;
	}

	public Optional<Integer> getCopyNumber(Integer partitionNumber) {
		if (!partitionToObject.containsKey(partitionNumber)) {
			return Optional.empty();
		}
		Long object = partitionToObject.get(partitionNumber);
		ArrayList<Integer>[] partitions = objectToPartition.get(object);
		for (int i = 0; i < partitions[0].size(); i++) {
			if (partitions[0].get(i).intValue() == partitionNumber.intValue()) {
				return Optional.of(partitions[1].get(i));
			}
		}
		return Optional.empty();
	}

	public Optional<Long> hashValue(String inputName) {
		if (isNullOrEmpty(inputName)) {
			return Optional.empty();
		}
		List<String> dividedNames = divideInputNameAccordingToChunkSize(inputName);
		List<Long> dividedNamesDec = new ArrayList<>();
		int[] index = { 0 };
		dividedNames.stream().forEachOrdered((str) -> {
			String hex = toHex(str);
			hex = addZero(hex, HEX_LENGTH);
			if (index[0] % 2 == 0) {
				long decimal = Long.valueOf(hex, 16);
				StringBuffer binary = new StringBuffer(Long.toBinaryString(decimal));
				String reversedBinary = binary.reverse().toString();
				reversedBinary = addZero(reversedBinary, BINARY_LENGTH);
				long reversedDecimal = Long.valueOf(reversedBinary, 2);
				// String reversedHex = Long.toHexString(reversedDecimal);
				dividedNamesDec.add(reversedDecimal);
			} else {
				long decimal = Long.valueOf(hex, 16);
				dividedNamesDec.add(decimal);
			}
			index[0]++;
		});
		long[] value = { 0 };
		dividedNamesDec.stream().forEachOrdered((v) -> {
			value[0] ^= v;
		});
		return Optional.of(value[0]);
	}

	private List<String> divideInputNameAccordingToChunkSize(String inputName) {
		int beginIndex = 0;
		int endIndex = HASH_CHUNK_SIZE;
		List<String> dividedNames = new ArrayList<String>();
		while (beginIndex < inputName.length()) {
			if (endIndex < inputName.length())
				dividedNames.add(inputName.substring(beginIndex, endIndex));
			else {
				dividedNames.add(inputName.substring(beginIndex, inputName.length()));
			}
			beginIndex += HASH_CHUNK_SIZE;
			endIndex += HASH_CHUNK_SIZE;
		}
		return dividedNames;
	}

	private String addZero(String input, int length) {
		StringBuffer str = new StringBuffer(input);
		int size = input.length() % length;
		size = size == 0 ? 0 : length - size;

		for (int i = 0; i < size; i++) {
			str.append("0");
		}
		return str.toString();
	}

	private String toHex(String arg) {
		return String.format("%x", new BigInteger(1, arg.getBytes()));
	}

	public int getPartitionNumber() {
		return (int) Math.pow(2, POWER_NUMBER);
	}

	private long getEachPartitionSpaceSize() {
		return (long) ((double) totalSpace / (double) getPartitionNumber());
	}

	private long getEachPartitionHashDuration() {
		return (long) ((double) MAX_HASHVALUE / (double) getPartitionNumber());
	}

	private Optional<Integer> getPartitionLocation(Optional<Long> hashValue) {
		if (!hashValue.isPresent()) {
			return Optional.empty();
		}
		return Optional.of((int) ((double) hashValue.get() / (double) getEachPartitionHashDuration()));
	}

	public ServerSocket getServerSocket() {
		return serverSocket;
	}

	public void setServerSocket(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	private void showDisk() {
		System.out.println("There are " + circleDiskMap.values().size() + " disks");
		circleDiskMap.values().stream().forEach((n) -> {
			Disk disk = (Disk) n;
			System.out.println(
					disk.getName() + " from " + disk.getBeginPartitionIndex() + " to " + disk.getEndPartitionIndex());
		});
	}

	private ArrayList<Disk> getDiskFromFile(UserFile userFile) throws Exception {
		Optional<Long> fileHashValueOptional = hashValue(userFile.getUser() + "/" + userFile.getName());
		if (!fileHashValueOptional.isPresent()) {
			throw new Exception("fileName is invalid");
		}
		long fileHashValue = fileHashValueOptional.get();
		ArrayList<Integer>[] partitions = objectToPartition.get(fileHashValue);
		ArrayList<Integer> origin = partitions[0];
		ArrayList<Integer> copy = partitions[1];
		ArrayList<Disk> results = new ArrayList<>();
		circleDiskMap.values().forEach((d) -> {
			Disk disk = (Disk) d;
			if (isPartitionInDisk(origin.get(0), disk)) {
				results.add(disk);
			}
			if (isPartitionInDisk(copy.get(0), disk)) {
				results.add(disk);
			}
		});
		return results;
	}
	private ArrayList<Disk> getDiskFromFileHash(Long fileHashValue) throws Exception {		
		ArrayList<Integer>[] partitions = objectToPartition.get(fileHashValue);
		ArrayList<Integer> origin = partitions[0];
		ArrayList<Integer> copy = partitions[1];
		ArrayList<Disk> results = new ArrayList<>();
		circleDiskMap.values().forEach((d) -> {
			Disk disk = (Disk) d;
			if (isPartitionInDisk(origin.get(0), disk)) {
				results.add(disk);
			}
			if (isPartitionInDisk(copy.get(0), disk)) {
				results.add(disk);
			}
		});
		return results;
	}
	private void moveFilesBetweenDisks(List<Long> fileList, Disk from){
		fileList.forEach((f)->{
			try {
				ArrayList<Disk> disks = getDiskFromFileHash(f);
				int i = 0;
				while(disks.get(i).getName().equals(from.getName()) && i < 2){
					i++;
				}
				Disk to = disks.get(i);
				CClient clientDownLoad = new CClient(from);
				UserFile usrAndFile = (UserFile) circleFileMap.get(f);
				String downloadedFile = clientDownLoad.sendToCServer(Constants.DOWNLOAD+ " " +usrAndFile.getUser()+"/"+usrAndFile.getName() , from, null);
				CClient clientDelete = new CClient(from);
				clientDelete.sendToCServer(Constants.DELETE+ " " +usrAndFile.getUser()+"/"+usrAndFile.getName() , from, null);
				CClient clientMove = new CClient(from);
				clientMove.sendToCServer(Constants.DELETE+ " " +usrAndFile.getUser()+"/"+usrAndFile.getName() , to, downloadedFile);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				ArrayList<Disk> disks;
				try {
					disks = getDiskFromFileHash(f);
					int i = 1;					
					Disk to = disks.get(i);
					CClient clientDownLoad = new CClient(from);
					UserFile usrAndFile = (UserFile) circleFileMap.get(f);
					String downloadedFile = clientDownLoad.sendToCServer(Constants.DOWNLOAD+ " " +usrAndFile.getUser()+"/"+usrAndFile.getName() , from, null);
					CClient clientDelete = new CClient(from);
					clientDelete.sendToCServer(Constants.DELETE+ " " +usrAndFile.getUser()+"/"+usrAndFile.getName() , from, null);
					CClient clientMove = new CClient(from);
					clientMove.sendToCServer(Constants.DELETE+ " " +usrAndFile.getUser()+"/"+usrAndFile.getName() , to, downloadedFile);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
			}
		});
	}
}
