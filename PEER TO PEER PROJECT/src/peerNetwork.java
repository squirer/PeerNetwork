import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.UUID;

public class peerNetwork {

	
	public static void main(String[] args) throws SocketException {
		
		InetSocketAddress ip = new InetSocketAddress("127.0.0.1", 8767);
		Node bootstrap = new Node(ip,"software");
		
		InetSocketAddress ip2 = new InetSocketAddress("127.0.0.1", 8796);
		Node joiner = new Node(ip2, "compute");
		printNodeInfo(bootstrap);
		printNodeInfo(joiner);
	
		int result = (int) joiner.joinNetwork(ip, "compute", "software");
		System.out.println("join result is:  " + result);
		
		
		String uuid; 
		InetSocketAddress ip3;
		int port=8798;
		Node temp;
		int carriedResult;
		for(int i=0; i<20; i++) {
			uuid = UUID.randomUUID().toString();
			 ip3 = new InetSocketAddress("127.0.0.1", port+i);
			 temp = new Node(ip3, uuid);
			 carriedResult = (int)temp.joinNetwork(ip, uuid, "software");
			 System.out.println("Successful: " + carriedResult);
			 printNodeInfo(temp);
		}
	}
	
	
	
	/*prints elements of a Node for testing purposes*/
	public static void printNodeInfo(Node n) {
		System.out.println("word: " + n.word);
		System.out.println("GUID: " + n.GUID);
		System.out.println("host name: " + n.ipInfo.getHostName());
		System.out.println("port: " + n.ipInfo.getPort());
		System.out.println("\n");
	}
	
}

