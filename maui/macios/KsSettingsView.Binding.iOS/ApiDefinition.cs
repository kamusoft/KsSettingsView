using System;
using Foundation;
using ObjCRuntime;
using UIKit;

namespace KsSettingsView.Bridge;

// KsSettingsViewBridge の `@objc` 公開表面をそのまま写した binding 定義。
// 型名・引数の並びは Native 側と 1:1 に保ち、変換や既定値の補完はここでは行わない。
//
// `dispose` だけは C# 側で DisposeBridge に改名する。NSObject が既に IDisposable の
// Dispose() を持っており、同名では JNI/ObjC ハンドル解放と Bridge 破棄が区別できないため。

/// <summary>
/// Cell 個別スタイルを interop 境界で輸送する DTO。色は ARGB を詰めた 32bit 整数、寸法は数値で表し、
/// null は未指定 (Theme から継承) を意味する (maui/ADR-0004)。
/// </summary>
[BaseType(typeof(NSObject))]
interface KsBridgeCellStyle
{
    /// <summary>タイトル文字色 (ARGB)</summary>
    [NullAllowed]
    [Export("titleColor", ArgumentSemantic.Strong)]
    NSNumber TitleColor { get; set; }

    /// <summary>タイトルフォント</summary>
    [NullAllowed]
    [Export("titleFont", ArgumentSemantic.Strong)]
    KsBridgeFont TitleFont { get; set; }

    /// <summary>説明文色 (ARGB)</summary>
    [NullAllowed]
    [Export("descriptionColor", ArgumentSemantic.Strong)]
    NSNumber DescriptionColor { get; set; }

    /// <summary>説明文フォント</summary>
    [NullAllowed]
    [Export("descriptionFont", ArgumentSemantic.Strong)]
    KsBridgeFont DescriptionFont { get; set; }

    /// <summary>値テキスト色 (ARGB)</summary>
    [NullAllowed]
    [Export("valueTextColor", ArgumentSemantic.Strong)]
    NSNumber ValueTextColor { get; set; }

    /// <summary>値テキストフォント</summary>
    [NullAllowed]
    [Export("valueTextFont", ArgumentSemantic.Strong)]
    KsBridgeFont ValueTextFont { get; set; }

    /// <summary>アイコンサイズ (pt)</summary>
    [NullAllowed]
    [Export("iconSize", ArgumentSemantic.Strong)]
    NSNumber IconSize { get; set; }

    /// <summary>アイコン角丸半径 (pt)</summary>
    [NullAllowed]
    [Export("iconRadius", ArgumentSemantic.Strong)]
    NSNumber IconRadius { get; set; }

    /// <summary>Cell 高さ (pt)</summary>
    [NullAllowed]
    [Export("cellHeight", ArgumentSemantic.Strong)]
    NSNumber CellHeight { get; set; }

    /// <summary>ヒントテキスト色 (ARGB)</summary>
    [NullAllowed]
    [Export("hintTextColor", ArgumentSemantic.Strong)]
    NSNumber HintTextColor { get; set; }

    /// <summary>ヒントテキストフォント</summary>
    [NullAllowed]
    [Export("hintTextFont", ArgumentSemantic.Strong)]
    KsBridgeFont HintTextFont { get; set; }

    /// <summary>Cell 個別背景色 (ARGB)</summary>
    [NullAllowed]
    [Export("backgroundColor", ArgumentSemantic.Strong)]
    NSNumber BackgroundColor { get; set; }

    /// <summary>Cell 個別 accent 色 (ARGB)</summary>
    [NullAllowed]
    [Export("accentColor", ArgumentSemantic.Strong)]
    NSNumber AccentColor { get; set; }
}

/// <summary>
/// interop 境界で Cell を輸送する DTO の共通基底。Cell 種ごとの派生 DTO を持ち (maui/ADR-0011)、
/// Section の Cells や CellUpdate はこの基底型で異種 Cell を混載する。基底そのものを Store へ
/// 載せた場合は共通フィールドだけを持つ LabelCell として構築される。
/// </summary>
[BaseType(typeof(NSObject))]
[DisableDefaultCtor]
interface KsBridgeCell
{
    /// <summary>Bridge が採番した canonical UUID 文字列の Cell ID。</summary>
    [Export("cellID")]
    string CellID { get; }

    /// <summary>タイトル (必須)</summary>
    [Export("title")]
    string Title { get; set; }

    /// <summary>説明文 (未指定は null)</summary>
    [NullAllowed]
    [Export("descriptionText")]
    string DescriptionText { get; set; }

    /// <summary>右側に表示する値文字列 (未指定は null)</summary>
    [NullAllowed]
    [Export("valueText")]
    string ValueText { get; set; }

    /// <summary>ヒントテキスト (未指定は null)</summary>
    [NullAllowed]
    [Export("hintText")]
    string HintText { get; set; }

    /// <summary>アイコン画像 (未指定は null)。上位層が解決した platform 画像をそのまま受け取る。</summary>
    [NullAllowed]
    [Export("icon", ArgumentSemantic.Strong)]
    UIImage Icon { get; set; }

    /// <summary>Cell 個別スタイルの上書き (未指定は null で Theme を継承)</summary>
    [NullAllowed]
    [Export("style", ArgumentSemantic.Strong)]
    KsBridgeCellStyle Style { get; set; }

    /// <summary>有効／無効フラグ</summary>
    [Export("isEnabled")]
    bool IsEnabled { get; set; }

    /// <summary>可視性フラグ。false の Cell は表示から除外される。</summary>
    [Export("isVisible")]
    bool IsVisible { get; set; }

    /// <summary>
    /// 既存 Cell の cellID を引き継ぐ。ReplaceSection で配下 Cell の採番済み cellID を温存するために
    /// 使う。canonical UUID として解釈できない値は引き継がず false を返す。
    /// </summary>
    [Export("adoptCellID:")]
    bool AdoptCellID(string cellID);

    /// <summary>タイトルのみを指定して DTO を生成する。</summary>
    [Export("initWithTitle:")]
    NativeHandle Constructor(string title);

    /// <summary>共通フィールドを指定して DTO を生成する。</summary>
    [Export("initWithTitle:descriptionText:valueText:hintText:isEnabled:isVisible:")]
    NativeHandle Constructor(
        string title,
        [NullAllowed] string descriptionText,
        [NullAllowed] string valueText,
        [NullAllowed] string hintText,
        bool isEnabled,
        bool isVisible);
}

/// <summary>読み取り専用の表示 Cell を interop 境界で輸送する DTO。固有のフィールドを持たない。</summary>
[BaseType(typeof(KsBridgeCell))]
[DisableDefaultCtor]
interface KsBridgeLabelCell
{
    /// <summary>タイトルのみを指定して DTO を生成する。</summary>
    [Export("initWithTitle:")]
    NativeHandle Constructor(string title);

    /// <summary>共通フィールドを指定して DTO を生成する。</summary>
    [Export("initWithTitle:descriptionText:valueText:hintText:isEnabled:isVisible:")]
    NativeHandle Constructor(
        string title,
        [NullAllowed] string descriptionText,
        [NullAllowed] string valueText,
        [NullAllowed] string hintText,
        bool isEnabled,
        bool isVisible);
}

/// <summary>
/// タップで処理を実行する Cell を輸送する DTO。タップは InteractionDelegate の CommandCellTapped で
/// 通知される。
/// </summary>
[BaseType(typeof(KsBridgeCell))]
[DisableDefaultCtor]
interface KsBridgeCommandCell
{
    /// <summary>タイトルのみを指定して DTO を生成する。</summary>
    [Export("initWithTitle:")]
    NativeHandle Constructor(string title);

    /// <summary>Disclosure Indicator を非表示にするフラグ</summary>
    [Export("hideArrow")]
    bool HideArrow { get; set; }
}

/// <summary>
/// 任意の view を行の内容として表示する Cell を輸送する DTO。内容は view の実体と、その実体の世代を
/// 表す ContentToken の対で運ぶ (maui/ADR-0020)。Native から見た内容の等価性は ContentToken の値等価で
/// 決まり、token が同じ間は他のプロパティ変更で再バインドが起きても埋め込まれた view は同一
/// インスタンスのまま維持される。View が null の DTO は内容なしの行として描画される。
/// 共通行レイアウトのスロットを持たないため、基底の Title / DescriptionText / ValueText / HintText /
/// Icon は Native へ写されない。タップは HasTapHandler が true のときだけ InteractionDelegate の
/// CustomCellTapped で通知される。
/// </summary>
[BaseType(typeof(KsBridgeCell))]
[DisableDefaultCtor]
interface KsBridgeCustomCell
{
    /// <summary>タイトルのみを指定して DTO を生成する。</summary>
    [Export("initWithTitle:")]
    NativeHandle Constructor(string title);

    /// <summary>行の内容として表示する view (null で内容なし)</summary>
    [NullAllowed]
    [Export("view", ArgumentSemantic.Strong)]
    UIView View { get; set; }

    /// <summary>内容として埋め込む view の世代。実体が入れ替わるたびに変わる値を上位層が振る。</summary>
    [Export("contentToken")]
    string ContentToken { get; set; }

    /// <summary>Disclosure Indicator を表示するフラグ</summary>
    [Export("showArrowIndicator")]
    bool ShowArrowIndicator { get; set; }

    /// <summary>行タップを通知するフラグ</summary>
    [Export("hasTapHandler")]
    bool HasTapHandler { get; set; }
}

/// <summary>
/// ボタン用途の Cell を輸送する DTO。説明文を持たないため基底の DescriptionText は Native へ
/// 写されない。タップは InteractionDelegate の ButtonCellTapped で通知される。
/// </summary>
[BaseType(typeof(KsBridgeCell))]
[DisableDefaultCtor]
interface KsBridgeButtonCell
{
    /// <summary>タイトルのみを指定して DTO を生成する。</summary>
    [Export("initWithTitle:")]
    NativeHandle Constructor(string title);

    /// <summary>ボタンテキストの色 (ARGB、未指定は null)</summary>
    [NullAllowed]
    [Export("titleColor", ArgumentSemantic.Strong)]
    NSNumber TitleColor { get; set; }

    /// <summary>タイトルの揃え位置の序数 (0 = Start / 1 = Center / 2 = End、未指定は null)</summary>
    [NullAllowed]
    [Export("titleAlignment", ArgumentSemantic.Strong)]
    NSNumber TitleAlignment { get; set; }
}

/// <summary>
/// ON/OFF スイッチを持つ Cell を輸送する DTO。値変更は InteractionDelegate の SwitchCellChanged で
/// 通知される。
/// </summary>
[BaseType(typeof(KsBridgeCell))]
[DisableDefaultCtor]
interface KsBridgeSwitchCell
{
    /// <summary>タイトルのみを指定して DTO を生成する。</summary>
    [Export("initWithTitle:")]
    NativeHandle Constructor(string title);

    /// <summary>現在の ON/OFF 値</summary>
    [Export("isOn")]
    bool IsOn { get; set; }

    /// <summary>スイッチ ON 時の色 (ARGB、未指定は null)</summary>
    [NullAllowed]
    [Export("accentColor", ArgumentSemantic.Strong)]
    NSNumber AccentColor { get; set; }
}

/// <summary>
/// チェックボックス Cell を輸送する DTO。値変更は InteractionDelegate の CheckboxCellChanged で
/// 通知される。
/// </summary>
[BaseType(typeof(KsBridgeCell))]
[DisableDefaultCtor]
interface KsBridgeCheckboxCell
{
    /// <summary>タイトルのみを指定して DTO を生成する。</summary>
    [Export("initWithTitle:")]
    NativeHandle Constructor(string title);

    /// <summary>チェック状態</summary>
    [Export("isChecked")]
    bool IsChecked { get; set; }

    /// <summary>チェックマーク色 (ARGB、未指定は null)</summary>
    [NullAllowed]
    [Export("accentColor", ArgumentSemantic.Strong)]
    NSNumber AccentColor { get; set; }
}

/// <summary>
/// 行全体のタップでチェックを切り替える Cell を輸送する DTO。値変更は InteractionDelegate の
/// SimpleCheckCellChanged で通知される。
/// </summary>
[BaseType(typeof(KsBridgeCell))]
[DisableDefaultCtor]
interface KsBridgeSimpleCheckCell
{
    /// <summary>タイトルのみを指定して DTO を生成する。</summary>
    [Export("initWithTitle:")]
    NativeHandle Constructor(string title);

    /// <summary>チェック状態</summary>
    [Export("isChecked")]
    bool IsChecked { get; set; }

    /// <summary>チェックマーク色 (ARGB、未指定は null)</summary>
    [NullAllowed]
    [Export("accentColor", ArgumentSemantic.Strong)]
    NSNumber AccentColor { get; set; }
}

/// <summary>
/// 同一グループ内で 1 つだけ選択される Cell を輸送する DTO。選択は InteractionDelegate の
/// RadioCellSelected で通知される。グループ内の他 Cell への追随は上位層の責務。
/// </summary>
[BaseType(typeof(KsBridgeCell))]
[DisableDefaultCtor]
interface KsBridgeRadioCell
{
    /// <summary>タイトルのみを指定して DTO を生成する。</summary>
    [Export("initWithTitle:")]
    NativeHandle Constructor(string title);

    /// <summary>同一選択グループの識別子</summary>
    [Export("groupID")]
    string GroupID { get; set; }

    /// <summary>この Cell の値</summary>
    [Export("value")]
    string Value { get; set; }

    /// <summary>グループ内の現在選択値 (Value と一致するときチェック表示)</summary>
    [Export("selectedValue")]
    string SelectedValue { get; set; }

    /// <summary>チェックマーク色 (ARGB、未指定は null)</summary>
    [NullAllowed]
    [Export("accentColor", ArgumentSemantic.Strong)]
    NSNumber AccentColor { get; set; }
}

/// <summary>
/// テキスト入力欄を持つ Cell を輸送する DTO。値文字列を持たないため基底の ValueText は Native へ
/// 写されない。テキスト変更は InteractionDelegate の EntryCellTextChanged で通知される。
/// </summary>
[BaseType(typeof(KsBridgeCell))]
[DisableDefaultCtor]
interface KsBridgeEntryCell
{
    /// <summary>タイトルのみを指定して DTO を生成する。</summary>
    [Export("initWithTitle:")]
    NativeHandle Constructor(string title);

    /// <summary>現在のテキスト値</summary>
    [Export("text")]
    string Text { get; set; }

    /// <summary>プレースホルダ (未指定は null)</summary>
    [NullAllowed]
    [Export("placeholder")]
    string Placeholder { get; set; }

    /// <summary>プレースホルダ文字色 (ARGB、未指定は null)</summary>
    [NullAllowed]
    [Export("placeholderColor", ArgumentSemantic.Strong)]
    NSNumber PlaceholderColor { get; set; }

    /// <summary>
    /// キーボード種別の序数 (0 = Default / 1 = Plain / 2 = Text / 3 = Chat / 4 = Url / 5 = Email /
    /// 6 = Numeric / 7 = Telephone)
    /// </summary>
    [Export("keyboard")]
    nint Keyboard { get; set; }

    /// <summary>パスワードマスクフラグ</summary>
    [Export("isPassword")]
    bool IsPassword { get; set; }

    /// <summary>テキスト配置の序数 (0 = Start / 1 = Center / 2 = End、未指定は null)</summary>
    [NullAllowed]
    [Export("textAlignment", ArgumentSemantic.Strong)]
    NSNumber TextAlignment { get; set; }

    /// <summary>caret 色および選択ハイライト色 (ARGB、未指定は null)</summary>
    [NullAllowed]
    [Export("accentColor", ArgumentSemantic.Strong)]
    NSNumber AccentColor { get; set; }

    /// <summary>最大文字数 (未指定は null で無制限)</summary>
    [NullAllowed]
    [Export("maxLength", ArgumentSemantic.Strong)]
    NSNumber MaxLength { get; set; }
}

/// <summary>
/// 一覧から項目を選ぶ Cell の候補 1 件 (主表示 + 任意の副表示) を輸送する DTO。表示射影は上位層で
/// 適用済みであり、SubText が null の候補は副表示を持たない。
/// </summary>
[BaseType(typeof(NSObject))]
[DisableDefaultCtor]
interface KsBridgePickerItem
{
    /// <summary>主表示と副表示を指定して候補を生成する。</summary>
    [Export("initWithText:subText:")]
    NativeHandle Constructor(string text, [NullAllowed] string subText);

    /// <summary>副表示を持たない候補を生成する。</summary>
    [Export("initWithText:")]
    NativeHandle Constructor(string text);

    /// <summary>主表示テキスト</summary>
    [Export("text")]
    string Text { get; set; }

    /// <summary>副表示テキスト (未指定は null)</summary>
    [NullAllowed]
    [Export("subText")]
    string SubText { get; set; }
}

/// <summary>
/// 一覧から項目を選ぶ Cell を輸送する DTO。選択値は Native の実体である index で運ぶ
/// (maui/ADR-0012)。選択変更は SelectionMode に応じて InteractionDelegate の
/// PickerCellSelectionChanged または PickerCellMultiSelectionChanged で通知される。
/// </summary>
[BaseType(typeof(KsBridgeCell))]
[DisableDefaultCtor]
interface KsBridgePickerCell
{
    /// <summary>タイトルのみを指定して DTO を生成する。</summary>
    [Export("initWithTitle:")]
    NativeHandle Constructor(string title);

    /// <summary>選択候補の項目 (表示整形済み)</summary>
    [Export("items", ArgumentSemantic.Copy)]
    KsBridgePickerItem[] Items { get; set; }

    /// <summary>選択モードの序数 (0 = Single / 1 = Multiple)</summary>
    [Export("selectionMode")]
    nint SelectionMode { get; set; }

    /// <summary>単一選択モードの選択 index (未選択は null)</summary>
    [NullAllowed]
    [Export("selectedIndex", ArgumentSemantic.Strong)]
    NSNumber SelectedIndex { get; set; }

    /// <summary>複数選択モードの選択 index 群</summary>
    [Export("selectedIndices", ArgumentSemantic.Copy)]
    NSNumber[] SelectedIndices { get; set; }

    /// <summary>複数選択モードでの選択上限 (0 で無制限)</summary>
    [Export("maxSelectedNumber")]
    nint MaxSelectedNumber { get; set; }

    /// <summary>選択面のタイトル (未指定は null)</summary>
    [NullAllowed]
    [Export("pageTitle")]
    string PageTitle { get; set; }

    /// <summary>選択強調色 (ARGB、未指定は null)</summary>
    [NullAllowed]
    [Export("accentColor", ArgumentSemantic.Strong)]
    NSNumber AccentColor { get; set; }
}

/// <summary>
/// 数値を選ぶ Cell を輸送する DTO。値変更は InteractionDelegate の NumberPickerCellChanged で
/// 通知される。
/// </summary>
[BaseType(typeof(KsBridgeCell))]
[DisableDefaultCtor]
interface KsBridgeNumberPickerCell
{
    /// <summary>タイトルのみを指定して DTO を生成する。</summary>
    [Export("initWithTitle:")]
    NativeHandle Constructor(string title);

    /// <summary>選択できる最小値</summary>
    [Export("min")]
    nint Min { get; set; }

    /// <summary>選択できる最大値</summary>
    [Export("max")]
    nint Max { get; set; }

    /// <summary>選択の刻み幅</summary>
    [Export("step")]
    nint Step { get; set; }

    /// <summary>現在の値</summary>
    [Export("value")]
    nint Value { get; set; }

    /// <summary>値に付ける単位文字列 (空文字列で単位なし)</summary>
    [Export("unit")]
    string Unit { get; set; }

    /// <summary>選択面のタイトル (未指定は null)</summary>
    [NullAllowed]
    [Export("pickerTitle")]
    string PickerTitle { get; set; }

    /// <summary>選択強調色 (ARGB、未指定は null)</summary>
    [NullAllowed]
    [Export("accentColor", ArgumentSemantic.Strong)]
    NSNumber AccentColor { get; set; }
}

/// <summary>
/// 時刻を選ぶ Cell を輸送する DTO。時刻は壁時計値として "HH:mm" で運ぶ (maui/ADR-0012)。値変更は
/// InteractionDelegate の TimePickerCellChanged で同じ書式で通知される。
/// </summary>
[BaseType(typeof(KsBridgeCell))]
[DisableDefaultCtor]
interface KsBridgeTimePickerCell
{
    /// <summary>タイトルのみを指定して DTO を生成する。</summary>
    [Export("initWithTitle:")]
    NativeHandle Constructor(string title);

    /// <summary>現在の時刻 ("HH:mm")</summary>
    [Export("time")]
    string Time { get; set; }

    /// <summary>表示フォーマット (未指定は null で Native 既定)</summary>
    [NullAllowed]
    [Export("format")]
    string Format { get; set; }

    /// <summary>選択面の時制 (true = 24時間制 / false = 12時間制、未指定は null で Native 既定)</summary>
    [NullAllowed]
    [Export("is24Hour", ArgumentSemantic.Strong)]
    NSNumber Is24Hour { get; set; }

    /// <summary>選択面のタイトル (未指定は null)</summary>
    [NullAllowed]
    [Export("pickerTitle")]
    string PickerTitle { get; set; }

    /// <summary>選択強調色 (ARGB、未指定は null)</summary>
    [NullAllowed]
    [Export("accentColor", ArgumentSemantic.Strong)]
    NSNumber AccentColor { get; set; }
}

/// <summary>
/// 日付を選ぶ Cell を輸送する DTO。日付は壁時計値として "yyyy-MM-dd" で運ぶ (maui/ADR-0012)。
/// 選択面の形式は統一 enum の序数で運び、未指定のときは Native 既定を使う (maui/ADR-0013)。値変更は
/// InteractionDelegate の DatePickerCellChanged で通知される。
/// </summary>
[BaseType(typeof(KsBridgeCell))]
[DisableDefaultCtor]
interface KsBridgeDatePickerCell
{
    /// <summary>タイトルのみを指定して DTO を生成する。</summary>
    [Export("initWithTitle:")]
    NativeHandle Constructor(string title);

    /// <summary>現在の日付 ("yyyy-MM-dd")</summary>
    [Export("date")]
    string Date { get; set; }

    /// <summary>表示フォーマット (未指定は null で Native 既定)</summary>
    [NullAllowed]
    [Export("format")]
    string Format { get; set; }

    /// <summary>選択できる最小日付 ("yyyy-MM-dd"、未指定は null)</summary>
    [NullAllowed]
    [Export("minDate")]
    string MinDate { get; set; }

    /// <summary>選択できる最大日付 ("yyyy-MM-dd"、未指定は null)</summary>
    [NullAllowed]
    [Export("maxDate")]
    string MaxDate { get; set; }

    /// <summary>選択面のタイトル (未指定は null)</summary>
    [NullAllowed]
    [Export("pickerTitle")]
    string PickerTitle { get; set; }

    /// <summary>選択強調色 (ARGB、未指定は null)</summary>
    [NullAllowed]
    [Export("accentColor", ArgumentSemantic.Strong)]
    NSNumber AccentColor { get; set; }

    /// <summary>選択面の形式の序数 (0 = Calendar / 1 = Wheels、未指定は null で Native 既定)</summary>
    [NullAllowed]
    [Export("uiStyle", ArgumentSemantic.Strong)]
    NSNumber UiStyle { get; set; }

    /// <summary>Today ボタンの表示文字列 (null または空で非表示)</summary>
    [NullAllowed]
    [Export("todayText")]
    string TodayText { get; set; }
}

/// <summary>
/// Bridge が表示中の Cell に対するユーザー操作を通知する delegate。Bridge instance あたり 1 個の
/// 通知チャネルで全 Cell の操作を運び、Cell 種別はメソッド名で識別する (maui/ADR-0003)。値の表現は
/// interop 境界の輸送規約に従う (maui/ADR-0012)。通知は Native の UI スレッド上で同期に呼ばれる。
/// </summary>
/// <remarks>
/// 全メソッドを Abstract とし、実装側に全 Cell 種の通知を受けることを要求する。Abstract を外しても
/// 生成される Model の実装本体は変わらず (未 override のまま呼ばれれば
/// You_Should_Not_Call_base_In_This_Method を送出する)、任意実装にはならない。Abstract の有無で
/// 変わるのは ProtocolMember の IsRequired と RequiredMember 属性、すなわち「必須である」ことが
/// メタデータに出るかどうかだけであり、Swift 側 protocol が必須メンバとして宣言している事実と
/// 揃える意味で付ける。Android の listener が C# の interface として束縛され実装が必須になるのとも
/// 対称になる。
/// </remarks>
[Protocol]
[Model]
[BaseType(typeof(NSObject))]
interface KsBridgeInteractionDelegate
{
    /// <summary>CommandCell がタップされた。</summary>
    [Abstract]
    [Export("commandCellTapped:")]
    void CommandCellTapped(string cellID);

    /// <summary>ButtonCell がタップされた。</summary>
    [Abstract]
    [Export("buttonCellTapped:")]
    void ButtonCellTapped(string cellID);

    /// <summary>
    /// CustomCell の行がタップされた。タップ通知を持たせずに構築された CustomCell は行タップ動作
    /// そのものを持たないため、このメソッドは呼ばれない。
    /// </summary>
    [Abstract]
    [Export("customCellTapped:")]
    void CustomCellTapped(string cellID);

    /// <summary>SwitchCell の値が変わった。</summary>
    [Abstract]
    [Export("switchCellChanged:isOn:")]
    void SwitchCellChanged(string cellID, bool isOn);

    /// <summary>CheckboxCell の値が変わった。</summary>
    [Abstract]
    [Export("checkboxCellChanged:isChecked:")]
    void CheckboxCellChanged(string cellID, bool isChecked);

    /// <summary>SimpleCheckCell の値が変わった。</summary>
    [Abstract]
    [Export("simpleCheckCellChanged:isChecked:")]
    void SimpleCheckCellChanged(string cellID, bool isChecked);

    /// <summary>RadioCell が選択された。</summary>
    [Abstract]
    [Export("radioCellSelected:value:")]
    void RadioCellSelected(string cellID, string value);

    /// <summary>EntryCell のテキストが変わった。</summary>
    [Abstract]
    [Export("entryCellTextChanged:text:")]
    void EntryCellTextChanged(string cellID, string text);

    /// <summary>PickerCell (単一選択) の選択が変わった。</summary>
    [Abstract]
    [Export("pickerCellSelectionChanged:index:")]
    void PickerCellSelectionChanged(string cellID, nint index);

    /// <summary>PickerCell (複数選択) の選択が変わった。index は昇順・重複なし。</summary>
    [Abstract]
    [Export("pickerCellMultiSelectionChanged:indices:")]
    void PickerCellMultiSelectionChanged(string cellID, NSNumber[] indices);

    /// <summary>NumberPickerCell の値が変わった。</summary>
    [Abstract]
    [Export("numberPickerCellChanged:value:")]
    void NumberPickerCellChanged(string cellID, nint value);

    /// <summary>TimePickerCell の時刻が変わった ("HH:mm")。</summary>
    [Abstract]
    [Export("timePickerCellChanged:time:")]
    void TimePickerCellChanged(string cellID, string time);

    /// <summary>DatePickerCell の日付が変わった ("yyyy-MM-dd")。</summary>
    [Abstract]
    [Export("datePickerCellChanged:date:")]
    void DatePickerCellChanged(string cellID, string date);
}

/// <summary>
/// Section を interop 境界で輸送する DTO。header / footer は text と view の両方を輸送でき、
/// 同じ位置に両方を指定した場合は view が優先される。
/// </summary>
[BaseType(typeof(NSObject))]
[DisableDefaultCtor]
interface KsBridgeSection
{
    /// <summary>Bridge が採番した canonical UUID 文字列の Section ID。</summary>
    [Export("sectionID")]
    string SectionID { get; }

    /// <summary>ヘッダテキスト (null でヘッダなし)</summary>
    [NullAllowed]
    [Export("headerText")]
    string HeaderText { get; set; }

    /// <summary>フッタテキスト (null でフッタなし)</summary>
    [NullAllowed]
    [Export("footerText")]
    string FooterText { get; set; }

    /// <summary>ヘッダに表示する view (null で view 指定なし)。非 null なら HeaderText より優先される。</summary>
    [NullAllowed]
    [Export("headerView", ArgumentSemantic.Strong)]
    UIView HeaderView { get; set; }

    /// <summary>フッタに表示する view (null で view 指定なし)。非 null なら FooterText より優先される。</summary>
    [NullAllowed]
    [Export("footerView", ArgumentSemantic.Strong)]
    UIView FooterView { get; set; }

    /// <summary>
    /// 可視性フラグ。false の Section は header / footer / 配下 Cell ごと表示から除外される。
    /// </summary>
    [Export("isVisible")]
    bool IsVisible { get; set; }

    /// <summary>
    /// Header の表示トグル。false のとき内容があっても Header を表示しない。内容が無い
    /// (null または空 text) Header をトグルで表示させることはできない。
    /// </summary>
    [Export("isHeaderVisible")]
    bool IsHeaderVisible { get; set; }

    /// <summary>Footer の表示トグル。意味論は IsHeaderVisible と対称。</summary>
    [Export("isFooterVisible")]
    bool IsFooterVisible { get; set; }

    /// <summary>ヘッダの固定高さ (pt)。null で Native 既定の自動高さ。</summary>
    [NullAllowed]
    [Export("headerHeight")]
    NSNumber HeaderHeight { get; set; }

    /// <summary>Section 内の Cell 群 (追加順)。Cell 種の異なる DTO を混載できる。</summary>
    [Export("cells")]
    KsBridgeCell[] Cells { get; }

    /// <summary>header / footer テキストを指定して空の Section DTO を生成する。</summary>
    [Export("initWithHeaderText:footerText:")]
    NativeHandle Constructor([NullAllowed] string headerText, [NullAllowed] string footerText);

    /// <summary>header / footer テキストと Cell 群を指定して Section DTO を生成する。</summary>
    [Export("initWithHeaderText:footerText:cells:")]
    NativeHandle Constructor(
        [NullAllowed] string headerText,
        [NullAllowed] string footerText,
        KsBridgeCell[] cells);

    /// <summary>Cell を末尾に追加し、Bridge が採番した cellID を返す。</summary>
    [Export("addCell:")]
    string AddCell(KsBridgeCell cell);
}

/// <summary>設定ツリーを組み立てて Bridge の SetRoot へ渡す Builder。</summary>
[BaseType(typeof(NSObject))]
interface KsBridgeRootBuilder
{
    /// <summary>追加順の Section 群。</summary>
    [Export("sections")]
    KsBridgeSection[] Sections { get; }

    /// <summary>header / footer テキストを持つ Section を生成して末尾に追加する。</summary>
    [Export("addSectionWithHeaderText:footerText:")]
    KsBridgeSection AddSection([NullAllowed] string headerText, [NullAllowed] string footerText);

    /// <summary>生成済みの Section DTO を末尾に追加し、その sectionID を返す。</summary>
    [Export("addSection:")]
    string AddSection(KsBridgeSection section);

    /// <summary>
    /// 指定 Section の末尾に Cell を追加する (Cell 種を問わない)。sectionID が Builder 内に
    /// 存在しない場合は null を返す (no-op)。
    /// </summary>
    [Export("addCell:sectionID:")]
    [return: NullAllowed]
    string AddCell(KsBridgeCell cell, string sectionID);

    /// <summary>
    /// 指定 Section の末尾に LabelCell を追加する。AddCell と同じ動作で、LabelCell に限った
    /// 書き味を残す。sectionID が Builder 内に存在しない場合は null を返す (no-op)。
    /// </summary>
    [Export("addLabelCell:sectionID:")]
    [return: NullAllowed]
    string AddLabelCell(KsBridgeLabelCell cell, string sectionID);
}

/// <summary>
/// 複数 Cell の内容をまとめて置換するときの 1 件分の指定。CellID は Bridge が採番して返した
/// 既存 Cell の ID で、Cell はその位置へ写し取る新しい内容。Cell 自身が持つ CellID は使われず、
/// 更新後も対象 Cell の identity は CellID のまま保たれる。
/// </summary>
[BaseType(typeof(NSObject))]
[DisableDefaultCtor]
interface KsBridgeCellUpdate
{
    /// <summary>更新対象の cellID。</summary>
    [Export("cellID")]
    string CellID { get; }

    /// <summary>更新後の内容。Cell 種の異なる更新を 1 バッチに混載できる。</summary>
    [Export("cell")]
    KsBridgeCell Cell { get; }

    /// <summary>更新対象と新しい内容を指定して生成する。</summary>
    [Export("initWithCellID:cell:")]
    NativeHandle Constructor(string cellID, KsBridgeCell cell);
}

/// <summary>フォントを interop 境界で輸送する記述子。</summary>
[BaseType(typeof(NSObject))]
[DisableDefaultCtor]
interface KsBridgeFont
{
    /// <summary>フォントファミリ名。null または解決できない名前のときはシステムフォントを使う。</summary>
    [NullAllowed]
    [Export("familyName")]
    string FamilyName { get; set; }

    /// <summary>ポイントサイズ。0 以下のときは本文既定サイズを使う。</summary>
    [Export("pointSize")]
    double PointSize { get; set; }

    /// <summary>太字にするか。</summary>
    [Export("isBold")]
    bool IsBold { get; set; }

    /// <summary>斜体にするか。</summary>
    [Export("isItalic")]
    bool IsItalic { get; set; }

    /// <summary>フォント記述子を生成する。</summary>
    [Export("initWithFamilyName:pointSize:isBold:isItalic:")]
    NativeHandle Constructor([NullAllowed] string familyName, double pointSize, bool isBold, bool isItalic);
}

/// <summary>
/// Theme を interop 境界で輸送する DTO。色は ARGB を詰めた 32bit 整数、寸法とフラグは数値で表し、
/// null は未指定を意味する (maui/ADR-0004)。
/// </summary>
[BaseType(typeof(NSObject))]
interface KsBridgeTheme
{
    /// <summary>セパレータ色 (ARGB)</summary>
    [NullAllowed]
    [Export("separatorColor", ArgumentSemantic.Strong)]
    NSNumber SeparatorColor { get; set; }

    /// <summary>SettingsView 自身の背景色 (ARGB)</summary>
    [NullAllowed]
    [Export("backgroundColor", ArgumentSemantic.Strong)]
    NSNumber BackgroundColor { get; set; }

    /// <summary>Cell 既定背景色 (ARGB)</summary>
    [NullAllowed]
    [Export("cellBackgroundColor", ArgumentSemantic.Strong)]
    NSNumber CellBackgroundColor { get; set; }

    /// <summary>Cell 選択時の背景色 (ARGB)</summary>
    [NullAllowed]
    [Export("selectedColor", ArgumentSemantic.Strong)]
    NSNumber SelectedColor { get; set; }

    /// <summary>アクセント色 (ARGB)</summary>
    [NullAllowed]
    [Export("cellAccentColor", ArgumentSemantic.Strong)]
    NSNumber CellAccentColor { get; set; }

    /// <summary>無効時のテキスト置換色 (ARGB)</summary>
    [NullAllowed]
    [Export("disabledTextColor", ArgumentSemantic.Strong)]
    NSNumber DisabledTextColor { get; set; }

    /// <summary>スクロールインジケータ表示 (真偽値)</summary>
    [NullAllowed]
    [Export("scrollIndicatorVisible", ArgumentSemantic.Strong)]
    NSNumber ScrollIndicatorVisible { get; set; }

    /// <summary>行高さ基準値 (整数 pt)</summary>
    [NullAllowed]
    [Export("rowHeight", ArgumentSemantic.Strong)]
    NSNumber RowHeight { get; set; }

    /// <summary>可変高さフラグ (真偽値)</summary>
    [NullAllowed]
    [Export("hasUnevenRows", ArgumentSemantic.Strong)]
    NSNumber HasUnevenRows { get; set; }

    /// <summary>Section ヘッダのテキスト色 (ARGB)</summary>
    [NullAllowed]
    [Export("headerTextColor", ArgumentSemantic.Strong)]
    NSNumber HeaderTextColor { get; set; }

    /// <summary>Section ヘッダの背景色 (ARGB)</summary>
    [NullAllowed]
    [Export("headerBackgroundColor", ArgumentSemantic.Strong)]
    NSNumber HeaderBackgroundColor { get; set; }

    /// <summary>Section ヘッダ既定フォントサイズ (pt)</summary>
    [NullAllowed]
    [Export("headerFontSize", ArgumentSemantic.Strong)]
    NSNumber HeaderFontSize { get; set; }

    /// <summary>Section ヘッダ既定フォント</summary>
    [NullAllowed]
    [Export("headerFont", ArgumentSemantic.Strong)]
    KsBridgeFont HeaderFont { get; set; }

    /// <summary>Section ヘッダの既定高さ (pt)</summary>
    [NullAllowed]
    [Export("headerHeight", ArgumentSemantic.Strong)]
    NSNumber HeaderHeight { get; set; }

    /// <summary>Section フッタのテキスト色 (ARGB)</summary>
    [NullAllowed]
    [Export("footerTextColor", ArgumentSemantic.Strong)]
    NSNumber FooterTextColor { get; set; }

    /// <summary>Section フッタの背景色 (ARGB)</summary>
    [NullAllowed]
    [Export("footerBackgroundColor", ArgumentSemantic.Strong)]
    NSNumber FooterBackgroundColor { get; set; }

    /// <summary>Section フッタ既定フォントサイズ (pt)</summary>
    [NullAllowed]
    [Export("footerFontSize", ArgumentSemantic.Strong)]
    NSNumber FooterFontSize { get; set; }

    /// <summary>Section フッタ既定フォント</summary>
    [NullAllowed]
    [Export("footerFont", ArgumentSemantic.Strong)]
    KsBridgeFont FooterFont { get; set; }

    /// <summary>Cell タイトル既定色 (ARGB)</summary>
    [NullAllowed]
    [Export("cellTitleColor", ArgumentSemantic.Strong)]
    NSNumber CellTitleColor { get; set; }

    /// <summary>Cell タイトル既定フォント</summary>
    [NullAllowed]
    [Export("cellTitleFont", ArgumentSemantic.Strong)]
    KsBridgeFont CellTitleFont { get; set; }

    /// <summary>Cell タイトル既定フォントサイズ (pt)</summary>
    [NullAllowed]
    [Export("cellTitleFontSize", ArgumentSemantic.Strong)]
    NSNumber CellTitleFontSize { get; set; }

    /// <summary>valueText 既定色 (ARGB)</summary>
    [NullAllowed]
    [Export("cellValueTextColor", ArgumentSemantic.Strong)]
    NSNumber CellValueTextColor { get; set; }

    /// <summary>valueText 既定フォント</summary>
    [NullAllowed]
    [Export("cellValueTextFont", ArgumentSemantic.Strong)]
    KsBridgeFont CellValueTextFont { get; set; }

    /// <summary>description 既定色 (ARGB)</summary>
    [NullAllowed]
    [Export("cellDescriptionColor", ArgumentSemantic.Strong)]
    NSNumber CellDescriptionColor { get; set; }

    /// <summary>description 既定フォント</summary>
    [NullAllowed]
    [Export("cellDescriptionFont", ArgumentSemantic.Strong)]
    KsBridgeFont CellDescriptionFont { get; set; }

    /// <summary>hintText 既定色 (ARGB)</summary>
    [NullAllowed]
    [Export("cellHintTextColor", ArgumentSemantic.Strong)]
    NSNumber CellHintTextColor { get; set; }

    /// <summary>hintText 既定フォント</summary>
    [NullAllowed]
    [Export("cellHintFont", ArgumentSemantic.Strong)]
    KsBridgeFont CellHintFont { get; set; }

    /// <summary>EntryCell の placeholder 既定色 (ARGB)</summary>
    [NullAllowed]
    [Export("cellPlaceholderColor", ArgumentSemantic.Strong)]
    NSNumber CellPlaceholderColor { get; set; }

    /// <summary>アイコン既定サイズ (pt)</summary>
    [NullAllowed]
    [Export("cellIconSize", ArgumentSemantic.Strong)]
    NSNumber CellIconSize { get; set; }

    /// <summary>アイコン既定角丸半径 (pt)</summary>
    [NullAllowed]
    [Export("cellIconRadius", ArgumentSemantic.Strong)]
    NSNumber CellIconRadius { get; set; }

    /// <summary>
    /// Section 単位の外側余白の上成分 (pt)。余白の 4 成分は全体で 1 つの指定として扱い、
    /// 1 つでも未指定なら余白全体が未指定になる。
    /// </summary>
    [NullAllowed]
    [Export("sectionMarginTop", ArgumentSemantic.Strong)]
    NSNumber SectionMarginTop { get; set; }

    /// <summary>Section 単位の外側余白の leading 成分 (pt)</summary>
    [NullAllowed]
    [Export("sectionMarginLeading", ArgumentSemantic.Strong)]
    NSNumber SectionMarginLeading { get; set; }

    /// <summary>Section 単位の外側余白の下成分 (pt)</summary>
    [NullAllowed]
    [Export("sectionMarginBottom", ArgumentSemantic.Strong)]
    NSNumber SectionMarginBottom { get; set; }

    /// <summary>Section 単位の外側余白の trailing 成分 (pt)</summary>
    [NullAllowed]
    [Export("sectionMarginTrailing", ArgumentSemantic.Strong)]
    NSNumber SectionMarginTrailing { get; set; }

    /// <summary>Section の箱の角丸半径 (pt)</summary>
    [NullAllowed]
    [Export("sectionCornerRadius", ArgumentSemantic.Strong)]
    NSNumber SectionCornerRadius { get; set; }

    /// <summary>Section の箱のボーダー幅 (pt)</summary>
    [NullAllowed]
    [Export("sectionBorderWidth", ArgumentSemantic.Strong)]
    NSNumber SectionBorderWidth { get; set; }

    /// <summary>Section の箱のボーダー色 (ARGB)</summary>
    [NullAllowed]
    [Export("sectionBorderColor", ArgumentSemantic.Strong)]
    NSNumber SectionBorderColor { get; set; }
}

/// <summary>
/// interop 境界から設定画面を操作する Bridge。内部所有 Store を持ち、公開 API を Store の
/// 公開操作へ変換する (maui/ADR-0001)。全 API を UI スレッドから呼ぶ (maui/ADR-0005)。
/// </summary>
[BaseType(typeof(NSObject))]
interface KsSettingsBridge
{
    /// <summary>
    /// 内部 Store に接続済みの Native Host を返す。生きている Host があればそれを返し、未生成または
    /// ReleaseHost で解放済みなら Store 現在状態から表示を復元した新しい Host を返す。
    /// 破棄済みの Bridge では null を返す。
    /// </summary>
    [Export("makeHostViewController")]
    [return: NullAllowed]
    UIViewController MakeHostViewController();

    /// <summary>
    /// Native Host だけを解放し、Store (設定ツリーと Theme) は維持する (maui/ADR-0007)。解放時に
    /// 旧 Host の Store 購読を解除するため、解放後の Store 更新は旧 Host の表示に反映されない。
    /// 冪等であり、Host 不在時および破棄済みの Bridge では no-op になる。
    /// </summary>
    [Export("releaseHost")]
    void ReleaseHost();

    /// <summary>Bridge を破棄する。冪等であり、破棄後の操作 API と Host 生成は no-op になる。</summary>
    [Export("dispose")]
    void DisposeBridge();

    /// <summary>Builder が組み立てた設定ツリーで root を全置換する。</summary>
    [Export("setRoot:")]
    void SetRoot(KsBridgeRootBuilder builder);

    /// <summary>
    /// Section を指定 index へ挿入し、その sectionID を返す。index は model 配列上の位置で、
    /// 範囲外は端へ丸められる。破棄済みの Bridge では null を返す。
    /// </summary>
    [Export("insertSection:at:")]
    [return: NullAllowed]
    string InsertSection(KsBridgeSection section, nint index);

    /// <summary>指定 ID の Section を削除する。未知の ID は no-op。</summary>
    [Export("removeSectionWithSectionID:")]
    void RemoveSection(string sectionID);

    /// <summary>
    /// Section の順序を変更する。index は model 配列上の位置で、範囲外の移動先は端へ丸められる。
    /// </summary>
    [Export("moveSectionFrom:to:")]
    void MoveSection(nint from, nint to);

    /// <summary>
    /// 指定 ID の Section の内容を置換し、置換後も有効な sectionID (対象と同じ ID) を返す。
    /// newSection 自身の SectionID は破棄されるため、以後の操作には戻り値の ID を使う。
    /// 破棄済み、または対象 Section が存在しない場合は null を返す (no-op)。
    /// </summary>
    [Export("replaceSectionWithSectionID:newSection:")]
    [return: NullAllowed]
    string ReplaceSection(string sectionID, KsBridgeSection newSection);

    /// <summary>
    /// 指定 Section の指定 index へ Cell を挿入し、その cellID を返す。破棄済み、または Section が
    /// 存在しない場合は null を返す (no-op)。
    /// </summary>
    [Export("insertCell:sectionID:at:")]
    [return: NullAllowed]
    string InsertCell(KsBridgeCell cell, string sectionID, nint index);

    /// <summary>指定 ID の Cell を削除する。未知の ID は no-op。</summary>
    [Export("removeCellWithCellID:")]
    void RemoveCell(string cellID);

    /// <summary>指定 ID の Cell を同一 Section 内で移動する。未知の ID は no-op。</summary>
    [Export("moveCellWithCellID:to:")]
    void MoveCell(string cellID, nint index);

    /// <summary>
    /// 指定 ID の Cell の内容を置換し、置換後も有効な cellID (対象と同じ ID) を返す。置換後も
    /// 行の identity は保たれる。newCell 自身の CellID は破棄されるため、以後の操作には
    /// 戻り値の ID を使う。破棄済み、または対象 Cell が存在しない場合は null を返す (no-op)。
    /// </summary>
    [Export("replaceCellWithCellID:newCell:")]
    [return: NullAllowed]
    string ReplaceCell(string cellID, KsBridgeCell newCell);

    /// <summary>
    /// 複数 Cell の内容をまとめて置換し、1 回のバッチ内容更新として反映する。未知の ID は無視される。
    /// </summary>
    [Export("replaceCells:")]
    void ReplaceCells(KsBridgeCellUpdate[] updates);

    /// <summary>
    /// Root / Section の header・footer に表示する text を更新する。text が null のときは
    /// accessory を解除する。
    /// </summary>
    [Export("updateAccessoryWithTarget:sectionID:text:")]
    void UpdateAccessory(KsBridgeAccessoryTarget target, [NullAllowed] string sectionID, [NullAllowed] string text);

    /// <summary>
    /// Root / Section の header・footer に表示する view を更新する。view が null のときは
    /// accessory を解除する。渡した view は取り付け直前に既存の親から切り離される。
    /// </summary>
    [Export("updateAccessoryViewWithTarget:sectionID:view:")]
    void UpdateAccessoryView(KsBridgeAccessoryTarget target, [NullAllowed] string sectionID, [NullAllowed] UIView view);

    /// <summary>
    /// 表示中の accessory 領域の高さを測り直すよう要求する。view accessory の中身が自分の計測結果を
    /// 変えたときに呼ぶ。一過性の要求であり Store の状態は変化しない。
    /// </summary>
    [Export("invalidateAccessoryMeasurementWithTarget:sectionID:")]
    void InvalidateAccessoryMeasurement(KsBridgeAccessoryTarget target, [NullAllowed] string sectionID);

    /// <summary>Theme を適用する。同値の Theme を再指定した場合は更新が通知されない。</summary>
    [Export("setTheme:")]
    void SetTheme(KsBridgeTheme theme);

    /// <summary>
    /// 見た目スタイルを適用する。値は列挙の序数 (Classic = 0 / Modern = 1) で、定義域外は
    /// Classic へ正規化される。Host 未生成のときは値が控えられ、次の Host 生成時に適用される。
    /// </summary>
    [Export("setStyle:")]
    void SetStyle(nint style);

    /// <summary>
    /// ユーザー操作の通知先 (弱参照)。managed 側の実体は呼び出し側が強参照で保持する。
    /// </summary>
    [NullAllowed]
    [Export("interactionDelegate", ArgumentSemantic.Weak)]
    NSObject WeakInteractionDelegate { get; set; }

    /// <summary>
    /// 型付きのユーザー操作通知先。null を設定すると解除でき、以後の操作は通知されない。
    /// </summary>
    [Wrap("WeakInteractionDelegate")]
    [NullAllowed]
    KsBridgeInteractionDelegate InteractionDelegate { get; set; }
}
