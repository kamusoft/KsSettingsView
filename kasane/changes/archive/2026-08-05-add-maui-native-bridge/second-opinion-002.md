# セカンドオピニオン: add-maui-native-bridge (002 回目)
**相方**: codex / **日付**: 2026-08-04 / **対象**: 指摘反映後の提案一式 (確認ラウンド、同一セッション continue)

---

## 再レビュー結果

判定は **NEEDS_DISCUSSION** です。

前回指摘は **解消 6 / 部分解消 6 / 未解消 0** でした。重大な改善はありますが、Bridge の識別子、UI スレッド、Host 生成・破棄、テスト可能性に実装前に確定すべき契約が残っています。

### 1. 前回指摘の対応状況

| # | 重要度 | 前回指摘 | 判定 | 理由 |
|---:|---|---|---|---|
| 1 | Critical | Store と Native Host の接続経路が未定義 | **解消** | Bridge が Store を所有し、生成した Host へ同一 Store を接続すること、`setRoot` の生成前後の挙動、破棄責務まで追加されています。`design.md:58`、`specs/maui-bridge/spec.md:21` |
| 2 | Major | Builder・公開 API・識別子契約が未定義 | **部分解消** | Section/Label 構築、String ID、iOS UUID 変換は追加されました。一方、正確な公開シグネチャ、必須・省略可能フィールド、既定値、null 許容性、namespace が未定義です。`specs/maui-bridge/spec.md:5` |
| 3 | Major | Theme の proposal/design/ADR 間の矛盾 | **部分解消** | MAUI 向け facade を Phase 2 に送り、Phase 1 を transport DTO に限定した点は整合しました。ただし DTO のフィールド構造と各ネイティブ Theme への変換規則がありません。また「利用者向け API として文書化しない」だけでは ADR 0004 の「interop DTO は非公開」を保証できません。`design.md:39`、`specs/maui-bridge/spec.md:66` |
| 4 | Major | `updateAccessory` の相互運用表現が未定義 | **部分解消** | 任意 View を除外し text/clear に限定した点は改善です。しかし target/payload の型、text のフィールド、clear 後の状態、null と clear の区別が未定義です。`design.md:67`、`specs/maui-bridge/spec.md:35` |
| 5 | Major | `replaceCells` と可視性維持条件の衝突 | **解消** | 同一 ID を維持する事前条件が追加され、構造変更経路と内容更新経路が分離されました。入力順で適用してから単一通知することも明記されています。`specs/ios-store/spec.md:7` |
| 6 | Major | unknown ID no-op と既存 `updateAccessory` 契約の衝突 | **解消** | unknown ID no-op を Cell/Section 更新に限定し、`updateAccessory` は既存契約を維持すると明記されました。`design.md:23`、`specs/ios-store/spec.md:37` |
| 7 | Major | 12 API の変換 Scenario が不足 | **部分解消** | 全 12 操作を対象とする Scenario とパラメータ化テスト task は追加されました。ただし「内部 Store メソッドが同じ引数で exactly once 呼ばれる」は現在の具象 Store では観測・差し替えできず、受け入れ条件として検証困難です。`specs/maui-bridge/spec.md:61`、`tasks.md:22` |
| 8 | Major | C# 統合 harness が一時的で再現不能 | **解消** | harness を削除せず、継続利用するテスト資産として保持する task が追加されています。`design.md:77`、`tasks.md:34` |
| 9 | Major | Delegate 対応範囲が文書間で矛盾 | **解消** | proposal/design の双方で Phase 4 に延期され、Non-Goals として統一されています。`proposal.md:13`、`design.md:32` |
| 10 | Minor | `replaceCells` の混在・重複・ID 不変条件が未定義 | **部分解消** | unknown 混在、入力順、重複時 last-wins、同一 ID 条件が追加されました。ただし通知される ID 配列の順序・重複有無と、replacement の ID が異なった場合の具体的挙動は未確定です。`specs/ios-store/spec.md:7` |
| 11 | Minor | interop spike の成功・失敗条件が曖昧 | **解消** | 成功条件、失敗条件、ブロック時に停止する後続 task が具体化されています。`design.md:53`、`tasks.md:5` |
| 12 | Minor | module 分割 Decision が ADR 候補から漏れている | **部分解消** | ADR 不要とする説明は追加されましたが、cross ADR 0001 は build root、0002 は命名規則が中心であり、「既存 UI module ではなく別 Bridge module とする」判断を直接は確定していません。`design.md:46`、`design.md:89` |

## 2. 改稿によって新たに生じた問題

### Major — String ID 契約がプラットフォーム間で非対称

該当箇所: `specs/maui-bridge/spec.md:9`

問題点:  
Bridge の ID を String としつつ、iOS だけ UUID として解釈し、変換不能値を unknown ID 扱いにしています。任意の String が Android では有効、iOS では no-op となり、同じ C# 操作が異なる結果になります。

また、invalid ID が新規 Section/Cell の ID として渡された場合、それは「unknown target」ではないため、現在の規定では処理方法が決まりません。

推奨修正:  
次のいずれかを Requirement と Scenario で固定してください。

- 公開 ID を canonical UUID string に限定し、両プラットフォームで同じ検証を行う。
- 任意 String から iOS UUID への安定した変換規則を定義する。
- invalid ID を明示的エラーにし、target ID と新規オブジェクト ID の双方について挙動を定義する。

### Major — Host 生成 API に Android `Context` の供給元がない

該当箇所: `specs/maui-bridge/spec.md:23`  
関連コード: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:53`

問題点:  
仕様は Bridge が Android Native Host を生成するとしていますが、現在の `KsSettingsView` コンストラクタには `Context` が必要です。Bridge の生成時または Host 生成時のどちらで Context を受け取るのか、その Context を保持してよいのかが定義されていません。

複数回 Host を要求した場合に同一インスタンスを返すのか、新規生成するのかも不明です。

推奨修正:  
Host 生成の正確な API と以下を規定してください。

- Android `Context` の引数位置と期待する種類
- Context の保持期間
- Host 生成の回数制約
- 複数生成時に Store を共有するか
- iOS 側も含めた Host 所有権

### Major — UI スレッド境界が未定義

該当箇所: `specs/maui-bridge/spec.md:23`、`specs/maui-bridge/spec.md:35`、`specs/maui-bridge/spec.md:66`  
関連コード: `ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:33`

問題点:  
iOS Store は `@MainActor` であり、Android View の生成・bind も UI スレッド依存です。一方、C# から Bridge API を呼ぶスレッドについて契約がありません。そのまま実装すると、呼び出し元スレッド次第でクラッシュ、順序逆転、プラットフォーム差が発生します。

推奨修正:  
以下のいずれかを明記し、順序を検証する Scenario を追加してください。

- Bridge が全操作を各プラットフォームの UI スレッドへ同期的または逐次的に marshal する。
- 利用者に UI スレッド呼び出しを要求し、違反時の挙動を定義する。

### Major — 破棄契約が Requirement/Scenario になっていない

該当箇所: `design.md:59`、`specs/maui-bridge/spec.md:21`、`tasks.md:19`

問題点:  
明示的な破棄 API を設ける方針と task はありますが、公開 API 一覧および Scenario に破棄操作が含まれていません。次の挙動が判定不能です。

- 複数回破棄した場合
- 破棄後に更新 API を呼んだ場合
- 破棄後の Host 参照
- Android の Store collection、iOS subscription、Context の解放
- Host を親 View/ViewController へ取り付けたまま破棄した場合

推奨修正:  
Bridge lifecycle を独立 Requirement にし、`Dispose`/`destroy` の正確な名称、冪等性、破棄後操作、参照解放を Scenario 化してください。

### Major — 「内部 Store メソッド exactly once」は検証不能

該当箇所: `specs/maui-bridge/spec.md:61`、`tasks.md:22`、`tasks.md:30`  
関連コード: `ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:34`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:37`

問題点:  
Scenario は各 Bridge 操作について、対応する Store の public operation が「exactly once、同じ引数内容で呼ばれる」ことを要求しています。しかし iOS Store は `final`、Kotlin Store も継承可能として設計されておらず、Bridge へ spy Store を注入する契約もありません。

これは利用者から観測できる振る舞いでもなく、デルタスペックの受け入れ条件として不適切です。

推奨修正:  
受け入れ条件を、最終 Store 状態と通知結果という観測可能な結果へ置き換えてください。内部委譲そのものを保証する必要があるなら、Store protocol/interface とテスト用注入点を設計に追加してください。

### Minor — Bridge の所有・破棄判断が ADR 候補から漏れている

該当箇所: `design.md:58`、`design.md:89`

問題点:  
Decision 7 は Bridge が Store と Host の所有権を持ち、明示的破棄 APIを提供するという長期的なアーキテクチャ判断です。Phase 2 の Handler/facade や将来の複数 Host 対応にも影響するため、Phase 1 限定の局所判断とは言いにくい内容です。

推奨修正:  
Decision 7 を maui domain の ADR 候補に追加し、少なくとも Store/Host の所有権、破棄責務、複数 Host 方針を記録してください。

静的レビューのみ実施しており、ファイル変更およびビルド・テスト実行は行っていません。

---

## 突き合わせ結果 (ホスト側判定: 2026-08-04)

確認ラウンド。前回12指摘は解消6 / 部分解消6 / 未解消0。新規指摘と部分解消の残りへの採否:

| 指摘 | 採否 | 対応 |
|---|---|---|
| [Major] String ID 契約の OS 非対称 | **採用** | ID を Bridge 採番 (canonical UUID 文字列を Builder/insert 系が返す) に変更。未知・不正 ID は両 OS 同一の no-op (design Decision 9) |
| [Major] Android Context の供給元 | **採用** | Host 生成 API の引数で受け取り Bridge は保持しない。同時1 Host・再生成は破棄後 (Decision 7 拡充) |
| [Major] UI スレッド境界 | **採用** | 呼び出し側契約として UI スレッドを要求、Bridge は marshal しない (design Decision 10) |
| [Major] 破棄契約の Requirement 化 | **採用** | 「Bridge の lifecycle」を独立 Requirement 化 (冪等・破棄後 no-op・破棄後 Host 非更新 + Scenario 2件) |
| [Major] exactly once の検証不能性 | **採用** | 受け入れ条件を観察可能な結果 (表示内容・通知) ベースに書き換え |
| [Minor] Decision 7 の ADR 候補漏れ | **採用** | maui/ADR-0005 (Bridge の所有モデル) を起票 — Decision 5・7・9・10 を包含。Decision 5 単独 ADR の残指摘もこれで解消 |
| 部分解消 #3 (Theme DTO 変換規則) | **部分採用** | 「DTO 項目は Theme 公開項目と 1:1・null は未指定」を spec に追記。binding assembly の可視性の完全保証は降格 (phase-2 facade 導入時に整理) |
| 部分解消 #4 (accessory 意味論) | **採用** | target 指定・text/clear・clear 後の表示を spec に追記 |
| 部分解消 #10 (配信順序・ID 一致) | **採用** | 配信 ID は適用順・重複含む (Android parity)、対象 cellID と新 Cell ID の一致を呼び出し側契約として追記 |
| 部分解消 #2 (完全シグネチャ表・namespace・既定値) | **降格** | デルタスペックは挙動契約 (実装方法を書かない規約)。namespace は cross/ADR-0002 が拘束、フィールドの正はコード (SSoT) |

- 採用 9 (部分採用1含む) / 降格 1 / 未解決 0
- 修正は契約の明文化のみ (新規モジュール・構造変更なし) のため、追加の相方ラウンドは回さずホスト側の反映確認で締める (オーナー合意)
