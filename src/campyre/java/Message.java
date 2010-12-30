package campyre.java;

import java.util.ArrayList;
import java.util.Date;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Message {
	// special message types
	public static final int UNSUPPORTED = -3;
	public static final int ERROR = -2; // can be used by clients to make messages that didn't come from the Campfire
	public static final int TRANSIT = -1; // can be used by clients to make messages that are still in transit
	
	// Campfire message types
	// The Android client depends on these beginning at 0, and going to SUPPORTED_MESSAGE_TYPES - 1.
	public static final int TEXT = 0;
	public static final int TIMESTAMP = 1;
	public static final int ENTRY = 2;
	public static final int LEAVE = 3;
	public static final int PASTE = 4;
	public static final int TOPIC = 5;
	
	// Here for the Android client: the number of supported message types (keep in sync with the constants above)
	public static final int SUPPORTED_MESSAGE_TYPES = 6;
	
	public int type;
	public String id, user_id, body;
	public Date timestamp;
	
	private String[] inFormat = new String[] {"yy/MM/dd HH:mm:ss Z"};
	
	// This is really just here to serve the Android client. 
	// It really needs the display name to put on the Message object itself for help in adapting it to the list.
	// It violates the intended separation between the two packages, but oh well.
	public String person;
	
	// for making artificial messages (really just intended to serve the Android client)
	// only make them if you know what you're doing (as they'll be missing fields!)
	public Message(String id, int type, String body) {
		this.id = id;
		this.type = type;
		this.body = body;
	}
	
	public Message(JSONObject json) throws JSONException, DateParseException {
		this.type = typeFor(json.getString("type"));
		this.id = json.getString("id");
		
		this.user_id = denull(json.getString("user_id"));
		this.body = denull(json.getString("body"));
		
		this.timestamp = DateUtils.parseDate(json.getString("created_at"), inFormat);
		
		this.person = null;
	}
	
	public static ArrayList<Message> allToday(Room room) throws CampfireException {
		return allToday(room, -1);
	}
	
	public static ArrayList<Message> allToday(Room room, int max) throws CampfireException {
		ArrayList<Message> messages = new ArrayList<Message>();
		try {
			JSONArray items = new CampfireRequest(room.campfire).getList(todayPath(room.id), "messages");
			int length = items.length();
			
			// we want the bottom-most messages, up to a maximum of "max"
			// if max is 0 or -1, then just return everything
			int start;
			if (max > 0 && max < length)
				start = length - max;
			else
				start = 0;
			
			for (int i=start; i<length; i++) {
				Message message = new Message(items.getJSONObject(i));
				
				if (message.type != UNSUPPORTED)
					messages.add(message);
			}
			
		} catch (JSONException e) {
			throw new CampfireException(e, "Could not load messages from their JSON.");
		} catch (DateParseException e) {
			throw new CampfireException(e, "Could not parse date from a message's JSON.");
		}
		return messages; 
	}
	
	private static int typeFor(String type) {
		if (type.equals("TextMessage"))
			return TEXT;
		else if (type.equals("PasteMessage"))
			return PASTE;
		else if (type.equals("TimestampMessage"))
			return TIMESTAMP;
		else if (type.equals("EnterMessage"))
			return ENTRY;
		else if (type.equals("LeaveMessage") || type.equals("KickMessage"))
			return LEAVE;
		else if (type.equals("TopicChangeMessage"))
			return TOPIC;
		else
			return UNSUPPORTED;
	}
	
	public static String todayPath(String room_id) {
		return "/room/" + room_id + "/transcript";
	}
	
	private String denull(String maybeNull) {
		if (maybeNull.equals("null"))
			return null;
		else
			return maybeNull;
	}
}