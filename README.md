# DirectChat

DirectChat is a secure, out-of-band chat system for Minecraft that uses an alternative communication channel outside standard Minecraft chat packets. It consists of a Fabric client-side mod and a Spigot/Paper server-side plugin.

## Features

- **Secure Communication**: Chat messages are sent via HTTP/HTTPS between the client and the server's API, using an alternative channel for private communication.
- **Direct Mode**: Toggle a mode where all your chat messages are automatically redirected to the secure channel.
- **Authentication**: Secure password-based authentication with token-based session management.
- **Captcha Support**: Optional integration with anti-bot systems for added security.
- **Command Blocking**: Prevents accidental leakage of messages through standard chat when in secure mode.
- **Real-time Updates**: Background polling ensures you receive messages instantly.
- **Cloudflared Tunnel Mode**: Automatic Cloudflare Quick Tunnel for servers that cannot open ports (e.g., Aternos, Play.hosting).

## Components

### 1. Fabric Client Mod (`/fabric-mod`)
The client-side component that intercepts chat input and communicates with the DirectChat API.
- **Commands**: `/directchat connect <url> <password>`, `/directchat toggle`, etc.
- **Configuration**: Local `config.json` stores connection details.
- [Detailed Mod Documentation](docs/FABRIC_MOD.md)

### 2. Spigot/Paper Plugin (`/spigot-plugin`)
The server-side component that hosts the API server and manages authenticated sessions.
- **API Server**: Runs on port `36769` (configurable).
- **Cloudflared Tunnel**: Optional auto-tunnel for servers without port access.
- **Access Control**: Restricts chat and commands for unauthenticated players.
- [Detailed Plugin Documentation](docs/SPIGOT_PLUGIN.md)
- [API Specification](docs/API.md)

## Getting Started

### Installation

#### Server (Spigot/Paper)
1. Download the `directchat-plugin.jar`.
2. Place it in your server's `plugins/` folder.
3. Restart the server.
4. Configure the `password` and `port` in `plugins/DirectChat/config.yml`.

#### Client (Fabric)
1. Download the `directchat-mod.jar`.
2. Place it in your Minecraft `mods/` folder.
3. Ensure you have the [Fabric API](https://modrinth.com/mod/fabric-api) installed.
4. Launch the game.

### Usage

1. Connect to the Minecraft server.
2. Use `/directchat connect <url> <password>` to authenticate.
   - Example (direct): `/directchat connect http://myserver.com:36769 secret123`
   - Example (tunnel): `/directchat connect https://abc-xyz.trycloudflare.com secret123`
3. Toggle "Direct Mode" with `/directchat toggle`. When ON, all your standard chat messages will be sent securely through the DirectChat API.

### Cloudflared Tunnel Mode

For servers hosted on platforms that don't allow port forwarding (Aternos, Play.hosting, etc.):

1. Install [cloudflared](https://github.com/cloudflare/cloudflared/releases) on the server.
2. In `config.yml`, set `cloudflared-tunnel-mode: true`.
3. Restart the server.
4. The plugin will automatically create a Cloudflare Quick Tunnel and display the public URL in the console.
5. Use `/directchat tunnel` in-game to view the tunnel URL and connection command.

**Note**: The tunnel URL changes every time the server restarts. Share it with players via your preferred method (Discord, etc.).

## Development

### Requirements
- Java 21+
- Gradle 8.x

### Building

```bash
# Build the Fabric mod
cd fabric-mod
./gradlew build

# Build the Spigot plugin
cd spigot-plugin
./gradlew build
```

## Privacy Notice

DirectChat sends chat messages to a user-chosen server via HTTP/HTTPS. No data is transmitted without the player explicitly connecting via `/directchat connect`. The server-side plugin runs entirely on the server host — all data stays between the connected client and the server. When Cloudflared Tunnel mode is enabled, traffic is routed through Cloudflare's network (see [Cloudflare Privacy Policy](https://www.cloudflare.com/privacypolicy/)).

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
