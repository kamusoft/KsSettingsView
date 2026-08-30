# Exploration: implement-modern-style

## 課題 / 動機

`KsSettingsViewStyle.Modern` の完全実装。イメージは iOS の設定画面 (ui/references/ の2枚)。セクションの中身は Classic と同じで、**セクションの表現がボックスになっているのが最大のポイント**。

制御したい属性 (4属性):

- Section Margin — セクションの上下左右の余白
- Section Border Radius — セクションの角丸半径
- Section Border Width — セクションのボーダー幅
- Section Border Color — セクションのボーダー色

Android も同じスタイルを実現する。

### 現状 (2026-08-19 調査、コードが正)

- iOS: Modern は `.insetGrouped` への list appearance 切替のみ (`KsSettingsViewController.swift:804-809`)。余白・角丸は UIKit 任せで制御手段なし
- Android: `ModernSectionDecoration` が Section 単位の角丸背景を Canvas 直描画済み。ただし縦 12dp・左右 16dp・角丸 12dp は private 定数のハードコード、色は `theme.cellBackgroundColor` 固定
- MAUI: style の公開 API 自体が存在しない (Classic 固定)
- Theme / CellStyle: 4属性に相当するプロパティは3層のどこにもない
- サンプル: Modern を使うデモなし。テストは配線レベルのみ (描画結果の検証なし)

## 検討した選択肢 (却下案と理由を含む)

### iOS の実現方式

- **`.insetGrouped` 維持**: 4属性の制御 API が存在せず要件未達 → 却下
- **`.plain` ベース + 自前装飾 (採用)**: 4属性を完全制御できる。Android と対称のアプローチ。トレードオフとして OS ネイティブ外観への自動追従 (iOS 26 の Liquid Glass 世代の大きな角丸など) を放棄し、既定値をライブラリが所有する
- **未指定なら `.insetGrouped`・指定があれば自前装飾に切替**: Modern の描画経路が2本になり、テスト・視覚保証が倍増 → 却下

ユーザー判断 (2026-08-20): 「iOS に自動で追随してしまう弊害の方が大きい」— 自前装飾を採用。

実現手段: 現行 layout がすでに `UICollectionViewCompositionalLayout` + `NSCollectionLayoutSection.list(using:)` の sectionProvider 構成 (`makeLayout(for:)`) であり、Classic で `.plain` の header pin を手動で外す下地もある。Modern はこの延長で section の contentInsets (余白) と decoration item (角丸・ボーダーの箱) を足す方向。

## 決定事項

- iOS の Modern は `.insetGrouped` を廃し、自前の Section 装飾で実現する (→ ios/ADR-0003, accepted 2026-08-20)
- 4属性の置き場所は Theme へフラットに4つ直置き (`sectionMargin` / `sectionCornerRadius` / `sectionBorderWidth` / `sectionBorderColor`)。型は platform native (iOS: `NSDirectionalEdgeInsets`・`CGFloat`・`UIColor` / Android: `PaddingValues`・`Dp`・`Color`)、中間型は作らない (2026-08-20)
  - 却下: `SectionStyle` 構造体の新設 (per-section 上書きと誤読される。Theme の現行文法はフラット)。Section モデル毎の style (要件外のスコープ拡大、Core に style を置かない契約と衝突)
  - 将来拡張の含み: Section 単位の設定は Header/Footer と同様の流儀で後から足してよい。今回は全体一括設定のみ
- 未指定時の既定: margin / radius は platform 既定 (iOS はライブラリが決める iOS 風の値、Android は現行 12dp/16dp/12dp)、borderWidth は 0、borderColor は透明 (2026-08-20)
- Section Header / Footer は箱の**外側**、両 OS 共通 (2026-08-20)。根拠は参照画像 (iOS 設定画面で Header / Footer は箱の外)。Android は現行「内側」実装 (`ModernSectionDecoration` が Header/Footer 行を箱に含める) の変更が必要で、list-appearance.md の該当契約も蒸留時に改訂する。Root Header / Footer は従来どおり Section 装飾の対象外のまま
  - 却下: 内側維持 (参照画像と食い違い、iOS を Android に合わせる形になる)。内外トグル (要件外のスコープ拡大、必要なら別変更で)
- `sectionMargin` は Classic にも適用するが**上下のみ** (2026-08-20)。左右成分は Classic では無視し、この非対称を concepts に明記する。既定 0 で現行 Classic の外観は不変。隙間には canvas 背景 (`Theme.backgroundColor`) が見える
  - 却下: Modern 専用 (flat リストのセクション間隔という自然な要望に応えられない)。Classic 4方向適用 (「Classic の Section 境界は全幅」契約と衝突)
  - 帰結: 未指定時の既定は「style ごとの platform 既定」になる (Classic: 0 / Modern: iOS 風の値・Android 12dp/16dp)
- Modern の separator 契約 (2026-08-20、trailing 側は同日改訂): Section 最初の Cell 上端・最後の Cell 下端には描かない (箱の縁が区切りを兼ねる)。中間 separator は leading 側が Classic と同じ inset 規則 (箱の内側 leading 端基準)、**trailing 側にも同量の inset を取る左右対称** (箱の両端まで引くと箱が分断されて見える — ユーザー指摘)。Classic の「trailing は端まで」とは意図的に異なる。色は `Theme.separatorColor`、icon の有無で inset を変えない
  - 却下: 本物の iOS 設定画面風の icon 連動の深い inset (Classic の「icon 非依存」文法と割れる。再現度より既存文法との一貫性を優先)
  - 実装メモ: iOS は `itemSeparatorHandler` に style 分岐を追加。Android は Modern decoration に罫線描画を追加 (Cell 背景に上書きされないよう `onDrawOver`。現行 Modern は罫線を一切描いていない)
- 既存ルール「Classic / Modern の platform 実装を同じ生の margin・radius 値へ統一しない」との整合 (2026-08-20): ルールの意図は**既定値**を platform 間で同じ生値に揃えないこと。今回の4属性導入後も既定値は platform native のまま維持して意図を守る。利用者が明示した値は各 platform の単位 (pt / dp) でそのまま適用し、同じ数字を両 OS に入れるかは利用者の自由。蒸留時に list-appearance.md の当該文言を「既定値を統一しない」趣旨へ改訂する
- 既定 Theme の白×白問題 (second-opinion-spec-001 M1) の決着 (2026-08-20 ユーザー提案): Modern は新たな色既定を導入せず、モックの下地をサンプル共通 SampleTheme の PaleBackColorPrimary に差し替える。モック=デモ Theme での見た目とし、サンプル実機との視覚照合も下地込みで一致させる
  - 却下: Modern のときだけ canvas 既定をグレーへ (backgroundColor の「未指定」を表現できず optional 化かヒューリスティックが必要)。既定値自体のグレー化 (Classic の既定外観まで変わる視覚破壊)
- MAUI は本 change のスコープ外 (2026-08-20)。Native 2層 + core 契約に集中し、MAUI 伝搬 (style 公開 API + Theme 4属性の BindableProperty + Bridge 伝搬 + サンプル) は maui-support ロードマップの新フェーズとして積む (ksn-roadmap の改訂)
  - 却下: 全層一括の cross change (phase-9 の前例はプロパティ2個のトグルで規模が違う。iOS のレイアウト機構作り替えと同時に MAUI をゼロから建てるのはリスク過大。Native の公開 API が固まってから写す方が安全)

## ADR 候補

- 作成済み: ios/ADR-0003 (accepted) — iOS Modern は自前 Section 装飾で実現する
- 未起票: 4属性の置き場所 (Theme への追加形) が決まったら core 側の ADR 候補になり得る

## 未決の論点
8. ~~Modern の platform 既定値の具体数値~~ → 決着 (2026-08-20): モック案 A (iOS 26 風) をユーザー承認。iOS 既定値は ui/mock/variant-a-ios26.html (approved.png) を正とする
9. (実装フェーズの詳細) iOS decoration view への Theme 値の受け渡し方式 (カスタム layoutAttributes か共有プロバイダ)。Modern 時の Cell 自前背景と角丸マスクの関係 (角がはみ出さないか)。Android の箱から Header/Footer 行を除外する位置判定の変更

## UI 素材 (ui/references/ の一覧と注釈)

- `ios-settings-top.png` — iOS 26 設定画面トップ。ボックス型セクションの全体イメージ
- `ios-settings-display-brightness.png` — 画面表示と明るさ。Section Header (外観モード)・Footer (Liquid Glass 説明文) が箱の外側にある例
- 注釈: ui/brief.md「リファレンス注釈」に記録済み (箱のまとまり・Header/Footer 外側配置を採用、検索バー・Liquid Glass 質感等は対象外)

## 変更級の推奨: L (理由)

- 複数能力横断: styling の2概念 (list-appearance / style-resolution) + iOS・Android 両 platform
- アーキテクチャ変更: iOS の Modern 描画機構の作り替え (ios/ADR-0003)
- 公開 API 変更: Theme に4属性 × 2 platform
- UI あり: ui/ 必須、モック承認が実装前ゲート
- MAUI は含めない (maui-support ロードマップの新フェーズへ)
