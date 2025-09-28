import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
public class ChatApp {
    public static class Message {
        private final String from;
        private final String toRoom; // null if private message directly to user (toUser used)
        private final String toUser; // if non-null then private message to user
        private final String text;
        private final LocalDateTime timestamp;

        public Message(String from, String toRoom, String toUser, String text) {
            this.from = from;
            this.toRoom = toRoom;
            this.toUser = toUser;
            this.text = text;
            this.timestamp = LocalDateTime.now();
        }

        public String getFrom() { return from; }
        public String getToRoom() { return toRoom; }
        public String getToUser() { return toUser; }
        public String getText() { return text; }
        public LocalDateTime getTimestamp() { return timestamp; }

        @Override
        public String toString() {
            String time = timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            if (toUser != null) {
                return String.format("[%s] (private) %s -> %s: %s", time, from, toUser, text);
            } else {
                return String.format("[%s] %s: %s", time, from, text);
            }
        }
    }

    public interface ChatListener {
        void onMessage(Message message);
        void onUserJoined(String username);
        void onUserLeft(String username);
    }

    public static class ChatRoom {
        private final String roomId;
        private final Map<String, User> users = new ConcurrentHashMap<>();
        private final List<Message> history = Collections.synchronizedList(new ArrayList<>());
        private final ExecutorService notifier = Executors.newCachedThreadPool();

        public ChatRoom(String roomId) {
            this.roomId = Objects.requireNonNull(roomId);
        }

        public String getRoomId() { return roomId; }

        public void join(User user) {
            if (user == null) return;
            users.put(user.getUsername(), user);
            user.setCurrentRoom(this);
            broadcastSystem(String.format("%s joined the room", user.getUsername()));
            // Send history to user asynchronously
            notifier.submit(() -> {
                user.getCommunicator().sendSystem(String.format("Chat history for %s (last %d messages):", roomId, history.size()));
                synchronized (history) {
                    for (Message m : history) {
                        user.onMessage(m);
                    }
                }
            });
        }

        public void leave(User user) {
            if (user == null) return;
            users.remove(user.getUsername());
            user.setCurrentRoom(null);
            broadcastSystem(String.format("%s left the room", user.getUsername()));
        }

        public Set<String> getActiveUsers() {
            return new TreeSet<>(users.keySet());
        }

        public List<Message> getHistory() {
            synchronized (history) {
                return new ArrayList<>(history);
            }
        }

        public void postMessage(Message message) {
            if (message == null) return;

            // If it's a private message to user inside this room, deliver only to target user (and sender).
            if (message.getToUser() != null) {
                User to = users.get(message.getToUser());
                User fromUser = users.get(message.getFrom());
                if (to != null) {
                    // add to history (optional: mark private)
                    synchronized (history) {
                        history.add(message);
                    }
                    // deliver
                    notifier.submit(() -> {
                        if (fromUser != null) fromUser.onMessage(message);
                        to.onMessage(message);
                    });
                } else {
                    // target not found — notify sender
                    if (fromUser != null) {
                        fromUser.getCommunicator().sendSystem("User " + message.getToUser() + " not found in room " + roomId);
                    }
                }
                return;
            }

            // public room message: add to history and notify all users
            synchronized (history) {
                history.add(message);
            }
            for (User u : users.values()) {
                notifier.submit(() -> u.onMessage(message));
            }
        }

        private void broadcastSystem(String sysText) {
            Message systemMessage = new Message("System", roomId, null, sysText);
            synchronized (history) { history.add(systemMessage); }
            for (User u : users.values()) {
                notifier.submit(() -> u.getCommunicator().sendSystem(sysText));
            }
        }

        public void shutdown() {
            notifier.shutdown();
        }
    }

    public static class ChatRoomManager {
        private static volatile ChatRoomManager instance;
        private final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();
        private ChatRoomManager() {}
        public static ChatRoomManager getInstance() {
            if (instance == null) {
                synchronized (ChatRoomManager.class) {
                    if (instance == null) instance = new ChatRoomManager();
                }
            }
            return instance;
        }

        public ChatRoom createOrGetRoom(String roomId) {
            return rooms.computeIfAbsent(roomId, ChatRoom::new);
        }

        public boolean roomExists(String roomId) {
            return rooms.containsKey(roomId);
        }

        public Collection<ChatRoom> listRooms() {
            return Collections.unmodifiableCollection(rooms.values());
        }

        public void removeRoomIfEmpty(String roomId) {
            ChatRoom r = rooms.get(roomId);
            if (r != null && r.getActiveUsers().isEmpty()) {
                r.shutdown();
                rooms.remove(roomId);
            }
        }
    }
    public interface Communicator {
        void send(String payload);       // standard message format
        void sendSystem(String system);  // system messages or meta messages
        String protocolName();
    }

    public static class WebSocketCommunicator implements Communicator {
        private final String username;

        public WebSocketCommunicator(String username) { this.username = username; }

        @Override
        public void send(String payload) {
            // In a real system, this would push to the client's websocket session
            System.out.printf("[WS -> %s] %s%n", username, payload);
        }

        @Override
        public void sendSystem(String system) {
            System.out.printf("[WS (SYSTEM) -> %s] %s%n", username, system);
        }

        @Override
        public String protocolName() { return "WebSocket"; }
    }

    public static class HttpCommunicator implements Communicator {
        private final String username;

        public HttpCommunicator(String username) { this.username = username; }

        @Override
        public void send(String payload) {
            // Simulate long-poll or SSE push
            System.out.printf("[HTTP -> %s] %s%n", username, payload);
        }

        @Override
        public void sendSystem(String system) {
            System.out.printf("[HTTP (SYSTEM) -> %s] %s%n", username, system);
        }

        @Override
        public String protocolName() { return "HTTP-Adapter"; }
    }
    public static class User implements ChatListener {
        private final String username;
        private final Communicator communicator;
        private volatile ChatRoom currentRoom;

        // Local metadata/flags can be added 
        public User(String username, Communicator communicator) {
            this.username = Objects.requireNonNull(username);
            this.communicator = Objects.requireNonNull(communicator);
        }

        public String getUsername() { return username; }
        public Communicator getCommunicator() { return communicator; }
        public ChatRoom getCurrentRoom() { return currentRoom; }
        public void setCurrentRoom(ChatRoom room) { this.currentRoom = room; }

        // User sends a public message to the room
        public void sendMessage(String text) {
            ChatRoom r = currentRoom;
            if (r == null) {
                communicator.sendSystem("You are not in a room.");
                return;
            }
            Message m = new Message(username, r.getRoomId(), null, text);
            r.postMessage(m);
        }

        // User sends a private message to another user in same room
        public void sendPrivateMessage(String toUser, String text) {
            ChatRoom r = currentRoom;
            if (r == null) {
                communicator.sendSystem("You are not in a room.");
                return;
            }
            Message m = new Message(this.username, r.getRoomId(), toUser, text);
            r.postMessage(m);
        }

        // Observer callbacks
        @Override
        public void onMessage(Message message) {
            // Format and send via communicator; could be JSON in real app
            communicator.send(message.toString());
        }

        @Override
        public void onUserJoined(String username) {
            communicator.sendSystem(username + " joined.");
        }

        @Override
        public void onUserLeft(String username) {
            communicator.sendSystem(username + " left.");
        }
    }
    public static void main(String[] args) throws InterruptedException {
        ChatRoomManager manager = ChatRoomManager.getInstance();

        System.out.println("=== ChatApp Demo (console simulation) ===");

        // Create or get a room
        ChatRoom room = manager.createOrGetRoom("Room123");

        // Create users with different communicator adapters (Adapter pattern demonstration)
        User alice = new User("Alice", new WebSocketCommunicator("Alice"));
        User bob   = new User("Bob", new HttpCommunicator("Bob"));
        User charlie = new User("Charlie", new WebSocketCommunicator("Charlie"));

        // Users join room
        room.join(alice);
        room.join(bob);
        room.join(charlie);

        // Show active users
        System.out.println("Active users in " + room.getRoomId() + ": " + room.getActiveUsers());

        // Alice sends a message
        alice.sendMessage("Hello, everyone!");

        // Bob replies
        bob.sendMessage("How's it going?");

        // Private message: Charlie -> Alice
        charlie.sendPrivateMessage("Alice", "Hey Alice, can you review the PR?");

        // Message history persists in memory
        System.out.println("--- Message history snapshot ---");
        for (Message m : room.getHistory()) {
            System.out.println(m);
        }

        // Bob leaves
        room.leave(bob);

        // Alice says goodbye
        alice.sendMessage("Goodbye!");

        // Show active users again
        System.out.println("Active users in " + room.getRoomId() + ": " + room.getActiveUsers());

        // Demonstrate creating another room and singleton manager listing
        ChatRoom room2 = manager.createOrGetRoom("SportsTalk");
        User dave = new User("Dave", new HttpCommunicator("Dave"));
        room2.join(dave);

        System.out.println("Current rooms: ");
        for (ChatRoom r : manager.listRooms()) {
            System.out.println(" - " + r.getRoomId() + " (users: " + r.getActiveUsers().size() + ")");
        }

        // Small delay to let async notifiers finish
        Thread.sleep(500);

        // Clean up
        room.shutdown();
        room2.shutdown();
        System.out.println("=== Demo finished ===");
    }
}
