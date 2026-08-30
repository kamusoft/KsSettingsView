# Exploration: android-hinttext-position

## 課題 / 動機

Android の hintText（右上 float の注釈）の位置が iOS とずれ、accessory（Switch 等）と重なる。

- iOS: hintLabel は cell 直下（contentView の余白の外）に addSubview され、cell 外縁から 上2pt / 右10pt（`ios/Sources/KsSettingsViewUI/KsListCellBase.swift` ensureHintLabel）
- Android: hintTextView は padding（横16dp / 縦4dp）を持つ root ConstraintLayout の内側に制約されており、同じ「2dp / 10dp」の数字が padding に食われて実効 上6dp / 右26dp になる（`android/ks-settingsview-ui/.../CellBaseLayout.kt`）

正は iOS の配置（右・上マージン最低限 = 外縁から 上2 / 右10）。

## 検討した選択肢 (却下案と理由を含む)

- **A. translation で padding 分を打ち消す** — 数行で済むが数字合わせのパッチ。構造の意図（hint は外縁基準）が表現されない。却下
- **B. iOS 同型のラップ構造（無余白 root → 余白付き内箱 + hint）** — 字面は iOS と同型だが階層が1層増え、`views.root` の意味が変わり全 ViewHolder・最低高さ保証・Z順テストに波及。却下
- **C. root の padding を廃止し内容側へマージン再配分（採用）** — フラット構造を維持したまま「余白は内容のもの、hint は外縁基準」を表現。icon 非表示時の goneMargin 調整が要注意点

裏取り: root の padding を外部から読むコードは無い（罫線 Decoration が読むのは RecyclerView 側の padding）。背景色・クリック・MinHeightConstraintLayout の最低高さ保証は root のままで無傷。

## 決定事項

- C 案を採用。hintTextView の実効位置を cell 外縁から 上2dp / 右10dp にする
- iOS パリティとして下端ガード（hint 下端 ≦ cell 下端 −12dp）も揃える
- 現行テスト（UnifyCellCommonFieldsTest の「root 右上に float 配置される」）は padding 込みの位置を正としているため、期待値を外縁基準に書き直す

## ADR 候補 (作成済み: なし / 未起票: なし)

S 級の局所レイアウト修正のため ADR は起こさない（決定はこのメモで足りる）。

## 未決の論点

なし

## UI 素材 (ui/references/ の一覧と注釈)

- `ui/references/ios-current.png` — iOS の現状（正）。hint「推奨」等が行右上・最小マージンに配置
- `ui/references/android-current.png` — Android の現状（誤）。hint が Switch と重なる

## 変更級の推奨: S (理由)

Android 単独・公開 API 変更なし・可逆・CellBaseLayout.kt とテストの数十行。
