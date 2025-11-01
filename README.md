## AeroChat — ultra‑light chat for Paper 1.21.x

AeroChat is a tiny, fast chat formatter focused on simplicity and modern color features.

### Highlights
- Paper 1.21.x only (requires Java 21)
- LuckPerms integration (group/prefix) and PlaceholderAPI support
- MiniMessage + gradients + legacy `&` and HEX (`&#RRGGBB`)
- Configurable join messages (private and broadcast)
- Commands: reload and clear chat
- Designed to be extremely low overhead

---

## Requirements
- Java 21 (JDK 21)
- Paper 1.21.x (not compatible with 1.19/1.20)
- Optional (recommended): PlaceholderAPI and LuckPerms

## Installation
1) Download the built JAR (or build from source).
2) Drop it into your server's `plugins/` folder.
3) Start Paper. A default `config.yml` will be generated.

## Commands & permissions
- `/aerochat reload` (alias: `/aec`)
	- Permission: `aerochat.reload` (default: op)
- `/clearchat [lines]` (alias: `/cc`)
	- Permission: `aerochat.clearchat` (default: op)

## Configuration guide (config.yml)
Tokens you can use in templates:
- `{prefix}` — resolved from group mapping or LuckPerms meta
- `{player}` — player name (colorized via `player_color`)
- `{message}` — the original chat component from Paper

What you can write in text parts:
- Legacy colors `&a`, bold `&l`, etc.
- HEX as `&#ff8800`
- MiniMessage tags, e.g. `<#ff8800>Hello</#ffaa00>` or `<gradient:#ff8800:#ffaa00>Hello</gradient>`
	- Shortcut: `<#AAAAAA>Your text</#BBBBBB>` becomes a gradient between those two colors (simple open/close only)

Key options:
- `player_color`: default color for `{player}`
- `groups`: list of group names in priority order (LuckPerms primary is used)
- `prefix.<group>`: prefix per group (accepts `&`, HEX, MiniMessage)
- `chat-format`: chat template combining the tokens above
- `join.welcome` / `join.broadcast`: enable and customize join messages
- `clearchat.lines` and `clearchat.broadcast` for `/clearchat`

Minimal example:
```yaml
player_color: "&f"
groups: [admin, mod, default]
prefix:
	admin: "<b><gradient:#C3613A:#E28787>ADMIN</gradient></b>"
	mod: "<b><gradient:#5732C4:#7779C4>MOD</gradient></b>"
	default: "<b><gradient:#857D7D:#D6D6D6>PLAYER</gradient></b>"
chat-format: "{prefix} &7{player}&7: &f{message}"
join:
	welcome:
		enabled: true
		message: "&aWelcome, {player}&a!"
	broadcast:
		enabled: true
		message: "<#f5a623>{player}</#f5a623> &ejoined the server"
```

## Compatibility
- Built against `paper-api:1.21.1-R0.1-SNAPSHOT`
- `api-version: '1.21'` — it will not load on older major versions

## Performance
AeroChat only runs on chat/join events and command invocations. It keeps no large caches and performs minimal string/component work per message. Expect negligible CPU and memory impact, even with MiniMessage. ClearChat sends N blank lines by design (brief burst of messages).

## Build (Windows PowerShell)
With Maven installed:
```powershell
cd C:\Users\USER\Desktop\plugin
mvn -q -DskipTests package
```
Output: `target/aerochat-1.0.0-SNAPSHOT.jar`

Without Maven installed (portable Maven):
```powershell
$mvnVer = "3.9.9"
$zipUrl = "https://downloads.apache.org/maven/maven-3/$mvnVer/binaries/apache-maven-$mvnVer-bin.zip"
$destZip = "$env:USERPROFILE\Downloads\apache-maven-$mvnVer-bin.zip"
$toolDir = "$env:USERPROFILE\tools"
$mvndir = "$toolDir\apache-maven-$mvnVer"
Invoke-WebRequest -Uri $zipUrl -OutFile $destZip
New-Item -ItemType Directory -Force -Path $toolDir | Out-Null
Expand-Archive -Path $destZip -DestinationPath $toolDir -Force
& "$mvndir\bin\mvn.cmd" -q -DskipTests package
```

## Troubleshooting
- “Unsupported API version '1.21'” → Your server isn’t 1.21.x.
- “Java 21 required” or class version errors → Update your JDK/server to Java 21.
- Placeholders aren’t resolving → Verify PlaceholderAPI is installed and the specific expansion you use is present.
- Gradients/tags not applying → Ensure you’re using MiniMessage syntax in the static parts (tokens like `{message}` are kept as components and not reparsed).

—

Main class: `me.sanepe.aerochat.PaperBasePlugin`

## License
MIT — see `LICENSE` for details.
