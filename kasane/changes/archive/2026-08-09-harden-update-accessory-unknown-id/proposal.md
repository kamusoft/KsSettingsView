# Proposal: harden-update-accessory-unknown-id

## Why

「canonical UUID だが未知の sectionID」で `updateAccessory` を呼ぶと、Store の内部 state 更新は no-op になる一方で Diff だけが無条件発行され、Native Host の missing ID 処理に到達する — iOS は DEBUG ビルドで `assertionFailure`、Android は `strictMode = true` (既定) で Diff 購読コルーチン内の `IllegalStateException` (例外ハンドラ次第で `storeCollectJob` だけが死ぬ「Host の沈黙」経路あり)。MAUI Bridge は Diff を素通しするため C# 呼び出し側からこのケースに到達可能で、公開 interop 表面として鋭利だった。

根本原因は `updateAccessory` だけが「state 更新が成立しなければ Diff を発行しない」ガード (`moveCell` / Cell / Section 操作の既存パターン) から漏れていること。core/ADR-0020 (accepted) で「state 更新が成立しない構造 Diff は発行しない」契約への統一を決定済み。

## What Changes

- iOS / Android の `SettingsRootStore.updateAccessory`: section 系 target (`sectionHeader` / `sectionFooter`) で sectionID が未知なら state 更新も Diff 発行も行わない no-op にする。Root 系 target (`rootHeader` / `rootFooter`) は state を持たないため従来どおり無条件発行
- 両 OS 対称の Store テストと、Bridge 表面 (未知 ID の `updateAccessory` が no-op) のテストを追加
- 影響する能力: `ios-store` / `android-store` / `maui-bridge` (Bridge は素通しのまま実装変更なし — Store 契約の適用拡大が interop 表面へ透過する)

## Non-Goals

- Host 側 missing ID 処理の変更 (iOS assert / Android strictMode は「Store が契約を守る限り到達しない内部整合性チェック」として温存 — ADR-0020)
- Android の `storeCollectJob` 死亡→沈黙経路への Host 側防御 (将来必要なら別途起票)
- Bridge の実装変更 (素通し構造は維持)

## Impact

- 観察可能挙動の変更: 未知 sectionID での `updateAccessory` が「iOS DEBUG クラッシュ / Android strictMode 例外」→「no-op」に変わる。この挙動に依存する利用コードは想定されない
- リスク: 呼び出し側の sectionID 間違いが黙って握り潰され開発時に気づきにくくなる (ADR-0020 Consequences で受容済み)
- ADR-0019 (attach 時 Store 復元) との潜在的な非一貫 (state に無い変更が Host にだけ流れる) も同時に解消される
- concepts 追随 (蒸留時の申し送り):
  - `concepts/core/core-model/structural-changes.md` — 「Section target の ID が存在することは呼び出し側の事前条件」という記述が古くなる (未知 sectionID は事前条件違反ではなく Store が保証する no-op になる)
  - `concepts/maui/api/native-bridge.md` (ID の interop 契約節) — 「`updateAccessory` はこの契約の対象外で、未知の sectionID でも Store が更新通知を発行する現行挙動へそのまま素通しされる」が変更後契約の正反対になるため**必須の追随先**
  - `concepts/core/architecture/store-and-update-streams.md` — Store の共通保証を置く文書として更新要否を蒸留時に確認する

## 級: M

公開 API のシグネチャは不変だが、観察可能挙動の契約変更 (phase-1 spec が明示的に除外していた `updateAccessory` への no-op 契約の適用拡大) のため。

capability は3つ (`ios-store` / `android-store` / `maui-bridge`) だが実体は単一契約 (core/ADR-0020) の platform 投影であり、複数の独立した能力を横断する変更ではないため L としない — `ios-store` / `android-store` は同一契約の対称適用、`maui-bridge` は実装変更なし (Store 契約の透過を明文化するテストのみ)。platform 対称の複数 spec 構成で M とした前例: fix-dsl-header-height-diff。

domain: core
