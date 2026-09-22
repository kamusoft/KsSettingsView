# Exploration: ios-root-accessory-unexpected-accent-background

## 課題 / 動機

(2026-09-18、change `android-accessory-view-late-insert-animation` の実機・Simulator 確認中に見つかったスコープ外の発見より簡易起票。出典: `kasane/changes/archive/2026-09-18-android-accessory-view-late-insert-animation/evidence/README.md`)

**症状** (ワーカーの観察。iOS Simulator iPhone 17 / iOS 26.1、および iOS Native Sample):

- MAUI Sample の `AccessoryViewsDemoPage` で、① Root Header View のすぐ下に、XAML に無い鮮やかな青 (アクセント色に見える) の帯が描かれる。証跡: `kasane/changes/archive/2026-09-18-android-accessory-view-late-insert-animation/evidence/README.md` の `after-ios-sim-accessory-views-settled-no-scroll.png` ほか
- iOS Native Sample の DSL 方式デモで、root footer の背景がアクセント色 (青) で塗られ、caption の文字が読みにくい。証跡: 同 README の `native-sample-ios-dsl.png`
- 2026-09-22 に別アプリから共有された画面でも、View 形式の Section Header / Footer に同じ青背景が現れることを確認した。証跡: この change の `evidence/reported-section-accessory-blue-1.png` と `evidence/reported-section-accessory-blue-2.png`
- どちらも上記 change の修正前ビルド (HEAD) でも同じに出る (前者は画素単位で同一)。同 change による退行ではない
- 探索の結果、Root と Section の各現象は同じ supplementary cell 共通描画の背景設定に由来すると判明した

**利用者への影響**: 機能面の問題は無い。利用者が指定していない色が root の header / footer の領域に出るため、見た目が崩れ、文字が読みにくくなる場合がある。

## 検討した選択肢 (却下案と理由を含む)

### 推奨: View 形式の accessory 背景を明示的な透明色にする

- iOS Host は `UIBackgroundConfiguration.clear()` を生成した後、View 形式では `backgroundColor = nil` を代入している (`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`)。Apple の `UIBackgroundConfiguration.backgroundColor` 契約では `nil` は透明ではなく view の tint 色を使い、透明にする値は `clear` である
- MAUI の Root Header 下の鮮青帯と Native SwiftUI Root Footer の青背景は、いずれも利用者 View が覆っていない Root supplementary cell の領域に tint 色が現れたもの。前者は XAML が指定した淡青色 `#DDEBFF` の領域とは別に、その直下へ鮮青帯がある。後者の Sample は `Text` と font だけを指定し、背景色を指定していない
- `kasane/concepts/core/styling/list-appearance.md` は、View 形式の Header / Footer にライブラリ背景を適用せず hosted view の見た目を利用者が所有することと、何も指定しない領域には list 下地が見えることを定めている。明示的な透明色への修正は新しい契約ではなく、現行契約の回復になる
- 共通描画関数を直し、Section / Root と UIKit / SwiftUI の View 形式を回帰テストの対象にする。特に現行テストは「背景色が `nil`」を正解としており、UIKit の `nil = tint` 契約と逆なので期待値を直す

### 却下候補: Sample 側だけで背景を塗りつぶす

- 画像上の症状は隠せるが、背景を指定しない利用者の View で tint 色が出るライブラリ本体の誤りが残る
- MAUI Sample と Native Sample の両方へ個別対応が必要になり、同じ共通原因を二重に回避することになる

### 却下候補: View 形式にも Theme の Header / Footer 背景色を適用する

- hosted view の見た目を利用者が所有する現行契約と、背景色の適用範囲を text 形式だけにした既存仕様に反する
- 契約を変える必要性は今回の症状からは確認できない

## 決定事項

- 変更級は S とし、iOS の accessory 共通描画で View 形式の背景を `nil` ではなく `.clear` にする
- Section / Root と UIKit / SwiftUI の View 形式を回帰テストで覆い、text 形式の Theme 背景色の契約は変えない
- Sample、Bridge、Android の製品コードは変更せず、iOS Simulator 上の Native Sample と MAUI Sample で Root / Section の修正後の見た目を確認する

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

- なし。既存契約に反した局所的な値の修正であり、覆すコストの高い新規判断・能力境界を越える判断・将来を制約する判断には当たらない

## 未決の論点

- なし。Android は UIKit の背景構成契約を使わず今回の原因が存在しないため、platform 間の既存契約を変えず対象外とする

## UI 素材 (ui/references/ の一覧と注釈)

- 別アプリから共有された Section Header / Footer の再現画像 2 枚を `evidence/reported-section-accessory-blue-1.png` と `evidence/reported-section-accessory-blue-2.png` に保存した。画面内に個人情報が無いことを原寸で確認済み
- 当初発見時の Root の画像は出典 change の evidence として git 履歴に存在し、archive 時の媒体削除規約により現在の作業ツリーには残っていない。観察内容は同 change の `evidence/README.md` に記録済み

## 変更級の推奨: S (理由)

iOS の単一描画能力内に閉じたバグ修正で、公開 API・データモデル・既存 ADR の変更はない。製品コードの候補は accessory 共通描画関数の背景色 1 箇所、残りは回帰テストと実行時の見た目確認であり、可逆で局所的である。
