# DirectChat System Instructions

## Overview
DirectChat is a secure chat system for Minecraft consisting of two components:
1. **Fabric Client Mod** - Intercepts local chat and redirects to an external API
2. **Spigot/Paper Plugin** - Hosts an internal web server for authenticated chat

---

## Fabric Client Mod

**Objective:** Create a Fabric mod that intercepts the local chat bar and redirects traffic to an external HTTP/HTTPS API.

### Requirements

#### 1. Chat Interception
- Use `ClientSendMessageEvents.ALLOW_CHAT` to cancel the packet if the message starts with `/directchat` or if "Direct Mode" is toggled ON.
- If "Direct Mode" is ON, redirect all standard chat typing to the HTTP POST endpoint instead of the Minecraft server.

#### 2. State Management
- Store `current_url`, `password`, and `auth_token` in a local `config.json`.
- Implement `/directchat connect <url> <password>`.

#### 3. Authentication Flow
- When connecting, send a request to `<url>/api/auth`.
- If the server responds with a `CAPTCHA_REQUIRED` status, render the captcha image in the chat or via a custom screen.
- Once verified, store the `token` provided by the server for all subsequent headers.

#### 4. Background Polling
- Use a `ScheduledExecutorService` to hit `<url>/api/fetch` every 1s.
- Use `MinecraftClient.getInstance().inGameHud.getChatHud().addMessage` to display received messages.

#### 5. Command Blocking
- Ensure the mod prevents sending ANY messages (even `/me` or `/msg`) to the vanilla server if the mod detects it's in "Secure Mode," forcing them through the API.

---

## Spigot/Paper Plugin

**Objective:** Create a server-side plugin that hosts an internal Web Server (using an embedded Javalin or Com.Sun.HttpServer) on port 36679.

### Requirements

#### 1. Embedded API Server
- Initialize a web server on port `36679` upon plugin enable.
- Endpoints required:
  - `/api/auth`: Validates the password and issues a UUID-linked token.
  - `/api/send`: Receives messages via POST.
  - `/api/fetch`: Serves a JSON list of recent messages.

#### 2. Chat & Command Restriction
- Listen to `AsyncPlayerChatEvent` and `PlayerCommandPreprocessEvent`.
- If a player is NOT authenticated via the DirectChat API, cancel all chat and commands (except `/directchat`).
- Send a message: "§cAccess Denied. Please connect via DirectChat Mod to speak."

#### 3. Token System
- Implement `token_for_each_account`. Generate a unique 16-character string per player session.
- Logged messages should bypass the `AsyncPlayerChatEvent` entirely, manually broadcasting to other authenticated DirectChat users.

#### 4. Config Handling
- `config.yml` must allow setting the global password and toggling the token/captcha requirement.

---

## API Endpoints Summary

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth` | POST | Authenticate with password, receive token |
| `/api/send` | POST | Send a chat message (requires token) |
| `/api/fetch` | GET | Fetch recent messages (requires token) |

## Token Format
- 16-character unique string per player session
- UUID-linked for player identification
- Must be included in all authenticated requests
