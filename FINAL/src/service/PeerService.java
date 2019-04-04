package service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Random;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import protocol.Protocol;
import channels.MC;
import channels.MDB;
import channels.MDR;
import channels.Messenger;
import data.Storage;
import database.Database;

public class PeerService {

	private static MulticastSocket socket;
	private static Peer localPeer;
	private static Messenger messenger;

	private static volatile Database database;
	private static final String DATABASE_STRING = "../database.data";
	private static File db;

	private static Storage storage;
	private static final String STORAGE_STRING = "../storage.data";
	private static File st;

	private static volatile MC mcThread;
	private static volatile MDB mdbThread;
	private static volatile MDR mdrThread;

	private static boolean isClient = false;

	public static String defaultServer = "225.0.0.0";
	public static int default_PeerPort = 8000;
	public static int default_MCport = 8001, default_MDBport = 8002, default_MDRport = 8003;

	public static void main(String args[]) throws Exception {		
		// check whether args are empty
		if(args.length < 2) {
			System.err.println("ERROR: Must call this application with at least <IP-address> <port-number>");
			return;
		}
		
		defaultServer = args[0];
		default_PeerPort = Integer.parseInt(args[1]);
		
		if(default_PeerPort < 1024) {
			System.err.println("ERROR: Port number can't be below 1024");
			return;
		}
		
		if(default_PeerPort == default_MCport || default_PeerPort == default_MDBport || default_PeerPort == default_MDRport) {
			System.err.println("ERROR: Port number is already taken");
			return;
		}

		socket = new MulticastSocket(default_PeerPort);
		localPeer = new Peer(getPeerAddr(), socket.getLocalPort());

		
		System.out.println("\nSTARTED PEER SERVICE");

			Messenger messenger = new Messenger(socket, localPeer, InetAddress.getByName(defaultServer));
			new Thread(messenger).start();
		
		
		//	Multicast Channels threads
		mcThread = new MC(InetAddress.getByName(defaultServer), default_MCport, localPeer.get_ip());
		new Thread(mcThread).start();
		mdbThread = new MDB(InetAddress.getByName(defaultServer), default_MDBport, localPeer.get_ip());
		new Thread(mdbThread).start();
		mdrThread = new MDR(InetAddress.getByName(defaultServer), default_MDRport, localPeer.get_ip());
		new Thread(mdrThread).start();

		
		db = new File(DATABASE_STRING);
		if(!db.exists()) 
			createDatabase();
		else 
			loadDatabase();

		st = new File (STORAGE_STRING);
		if(!st.exists())
			createStorage();
		else
			loadStorage();

		if(args.length > 2)
			InterpretArgs(args);
	}
	
	public static void InterpretArgs(String[] args) {
		switch(args[2]) {
		case "BACKUP":
			if(args.length != 5)
				System.err.println("ERROR: Wrong number of args for BACKUP protocol. Try <BACKUP> <filename> <replication degree>");
			else
				Protocol.initiateBackup(args[3], Integer.parseInt(args[4]));
			break;
		case "RESTORE":
			if(args.length != 4)
				System.err.println("ERROR: Wrong number of args for RESTORE protocol. Try <RESTORE> <filename>");
			else
				Protocol.initiateRestore(args[3]);
			break;
		case "DELETE":
			if(args.length != 4)
				System.err.println("ERROR: Wrong number of args for DELETE protocol. Try <DELETE> <filename>");
			else
				Protocol.initiateDelete(args[3]);
			break;
		default:
			System.out.println("Protocol " + args[2] + " does not exist\n");
			break;
		}
	}

	public static Peer getLocalPeer() {
		return localPeer;
	}

	// Function for getting the local peer's address
	public static InetAddress getPeerAddr() throws IOException {
		MulticastSocket socket = new MulticastSocket();
		socket.setTimeToLive(0);

		InetAddress addr = InetAddress.getByName("224.0.0.0");
		socket.joinGroup(addr);

		byte[] bytes = new byte[0];
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, addr,
				socket.getLocalPort());

		socket.send(packet);
		socket.receive(packet);

		socket.close();

		return packet.getAddress();
	}

	public static MulticastSocket getSocket() {
		return socket;
	}


	/*
	 * database functions
	 */

	private static void createDatabase(){
		database = new Database();

		saveDatabase();
	}

	public static Database getDatabase() {
		return database;
	}

	public static void saveDatabase(){
		try{
			FileOutputStream fileOutputStream = new FileOutputStream(DATABASE_STRING);
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

			objectOutputStream.writeObject(database);

			objectOutputStream.close();

		} catch (FileNotFoundException e){
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}

	}

	private static void loadDatabase() throws ClassNotFoundException, IOException{
		try{
			FileInputStream fileInputStream = new FileInputStream(DATABASE_STRING);
			ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

			database = (Database) objectInputStream.readObject();

			objectInputStream.close();
		} catch(FileNotFoundException e){
			e.printStackTrace();
		}
	}

	private static void createStorage(){
		storage = new Storage();

		saveStorage();
	}

	public static void saveStorage() {
		try{
			FileOutputStream fileOutputStream = new FileOutputStream(STORAGE_STRING);
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

			objectOutputStream.writeObject(storage);

			objectOutputStream.close();
		} catch (FileNotFoundException e) {
			System.err.println("ERROR: Storage Not Found");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("STORAGE: current local storage = " + Storage.getFreeStorage() + "bytes");
	}

	private static void loadStorage() throws ClassNotFoundException, IOException {
		try {
			FileInputStream fileInputStream = new FileInputStream(STORAGE_STRING);
			ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);

			storage = (Storage) objectInputStream.readObject();

			objectInputStream.close();
			System.out.println("STORAGE: current local storage = " + Storage.getFreeStorage() + "bytes");
			
		} catch (FileNotFoundException e) {
			System.out.println("Storage Not Found");

		}
	}



}
