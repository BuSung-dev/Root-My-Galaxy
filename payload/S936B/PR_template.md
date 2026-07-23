### Add payload for Samsung Galaxy S25+ (SM-S936B) — BP4A.251205.006.S936BXXUACZF1
**Descripción:**
Añado la estructura inicial de payload y metadatos para el modelo **Samsung Galaxy S25+ (SM-S936B)**. 
Debido a que el dispositivo utilizado para la recolección mantiene el bootloader bloqueado y el firmware stock sin rootear, se aportan los metadatos y registros del sistema accesibles sin superusuario (`getprop.txt` y `uname.txt`).

**Detalles del dispositivo:**
- **Modelo:** SM-S936B
- **Firmware:** BP4A.251205.006.S936BXXUACZF1
- **Android / One UI:** Android 16 / One UI 8.5
- **Bootloader:** Bloqueado
- **Estado de Root:** No

**Archivos añadidos:**
- `payload/S936B/README.md`
- `payload/S936B/metadata.json`
- `payload/S936B/install.sh`
- `payload/S936B/logs/getprop.txt`
- `payload/S936B/logs/uname.txt`
