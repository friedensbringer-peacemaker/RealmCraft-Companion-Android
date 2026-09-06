# Android 0.8.0 feature status

Reference checked: macOS origin/main 5b1dbce. `AIContextExport.swift` still lists playerPosition and playerDimension as unsupported, and `AIContextExportView.swift` labels player position unavailable. A known-position reference snapshot is required before adding an automatic decoder; manual coordinates are never relabeled as decoded data.

Implemented: viewport-triggered large-world window loading, bounded persistent terrain caching, comparison overlays, snapshot ZIP export, same-world notebook transfer, named material projects, straight-line collection ordering, import byte progress/storage checks/cancellation, interactive guide and legend.

Limits: streaming uses bounded windows and loaded coverage; no obstacle-aware routes, automatic game writes, or inferred player positions. Export/import uses system document providers. Live Shizuku import and external document-provider behavior require device-specific checks beyond synthetic UI/core tests. No new screenshots are created.
