# Android exploration feature review — 0.6.0

Reference: macOS 1.7.16 source e022b05; Android baseline d32130c (0.5.0). This is a scoped source review, not a live parity audit of macOS or the web demo. Android is a native snapshot companion. No macOS or web files are changed.

| Area | macOS source evidence | Android 0.6.0 | Web |
| --- | --- | --- | --- |
| Named places | HelpView.swift describes sign-based saved places | Local editable markers and favorites; existing sign search retained | Unchecked |
| Player position | AIContextExportView.swift and ConversationKnowledge.swift explicitly report unavailable position | Automatic position remains unavailable; manual reference is labeled and offers distances | Unchecked |
| Map height | HelpView.swift describes layer selection and all-height signs | Exact Y slices, surface mode and layer stepping; all-height points retained | Unchecked |
| Import | Library.swift / SetupView.swift manage saved copies and device setup | Guided Shizuku connection, explicit stop/selection and ZIP fallback | Unchecked |
| View persistence | MapsView.swift / WindowFramePersistence.swift | Per-world render settings, viewport, filters and compact/large mode | Unchecked |
| Comparison | StatisticsModels.swift / StatisticsView.swift provide saved-data inspection | Same-world quantities, readable chest changes and shared terrain coverage | Unchecked |
| Quest interaction | Desktop UI remains the product reference | Native touch targets, scrollable tools and compact panel mode | Unchecked |

Validation uses synthetic core and Android UI tests without new screenshots. Real Shizuku import and automatic player-position decoding are not claimed verified. Cache and coverage limits remain explicit. Project developed with OpenAI Codex; Codex Astra is an AI contributor.
