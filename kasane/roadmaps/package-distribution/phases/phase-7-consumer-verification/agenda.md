# phase-7-consumer-verification

配布物を参照する消費者プロジェクトを `verification/` に platform ごとに持ち、publish 前の dry-run (ローカルフィード) と publish 後の smoke (実レジストリ) で配信経路を検証できるようにする。Sample はソース参照のまま維持する。

## 論点

- `verification/` の構成 (iOS / Android / MAUI それぞれ最小 app、LabelCell 1 個程度) と、README のインストール手順の雛形を兼ねる書き方
- ローカルフィード方式: SwiftPM は配信リポジトリの prerelease tag または生成したスナップショットの `path:` 参照 (monorepo の file:// ではない)、Android は mavenLocal (または Central Portal の保留状態)、MAUI はローカルフォルダフィード
- dry-run と smoke の切り替え方 (参照先をパラメータ化するか、2 構成を持つか)
- CI からの起動方法 (phase-8 の release workflow の publish 前ステップ / publish 後ジョブ) と検証範囲 (解決・ビルドまでか、起動まで含めるか)
- MAUI 消費者でのトランジティブ依存の確認 (binding 2 件、AndroidX 版)
- cross/ADR-0016 (Sample パリティ) との関係: verification/ はパリティ対象外とする整理

## 決定事項

(議論で確定したらここに移動)

## TODO

- [ ] 論点の解消
- [ ] ksn-propose で変更提案を起こす
- [ ] **phase-9 からの申し送り** (2026-08-30): `verification/` の消費者プロジェクトに、ルート README / `skills/` と同じ最小コード例を入れて CI でビルドさせる。README の例は現状 `skills/` の Skill 本文との文字列一致でしか担保されておらず、実際にビルドが通るかは未検証
