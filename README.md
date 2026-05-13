# DirectChat

DirectChat is a secure, out-of-band chat system for Minecraft that allows private communication by bypassing the standard Minecraft chat packets. It consists of a Fabric client-side mod and a Spigot/Paper server-side plugin.

## 🌟 Features

- **Secure Communication**: Chat messages are sent via HTTP/HTTPS directly between the client and the server's API, bypassing standard Minecraft chat logging and interception.
- **Direct Mode**: Toggle a mode where all your chat messages are automatically redirected to the secure channel.
- **Authentication**: Secure password-based authentication with token-based session management.
- **Captcha Support**: Optional integration with anti-bot systems for added security.
- **Command Blocking**: Prevents accidental leakage of messages through standard chat when in secure mode.
- **Real-time Updates**: Background polling ensures you receive messages instantly.

## 📦 Components

### 1. Fabric Client Mod (`/fabric-mod`)
The client-side component that intercepts chat input and communicates with the DirectChat API.
- **Commands**: `/directchat connect <url> <password>`, `/directchat toggle`, etc.
- **Configuration**: Local `config.json` stores connection details.
- [Detailed Mod Documentation](docs/FABRIC_MOD.md)

### 2. Spigot/Paper Plugin (`/spigot-plugin`)
The server-side component that hosts the API server and manages authenticated sessions.
- **API Server**: Runs on port `36769` (configurable).
- **Access Control**: Restricts chat and commands for unauthenticated players.
- [Detailed Plugin Documentation](docs/SPIGOT_PLUGIN.md)
- [API Specification](docs/API.md)

## 🚀 Getting Started

### Installation

#### Server (Spigot/Paper)
1. Download the `directchat-plugin-1.0.1.jar`.
2. Place it in your server's `plugins/` folder.
3. Restart the server.
4. Configure the `password` and `port` in `plugins/DirectChat/config.yml`.

#### Client (Fabric)
1. Download the `directchat-mod.jar`.
2. Place it in your Minecraft `mods/` folder.
3. Ensure you have the [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api) installed.
4. Launch the game.

### Usage

1. Connect to the Minecraft server.
2. Use `/directchat connect <url> <password>` to authenticate.
   - Example: `/directchat connect http://myserver.com:36769 secret123`
3. Toggle "Direct Mode" with `/directchat toggle`. When ON, all your standard chat messages will be sent securely through the DirectChat API.

## 🛠 Development

### Requirements
- Java 21+
- Gradle 8.x

### Building

To build both components, run:

```bash
# Build the Fabric mod
cd fabric-mod
./gradlew build

# Build the Spigot plugin
cd spigot-plugin
./gradlew build
```

The artifacts will be available in:
- `fabric-mod/build/libs/directchat-1.0.0.jar`
- `spigot-plugin/build/libs/directchat-plugin-1.0.1.jar`

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
