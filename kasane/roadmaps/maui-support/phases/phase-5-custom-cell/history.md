# phase-5-custom-cell 議論履歴

## 2026-08-12: 論点4 — カスタムセル3層のうち MAUI でどの層まで提供するか

- 選択肢: 案A「①インライン + ②ラップ再利用のみ提供 (③ UserDefinedCell 非提供)」/ 案B「③まで全部提供 (Registry を MAUI 公開)」
- 採用: **案A**
- 理由:
  - ③は per-type 輸送契約 (maui/ADR-0011: facade 派生・Snapshot・Bridge DTO を種ごとに 1:1) に利用者定義型が乗らない。union 的な逃げ道は ADR-0002 の却下経緯と衝突する
  - ③を MAUI から使うには利用者が Swift/Kotlin で Renderer を書き binding 拡張まで用意する必要があり、「C# だけで完結」という MAUI 利用者像から外れる
  - MAUI 版 CustomCell の content は任意 MauiView になるため、「プリセット外 UI を1行差し込む」ニーズは①でほぼ埋まる
  - ③の後付けは公開面の追加であり可逆 (先送りのコストが低い)
- 補足: ②の C# での実現形はファクトリメソッド形 (`static CustomCell SliderCell(...)`) と派生サブクラス形 (`class SliderCell : CustomCell`、XAML から使える) の2つがありえる。どちらも追加機構不要で、等価性は型ではなくプロパティ値で決まる。形の選択 (XAML 対応の度合い) は論点2 で扱う
- 波及: 論点3 (Registry の MAUI 提供) は「非公開のまま」で自然消滅する見込み (次ターンで確認)
- ADR: maui/ADR-0019 として起票 (proposed)

## 2026-08-12: 論点3 — 利用者定義 Cell の拡張境界 (Registry) の MAUI 提供

- 論点4 の決定 (③ UserDefinedCell 非提供) に吸収してクローズ。Registry は MAUI 非公開のまま native 側利用者向けの拡張境界として維持する (maui/ADR-0019 の Decision に明記済み)

## 2026-08-12: 論点1 — content 等価性による再バインド制御の MAUI 実現

- 前提: native の「content 値の等価性で builder 再実行を制御」は、binding で View が生きたまま変わる MAUI に直訳できない。旧案の KsAnyView 直持ちは「等価性に参加しない包みを content に置くと再バインドが暴発する」契約違反 (custom-cell.md してはいけないこと) で不成立
- 選択肢:
  - 案A「live view + 世代トークン」— CustomCell は MauiView を持ち内容は live 更新 (ADR-0018 と同規律)。native へは phase-6 実体化機構の wrapper platform view を輸送、builder は interop 埋め込みの定数返しクロージャ (ADR-0017 と同型)。native content は facade が振る世代トークンで、View 差し替え再発行時のみ変わる
  - 案B「content 値 + DataTemplate で native 忠実に再バインド」— content 変更ごとに template 再実体化
  - 案C「KsAnyView 直持ち (旧案)」
- 採用: **案A**
- 理由: MAUI 慣例 (live binding) に適合 / トークンは等価性へ正しく参加し contract と無衝突 / phase-6 の ADR-0016〜0018 パターンをそのまま再利用でき実装コスト小 / 案Bは View 作り直しと退役順序管理が重く、その価値 (template 前提の仮想化) は phase-10 でセット設計すべき先取り。案Cは却下済み衝突そのもの
- ADR: maui/ADR-0020 として起票 (proposed)
- 補足 (ユーザー質問への回答): 案Aでも DataTemplate は生成機構としては使える — ItemTemplate (ADR-0008 踏襲) から生成された各 CustomCell は別 View インスタンスを持ち、それぞれ独立に案Aの経路へ乗る (Handler 1:1 と相性良)。使わないのは「再バインド機構としての template 再インフレート」と「行ビューへの View 載せ替え (仮想化)」で、後者は phase-10 の担当。この整理の言語化は論点2 で正式に扱う

## 2026-08-12: 論点2 — DataTemplate との対応関係と CustomCell の API 表現

- 調査事実: 原典 AiForms は `CustomCell : CommandCell` + `[ContentProperty("Content")]` + `ShowArrowIndicator` / `IsSelectable` / `IsMeasureOnce` / `UseFullSize` / `LongCommand`。自 facade の CellBase は Title / Description / IconSource / テキスト系 style 等の共通スロット持ち、CommandCell は `Tapped` / `ValueText` / `HideArrow` / `Command` / `CommandParameter`
- 選択肢: 案A「CellBase 継承 + 自前挙動プロパティ」/ 案B「専用の細い基底を新設」/ 案C「CommandCell 継承 (AiForms 同形)」
- 採用: **案A** (付帯: 不適用プロパティは隠蔽せず露出のまま silent no-op + ドキュメント明記)
- 理由: 案Bは phase-4 で確定済みの CellBase 再編が必要で波及大。案Cは ValueText まで露出し `HideArrow` の既定値 (矢印あり) が core の CustomCell 既定 (矢印なし) と衝突。案Aは既存 API 無波及で Section 配置・共通機構にそのまま乗り、AiForms 移行性も ContentProperty 同形で確保
- 不適用プロパティの扱い (ユーザー質問「new で上書き無効化できないか」への回答): `new` 隠蔽は XAML (基底 BindableProperty への SetValue にコンパイルされる)・Binding (BindableProperty 直指し)・基底型アクセス (静的束縛) のいずれにも効かず、「見かけ上消えるのに設定できて効かない」中途半端さが混乱を招くため不採用。例外送出も CellBase 対象の共有 Style の正当な適用を壊すため不採用。silent no-op (変換が読まない) + XML doc / 利用者ドキュメントでの不適用一覧明記を正とする
- 非提供: `IsSelectable` / `IsMeasureOnce` / `UseFullSize` / `LongCommand` — 現行コア契約に無い (maui/ADR-0008)。IsMeasureOnce は「用途固有ポリシーを wrapper 本体に持ち込まない」(maui/ADR-0016) とも衝突
- DataTemplate との関係 (論点1 の補足を正式化): 生成機構としては共存、再バインド機構・仮想化としては使わない
- ADR: maui/ADR-0021 として起票 (proposed)

## 2026-08-12: 論点5 — サンプルページ追加

- 採用: 既存慣行 (XxxDemoPage.xaml + ViewModel の対) に倣い `CustomCellDemoPage` を1ページ追加
- 構成 (決定事項の検証点対応で6項目): ①インライン Content + live binding 更新 (ADR-0020「内容は live」の実証) ②派生サブクラス再利用 ③Command + ShowArrowIndicator・二重発火なし確認 ④IsEnabled / IsVisible トグル ⑤サイズ変化の行高さ追従 ⑥Content 差し替え (世代トークン更新経路) + ItemTemplate 生成 (行ごと独立 View)
- 前提確認: ItemsSource / ItemTemplate は facade 実装済み (Section.cs / KsItemsSourceBinder.cs) のためデモ可能
- これで全5論点が解消。フェーズは ksn-propose での提案化待ち

## 2026-08-12: 論点5 決定の改訂 (提案化フェーズの spec-review より)

- ksn-propose (add-maui-custom-cell) の相方 spec-review が sample-parity 規約違反を指摘: sample-parity.md は「MAUI 未追随のデモ画面は CustomCellDemo のみ — CustomCell の MAUI 対応フェーズで追随予定」と明記しており、独自6項目1ページは規約の要求 (native 5構成・文言一致への収束) と矛盾していた
- オーナー承認の上で改訂: ①パリティ画面 (native と同一5構成・文言一致。スクロール耐性の多数行構成を含み、行リサイクルリスクの検証を兼ねる) + ②MAUI 固有デモページ (AccessoryViewsDemoPage のオーナー裁定前例に倣う「MAUI 固有」区分)。当初6項目の検証内容は2画面に再配分され欠落なし
