import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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


	/*My own constructor which initialises everything required for the current
	 * Node and also starts the message receiver Thread to constantly listen for
	 * messages */
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
		}
	}



	@Override
	public void init(DatagramSocket udp_socket) {
		dataSock = udp_socket;
	}





	/*The bootstrap Node details are passed here and the word that the joining node will have
	 * along with the target identifier of the bootstrap Node
	 * A JSON message is created and a new datagramPacket is sent to IP and port of bootstrap Node
	 * The routing information of bootstrap node is then added to joining nodes HashTable of routing
	 * information 
	 * */
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

			byte[] sendData = null;
			try {
				sendData = joinMessage.toString().getBytes("UTF8");
				DatagramPacket sendPacket = new DatagramPacket(sendData, 0, sendData.length, 
						bootstrapInfo.getAddress(), bootstrapInfo.getPort());
				dataSock.send(sendPacket);
				routingInformation.put(targetGUID, bootstrap_node.getHostName());
				return 1;
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}
		return 0;
	}




	/*Simply closes the DatagramSocket from any messaging*/
	@Override
	public boolean leaveNetwork(long network_id) {
		dataSock.close();
		return false;
	}



	/*A URL and the strings it is associated with are passed here as arguements
	 * for all of the array of unique words passed, a message must be sent to these
	 *  Nodes one by one requesting to index the passed URL as an arguement*/
	@Override
	public void indexPage(String url, String[] unique_words) {
		int target_GUID=0;
		JSONObject indexMessage = new JSONObject();
		for(int i=0; i< unique_words.length; i++) {
			target_GUID= hashCode(unique_words[i]);
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
					e.printStackTrace();
				}
			} 
			catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}




	/*This method passed an array of words as arguement and the method returns
	 * an array of searchResults for these words
	 * A message is sent to the node whos GUID is the hashCode of each search word
	 * A timeout is then implemented to try to receive the search result within 3 seconds*/
	@Override
	public SearchResult[] search(String[] words) {

		SearchResult[] results = new SearchResult[words.length];
		for(int i =0; i< words.length; i++) {
			int target_GUID = hashCode(words[i]);
			JSONObject searchMessage = new JSONObject();
			try {
				searchMessage.put("type","SEARCH");
				searchMessage.put("word", words[i]);
				searchMessage.put("target_id", target_GUID);
				searchMessage.put("sender_id", GUID);

				byte[] sendData = null;
				InetSocketAddress target = new InetSocketAddress("127.0.0.1", 8767);
				try {
					sendData = searchMessage.toString().getBytes("UTF8");
					DatagramPacket sendPacket = new DatagramPacket(sendData, 0, sendData.length, 
							target.getAddress(), target.getPort());
					dataSock.send(sendPacket);
					dataSock.setSoTimeout(5000);

					byte[] receiveData = new byte[sendData.length];
					DatagramPacket receivePacket = new DatagramPacket(receiveData,receiveData.length);

					while(true){       
						try {
							dataSock.receive(receivePacket);
						}
						catch (SocketTimeoutException e) {
							e.printStackTrace();
						}
					}
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return null;
	}




	/*Given to hash the code of a string to a unique ID providing
	 * the GUID for individual Nodes*/
	@Override
	public int hashCode(String str) {
		int hash = 0;
		for (int i = 0; i < str.length(); i++) {
			hash = hash * 31 + str.charAt(i);
		}
		return Math.abs(hash);
	}





	/*this is a Thread that deals with incoming messages to the current NODE 
	 * It constantly attempts to receive data, if successful it passes the received
	 * message the handleJsonMessage method.*/
	@Override
	public void run() {
		try{
			byte[] receiveData = new byte[1024];
			while(true)
			{
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				dataSock.receive(receivePacket);
				String JSON_message = new String( receivePacket.getData());
				handleJSONmessage( JSON_message, receivePacket);
			}
		}catch (Exception e){
			e.printStackTrace();
		}
	}





	/*This method checks the type of JSON message which was received
	 * and then conducts a switch statement to understand what to do next*/
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
				break;

			case "SEARCH":
				break;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}





	/*Simply checks to see if URL is already indexed if so add 1 to rank otherwise insert 
	 * with rank now = to 1 */
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
	}






	/*replies to a join message with a relay*/
	public void sendJoinRelayMessage(JSONObject receivedMessage, DatagramPacket receivePacket) {
		JSONObject jsonRelay = new JSONObject();

		try {
			targetGUID = (int) receivedMessage.get("target_id");
		} catch (JSONException e1) {
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
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}






	/*This method adds routingInformation to the HASHTABLE "routingInformation of the
	 * current Node
	 * If there are less than 3 neighbours within the routing table for this Node then
	 * immediately add the Node until future Nodes join the network.
	 * Otherwise we must determine if a swap must occur between Nodes already within
	 * our routing table and new Nodes.
	 * A swap should occur if the absolute value of the gap between the current Node and
	 * joining Node is less than that of a gap between a node already within the routing
	 * table and the current Node.
	 * An Iterator is used to advance through the map of keys and values.
	 * boolean swap variable is used to determine when a swap should occur
	 * Routing removes greatest gap valued Node if swap is possible and then adds new Node in
	 * */
	public void addRoutingInfo(JSONObject receivedMessage) {
		int node_id;
		String ip;
		Enumeration<?> iterator;
		try{
			iterator = routingInformation.keys();
			node_id = Integer.parseInt((receivedMessage.get("node_id").toString()));
			ip = receivedMessage.getString("ip_address");
			if(neighboursRouted <3) {
				neighboursRouted++;
				routingInformation.put(node_id, ip);
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
			e1.printStackTrace();
		}
	}
}


