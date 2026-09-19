# Exploration: ios-swiftui-representable-safe-area

## 課題 / 動機

- **事象**: iOS の SwiftUI ラッパ `KsSettingsView` (`ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift` の `UIViewControllerRepresentable`: `StoreBackedRepresentable` / `DSLBackedRepresentable`) を `NavigationStack` の中身として全画面で使うと、SwiftUI が Representable をセーフエリアの内側に配置するため、一覧がタブバー・ナビバーの後ろまで回り込まない (タブバー手前で背景が切れ、下端に白い帯が出る)。iOS 26 ではさらに、スクロールビューがナビバー下まで伸びていないことで大タイトルがインラインに畳まれず流れて消える (WWDC25 session 284: 大タイトルはコンテンツのスクロールビュー先頭に置かれ、保つにはスクロールビューをナビバー下まで伸ばす必要がある)。

- **発見の文脈**: KsAppKMP リポジトリのリファレンスアプリ (iOS 26.4 シミュレータ) で 2026-09-19 に再現。KsAppKMP の change `ios-large-title-collapse-ios26` の探索 (spike) で切り分け済み。利用側で `.ignoresSafeArea()` を 1 つ足すだけで、帯の消失・先頭/末尾セルの正しい inset・大タイトルの畳み込みの 3 点がすべて解消し、上端の位置は変化なし → ライブラリ側 (UIKit の `KsSettingsViewController`) はセーフエリアを自前で正しく扱っており、SwiftUI 側の配置縮小だけが阻害要因。

- **Apple 公式の筋** (KsAppKMP 側の調査): スクロールコンテナは実際にバーに覆われる位置まで広げ、inset は `UIScrollView.contentInsetAdjustmentBehavior = .automatic` に任せるのが正 (`UIView.safeAreaInsets` の文書、WWDC25 284)。SwiftUI 側の公式レバーは `.ignoresSafeArea()` のみ。

- **オーナー判断 (KsAppKMP 側、2026-09-19)**: 利用者に毎回 `.ignoresSafeArea()` を書かせるのではなく、KsSettingsView 側で「SwiftUI ラッパが既定でセーフエリアを無視して全面に広がる」を検討する。

- **参考証跡**: `../KsAppKMP/kasane/changes/ios-large-title-collapse-ios26/exploration.md`、および同 change の `ui/references/ios26-spike-safearea-*.png`。

## 検討した選択肢 (却下案と理由を含む)

- **A: 既定で全面 (container 領域・全辺) に広げ、必要な人だけ modifier で戻す (opt-out)** — SwiftUI の `List` を置いたときの期待と揃う。ライブラリ自身のサンプル 7 画面がすべて利用側で `.ignoresSafeArea(.container, edges: .bottom)` を書いており、「広げたくない用法」が少数派である証跡。iOS 26 の大タイトルは全面化なしでは成立しない。**採用 (2026-09-19)**
- **B: 今のまま + opt-in modifier** — 既存利用者への影響はないが、毎回 1 行足す約束が残り、書き忘れで帯と畳み不全が再発する。却下
- **C: 今のまま (利用側で書く)** — 現状維持。KsAppKMP 側の探索で却下済み (利用者が毎回書く約束になる)。却下

## 決定事項

- 既定の挙動は A: SwiftUI ラッパ (`KsSettingsView`) が既定で container のセーフエリアを全辺で無視して全面に広がる。bar 分の inset は UIKit (`contentInsetAdjustmentBehavior = .automatic`) に任せる (2026-09-19)
- opt-out の口は `KsSettingsView` の Root modifier (既存の `.style(_:)` / `.theme(_:)` と同じ並び) で、on / off の切替 1 つだけを設ける (「セーフエリアを尊重する側に戻す」意味)。辺の指定は用法の裏付けが無いため今は足さない。init 引数案は Store / DSL の 2 init に同じ引数が要り冗長、利用側の `.safeAreaPadding` 等で相殺する案は保証しづらいため却下 (2026-09-19)
- 無視する領域は `.container` に限定する。`KsSettingsViewController` はキーボード出現時の inset 調整を持たず、EntryCell の入力時の回避は Representable がセーフエリア (keyboard 領域) 内に縮むことで SwiftUI が担っているため、keyboard 領域まで無視すると回避が壊れる

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

- 作成済み: ios/ADR-0006 (proposed) — SwiftUI ラッパは既定で container のセーフエリアを無視して全面に広がり、inset は UIKit に任せる

## 未決の論点

- 既定挙動と opt-out の形は決定済み (上記)。以下は実装フェーズで確かめる項目として change に持ち越す:
  - `.ignoresSafeArea(.container)` を Representable の内側 (ライブラリの body) で掛けても、利用側で掛けた場合と同じ効き目か — 実装の最初に spike で確認する (設計上は同じはず)
  - iOS 18 系での見え方・副作用の確認 (iOS 26 と両方で証跡を取る)
  - EntryCell のキーボード回避が現状どおり動くこと (`.container` 限定の裏付け)
- スコープに含めるもの:
  - `samples/ios` の 7 画面にある利用側の `.ignoresSafeArea(.container, edges: .bottom)` を取り除く (既定で不要になるため。handbook cross/sample-parity.md に従う)
  - concepts `ios/api/ios-swiftui.md` の Root modifier 一覧と「保証すること」への追随は蒸留時 (ksn-distill)。`skills/` は後日 docs-refresh
- 対象外: UIKit ホスト (`KsSettingsViewController` 直接利用) は既に正しく扱えており触らない。MAUI は UIKit Controller を直接ホストする経路で影響なし

## UI 素材 (ui/references/ の一覧と注釈)

- 目指す見た目の証跡は `../KsAppKMP/kasane/changes/ios-large-title-collapse-ios26/ui/references/ios26-spike-safearea-ignore-all-{top,bottom}.png` (利用側で `.ignoresSafeArea()` を付けた状態)、比較用の baseline は同 `ios26-spike-safearea-baseline-{top,bottom}.png`。提案化時に ui/references/ へ複製する

## 変更級の推奨: M

- 公開 API の既定挙動の変更 (全面化) + Root modifier の追加 (公開 API の小変更) で、能力は ios の SwiftUI ラッパ 1 つに閉じる
- 可逆性は低め (出荷後に既定を戻すと利用者の再対応が要る) → ADR-0006 で捕捉済み
- UI に触れるが、目指す見た目は KsAppKMP の spike 画像で確定しており、新規モックは不要 (ui/references/ に証跡を複製して照合の正にする)
