# DirectChat Fabric Mod

The DirectChat Fabric mod is the client-side component of the DirectChat system. It intercepts player chat messages and redirects them to a secure API hosted by the DirectChat Spigot plugin.

## Features

- **Chat Interception**: Automatically catches outgoing chat messages.
- **Direct Mode**: When enabled, every message you type in the standard chat bar is sent via the secure API.
- **Secure Authentication**: Authenticates with the server using a password and receives a session token.
- **Captcha Handling**: Supports captcha challenges if required by the server.
- **Message Polling**: Periodically fetches new messages from the server and displays them in your chat.

## Commands

| Command | Description |
|---------|-------------|
| `/directchat connect <url> <password>` | Connect to a DirectChat server. |
| `/directchat disconnect` | Disconnect from the current server. |
| `/directchat toggle` | Toggle Direct Mode on/off. |
| `/directchat status` | Show current connection and mode status. |
| `/directchat help` | Show help information. |

## Configuration

The mod stores its configuration in `.minecraft/config/directchat/config.json`.

```json
{
  "currentUrl": "http://example.com:36769",
  "password": "yourpassword",
  "authToken": "...",
  "directModeEnabled": true,
  "secureConnection": false
}
```

- `currentUrl`: The URL of the DirectChat API server.
- `password`: The global password for the server.
- `authToken`: The session token received after authentication.
- `directModeEnabled`: Whether Direct Mode is currently enabled.
- `secureConnection`: Whether the connection is using HTTPS.

## Security Note

- **HTTPS**: It is highly recommended to use HTTPS for the API server to protect your password and chat messages from interception.
- **Command Leakage**: The mod attempts to block standard chat packets when in secure mode, but always be aware of what you are typing.
