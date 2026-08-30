# レビュー結果 - add-cell-types-input (iOS 側実装)

**レビュー日時**: 2026年06月14日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-cell-types-input
**レビュー対象**: iOS 側実装のみ（`ios/Sources/KsSettingsViewUI/` の入力 Cell 5 種、`InputCellsTests`）
**対象外**: Android (`ks-settingsview-ui` Kotlin) / Compose DSL / Sample / docs（段階実装中につき本レビューでは指摘しない）

---

## サマリー

iOS 側実装は spec の MUST 要件をすべて満たしている。設計判断（`MainActor.assumeIsolated` の採用、`tapHandler` 三項演算子分解、DatePickerModalController の Time/Date 共通化、ファイル名 `KsCellRegistry+InputCells.swift` の既存 BasicCells 規約への揃え）はいずれも合理的で、既存基本 Cell の実装パターンと一貫している。

- `swift build` 成功（warning なし、本提案部分）
- `xcodebuild test -scheme KsSettingsView-Package -destination 'iPhone 17'` で **全 313 件成功**（`InputCellsTests` 39 件含む）。既存テストの回帰なし
- Native 型直接公開方針（`UIKeyboardType` / `Foundation.Date`）は spec Decision 1 と完全に整合
- 共通規約（`cell-types-basic` への opt-in：`description` / `valueText` / `icon` / `hintText` / `isEnabled` / `isVisible` / `VisibilityAware` / `applyCellBaseLayout` 経由 / `CellStyle.titleColor / titleFont` の 3 段階解決 / `accentColor` の 4 段階解決）はすべて準拠

Critical / Major 指摘はなし。Minor / Suggestion レベルの改善余地のみ。

**判定**: ✅ `APPROVED`（Android / Compose / Sample / docs は別フェーズで対応の前提）

---

## 指摘事項

### 🟡 Minor

#### 🟡 [Minor] PickerCell の複数選択 DSL init が `selectionMode` 引数を受け取って precondition でクラッシュさせる設計はやや危険

**該当箇所**: `ios/Sources/KsSettingsViewUI/PickerCell.swift:186-204`

**問題点**:
複数選択 init（`selectedIndices: Binding<Set<Int>>` 受領経路）は `selectionMode: PickerSelectionMode = .multiple` をパラメータとして公開しつつ、内部の `precondition(selectionMode == .multiple, "Use single-selection init for ...")` で `.single` を渡したケースをクラッシュさせている。同様に Store 経路 init（154 行付近）も同じ precondition を持つ。

```swift
public init(
    ...,
    selectedIndices: Binding<Set<Int>>,
    selectionMode: PickerSelectionMode = .multiple,   // ← `.single` を渡すと
    maxSelectedNumber: Int = 0,
    ...
) {
    precondition(selectionMode == .multiple, ...)     // ← クラッシュ
    ...
}
```

利用者が「単一選択にしたければ単一選択 init を使うべき」だが、コード補完で `selectionMode` 引数が見えてしまい誤用の余地がある。spec の DSL シグネチャ規約は「複数選択 overload は `selectedIndices` 受領 + `selectionMode` 引数を持たない」形だが、iOS 実装は spec と僅かに差異がある。

**推奨修正（任意）**:
複数選択 init から `selectionMode` 引数自体を削除して `.multiple` を内部固定にする。`fullspec internal init` には残し、public DSL 経路にのみ削除する。

```swift
public init(
    id: UUID = UUID(),
    style: CellStyle = CellStyle(),
    title: String,
    ...,
    items: [String],
    selectedIndices: Binding<Set<Int>>,
    // selectionMode 引数を削除（複数選択 init である事自体が情報）
    maxSelectedNumber: Int = 0,
    ...
)
```

ただし spec 上は MUST 違反ではない（precondition でクラッシュ保証している）ため、対応は任意。Android 実装時には同じ設計判断を引き継ぐと一貫性が保たれる。

---

#### 🟡 [Minor] TimePickerCell / DatePickerCell の `effectiveValueText` で `DateFormatter` を毎回新規生成している

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/TimePickerCell.swift:100-105`
- `ios/Sources/KsSettingsViewUI/DatePickerCell.swift:102-107`

**問題点**:
`effectiveValueText()` は render ごとに呼ばれ、その都度 `DateFormatter()` を新規生成して `dateFormat` を設定している。`DateFormatter` のインスタンス生成は比較的コストの高い処理（ICU バインディング含む）で、設定画面で多数の TimePicker / DatePicker が並ぶケース、または再 render が頻発するケースで微小なオーバーヘッドが累積する。

**推奨修正**:
形式文字列をキーにしたキャッシュ、または render 側で format を組み立ててから渡す。最低限、`Cell` 自身が値型のため static cache をスレッドセーフに用意できない場合は、CellView 側で format 結果を組み立てる。

```swift
// 例: CellView 側で組み立てる
internal final class TimePickerCellView: ... {
    private let formatter = DateFormatter()
    private var lastFormat: String = ""

    func render(cell: any KsCell, theme: Theme) {
        ...
        if formatter.dateFormat != tc.format {
            formatter.dateFormat = tc.format
        }
        let valueText = tc.valueText ?? formatter.string(from: tc.time)
        ...
    }
}
```

軽微なため後続で対応で可。

---

#### 🟡 [Minor] PickerListViewController の "完了" ボタンラベルがハードコード（多言語対応漏れの種）

**該当箇所**: `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:92`

**問題点**:
`UIBarButtonItem(title: "完了", ...)` で日本語文字列がハードコードされている。Cancel / Done は `UIBarButtonSystemItem` 利用で OS localization が効くが、「完了」のみ手書き。`.done` システムアイテムに置き換えれば「Done / 完了 / 完了 / 완료 / 完成」など各言語で自動表示される。

**推奨修正**:
```swift
navigationItem.rightBarButtonItem = UIBarButtonItem(
    barButtonSystemItem: .done,
    target: self,
    action: #selector(handleDone)
)
```

`NumberPickerModalController` の Done / Cancel は既に `barButtonSystemItem: .done / .cancel` を使っていて整合的（NumberPickerCellView.swift:140-145）。PickerListViewController も同じパターンにすべき。

---

#### 🟡 [Minor] EntryCellView.prepareForReuse で `tintColor` / `textAlignment` / `textColor` / `font` がリセットされない

**該当箇所**: `ios/Sources/KsSettingsViewUI/EntryCellView.swift:104-118`

**問題点**:
`prepareForReuse` で `text` / `placeholder` / `keyboardType` / `isSecureTextEntry` / `isEnabled` をクリアしているが、`tintColor` / `textAlignment` / `textColor` / `font` は前回 bind 時の値が残る。Cell が他の `EntryCell` 設定（accentColor, textAlignment）に再 bind される直前なら `render` で必ず上書きされるので実害は無いが、一瞬の視覚的グリッチや、もし将来別 Cell 型に reuse される設計に変わった場合に拭われない属性となる。

**推奨修正**:
完全リセットを徹底するなら以下も追加：
```swift
textField.tintColor = nil
textField.textAlignment = .right
textField.textColor = nil
textField.font = nil
```

ただし現状 reuse identifier が型別なので別 Cell 型に reuse されることはなく、影響は軽微。後続で対応可。

---

### 🔵 Suggestion

#### 🔵 [Suggestion] keyWindowRootViewController 取得ロジックが PickerCellView / NumberPickerCellView / TimePickerCellView / DatePickerCellView に重複

**該当箇所**:
- `PickerCellView.swift:106-116`
- `NumberPickerCellView.swift:80-92`
- `TimePickerCellView.swift:89-101`
- `DatePickerCellView.swift:92-104`

**問題点**:
ほぼ同一の `keyWindowRootViewController()` 実装が 4 ファイルにコピペされている。`presentingViewController.topMostPresented()` の有無や微妙な差異（PickerCellView は `topMostPresented()` ヘルパ extension を持つ、他 3 つは while ループ展開）はあるが、本質は同じ。

**推奨修正**:
`KsCellViewSupport` や `KsListCellBase` の internal 拡張として `keyWindowRootViewController()` を共通化する。テスト用に protocol で抽象化すればモック差し替えも可能。

```swift
// 例: 共通ヘルパ
@MainActor
internal enum KsModalPresentation {
    static func keyWindowRootViewController() -> UIViewController? {
        ...
    }
}
```

ただし機能上の問題はなく、Android 実装時の対比を考えると現状の重複もそれほど有害ではない。

---

#### 🔵 [Suggestion] 各 Cell の View キャストエラーで assertionFailure のみで継続している

**該当箇所**: 各 *CellView.swift の `render(cell:theme:)` の `guard let ... = cell as? ... else`

**問題点**:
型不一致時 `assertionFailure` のみで `return` し、画面上には「何も描画されない空 Cell」が残る。Debug ビルドでは検出できるが Release ビルドでは表示崩れに繋がる可能性。これは既存基本 Cell も同じパターンなので、本提案で導入された問題ではない。一貫性を保つために変更不要。

**推奨**: 何もしない（既存パターンに合わせる）。

---

#### 🔵 [Suggestion] PickerCell の `displayFormatter` が Hashable / Equatable から除外されているのは仕様通りだが、Cell 差分検出時に「フォーマッタだけが変わった」場合は再描画されない

**該当箇所**: `ios/Sources/KsSettingsViewUI/PickerCell.swift:302-342`

**問題点**:
クロージャは比較できないため `Equatable` から除外するのは妥当だが、結果として `displayFormatter` のみを差し替えた `replaceCell` Diff を発行しても、Diff 計算側で「内容が変わっていない」と判定されて reconfigure がスキップされる可能性。これは Cell モデルとしての普遍的問題であり、本提案範囲では受容可。利用者が `displayFormatter` 変更を反映したい場合は他のフィールド（valueText 等）も併せて変えるか、Cell 自体を新規 id で差し替える。

**推奨**: 何もしない（Cell モデル全体の課題）。

---

## アクションプラン

優先度順：

1. **（任意）PickerListViewController の "完了" を `.done` システムアイテムに置き換え** — 1 行修正、多言語対応の地ならし
2. **（任意）EntryCellView の prepareForReuse に tintColor / textAlignment 等のリセット追加** — 軽微、後続で可
3. **（任意）TimePickerCellView / DatePickerCellView の DateFormatter キャッシュ化** — 性能改善（軽微）
4. **（任意）複数選択 PickerCell init からの `selectionMode` 引数削除** — API 安全性向上、Android 実装前に方針確定すると一貫性が取れる
5. **（任意）keyWindowRootViewController の共通化** — 内部リファクタ、機能影響なし

いずれも本提案の MUST 要件には影響しないため、修正なしでマージ可。

---

## 判定結果

**ステータス**: ✅ **APPROVED**

### 判定理由

- **spec MUST 要件をすべて充足**:
  - 共通 Optional フィールド（description / valueText / icon / hintText / isEnabled / isVisible）の保持
  - EntryCell が valueText を持たない（MUST NOT 遵守）
  - VisibilityAware opt-in 準拠
  - 共通行レイアウト関数 `applyCellBaseLayout(...)` 経由での描画
  - Theme.cellTitleColor / cellTitleFont の 3 段階解決
  - accentColor の 4 段階解決
  - Native 型直接公開（`UIKeyboardType` / `Foundation.Date`）
  - TwoWay binding（`@Binding<T>`）と callback 経路の併設
  - PickerCell の単一/複数選択両モード、maxSelectedNumber 上限制御
  - PickerSelectionMode 列挙型の存在と 2 ケース
  - id デフォルト値 `UUID = UUID()` の自動採番
  - registerInputCells API（5 種登録）
  - autoRegisterInputCells を経由した KsSettingsViewController init での自動登録
  - TapNotifyingRenderer extension に 4 種ピッカーの追加
  - PickerCell の valueText 自動表示（単一: `items[selectedIndex]` / 複数: `, ` 連結 / displayFormatter 適用 / 明示指定が優先）

- **テスト網羅性**:
  - InputCellsTests 39 件すべて成功
  - 全 313 件のテストが成功（既存テストの回帰なし）
  - 各 Cell の bind / TwoWay 入力反映 / 共通フィールド表示 / VisibilityAware / isEnabled / 再利用クリア / 既定値 / id 自動採番 / Native 型受け渡し / valueText 自動表示 / displayFormatter / maxLength 境界値 / モーダル単一/複数選択 / maxSelectedNumber 上限挙動 / NumberPickerModalController の done 通知 / DatePickerModalController の date 通知 が網羅されている

- **設計判断の妥当性**:
  - `MainActor.assumeIsolated` を Swift 6 strict concurrency 下で安全に使えており、`Task { @MainActor in ... }` の ambiguous overload 回避という選択は適切
  - `tapHandler` を if/else 分解して compiler crash 回避は実装的妥当
  - `DatePickerModalController` を Time/Date で共通化（mode 分岐）はコード重複削減として妥当
  - Time/DatePicker の `Calendar.current.dateComponents` で必要成分のみ抽出して他成分保持は spec Risks 緩和策と完全に一致
  - ファイル名 `KsCellRegistry+InputCells.swift` は既存 BasicCells と命名規約を揃えており妥当

- **Critical / Major 指摘なし**。Minor / Suggestion レベルの改善余地（多言語対応の "完了"、prepareForReuse の追加リセット、DateFormatter キャッシュ、API 安全性改善、ヘルパ共通化）はあるが、いずれも本提案の MUST 要件に影響せず、後続でも対処可能。

iOS 側実装はマージ可能。Android / Compose / Sample / docs 実装は別フェーズで進める前提のため、本レビュー結果に基づき次のフェーズへ進行できる。
