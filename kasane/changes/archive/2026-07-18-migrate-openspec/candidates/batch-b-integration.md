# バッチB 統合結果 — プラットフォーム Host と DSL

統合日: 2026-07-18
レビュー結果: 2026-07-18 承認

対象:

- `settings-view-ios-host`
- `settings-view-ios-swiftui`
- `settings-view-android-host`
- `settings-view-android-compose`
- 補助資料: `docs/architecture.md`、`docs/platform-guide-ios.md`、`docs/platform-guide-android.md`

抽出結果:

- 概念候補: 15件
- ADR候補: 10件
- drift所見: 24件

## 統合後の concepts 案

### `architecture/native-host-boundary.md`

- Coreの完全モデルと変更意図をNativeリストへ接続する、iOS / Android共通のHost責務を定義する。
- Hostは完全モデル、visible projection、Native表示構造、購読ライフサイクルを整合させる。
- SwiftUI / ComposeはNative Hostを再利用し、描画実装を重複して持たない。
- 宣言ツリーの差分算出、Coreモデル定義、Theme / CellStyleの値解決はHostの外側へ置く。
- iOSのsnapshotとAndroidの平坦リストという適用差は、同じ責務を実現するプラットフォーム差として扱う。

統合元: iOS / AndroidのHost境界候補、SwiftUI / Composeの宣言的ラッパ境界候補。

### `architecture/store-and-update-streams.md`

- Storeは非表示要素を含む完全モデルとThemeの永続スナップショットを保持する。
- 成功した操作は状態を先に更新し、同じ操作を表す一過性の変更通知を発行する。
- 無効な操作は状態も通知も変えない。
- Themeは構造変更通知から分離し、同値適用で不要な通知を生じさせない。
- iOSのobservable stateとAndroidのstate / event streamは、同じ永続状態と一過性イベントの契約を各プラットフォームへ写像したものとする。

統合元: iOS observable Store、Android Storeのスナップショットと変更通知。

### `architecture/declarative-ui-bridge.md`

- SwiftUI / Composeの宣言ツリー方式と、利用者所有Store方式を併存させる。
- 宣言ツリー方式はView identityの間、内部Storeと前回ツリーを保持し、既存のStore / Diff / Native Host経路へ収束する。
- 宣言評価時の値を不変なCell値へ写し、ユーザー操作はコールバックを通じて宣言UI側の状態へ戻す。
- 宣言評価中にNative表示を直接変更せず、各フレームワークのNative更新境界から反映する。
- Root Header / Footer、Theme、描画Styleは画面側の状態であり、CoreのRootへ混在させない。

統合元: SwiftUI / Composeの宣言的ラッパ境界、Compose StateとCell値の接続境界。

### `architecture/declarative-tree-identity.md`

- 宣言値が再生成されても、同じSection / Cellを追跡できる安定同一性を定義する。
- 動的構造ではコレクションkeyまたは明示IDを要求し、位置fallbackは静的構造に限定する。
- 表示内容、現在値、modifierは同一性を変えない。
- ID再割り当て契約へ参加しない独自Cellでは、利用者が安定IDを保証する。
- ヒント優先順位と名前空間化はdrift B-1の後続探索後に確定する。

統合元: SwiftUI / Composeの宣言ツリー安定同一性。

### `architecture/cell-renderer-registry.md`

- Cellモデル型とNative描画型の対応をHost本体の型分岐から分離し、利用者定義Cellを追加できる登録境界を定義する。
- 登録集合は既定値と注入可能な独立集合を持ち、テストや隔離構成に対応する。
- 未登録Cellは厳格モードで早期検出し、非厳格モードでは画面全体を停止させない代替表示へ退避する。
- 再利用される描画型は、前のモデルの表示状態、listener、非同期処理、画像、Compositionを漏らさない。
- 具体的な登録API、viewType値、Nativeクラス名はコードを正としconceptsへ列挙しない。

統合元: iOS / AndroidのCell描画拡張境界。

### 既存 `architecture/display-state-synchronization.md` の更新

- 連動する複数Cellの内容変更は、一つの更新世代として同じcommit後に再構成する原則を追加する。
- Themeは構造・内容・可視性の三分類とは独立した更新経路であることを明記する。
- SwiftUI / Composeの「宣言ツリーの更新分類」は、この既存概念へ合流させる。

## 見送る独立 concepts

- iOS / Android別のHost、Store、宣言ラッパ概念は、共通責務を重複記載するため作らない。
- SwiftUI / Compose別のState接続は `declarative-ui-bridge.md` に統合する。
- 個別クラス、メソッド、Diffケース、builderシグネチャ、viewType値、debounce時間はコードから再導出できるため移さない。
- 共通行レイアウトはWrapper capabilityの責務外であり、Batch C / Dで再評価する。

## ADR 統合判断

抽出された候補のうち、Native Host再利用とRegistryはADR-0004、Store / Diff境界はADR-0006、DSLとStoreの収束はADR-0007、Theme分離はADR-0009、更新三分類はADR-0010で充足する。

iOSの差分アニメーション中のlayout更新方式は、重要な実装修正ではあるが、特定APIの局所的な実装戦略でありコードとテストから再導出できるため、新規ADRにはしない。

現時点では新規ADRを起票しない。drift B-1は単純な優先順位反転では安全に解消できず、コレクションkeyと子要素のローカルID / 位置を合成して名前空間化する設計探索が必要である。探索で新しい方針を合意した場合にだけ、ADR-0008をsupersedeする。

## drift 所見の統合

| ID | 所見 | 主な根拠 | 推奨する扱い |
|---|---|---|---|
| B-1 | ADR-0008とspec Requirements / docsはコレクションkeyを明示IDより優先するが、両プラットフォームのコードは明示IDをkeyで上書きしない。key優先では1 itemから複数要素を出す場合、明示ID優先では複数itemで同じローカルIDを使う場合に衝突し得る | code/test ↔ ADR/spec/docs | ADR-0008を維持し、現行コードをdriftとして記録する。単純な優先順位反転はせず、keyと子要素ID / 位置の合成・名前空間化を後続探索する |
| B-2 | iOS Hostは画面ロード前に受けた部分Diffを内部モデルへ適用せず、Storeの最新状態と初回表示がずれる可能性がある | code ↔ spec | 実装不具合候補。conceptsへ正当化せず、後続Kasane changeで修正する |
| B-3 | 両DSLの無効化modifierがno-opで、iOSのSection H/F modifierは可視性を引き継がない | code ↔ spec/docs | 実装不具合候補。Batch B conceptsには載せず、後続changeでAPI契約を修復する |
| B-4 | 公開Wrapperの内部構成、builder戻り値、result builder属性がspec/docsの旧構成から進化している | code/test ↔ spec/docs | コードを正とし、具体シグネチャはconceptsへ移さない。docsは移行完了後に案内更新する |
| B-5 | Android specはStateオブジェクト保持、ReplaceCell、200ms debounceを要求するが、実装は値snapshotとcallback、専用内容更新を使いdebounceを持たない | code/test ↔ spec | 現行の値snapshot + callback + 内容更新を正とし、debounceはDSL責務にしない |
| B-6 | Android spec/docsに旧Theme名、旧Store引数名、古いAdapter対応、build種別連動strictModeが残る | code/test ↔ spec/docs | コードを正とし、低腐食概念だけを統合する。docsは後続更新する |
| B-7 | Android RegistryはCell用viewTypeの推奨範囲を示すが、予約値との衝突を登録時に拒否しない | code/spec | 実装不具合候補。conceptには「予約領域と衝突させない」原則だけを置き、検証追加は後続changeで扱う |
| B-8 | Android Storeには複数Cellのバッチ内容更新契約とテストがあるがspecの公開操作一覧にない | code/test ↔ spec | コードとテストを正とし、表示状態同期conceptへ更新世代の原則を追加する |
| B-9 | Wrapper specがStyle変更契約をCoreへ置き、共通行レイアウトまでWrapper責務へ含める | spec ↔ code/ADR | ADR-0009とコードを正とし、Style契約はUI層、行レイアウトはBatch C / Dへ送る |
| B-10 | AndroidのRoot H/F更新は一過性通知で、Store再bind時の復元契約がコード・specから確定できない | code/spec | conceptsへ昇格しない。永続化の要否を後続の探索課題として残す |

## 推奨レビュー結果

1. 新規5 conceptsと既存1 concept更新を承認する。
2. B-1はADR-0008を維持し、現行コードとの差を未解消driftとして残す。keyと子要素ID / 位置の合成・名前空間化を後続探索し、結論が出るまで新規ADRを起票しない。
3. B-2、B-3、B-7は実装不具合候補として移行範囲外に残し、後続Kasane changeで扱う。
4. B-4〜B-6、B-8、B-9は推奨どおりコード / accepted ADRを正とする。
5. B-10は未確定のままconceptsへ含めず、後続探索へ送る。
