# phase-8-scroll-control

スクロール制御 (ScrollTo 系) を Native 起点で設計して MAUI から利用できるようにする。

## 論点

- API の形 (top / bottom / 指定 Cell / 指定 Section へのスクロール、アニメーション有無)
- Store を経由しない命令系 API の層配置 — 「更新経路は Store 一本」原則 (maui/ADR-0001) と共存する imperative 経路の設計 (Host 直か Bridge 経由か)
- MAUI からの公開形 (SettingsView のメソッド公開と Handler への委譲パターン)

## 決定事項

(議論で確定したらここに移動)

## TODO

- [ ] 論点の解消
- [ ] ksn-propose で変更提案を起こす
