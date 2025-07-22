# JavaFX ChatApp 💬

A modern desktop chat application built using JavaFX, featuring Firebase authentication, a basic AI chatbot, dark/light themes, emoji sharing, and file-sharing UI.

---

## 📁 Folder Structure

```
chatapp/
├── pom.xml                     # Maven config
├── secretKey.dat               # Encrypted file (possibly for storing preferences)
├── downloads/                  # Screenshots and assets
├── src/
│   ├── main/
│   │   ├── java/com/chatapp/
│   │   │   ├── App.java
│   │   │   ├── AppLauncher.java
│   │   │   ├── Chatbot.java
│   │   │   ├── FirebaseService.java
│   │   │   ├── LoginController.java
│   │   │   ├── JSON.json        # Firebase credentials
│   │   │   ├── CSS.css          # JavaFX UI styling
│   ├── resources/
│   │   ├── chatbotprompts.txt  # Bot response data
│   │   ├── icons/              # UI icons
│   │   ├── images/             # App logos and default images
```

---

## 🔑 Features

* ✅ Firebase authentication via service account
* 🤖 Rule-based chatbot using `chatbotprompts.txt`
* 🌙 Dark & Light theme support using `CSS.css`
* 📁 Emoji & file sharing interface (icons available)
* 📷 Image assets included for profile/default/chat icons

---

## 🔧 How to Run

1. Ensure you have **Java 8** and **Maven** installed.
2. Replace the `JSON.json` file with your own Firebase Admin SDK credentials.
3. Navigate to the `chatapp/` directory and run:

```bash
mvn clean install
mvn exec:java -Dexec.mainClass="com.chatapp.AppLauncher"
```

---

## 🧠 Chatbot Example

```java
Chatbot bot = new Chatbot();
System.out.println(bot.getResponse("hello"));  // → "Hi! How can I assist you today?"
```

---

## ✨ UI Themes & Styling

All style customizations are handled via `CSS.css`. Includes:

* Custom buttons
* Chat bubbles (sent/received)
* Profile image roundings
* Status indicators (online/offline)

## ALTERNATIVE 
You can also unzip the uploaded zip file and get access to complete project
