package orca.flukes;

import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

public class TwitterOAuth {
	public static void main(String[] argv) {
		// The factory instance is re-useable and thread safe.
		try {
		Twitter twitter = TwitterFactory.getSingleton();
		Paging p = new Paging(1,10);
		List<Status> statuses = twitter.getUserTimeline("exogeni_ops", p);
		System.out.println("Showing home timeline.");
		for (Status status : statuses) {
			System.out.println(status.getCreatedAt() + ": " + status.getUser().getName() + ":" +
					status.getText());
			
		}
		} catch (TwitterException te) {
			System.err.println(te);
		}
	}
}
