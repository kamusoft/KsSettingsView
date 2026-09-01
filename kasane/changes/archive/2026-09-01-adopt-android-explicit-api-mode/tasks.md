# Tasks: adopt-android-explicit-api-mode

## 1. Strict の有効化と公開宣言の明示

- [x] 1.1 ソース変更前の release AAR を組み立て、全 class の `javap -public` 出力を同一手順で列挙・正規化して比較用 baseline を取得する (→ Requirement: 公開 API 差分の限定)
- [x] 1.2 `android/kssettingsview` の Kotlin 設定で Explicit API Strict を有効にする。コメントには公開境界をコンパイラで強制する理由を自己完結して書き、`android/ADR-0022` は補助参照とする。Bridge module の設定は変更しない (→ Requirement: Android 公開ライブラリの明示 API 境界)
- [x] 1.3 公開宣言を明示する前に debug / release compilation を実行し、両方が Explicit API Strict の診断で失敗する陽性対照を取得する (→ Requirement: Android 公開ライブラリの明示 API 境界 / Scenario: 公開宣言の明示不足を拒否する)
- [x] 1.4 Strict の診断を基準に production source の公開宣言へ `public` と必要な型を明示する。明示した宣言を全走査し、利用者向け契約に照らして keep / demote を判定した一覧を実装報告へ残す (→ Requirement: Android 公開ライブラリの明示 API 境界 / 公開 API 差分の限定)

## 2. 公開面の縮小

- [x] 2.1 `KsCellRegistry.viewTypeOf` と `KsCellRegistry.isRegistered` を `internal` へ降格し、同一 module の Adapter・テストからの利用を維持する (→ Requirement: Cell Registry の利用者向け公開面)
- [x] 2.2 `SettingsRootStore.preview` を `internal` へ降格し、public コンストラクタを利用者向け生成経路として維持する (→ Requirement: SettingsRootStore の生成境界)
- [x] 2.3 変更後の release AAR を 1.1 と同じ手順で列挙し、公開 ABI 差分が 3 API の internal 化に伴う変化だけであることを確認する (→ Requirement: 公開 API 差分の限定)

## 3. テストと外部境界の検証

- [x] 3.1 `KsCellRegistry` の公開登録 API と標準 Cell 一括登録 API が引き続き利用できることを既存テストで照合し、不足する回帰ケースを追加する (→ Requirement: Cell Registry の利用者向け公開面)
- [x] 3.2 `SettingsRootStore(root, theme)` 経由で root / theme が初期化されることを検証するテストが存在する状態にし、Preview factory だけに依存するテストを残さない (→ Requirement: SettingsRootStore の生成境界)
- [x] 3.3 独立 build の `samples/android` へ一時 probe source を置き、公開維持する Registry API・一括登録 API・`CellViewHolder`・Store コンストラクタを参照する正の probe が成功することを確認する。続けて `viewTypeOf` / `isRegistered` / `preview` を 1 件ずつ参照する負の probe が internal visibility を理由に失敗することを確認し、一時 source は `trash` で除去する (→ Requirement: Cell Registry の利用者向け公開面 / SettingsRootStore の生成境界)
- [x] 3.4 `:kssettingsview:compileDebugKotlin` と `:kssettingsview:compileReleaseKotlin` を実行し、Explicit API Strict の診断が 0 件であることを確認する (→ Requirement: Android 公開ライブラリの明示 API 境界)
- [x] 3.5 `samples/android` の通常 source で `:app:assembleDebug` を実行し、実在する外部消費者が公開 API を参照できることを確認する (→ Requirement: Cell Registry の利用者向け公開面 / SettingsRootStore の生成境界)
- [x] 3.6 `./gradlew test` と Bridge の debug / release Kotlin compilation を実行し、本体 test source と Bridge に Strict 診断が波及しないこと、debug / release 両 variant のテスト実行件数と失敗数を確認する (→ Requirement: Android 公開ライブラリの明示 API 境界 / Scenario: 対象外の compilation に Strict が波及しない)
- [x] 3.7 1.3、2.3、3.3 の判定に必要な出力だけを sanitize し、`evidence/` に陽性対照・公開 ABI 差分・外部 visibility probe の抜粋として保存する (→ 全 Requirement)

## 4. 品質確認

- [x] 4.1 Kotlin 実装規約、ソースコメント規約、差分検査、ローカルパス・個人情報・コメント lint を確認する (→ 全 Requirement)
