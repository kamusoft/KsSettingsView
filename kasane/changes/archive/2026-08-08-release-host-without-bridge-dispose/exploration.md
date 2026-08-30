# Exploration: release-host-without-bridge-dispose

- 起票日: 2026-08-05
- 起票経緯: add-maui-native-bridge の独立レビュー (review-001 Minor-4) からの簡易起票。phase-1 のコード変更は不要と判定済み。実装は未着手

## 課題

Bridge を破棄せずに Native Host だけを解放・再生成する手段がない。現行契約は「`makeHost*` は同じ Host を返す / `dispose()` 後は二度と Host を生成できない」で、maui/ADR-0005 に忠実。しかし phase-2 の MAUI Handler 設計では:

- DisconnectHandler で `DisposeBridge` を呼ぶと、再接続 (ページ再表示・Shell の再生成) 時に Store 内容ごと失われ、facade 側で root の組み直しが必要になる
- 「同じ Store 内容を保ったまま Host だけ作り直す」経路が存在しないため、Handler の再接続パターンが塞がっている

## 検討方向 (phase-2 の agenda 論点候補)

- 案A: `releaseHost()` 相当を Bridge に追加する (Host のみ解放し、Store は維持。次の `makeHost*` で状態復元付きの新 Host を返す)
- 案B: 契約を「Bridge の寿命 = cross-platform control の寿命であり、DisconnectHandler では破棄しない」と固める (コード変更なし、facade の設計規約で吸収)
- phase-2 (MAUI Handler) の設計前に決着させる必要がある → maui-support ロードマップの phase-2 agenda へ

## 級の推奨

案A なら M (公開 API 追加 + 両 OS 対称テスト)、案B なら S (契約文書化のみ)
