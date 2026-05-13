# DirectChat API Specification

This document details the HTTP API provided by the DirectChat Spigot plugin.

## Base URL
The API is hosted on the Minecraft server on the configured port (default: `36769`).
`http://<server-ip>:36769`

## Authentication Flow

1. **Auth Request**: Mod sends password and player UUID to `/api/auth`.
2. **Challenge (Optional)**: If captcha is enabled, server returns `CAPTCHA_REQUIRED` with a challenge.
3. **Verification**: Mod sends auth request again with `captcha_response`.
4. **Token**: Server returns a session `token` which must be included in subsequent requests.

## Endpoints

### 1. Authenticate
`POST /api/auth`

**Request Body:**
```json
{
  "uuid": "player-uuid-string",
  "password": "server-password",
  "captcha_response": "15" (optional)
}
```

**Response (Success):**
```json
{
  "status": "OK",
  "token": "16-character-token",
  "player_name": "PlayerName"
}
```

**Response (Captcha Required):**
```json
{
  "status": "CAPTCHA_REQUIRED",
  "captcha_image": "What is 7 + 8?" (or base64 image)
}
```

### 2. Send Message
`POST /api/send`

**Headers:**
- `Authorization: <token>`

**Request Body:**
```json
{
  "message": "Hello world!"
}
```

**Response:**
```json
{
  "status": "OK"
}
```

### 3. Fetch Messages
`GET /api/fetch`

**Headers:**
- `Authorization: <token>`

**Query Parameters:**
- `since`: (Optional) Unix timestamp in milliseconds.

**Response:**
```json
{
  "status": "OK",
  "messages": [
    {
      "sender": "PlayerName",
      "message": "Hello world!",
      "timestamp": 1715580000000
    }
  ]
}
```

### 4. Health Check
`GET /api/health`

**Response:**
```json
{
  "status": "OK",
  "version": "0.1.0.1.2026.05.13"
}
```
