# phase-8-scroll-control

スクロール制御 (ScrollTo 系) を Native 起点で設計して MAUI から利用できるようにする。

## 論点

- API の形 (top / bottom / 指定 Cell / 指定 Section へのスクロール、アニメーション有無)
- Store を経由しない命令系 API の層配置 — 「更新経路は Store 一本」原則 (maui/ADR-0001) と共存する imperative 経路の設計 (Host 直か Bridge 経由か)
- MAUI からの公開形 (SettingsView のメソッド公開と Handler への委譲パターン)
- Host 世代をまたぐスクロール位置の保持 (下記「Host 世代をまたぐスクロール位置」)

### Host 世代をまたぐスクロール位置

2026-09-15、settingsview-migration-defects の探索より。Pop されたページ自身の再 Push や Activity 再生成で Host が作り直されると、両 OS でスクロール位置が先頭に戻る (証跡: `kasane/changes/archive/2026-09-17-settingsview-migration-defects/evidence/*-sample-reconnect-*`。archive 後は媒体が削除されるので exploration.md の実測表を参照)。

保つなら facade 側で位置を控えて再配信する仕組み (maui/ADR-0023 の見た目スタイルと同型) と、Native に位置を渡す窓口が要る。命令系 API の層配置と同じ設計判断なので、そもそも保つべきか (Store 外の表示状態をどこまで facade が所有するか) を含めてここで扱う。同一 View 内の付け外し (通常遷移) は同 change で Android Native 側に閉じて解決済み。

## 決定事項

(議論で確定したらここに移動)

## TODO

- [ ] 論点の解消
- [ ] ksn-propose で変更提案を起こす
