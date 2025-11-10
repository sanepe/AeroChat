## AeroChat — ultra‑light chat for Paper 1.21.x

Small, fast chat formatter with modern color features (MiniMessage, gradients, HEX) and optional LuckPerms / PlaceholderAPI integration.

### ✨ Highlights
- Paper 1.21.x only (Java 21)
- LuckPerms prefix/group integration (+ fallback to PlaceholderAPI)
- MiniMessage + gradients + legacy `&` + HEX (`&#RRGGBB`)
- Configurable join messages (private & broadcast)
- Commands: reload config, clear chat
- Zero heavy caching; minimal overhead per message

---

## 📦 Requirements
- Java 21 (JDK 21)
- Paper 1.21.x
- Optional: PlaceholderAPI, LuckPerms

## 🔧 Installation
1. Download the JAR (or build from source).
2. Place it in `plugins/`.
3. Start the server; `config.yml` is generated.
4. Edit `config.yml` and run `/aerochat reload` to apply changes.

## 🗨️ Chat Tokens & Formatting
Available tokens in templates:
- `{prefix}` → resolved from `prefix.<group>` or LuckPerms meta
- `{player}` → player name colored with `player_color`
- `{message}` → original chat component (kept intact; not reparsed)

Supported style syntaxes in static parts:
- Legacy `&` codes (`&a`, `&l`, etc.)
- HEX via `&#RRGGBB`
- MiniMessage tags like `<rainbow>Text</rainbow>` or `<gradient:#ff8800:#ffaa00>Text</gradient>`
- Shortcut: `<#AAAAAA>Your text</#BBBBBB>` becomes a `<gradient:#AAAAAA:#BBBBBB>` wrapper

## ⚙️ Key Config Options
| Key | Description |
|-----|-------------|
| `player_color` | Base color for `{player}` token. |
| `groups` | Ordered list of known groups (used for mapping). |
| `prefix.<group>` | MiniMessage / legacy prefix per group. Fallback to LuckPerms meta prefix. |
| `chat-format` | Template combining tokens. Static parts parsed; `{message}` kept raw. |
| `join.welcome` | Direct message to joining player (enable + text). |
| `join.broadcast.message` | Server-wide join message text. |
| `join.broadcast.show_player_their_broadcast` | If false, only other players see the normal join broadcast. |
| `join.first_broadcast.show_player_their_broadcast` | Same behavior for first join broadcast. |
| `clearchat.lines` | Default line count to push old chat off screen. |
| `clearchat.broadcast.message` | Message shown after clearing chat. |

### Minimal Example
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

---

## 🧪 Commands & Permissions
| Command | Aliases | Permission | Default | Purpose |
|---------|---------|------------|---------|---------|
| `/aerochat reload` | `/aec` | `aerochat.reload` | op | Reload configuration file |
| `/clearchat [lines]` | `/cc` | `aerochat.clearchat` | op | Clear public chat (push blank lines) |

Potential future wildcard: `aerochat.*` (not currently defined).

---

## ⚡ Performance Notes
Runs only on chat/join events and explicit commands. Formatting involves minimal component assembly and optional placeholder expansion. No large caches: adds negligible CPU/memory overhead. `ClearChat` intentionally sends multiple blank messages; capped to avoid abuse.

---

## 🛠️ Build (Windows PowerShell)
With Maven installed:
```powershell
cd C:\Users\USER\Desktop\Plugins\aerochat
mvn -q -DskipTests package
```
Output: `target\aerochat-1.1.0.jar`

Portable Maven (if you lack a global install):
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

---

## 🔍 Troubleshooting
| Symptom | Cause | Fix |
|---------|-------|-----|
| Unsupported API version | Server not 1.21.x | Update Paper to 1.21.x |
| Java class version error | Running Java < 21 | Upgrade JDK/server runtime |
| Placeholders show raw text | Missing PlaceholderAPI or expansion | Install PlaceholderAPI + needed expansions |
| Gradient/mini tags ignored | Using tags inside `{message}` or malformed syntax | Tags only parsed in static template segments; check syntax |

Enable debug (future) if deeper inspection needed.

---

## 🔌 Integration Details
Main class: `me.sanepe.aerochat.PaperBasePlugin`
Built against: `paper-api:1.21.1-R0.1-SNAPSHOT`
`api-version: 1.21`

Soft dependencies are detected at enable-time and reported in console banner.

---

## 📄 License
MIT — see `LICENSE`.

---

## ✅ Roadmap (Ideas)
- Prefix caching to reduce repeated lookups
- Optional global disable flag for chat formatting
- Wildcard permission `aerochat.*`
- Adventure components for command feedback
- Debug toggle for placeholder failures

Contributions & PRs welcome.
