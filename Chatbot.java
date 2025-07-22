
package com.chatapp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple chatbot implementation for the P2P Chat Application.
 * The chatbot can respond to basic queries, perform simple tasks,
 * and provide helpful information to users.
 */
public class Chatbot {
    private static final String BOT_NAME = "ChatBot";
    private static final String ACTIVATION_COMMAND = "@bot";
    private final App app;
    private final Random random = new Random();
    
    // Basic responses for common queries
    private final Map<String, List<String>> responsePatterns = new HashMap<>();
    
    // Commands that the chatbot can execute
    private final Map<String, String> commandDescriptions = new HashMap<>();
    
    /**
     * Creates a new Chatbot instance.
     * 
     * @param app The main application instance for callbacks
     */
    public Chatbot(App app) {
        this.app = app;
        initializeResponses();
        initializeCommands();
    }
    
    /**
     * Initializes the response patterns for the chatbot
     */
    private void initializeResponses() {
        // Greetings
        responsePatterns.put("greeting", Arrays.asList(
            "Hello there! How can I help you today?",
            "Hi! I'm " + BOT_NAME + ". What can I do for you?",
            "Greetings! How may I assist you?"
        ));
        
        // Help responses
        responsePatterns.put("help", Arrays.asList(
            "I can help with various tasks. Try: 'time', 'date', 'flip a coin', 'roll dice', 'joke', or 'commands' for more options.",
            "Need help? I can tell you the time, date, flip coins, roll dice, tell jokes, and more. Type 'commands' to see all options.",
            "I'm here to assist! Try asking me for the time, date, to flip a coin, roll dice, or tell a joke. For a full list, type 'commands'."
        ));
        
        // Jokes
        responsePatterns.put("joke", Arrays.asList(
            "Why don't scientists trust atoms? Because they make up everything!",
            "Why did the Java developer need glasses? Because they couldn't C#!",
            "How many programmers does it take to change a light bulb? None, it's a hardware problem!",
            "I told my wife she was drawing her eyebrows too high. She looked surprised.",
            "What do you call a fake noodle? An impasta!"
        ));
        
        // Thank you responses
        responsePatterns.put("thanks", Arrays.asList(
            "You're welcome! Let me know if you need anything else.",
            "Glad I could help! Feel free to ask if you have more questions.",
            "No problem at all! I'm here if you need more assistance."
        ));
        
        // Weather responses (simulate since we don't have actual weather data)
        responsePatterns.put("weather", Arrays.asList(
            "I don't have access to real weather data, but I hope it's nice where you are!",
            "Without access to weather services, I can't provide accurate weather information. Consider checking a weather app!",
            "If I could look outside, I would tell you! Maybe check your local weather service?"
        ));
        
        // Goodbye responses
        responsePatterns.put("goodbye", Arrays.asList(
            "Goodbye! Have a great day!",
            "See you later! Don't hesitate to chat again if you need anything.",
            "Farewell! I'll be here if you need assistance later."
        ));
    }
    
    /**
     * Initializes the commands that the chatbot can execute
     */
    private void initializeCommands() {
        commandDescriptions.put("help", "Show available commands and general help");
        commandDescriptions.put("time", "Display the current time");
        commandDescriptions.put("date", "Display the current date");
        commandDescriptions.put("flip", "Flip a coin (heads or tails)");
        commandDescriptions.put("roll", "Roll a dice (1-6)");
        commandDescriptions.put("joke", "Tell a random joke");
        commandDescriptions.put("commands", "List all available commands");
        commandDescriptions.put("about", "Information about the chatbot");
    }
    
    /**
     * Processes a message to determine if the chatbot should respond,
     * and generates an appropriate response if necessary.
     * 
     * @param message The message to process
     * @return true if the chatbot processed the message, false otherwise
     */
    public boolean processMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            System.out.println("DEBUG: Empty or null message received");
            return false;
        }
        
        System.out.println("DEBUG: Processing message: '" + message + "'");
        
        // Check if the message is directed to the bot
        String normalizedMessage = message.toLowerCase().trim();
        if (!normalizedMessage.startsWith(ACTIVATION_COMMAND.toLowerCase())) {
            System.out.println("DEBUG: Message doesn't start with activation command");
            return false;
        }
        
        System.out.println("DEBUG: Message starts with activation command");
        
        // Remove the activation command from the message
        String query = message.substring(ACTIVATION_COMMAND.length()).trim();
        System.out.println("DEBUG: Extracted query: '" + query + "'");
        
        // Process empty queries
        if (query.isEmpty()) {
            System.out.println("DEBUG: Empty query, responding with greeting");
            respondWithRandomPattern("greeting");
            return true;
        }
        
        // Process the query
        processQuery(query);
        return true;
    }
    
    /**
     * Processes a query and generates an appropriate response
     * 
     * @param query The query to process
     */
    private void processQuery(String query) {
        String normalizedQuery = query.toLowerCase().trim();
        System.out.println("DEBUG: Processing query: '" + normalizedQuery + "'");
        
        // Check for commands first
        if (executeCommand(normalizedQuery)) {
            System.out.println("DEBUG: Command executed successfully");
            return;
        }
        
        // Check for patterns
        if (matchesPattern(normalizedQuery, "hello|hi|hey|greetings")) {
            System.out.println("DEBUG: Greeting pattern matched");
            respondWithRandomPattern("greeting");
        } else if (matchesPattern(normalizedQuery, "help|assist|support")) {
            System.out.println("DEBUG: Help pattern matched");
            respondWithRandomPattern("help");
        } else if (matchesPattern(normalizedQuery, "joke|funny|laugh|humor")) {
            System.out.println("DEBUG: Joke pattern matched");
            respondWithRandomPattern("joke");
        } else if (matchesPattern(normalizedQuery, "thanks|thank you|appreciate")) {
            System.out.println("DEBUG: Thanks pattern matched");
            respondWithRandomPattern("thanks");
        } else if (matchesPattern(normalizedQuery, "weather|temperature|forecast")) {
            System.out.println("DEBUG: Weather pattern matched");
            respondWithRandomPattern("weather");
        } else if (matchesPattern(normalizedQuery, "bye|goodbye|farewell|see you")) {
            System.out.println("DEBUG: Goodbye pattern matched");
            respondWithRandomPattern("goodbye");
        } else {
            // Default response for unrecognized queries
            System.out.println("DEBUG: No pattern matched, using default response");
            respond("I'm not sure how to help with that. Type '@bot help' for a list of things I can do.");
        }
    }
    
    /**
     * Executes a command if it matches a known command
     * 
     * @param query The query that might contain a command
     * @return true if a command was executed, false otherwise
     */
    private boolean executeCommand(String query) {
        System.out.println("DEBUG: Checking command: '" + query + "'");
        
        if (query.equals("time")) {
            showTime();
            return true;
        } else if (query.equals("date")) {
            showDate();
            return true;
        } else if (matchesPattern(query, "flip( coin)?")) {
            flipCoin();
            return true;
        } else if (matchesPattern(query, "roll( dice)?|dice")) {
            rollDice();
            return true;
        } else if (query.equals("commands")) {
            listCommands();
            return true;
        } else if (query.equals("about")) {
            showAbout();
            return true;
        } else if (query.equals("help")) {
            respondWithRandomPattern("help");
            return true;
        }
        
        System.out.println("DEBUG: No command matched");
        return false;
    }
    
    /**
     * Shows the current time
     */
    private void showTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalDateTime.now().format(formatter);
        respond("The current time is " + time);
    }
    
    /**
     * Shows the current date
     */
    private void showDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        String date = LocalDateTime.now().format(formatter);
        respond("Today is " + date);
    }
    
    /**
     * Simulates flipping a coin
     */
    private void flipCoin() {
        boolean isHeads = random.nextBoolean();
        respond("I flipped a coin and it's " + (isHeads ? "HEADS" : "TAILS") + "!");
    }
    
    /**
     * Simulates rolling a dice
     */
    private void rollDice() {
        int result = random.nextInt(6) + 1;
        respond("I rolled a dice and got " + result + "!");
    }
    
    /**
     * Lists all available commands
     */
    private void listCommands() {
        StringBuilder sb = new StringBuilder("Available commands:\n");
        
        for (Map.Entry<String, String> entry : commandDescriptions.entrySet()) {
            sb.append("• ").append(ACTIVATION_COMMAND).append(" ").append(entry.getKey())
              .append(" - ").append(entry.getValue()).append("\n");
        }
        
        respond(sb.toString().trim());
    }
    
    /**
     * Shows information about the chatbot
     */
    private void showAbout() {
        respond("I'm " + BOT_NAME + ", a simple chatbot for your P2P Chat Application. " +
                "I can help with basic tasks and answer simple questions. " +
                "Start your message with '" + ACTIVATION_COMMAND + "' to talk to me!");
    }
    
    /**
     * Responds with a random pattern from the specified category
     * 
     * @param category The category of responses to choose from
     */
    private void respondWithRandomPattern(String category) {
        List<String> responses = responsePatterns.get(category);
        if (responses != null && !responses.isEmpty()) {
            String response = responses.get(random.nextInt(responses.size()));
            respond(response);
        } else {
            respond("I'm not sure how to respond to that.");
        }
    }
    
    /**
     * Sends a response message
     * 
     * @param message The message to send
     */
    private void respond(String message) {
        System.out.println("DEBUG: Sending response: '" + message + "'");
        if (app != null) {
            app.addChatbotMessage(message);
        } else {
            System.err.println("ERROR: App instance is null!");
        }
    }
    
    /**
     * Checks if a string matches a pattern
     * 
     * @param input The input string to check
     * @param patternString The pattern to match against
     * @return true if the input matches the pattern, false otherwise
     */
    private boolean matchesPattern(String input, String patternString) {
        try {
            Pattern pattern = Pattern.compile("\\b(" + patternString + ")\\b");
            Matcher matcher = pattern.matcher(input);
            boolean matches = matcher.find();
            System.out.println("DEBUG: Pattern '" + patternString + "' matches '" + input + "': " + matches);
            return matches;
        } catch (Exception e) {
            System.err.println("ERROR: Pattern matching failed: " + e.getMessage());
            return false;
        }
    }
}