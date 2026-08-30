---
type: reference
title: 移植元 AiForms の仕様要約
description: 移植元 AiForms.Maui.SettingsView / NativeCollectionView の公開 API・構造・実装パターンの要約。凍結された歴史資料であり、最終的な正は移植元コード
tags: [aiforms, origin, porting, reference, legacy]
timestamp: 2026-08-26
---

# 移植元 AiForms の仕様要約

移植元ライブラリ **AiForms.Maui.SettingsView** / **AiForms.Maui.NativeCollectionView** の公開 API・構造・実装パターンの要約である。KsSettingsView の未移植機能の実装や、移植時の退行と仕様の判別で「移植前の正」を素早く見渡すために読む。

> **凍結された歴史資料**: 本文書は移植初期に作成された要約であり、以後は更新しない。移植元は upstream で修正が続いているため記述は古くなり得る。**最終的な正は移植元コードにしかない** — 実装・調査の判断は必ず移植元の該当ソースを読んで確定する。参照先の在り処と参照ルールは [aiforms-origin-reference.md](aiforms-origin-reference.md) を参照。

## 0. リポジトリの位置

| 移植元 | パス | 役割 |
|---|---|---|
| AiForms.Maui.SettingsView | `../AiForms.Maui.SettingsView/` | MAUI 専用の設定画面ライブラリ。**API・振る舞いの主要参照元** |
| AiForms.Maui.NativeCollectionView | `../AiForms.Maui.NativeCollectionView/` | UICollectionView / RecyclerView の高パフォーマンス実装試作。**Native UI 層のパターン参照元** |

KsSettingsView は両者から仕様と実装パターンを継承するが、互換 shim は提供しない（独立ブランド）。

## 1. AiForms.Maui.SettingsView の構造

```
AiForms.Maui.SettingsView/SettingsView/
├── Cells/                          # 15 種の Cell（BindableObject）
│   ├── CellBase.cs                 # 全 Cell の基底（Title/Description/Icon/HintText/Background 等の共通プロパティ）
│   ├── LabelCell.cs                # CellBase + ValueText
│   ├── CommandCell.cs              # LabelCell + Command/CommandParameter + Disclosure Indicator
│   ├── ButtonCell.cs               # CellBase + Command + 中央揃えタイトル
│   ├── SwitchCell.cs               # CellBase + On(双方向) + AccentColor
│   ├── CheckboxCell.cs             # CellBase + Checked(双方向) + AccentColor
│   ├── RadioCell.cs                # CellBase + Value + AccentColor + Group(添付プロパティ)
│   ├── SimpleCheckCell.cs          # CellBase + Checked(単方向) + Value + AccentColor
│   ├── EntryCell.cs                # CellBase + ValueText(双方向) + Keyboard + Placeholder + IsPassword
│   ├── PickerCell.cs               # CommandCell + ItemsSource + SelectedItem(s)(双方向) + 単一/複数選択
│   ├── TextPickerCell.cs           # LabelCell + Items(string[]) + SelectedItem(双方向)
│   ├── NumberPickerCell.cs         # LabelCell + Number(双方向) + Min/Max
│   ├── TimePickerCell.cs           # LabelCell + Time(双方向) + Format
│   ├── DatePickerCell.cs           # LabelCell + Date(双方向) + Min/MaxDate + Format
│   └── CustomCell.cs               # CommandCell + Content(View) + IsSelectable + UseFullSize
├── Handlers/                       # 各 Cell の Handler（PropertyMapper パターン）
│   ├── CellBase/                   # CellBaseHandler — 共通プロパティを Native に反映
│   ├── LabelCellBase/              # LabelCellBaseHandler — ValueText の共通実装
│   ├── EntryCellBase/              # EntryCellBaseHandler — テキスト入力共通
│   ├── (各 Cell 名)/               # 各 Cell 固有 Handler
│   ├── Template/                   # ItemTemplate / DataTemplateSelector 用
│   └── SettingsViewHandler.{cs,iOS.cs,Android.cs,Net.cs}
├── Native/                         # Cell の Native 実装
├── Pages/                          # PickerCell モーダル用 Page
├── Platforms/Android/Resources/    # ドローアブル
├── Section.cs                      # Section（複数 Cell）
├── SettingsModel.cs                # ItemsSource / DataTemplate 対応のモデル
├── SettingsRoot.cs                 # ルート（複数 Section）
├── SettingsView.cs                 # ルート View（部分クラス）
├── SettingsView.DefineProperites.cs # 40+ の View 全体プロパティ
└── MauiAppBuilderExtension.cs      # AddSettingsViewHandler() で全 Handler 登録
```

## 2. CellBase の共通 BindableProperty 一覧

`CellBase` は **22 個** の BindableProperty を持つ。すべての Cell 派生型がこれらを継承する。原典：`../AiForms.Maui.SettingsView/SettingsView/Cells/CellBase.cs`

| プロパティ | 型 | デフォルト | BindingMode | 用途 |
|---|---|---|---|---|
| Title | string | null | OneWay | タイトル表示 |
| TitleColor | Color | KnownColor.Default | OneWay | タイトル色 |
| TitleFontSize | double | -1.0 | OneWay | -1 = SettingsView 既定 |
| TitleFontFamily | string | null | OneWay | |
| TitleFontAttributes | FontAttributes? | null | OneWay | Bold/Italic |
| Description | string | null | OneWay | 説明（タイトル下） |
| DescriptionColor | Color | KnownColor.Default | OneWay | |
| DescriptionFontSize | double | -1.0 | OneWay | |
| DescriptionFontFamily | string | null | OneWay | |
| DescriptionFontAttributes | FontAttributes? | null | OneWay | |
| HintText | string | null | OneWay | Cell 右上の小さなヒント |
| HintTextColor | Color | KnownColor.Default | OneWay | |
| HintFontSize | double | -1.0 | OneWay | |
| HintFontFamily | string | null | OneWay | |
| HintFontAttributes | FontAttributes? | null | OneWay | |
| BackgroundColor | Color | KnownColor.Default | OneWay | Cell 背景色 |
| IconSource | ImageSource | null | OneWay | アイコン画像（メモリキャッシュあり） |
| IconSize | Size | default | OneWay | アイコンサイズ |
| IconRadius | double | -1.0 | OneWay | 角丸半径 |
| IsVisible | bool | true | OneWay | Cell 表示/非表示 |
| Height | double | -1.0 | OneWay | Cell 個別高さ |
| IsEnabled | bool | true | OneWay | タップ等の有効/無効 |

**重要な特殊事項:**
- `Tapped` イベント (`internal void OnTapped()`) — Native 側からタップ通知される（CommandCell/CustomCell が Command Execute に使う）
- `Section` プロパティ — 親 Section への参照（`Reload()` の起点）
- `SetEnabledAppearance(bool)` — Handler.Invoke 経由で見た目を更新
- `Reload()` — Section の同位置に同 Cell を再代入する形で強制再描画

## 3. 各 Cell 固有の BindableProperty（移植時の確認用）

### LabelCell（CellBase 継承）
| プロパティ | 型 | デフォルト | BindingMode | 用途 |
|---|---|---|---|---|
| ValueText | string | null | OneWay | 右側に表示する値文字列 |
| ValueTextColor | Color | Default | OneWay | |
| ValueTextFontSize | double | -1.0 | OneWay | |
| ValueTextFontFamily | string | null | OneWay | |
| ValueTextFontAttributes | FontAttributes? | null | OneWay | |
| IgnoreUseDescriptionAsValue | bool | false | OneWay | SettingsView 全体の `UseDescriptionAsValue` を無視するか |

### CommandCell（LabelCell 継承）
| プロパティ | 型 | デフォルト | BindingMode | 用途 |
|---|---|---|---|---|
| Command | ICommand | null | OneWay | タップで Execute |
| CommandParameter | object | null | OneWay | |
| KeepSelectedUntilBack | bool | false | OneWay | 選択ハイライトを戻るまで保持 |
| HideArrowIndicator | bool | false | OneWay | Disclosure Indicator 非表示 |

### ButtonCell（CellBase 継承）
| プロパティ | 型 | デフォルト | BindingMode | 用途 |
|---|---|---|---|---|
| TitleAlignment | TextAlignment | Center | OneWay | |
| Command | ICommand | null | OneWay | |
| CommandParameter | object | null | OneWay | |

### SwitchCell（CellBase 継承）
| プロパティ | 型 | デフォルト | BindingMode | 用途 |
|---|---|---|---|---|
| **On** | bool | false | **TwoWay** | スイッチ ON/OFF |
| AccentColor | Color | default | OneWay | ON 時の色 |

### CheckboxCell（CellBase 継承）
| プロパティ | 型 | デフォルト | BindingMode | 用途 |
|---|---|---|---|---|
| **Checked** | bool | false | **TwoWay** | |
| AccentColor | Color | default | OneWay | |

### RadioCell（CellBase 継承）
| プロパティ | 型 | デフォルト | BindingMode | 用途 |
|---|---|---|---|---|
| Value | object | null | OneWay | この Cell の値 |
| AccentColor | Color | default | OneWay | |
| **GroupProperty** | object | (添付プロパティ) | TwoWay | 同グループの選択値（Section に付ける） |

### SimpleCheckCell（CellBase 継承）
| プロパティ | 型 | デフォルト | BindingMode | 用途 |
|---|---|---|---|---|
| **Checked** | bool | false | **OneWay** | （SimpleCheck は SwitchCell と異なり片方向） |
| Value | object | null | OneWay | |
| AccentColor | Color | default | OneWay | |

### EntryCell（CellBase 継承、IEntryCellController 実装）
| プロパティ | 型 | デフォルト | BindingMode | 用途 |
|---|---|---|---|---|
| **ValueText** | string | null | **TwoWay** | テキスト入力値 |
| MaxLength | int | int.MaxValue | OneWay | |
| ValueTextColor | Color | Default | OneWay | |
| ValueTextFontSize/FontFamily/FontAttributes | (各種) | | OneWay | |
| Keyboard | Keyboard | Default | OneWay | MAUI の Keyboard 型 |
| CompletedCommand | ICommand | null | OneWay | フォーカスアウト時の確定通知 |
| Placeholder | string | null | OneWay | |
| PlaceholderColor | Color | default | OneWay | |
| TextAlignment | TextAlignment | Start | OneWay | |
| AccentColor | Color | default | OneWay | キャレット・選択ハイライト |
| IsPassword | bool | false | OneWay | |

### PickerCell（CommandCell 継承）
| プロパティ | 型 | デフォルト | BindingMode | 用途 |
|---|---|---|---|---|
| PageTitle | string | null | OneWay | モーダル Page のタイトル |
| ItemsSource | IList | null | OneWay | 候補リスト |
| DisplayMember | string | null | OneWay | 表示用プロパティパス |
| SubDisplayMember | string | null | OneWay | サブ表示 |
| **SelectedItems** | IList | null | **TwoWay** | 複数選択結果 |
| **SelectedItem** | object | null | **TwoWay** | 単一選択結果 |
| SelectionMode | SelectionMode | Single | OneWay | Single/Multiple |
| MaxSelectedNumber | int | 0 | OneWay | 0 = 制限なし |
| AccentColor | Color | default | OneWay | |
| SelectedItemsOrderKey | string | null | OneWay | ソートキー |
| SelectedCommand | ICommand | null | OneWay | 選択時に Execute |
| UseNaturalSort | bool | false | OneWay | 自然順ソート |
| UseAutoValueText | bool | true | OneWay | 選択結果を自動で valueText に反映 |

### TextPickerCell（LabelCell 継承）
| プロパティ | 型 | デフォルト | BindingMode | 用途 |
|---|---|---|---|---|
| Items | IList\<string\> | null | OneWay | 候補文字列リスト |
| PageTitle | string | null | OneWay | |
| AccentColor | Color | default | OneWay | |
| **SelectedItem** | string | null | **TwoWay** | |
| PickerTitle | string | null | OneWay | |
| SelectedCommand | ICommand | null | OneWay | |
| IsCircularPicker | bool | false | OneWay | 循環ピッカー |

### NumberPickerCell（LabelCell 継承）
| プロパティ | 型 | デフォルト | BindingMode | 用途 |
|---|---|---|---|---|
| **Number** | int | 0 | **TwoWay** | |
| Min | int | 0 | OneWay | |
| Max | int | 100 | OneWay | |
| PickerTitle | string | null | OneWay | |
| SelectedCommand | ICommand | null | OneWay | |

### TimePickerCell（LabelCell 継承）
| プロパティ | 型 | デフォルト | BindingMode | 用途 |
|---|---|---|---|---|
| **Time** | TimeSpan | TimeSpan.Zero | **TwoWay** | |
| Format | string | "t" | OneWay | |
| PickerTitle | string | null | OneWay | |

### DatePickerCell（LabelCell 継承）
| プロパティ | 型 | デフォルト | BindingMode | 用途 |
|---|---|---|---|---|
| **Date** | DateTime | DateTime.Today | **TwoWay** | |
| InitialDate | DateTime? | null | OneWay | |
| MaximumDate | DateTime | DateTime.MaxValue | OneWay | |
| MinimumDate | DateTime | DateTime.MinValue | OneWay | |
| Format | string | "d" | OneWay | |
| TodayText | string | null | OneWay | |
| IsAndroidSpinnerStyle | bool | false | OneWay | Android のみ Spinner 形式 |
| AndroidButtonColor | Color | default | OneWay | |

### CustomCell（CommandCell 継承）
| プロパティ | 型 | デフォルト | BindingMode | 用途 |
|---|---|---|---|---|
| ShowArrowIndicator | bool | false | OneWay | |
| **Content** | View | null | OneWay | 任意の MAUI View |
| IsSelectable | bool | false | OneWay | タップ可能か |
| IsMeasureOnce | bool | false | OneWay | 高さ計測キャッシュ |
| UseFullSize | bool | false | OneWay | Cell パディング無視で全画面利用 |
| LongCommand | ICommand | null | OneWay | 長押し |
| LongCommandParameter | object | null | OneWay | |

## 4. SettingsView 全体プロパティ（40+ 個）

`SettingsView.DefineProperites.cs` で定義される全体プロパティ。これらは Cell 個別プロパティが未指定時のデフォルトを供給する。原典：`../AiForms.Maui.SettingsView/SettingsView/SettingsView.DefineProperites.cs`

### 全体スタイル
- `BackgroundColor` — Transparent
- `SeparatorColor` — Color.FromRgb(199, 199, 204)（iOS の灰色 separator）
- `SelectedColor` — Transparent

### Cell 既定スタイル（個別 CellStyle が未指定時のデフォルト）
- `CellTitleColor` / `CellTitleFontSize` / `CellTitleFontFamily` / `CellTitleFontAttributes`
- `CellDescriptionColor` / `CellDescriptionFontSize` / `CellDescriptionFontFamily` / `CellDescriptionFontAttributes`
- `CellValueTextColor` / `CellValueTextFontSize` / `CellValueTextFontFamily` / `CellValueTextFontAttributes`
- `CellHintTextColor` / `CellHintFontSize` / `CellHintFontFamily` / `CellHintFontAttributes`
- `CellBackgroundColor`
- `CellAccentColor` — Switch / Checkbox / Radio 等のアクセント色
- `CellIconSize` / `CellIconRadius`

### Header / Footer
- `HeaderPadding` — `new Thickness(14, 8, 8, 8)`
- `HeaderTextColor` / `HeaderFontSize`（iOS=14, Android=18） / `HeaderFontFamily` / `HeaderFontAttributes`
- `HeaderBackgroundColor` — Transparent
- `HeaderHeight` / `HeaderTextVerticalAlign` — End（下寄せ）
- `FooterTextColor` / `FooterFontSize` / `FooterFontFamily` / `FooterFontAttributes`
- `FooterBackgroundColor` / `FooterPadding`

### レイアウト
- `RowHeight` — -1（自動）
- `HasUnevenRows` — false
- `ShowArrowIndicatorForAndroid` — Android で Disclosure 表示する CommandCell 系の挙動
- `ShowSectionTopBottomBorder` — Section 上下境界線

### データバインディング（ItemsSource パターン）
- `ItemsSource` — Section または Cell のリストをバインド
- `ItemTemplate` — DataTemplate / DataTemplateSelector
- `TemplateStartIndex` — テンプレート挿入開始位置

### スクロール / 動作
- `ScrollToTop` / `ScrollToBottom` — bool（true で先頭/末尾へスクロール、ワンショット）
- `VisibleContentHeight` — double（読み取り専用、コンテンツの可視高さ）
- `UseDescriptionAsValue` — bool（Description を ValueText として表示）

### ドラッグ＆ドロップ
- `ItemDroppedCommand` — ICommand
- `ItemDropped` イベント — `EventHandler<DropEventArgs>`

## 5. Handler 階層と PropertyMapper パターン

旧版は MAUI Handler パターンに完全準拠。原典：`../AiForms.Maui.SettingsView/SettingsView/Handlers/`

```
SettingsViewHandler          (ルート)
    └ partial class for iOS / Android / Net

CellBaseHandler<TCell, TNativeCell>     (全 Cell 共通)
    │  BasePropertyMapper: Title/Desc/Icon/HintText/Background/IsVisible/IsEnabled
    │
    ├ LabelCellBaseHandler<...>         ← LabelCell 系の共通（ValueText）
    │   └ LabelCellHandler
    │   └ CommandCellHandler
    │   └ NumberPickerCellHandler
    │   └ TimePickerCellHandler
    │   └ DatePickerCellHandler
    │   └ TextPickerCellHandler
    │   └ PickerCellHandler              ← さらに CommandCell 経由
    │
    ├ EntryCellBaseHandler<...>         ← Entry の共通
    │   └ EntryCellHandler
    │
    ├ ButtonCellHandler
    ├ SwitchCellHandler                  ← MapOn / MapAccentColor
    ├ CheckboxCellHandler
    ├ RadioCellHandler
    ├ SimpleCheckCellHandler
    └ CustomCellHandler
```

**PropertyMapper の典型例**（`Handlers/SwitchCell/SwitchCellHandler.iOS.cs`）:
```csharp
public static IPropertyMapper<SwitchCell, SwitchCellHandler> SwitchMapper =
    new PropertyMapper<SwitchCell, SwitchCellHandler>(BasePropertyMapper)
    {
        [nameof(SwitchCell.AccentColor)] = MapAccentColor,
        [nameof(SwitchCell.On)] = MapOn
    };

private static void MapOn(SwitchCellHandler handler, SwitchCell cell)
{
    if (handler.IsDisconnect) return;     // ← Disconnect 後の安全ガード
    handler.PlatformView.UpdateOn();
}
```

**重要パターン:**
- `IsDisconnect` フラグで `DisconnectHandler` 後の no-op 化
- `BasePropertyMapper` を継承して各 Cell が拡張
- Native View 側で `UpdateXxx()` ヘルパを呼ぶ（PlatformView extension method）

## 6. iOS Native 実装の特徴

旧版は **UITableView ベース**（`AiTableView : UITableView`、Grouped スタイル、AutomaticDimension で高さ自動）。原典：`../AiForms.Maui.SettingsView/SettingsView/Handlers/SettingsViewHandler.iOS.cs`

- `IUITableViewDragDelegate` / `IUITableViewDropDelegate` でドラッグ＆ドロップ
- TextHeaderView / CustomHeaderView 等の Section ヘッダ ViewHolder 登録
- `ContentSize` の KVO 観察で `VisibleContentHeight` を更新
- IconSource は MAUI 標準の `IImageSourcePart` を経由してメモリキャッシュ

**KsSettingsView での刷新方針**: UITableView → **UICollectionView + UICollectionViewDiffableDataSource + UICollectionLayoutListConfiguration** に置き換える。

## 7. Android Native 実装の特徴

旧版は **RecyclerView ベース**（`AiRecyclerView`、`SettingsViewRecyclerAdapter`）。原典：`../AiForms.Maui.SettingsView/SettingsView/Handlers/SettingsViewHandler.Android.cs`

- `ItemTouchHelper` でドラッグ＆ドロップ
- `SVItemDecoration` で区切り線描画
- `NestedScrollingEnabled = false`（ScrollView 内に置かれることが多いため）
- ViewHolder パターンで再利用

**KsSettingsView での刷新方針**: RecyclerView は維持しつつ、Adapter を **ListAdapter + DiffUtil** ベースに変更（手動 NotifyDataSetChanged を排除）。

## 8. メモリリーク対策（旧版で確立されたパターン）

MAUI 9 でも `DisconnectHandler` の自動呼び出しは信頼できないため、旧版は `HandlerCleanUpHelper` を導入：

- `UseSettingsView(true)` オプションで `HandlerCleanUpHelper` 経由の明示 Disconnect を有効化
- `Page.Unloaded` / `Page.NavigatedFrom` のフックで Page 内の SettingsView Handler を強制 disconnect
- 各 Handler の MapXxx で `if (handler.IsDisconnect) return;` の安全ガード

KsSettingsView でもこのパターンを踏襲し、CI に `WeakReference` リーク検出テストを組み込む。

## 9. AiForms.Maui.NativeCollectionView から引き継ぐパターン

原典：`../AiForms.Maui.NativeCollectionView/`

| パターン | iOS | Android |
|---|---|---|
| 差分更新 | `UICollectionViewDiffableDataSource<NSUuid, NSUuid>` + `ApplySnapshot()` | `ListAdapter` + `DiffUtil.ItemCallback` |
| Cell 再利用 | `MauiCell` ラッパ + `RegisterClassForCell` + `PrepareForReuse()` | ViewHolder.ResetCell()→BindCell() |
| 複合 Adapter | -（不使用） | `ConcatAdapter` でヘッダ/フッタ/ロードモア合成 |
| 非同期ハンドリング | `CancellationTokenSource` を ViewHolder に保持し ResetCell で破棄 | 同左 |
| ネストスクロール | -（不要） | `NestedScrollingEnabled = false` |

**KsSettingsView での採用範囲:**
- iOS: DiffableDataSource パターンをそのまま継承
- Android: ListAdapter + DiffUtil パターンを継承（ConcatAdapter は採用しない方針）

**未完成箇所（引き継がない）:**
- `AiForms.Maui.NativeCollectionView.Generator/Class1.cs` — Source Generator は未実装、KsSettingsView でも採用しない
- `NativeCollectionViewHandler.Net.cs` — Windows/Net 対応は NotImplementedException、KsSettingsView でも対象外
- 一部 Dispose パターン未完成 — KsSettingsView では完成形として実装する

## 10. Sample コードの場所

XAML での使用例：`../AiForms.Maui.SettingsView/Sample/Views/`

- 各 Cell の典型的な XAML 記述例
- Style / DataTemplate / DataTemplateSelector の使用例
- グローバル設定（App.xaml）でのテーマ適用例

KsSettingsView の MAUI Sample（`samples/maui/`）は本 Sample をベースに書き直す。

## 11. 移植時に注意すべき差分

| 項目 | 旧 AiForms | KsSettingsView |
|---|---|---|
| 名前空間 | `AiForms.Settings` | `KsSettingsView.Maui` |
| MauiAppBuilder 拡張 | `.AddSettingsViewHandler()` | `.AddKsSettingsView()` |
| iOS UI | UITableView | UICollectionView + DiffableDataSource |
| Android Adapter | 自前 RecyclerAdapter | ListAdapter + DiffUtil |
| Native 公開 | なし（MAUI 専用） | iOS/Android 単独 SDK + Bridge 経由で MAUI |
| 最低 OS | iOS 14.2 / Android 8.0 | iOS 16.0 / Android 10 (API 29) |
| KsImage | （なし、ImageSource を直接利用） | `KsImage(name, url, systemName)` 値型 |
| KsTime / KsDate | （なし、TimeSpan/DateTime 直接） | Core 独自値型（Bridge で C# 型に変換） |
| ドラッグ＆ドロップ | あり | 移植初期は対象外 |
| 永続化 | スコープ外（変更なし） | 同左 |

> 上表の「KsSettingsView」列は移植初期の方針であり、現行の公開 API・最低 OS の正は本 concepts 群と実装コードにある。

## 12. 移植初期の変更提案と必読セクションの対応

作成当時、移植初期の各変更提案（現在は凍結資料 `openspec/` に残る）から本文書の該当セクションを参照していた。当時の対応表を記録として残す。

| 変更提案 | 必読セクション | ピンポイント参照ファイル |
|---|---|---|
| `add-monorepo-foundation` | §1（構造） | -（構造把握のみ） |
| `add-settings-view-core` | §2（CellBase）、§4（SettingsView 全体プロパティ）、§11（差分） | `Cells/CellBase.cs`、`SettingsView.DefineProperites.cs` |
| `add-settings-view-ios-ui` | §6（iOS Native）、§9（NativeCollectionView） | `Handlers/SettingsViewHandler.iOS.cs`、`AiForms.Maui.NativeCollectionView/Platforms/iOS/` |
| `add-settings-view-android-ui` | §7（Android Native）、§9（NativeCollectionView） | `Handlers/SettingsViewHandler.Android.cs`、`AiForms.Maui.NativeCollectionView/Platforms/Android/` |
| `add-cell-types-basic` | §2、§3（基本7種）、§5（Handler）、§11 | `Cells/{Label,Command,Button,Switch,Checkbox,Radio,SimpleCheck}Cell.cs`、対応 Handler |
| `add-cell-types-input` | §2、§3（入力系6種）、§5、§11 | `Cells/{Entry,Picker,TextPicker,NumberPicker,TimePicker,DatePicker}Cell.cs`、`Pages/`、対応 Handler |
| `add-cell-types-custom` | §3（CustomCell）、§5、§11 | `Cells/CustomCell.cs`、`Handlers/CustomCell/` |
| `add-maui-bindings` | §5（Handler）、§8（メモリ）、§11、§10（Sample） | `Handlers/` 全体、`MauiAppBuilderExtension.cs`、`Sample/Views/` |

## 関連

- [aiforms-origin-reference.md](aiforms-origin-reference.md) — 移植元リポジトリの在り処と参照ルール（移植完了までの時限規約）
- [cross/ADR-0017](../../../decisions/cross/0017-port-aiforms-to-native.md) — Native ベースへ移植・リファインする決定
