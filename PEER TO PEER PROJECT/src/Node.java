import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Hashtable;
import org.json.JSONException;
import org.json.JSONObject;


public class Node implements PeerSearch , Runnable {
	int GUID;
	InetSocketAddress ipInfo;   
	DatagramSocket dataSock;
	Thread messageReceiver;
	String word;
	int targetGUID;
	InetSocketAddress bootstrapInfo;

	Hashtable <String, Integer> indexes = new Hashtable<String,Integer>();

	Hashtable<Integer, String> routingInformation = new Hashtable<Integer, String>();
	int neighboursRouted = 0;

	public Node(String word) {
		GUID = word.hashCode();
	}



	public Node(InetSocketAddress ip, String word) {
		this.word = word;
		ipInfo = ip;
		GUID = word.hashCode();
		try {
			dataSock = new DatagramSocket(ipInfo.getPort());
			messageReceiver = new Thread(this, "receiving message Thread");
			messageReceiver.start(); 
		} catch (SocketException e) {
			e.printStackTrace();
			System.out.println("unsuccessful Allocation of portNumber...................." );
		}
	}



	@Override
	public void init(DatagramSocket udp_socket) {
		dataSock = udp_socket;
	}



	@Override
	public long joinNetwork(InetSocketAddress bootstrap_node, String identifier, String target_identifier) {
		bootstrapInfo = bootstrap_node;
		JSONObject joinMessage = new JSONObject();

		targetGUID = hashCode(target_identifier);
		try {
			joinMessage.put("type","JOINING_NETWORK_SIMPLIFIED");
			joinMessage.put("node_id", GUID);
			joinMessage.put("target_id", targetGUID);
			joinMessage.put("ip_address", ipInfo.getHostString());

			//	System.out.println("\n" + "JSON SEND DATA by GUID:  "+ GUID + "  " + joinMessage.toString() + "\n");

			byte[] sendData = null;
			try {
				sendData = joinMessage.toString().getBytes("UTF8");
				DatagramPacket sendPacket = new DatagramPacket(sendData, 0, sendData.length, 
						bootstrapInfo.getAddress(), bootstrapInfo.getPort());
				//System.out.println("\n" + "SENDPACKETINFO:  " + bootstrapInfo.getAddress() + "  , " + bootstrapInfo.getPort());
				dataSock.send(sendPacket);
				routingInformation.put(targetGUID, bootstrap_node.getHostName());
				//System.out.println("---------------GUID: " +targetGUID+ "-------HOSTNAME: " +  bootstrap_node.getHostName() );
				return 1;
			} 
			catch (IOException e) {
				System.out.println("IO EXCEPTION THROWN HERE!!!");
			}
		} 
		catch (JSONException e) {
			System.out.println("JSON ENCODING EXCEPTION........!!!!");
		}
		return 0;
	}



	@Override
	public boolean leaveNetwork(long network_id) {
		// TODO Auto-generated method stub
		return false;
	}



	@Override
	public void indexPage(String url, String[] unique_words) {
		int target_GUID=0;
		JSONObject indexMessage = new JSONObject();
		for(int i=0; i< unique_words.length; i++) {
			target_GUID= hashCode(unique_words[i]);
			//System.out.println("TARGET GUID FOR INDEX MESSAGE IS:  " + target_GUID);
			try {
				indexMessage.put("type","INDEX");
				indexMessage.put("target_id", target_GUID);
				indexMessage.put("sender_id", GUID);
				indexMessage.put("keyword", unique_words[i]);
				indexMessage.put("link", url);

				byte[] sendData = null;
				InetSocketAddress target = new InetSocketAddress("127.0.0.1", 8767);
				try {
					sendData = indexMessage.toString().getBytes("UTF8");
					DatagramPacket sendPacket = new DatagramPacket(sendData, 0, sendData.length, 
							target.getAddress(), target.getPort());
					dataSock.send(sendPacket);
				} 
				catch (IOException e) {
					System.out.println("IO EXCEPTION THROWN HERE!!!");
				}
			} 
			catch (JSONException e) {
				System.out.println("JSON ENCODING EXCEPTION........!!!!");
			}
		}
	}

	@Override
	public SearchResult[] search(String[] words) {
		return null;
	}





	@Override
	public int hashCode(String str) {
		// TODO Auto-generated method stub
		int hash = 0;
		for (int i = 0; i < str.length(); i++) {
			hash = hash * 31 + str.charAt(i);
		}
		return Math.abs(hash);
	}




	@Override
	/*this Thread deals with incoming messages to "this" NODE */
	public void run() {
		try{
			byte[] receiveData = new byte[1024];
			while(true)
			{
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				dataSock.receive(receivePacket);
				String JSON_message = new String( receivePacket.getData());
				//		System.out.println("RECEIVED By GUID: "+ GUID + "  " + JSON_message); // this prints bytes -- we need JSON now

				handleJSONmessage( JSON_message, receivePacket);
			}
		}catch (Exception e){
			System.out.println("THERE IS A PROBLEM WITH THIS RECEIVER THREAD");
			e.printStackTrace();
		}
	}

	/*This method will figure out what to do with the JSON message*/
	public void handleJSONmessage(String JSON_message, DatagramPacket receivePacket) {

		String messageType = "";
		try {
			JSONObject json = new JSONObject(JSON_message);

			messageType = json.get("type").toString();
			switch (messageType) {

			case "JOINING_NETWORK_SIMPLIFIED":
				addRoutingInfo(json);
				sendJoinRelayMessage(json, receivePacket);
				break;

			case "JOINING_NETWORK_RELAY_SIMPLIFIED":
				break;

			case "INDEX":
				addURLAndUpdateRank(json);
				//System.out.println("INDEX MESSAGE RECEIVED........");
				break;
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}


	/*Simply checks to see if URL is already indexed if so add 1 to rank otherwise insert 
	 * with rank now = to 1*/
	public void addURLAndUpdateRank(JSONObject json) {
		int rank=0;
		try {
			if(indexes.containsKey(json.get("link"))) {
				System.out.println("JSON ALREADY CONTAINS THE KEY ++++ " + json.get("link"));
				rank = indexes.get(json.get("link").toString()) + 1; 
			}
			else {
				rank = 1;
			}
			indexes.put(json.get("link").toString(), rank);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		System.out.println("HASH TABLE IS........." + indexes.toString());
	}

	
	
	/*replies to a join message with a relay*/
	public void sendJoinRelayMessage(JSONObject receivedMessage, DatagramPacket receivePacket) {
		JSONObject jsonRelay = new JSONObject();

		try {
			targetGUID = (int) receivedMessage.get("target_id");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			jsonRelay.put("type","JOINING_NETWORK_RELAY_SIMPLIFIED");
			jsonRelay.put("node_id", GUID);
			jsonRelay.put("target_id", receivedMessage.get("node_id"));
			jsonRelay.put("gateway_id", targetGUID);

			byte[] sendData = null;
			try {
				sendData = jsonRelay.toString().getBytes("UTF8");
				DatagramPacket sendPacket = new DatagramPacket(sendData, 0, sendData.length, 
						receivePacket.getAddress(), receivePacket.getPort());
				dataSock.send(sendPacket);
				//	System.out.println("The Node GUID:  " + GUID + "  received join Request from Address:  " +
				//		receivePacket.getAddress() + "  and port:  " + receivePacket.getPort() );
			} 
			catch (IOException e) {
				System.out.println("IO EXCEPTION THROWN HERE!!!");
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			System.out.println("PROBLEM WITH JSON RELAY..........");
			e.printStackTrace();
		}
	}


	/*This method adds routingInformation*/
	public void addRoutingInfo(JSONObject receivedMessage) {
		int node_id;
		String ip;
		Enumeration<?> iterator;
		try{
			iterator = routingInformation.keys();
			node_id = Integer.parseInt((receivedMessage.get("node_id").toString()));
			System.out.println("node_ID:  -------" + node_id);
			ip = receivedMessage.getString("ip_address");
			if(neighboursRouted <3) {
				neighboursRouted++;
				routingInformation.put(node_id, ip);
				//	System.out.println("neighbours: " + neighboursRouted);
				//	System.out.println("------------------------------------------" + "\n" + routingInformation + "\n");
			}
			else {
				boolean swap = false;
				int swapReferenceGUID=0;
				int newNodeGap = Math.abs(GUID - node_id);
				int currentGUID = 0;
				int thisGap = 0;
				while(iterator.hasMoreElements()) {
					currentGUID = Integer.parseInt(iterator.nextElement().toString());
					thisGap = Math.abs(currentGUID - GUID);
					if(newNodeGap < thisGap) {
						swap = true;
						if(swapReferenceGUID < currentGUID) {
							swapReferenceGUID = currentGUID;
						}
					}
				}
				if(swap == true) {
					routingInformation.remove(swapReferenceGUID);
					routingInformation.put(node_id, ip);
				}
			}
		}
		catch(JSONException e1) {
			System.out.println("Problem with JSON - exception thrown");
		}
	}
}


