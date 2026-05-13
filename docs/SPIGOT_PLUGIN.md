# DirectChat Spigot Plugin

The DirectChat Spigot plugin is the server-side component of the DirectChat system. It hosts an internal web server that provides an API for the Fabric mod to communicate with.

## Features

- **Embedded Web Server**: Hosts a lightweight API server (default port 36769).
- **Session Management**: Manages authenticated tokens for players.
- **Chat Relay**: Receives messages from the mod and broadcasts them to other authenticated players.
- **Access Control**: Can be configured to block unauthenticated players from using standard chat.
- **Captcha Integration**: Supports integration with anti-bot plugins for captcha verification.

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth` | POST | Authenticate with password, receive a session token. |
| `/api/send` | POST | Send a chat message (requires valid token). |
| `/api/fetch` | GET | Fetch recent messages (requires valid token). |
| `/api/health` | GET | Check API server health. |

## Configuration (`config.yml`)

```yaml
# Global password for authentication
password: "changeme"

# API server port
port: 36769

# HTTPS settings
require-https: false
keystore-path: ""
keystore-password: ""

# Captcha provider integration
# Options: none, nantibot, captcha-api
captcha-provider: none

# Message history settings
message-history-size: 100

# Token expiry time in seconds (0 = never expires)
token-expiry: 3600

# Debug mode
debug: false
```

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `directchat.admin` | Allows access to DirectChat admin commands. | op |
| `directchat.bypass` | Bypass DirectChat authentication requirement. | false |

## Commands

- `/directchat reload`: Reloads the plugin configuration.
- `/directchat stats`: Shows API server statistics.

## Security

- **Password**: Change the default password immediately.
- **Firewall**: Ensure the configured port (36769) is open on your server's firewall if you want players to connect from outside the local network.
- **HTTPS**: For production use, it is strongly recommended to configure HTTPS by providing a keystore.
