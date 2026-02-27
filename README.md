# RedPockets - Minecraft Red Packet Plugin

A feature-rich Minecraft red packet plugin that supports both currency red packets and item red packets.

## Features

- **Currency Red Packets**: Supports both random distribution and equal distribution modes
- **Item Red Packets**: Players can put in items for others to draw randomly
- **Visual GUI**: Full graphical interface operation, easy to use
- **Broadcast Messages**: Real-time broadcasts for sending and grabbing red packets
- **Multi-language Support**: Built-in Chinese and English language packs
- **Data Persistence**: Supports MySQL and SQLite databases
- **Economy System**: Compatible with mainstream economy plugins (Vault)

## System Requirements

- Minecraft 1.21+
- Java 17+
- Spigot/Paper/Folia server
- Vault (optional, for currency red packet feature)

## Installation

1. Download the latest version of RedPockets.jar
2. Place the plugin into your server's `plugins` folder
3. Restart the server or load the plugin
4. Configure the `config.yml` file (optional)
5. Restart the server to apply configurations

## Configuration

### Database Configuration

```yaml
database:
  # Database type: mysql or sqlite
  type: sqlite
  
  # MySQL configuration (only needed when type is mysql)
  mysql:
    host: localhost
    port: 3306
    database: redpockets
    username: root
    password: password
```

### Red Packet Settings

```yaml
redpocket:
  # Red packet expiration time (seconds), 0 means never expire
  expire-time: 3600
  
  # Maximum amount for currency red packets
  max-amount: 100000.0
  
  # Maximum number of red packets
  max-count: 100
```

## Commands

### Player Commands

| Command | Description |
|---------|-------------|
| `/redpocket` | Open the red packet creation GUI |
| `/redpocket create` | Create a red packet (opens GUI) |
| `/grab <RedPacketID>` | Grab a specific red packet |
| `/redpocket preview <RedPacketID>` | Preview item red packet contents |

### Admin Commands

| Command | Description |
|---------|-------------|
| `/redpocketadmin delete <RedPacketID>` | Delete a specific red packet |
| `/redpocketadmin reload` | Reload plugin configuration |

## Permissions

| Permission Node | Description | Default |
|-----------------|-------------|---------|
| `redpockets.use` | Use red packet features | OP |
| `redpockets.create` | Create red packets | OP |
| `redpockets.grab` | Grab red packets | OP |
| `redpockets.admin` | Admin commands | OP |

## Usage Guide

### Creating Currency Red Packets

1. Type `/redpocket` to open the main interface
2. Click on the "Currency Red Packet" icon
3. Set the total amount and quantity
4. Choose distribution method (random/equal)
5. Click "Confirm Send"

### Creating Item Red Packets

1. Type `/redpocket` to open the main interface
2. Click on the "Item Red Packet" icon
3. Click "Edit Items" to open the editing interface
4. Drag items into the editing area
5. Return to the main interface and click "Confirm Send"

### Grabbing Red Packets

- Click on the red packet link in the chat
- Or type `/grab <RedPacketID>`

## Support and Feedback

For issues or suggestions, please contact the plugin author.

## Open Source License

This project is open-sourced under the GPL-3.0 License.
