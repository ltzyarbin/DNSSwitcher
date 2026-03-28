# DNS Switcher

A lightweight Android app for quick DNS-over-HTTPS (DoH) server management. Built for users who frequently switch between DoH providers.

## Features

- **Quick Settings Tile** — tap to toggle DNS on/off, long-press to pick a server from the list
- **Server availability check** — automatic DoH health check with latency display on app launch
- **Drag-and-drop** — reorder servers by priority with long-press drag
- **Swipe to delete** — remove servers with a left swipe or delete button
- **Material 3** — dark theme, card-based UI

## How it works

The app writes to `Settings.Global` (`private_dns_mode` and `private_dns_specifier`) to switch the system Private DNS setting. This requires the `WRITE_SECURE_SETTINGS` permission, which must be granted via ADB:

```bash
adb shell pm grant com.yarbin.dnsswitcher android.permission.WRITE_SECURE_SETTINGS
```

## Requirements

| | Minimum | Recommended |
|---|---|---|
| Android | 14 (API 34) | 15+ |
| Architecture | arm64-v8a, armeabi-v7a, x86, x86_64 | — |
| RAM | 2 GB | 4 GB+ |
| Storage | ~5 MB | — |

- ADB access required for initial permission setup (one-time)
- Internet permission for DoH server availability checks
- No root required

## Build

```bash
git clone https://github.com/ltzyarbin/DNSSwitcher.git
cd DNSSwitcher
./gradlew assembleDebug
```

APK will be at `app/build/outputs/apk/debug/app-debug.apk`

## License

[MIT](LICENSE)

---

# DNS Switcher (RU)

Легковесное Android-приложение для быстрого переключения DNS-over-HTTPS (DoH) серверов. Создано для тех, кто часто меняет DoH-провайдеров.

## Возможности

- **Quick Settings тайл** — тап для вкл/выкл DNS, зажатие для выбора сервера из списка
- **Проверка доступности** — автоматическая проверка DoH-серверов с отображением задержки при открытии приложения
- **Drag-and-drop** — сортировка серверов по приоритету перетаскиванием
- **Свайп для удаления** — удаление серверов свайпом влево или кнопкой
- **Material 3** — тёмная тема, карточный интерфейс

## Как это работает

Приложение записывает в `Settings.Global` (`private_dns_mode` и `private_dns_specifier`) для переключения системного Private DNS. Для этого требуется разрешение `WRITE_SECURE_SETTINGS`, которое выдаётся через ADB:

```bash
adb shell pm grant com.yarbin.dnsswitcher android.permission.WRITE_SECURE_SETTINGS
```

## Требования

| | Минимум | Рекомендуется |
|---|---|---|
| Android | 14 (API 34) | 15+ |
| Архитектура | arm64-v8a, armeabi-v7a, x86, x86_64 | — |
| ОЗУ | 2 ГБ | 4 ГБ+ |
| Хранилище | ~5 МБ | — |

- ADB-доступ для начальной настройки разрешения (однократно)
- Разрешение на интернет для проверки доступности DoH-серверов
- Root не требуется

## Сборка

```bash
git clone https://github.com/ltzyarbin/DNSSwitcher.git
cd DNSSwitcher
./gradlew assembleDebug
```

APK будет в `app/build/outputs/apk/debug/app-debug.apk`

## Лицензия

[MIT](LICENSE)
