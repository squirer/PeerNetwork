import java.net.DatagramSocket;
import java.net.InetSocketAddress;


class SearchResult{
   String words; // strings matched for this url
   String[] url;   // url matching search query 
   long frequency; //number of hits for page
}

interface PeerSearch {
    void init(DatagramSocket udp_socket); // initialise with a udp socket
    long joinNetwork(InetSocketAddress bootstrap_node, String identifier, String target_identifier ); //returns network_id, a locally 
                                       // generated number to identify peer network
    boolean leaveNetwork(long network_id); // parameter is previously returned peer network identifier
    void indexPage(String url, String[] unique_words);
    SearchResult[] search(String[] words);
    int hashCode(String str); 
}
	

