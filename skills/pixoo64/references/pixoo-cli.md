# pixoo-cli Reference

The `pixoo-cli` (or `pixoo-cli.exe` on Windows) is the native CLI to control the Divoom Pixoo 64. 

## Auto-Discover Devices
Discover Pixoo 64 devices on the local network (useful to find the IP address).
```bash
pixoo-cli discover
```

## Global Options
Set the target Pixoo 64 device IP address via `-H` / `--host` or environment variable `PIXOO_HOST`:
```bash
pixoo-cli -H 192.168.1.100 <subcommand>
# OR
export PIXOO_HOST="192.168.1.100"
pixoo-cli <subcommand>
```

## Subcommands

### `channel`
Switch between standard channels.
Available channels: `clock, cloud, visualizer, custom, black-screen`
```bash
pixoo-cli channel clock
```

### `brightness`
Set LED brightness (0-100%).
```bash
pixoo-cli brightness 80
```

### `screen`
Power the screen display `on` or `off`.
```bash
pixoo-cli screen off
```

### `rotation`
Rotate the screen contents. Options: `0` (Normal), `90` (Clockwise), `180`, `270`.
```bash
pixoo-cli rotation 90
```

### `text`
Draw text on the screen (`send`) or clear it (`clear`).
```bash
pixoo-cli text send -t "Hello World!" -x 0 -y 24 -c "#00FFFF" --dir left -s 50 -a center
pixoo-cli text clear
```

### `color`
Fill the screen with a solid HTML/CSS hex color.
```bash
pixoo-cli color #23ED23
```

### `image`
Display a static image (PNG/JPG/BMP). The CLI will automatically resize it.
```bash
pixoo-cli image path/to/artwork.png
```

### `gif`
Play an animated GIF.
```bash
pixoo-cli gif --file path/to/animation.gif
pixoo-cli gif --url "http://example.com/animation.gif"
```

### `buzzer`
Trigger the piezoelectric buzzer.
```bash
pixoo-cli buzzer --active-ms 500 --off-ms 500 --total-ms 3000
```

### `raw`
Send a raw JSON payload directly to the device.
```bash
pixoo-cli raw --json '{"Command": "Channel/SetIndex", "SelectIndex": 0}'
```
*Note: You can find all the raw JSON payloads of the Pixoo HTTP protocol in the [SPECIFICATION.md](https://raw.githubusercontent.com/glaforge/jixoo/main/SPECIFICATION.md).*

---
**Tip**: Run `pixoo-cli --help` or `pixoo-cli <subcommand> --help` to see the full list of options, defaults, and requirements for any command!
