import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Enumeration;


public class peerNetwork {


	public static void main(String[] args) throws SocketException {

		int joinResult=0;

		InetSocketAddress ip = new InetSocketAddress("127.0.0.1", 8767);
		Node bootstrap = new Node(ip,"software");


		InetSocketAddress ip2 = new InetSocketAddress("127.0.0.1", 8796);
		Node joiner = new Node(ip2, "compute");
		joinResult = (int) joiner.joinNetwork(ip, "compute", "software");
		System.out.println("join result is:  " + joinResult);
		
		String words[] = {"software"};

		joiner.indexPage("www.google.com", words);
		joiner.indexPage("www.google.com", words);
		joiner.indexPage("www.bebo.ie", words);
		

		 SearchResult[] results = joiner.search(words);
/*
		InetSocketAddress ip3 = new InetSocketAddress("127.0.0.1", 8797);
		Node joiner2 = new Node(ip3, "chicken");
		joinResult = (int) joiner2.joinNetwork(ip, "chicken", "software");
		System.out.println("join result is:  " + joinResult);

		InetSocketAddress ip4 = new InetSocketAddress("127.0.0.1", 8798);
		Node joiner3 = new Node(ip4, "mouse");
		joinResult = (int) joiner3.joinNetwork(ip, "mouse", "software");
		System.out.println("join result is:  " + joinResult);

		InetSocketAddress ip5 = new InetSocketAddress("127.0.0.1", 8799);
		Node joiner4 = new Node(ip5, "click");
		joinResult = (int) joiner4.joinNetwork(ip, "click", "software");
		System.out.println("join result is:  " + joinResult);

		InetSocketAddress ip6 = new InetSocketAddress("127.0.0.1", 8800);
		Node joiner5 = new Node(ip6, "key");
		joinResult = (int) joiner5.joinNetwork(ip, "key", "software");
		System.out.println("join result is:  " + joinResult);
*/
		printNodeInfo(bootstrap);
		printNodeInfo(joiner);
/*		printNodeInfo(joiner2);
		printNodeInfo(joiner3);
		printNodeInfo(joiner4);
		printNodeInfo(joiner5);
*/

		/*
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
		 */

		Enumeration<?> iterator;
		iterator = bootstrap.routingInformation.keys();
		while(iterator.hasMoreElements()) {
			System.out.println(iterator.nextElement());
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

