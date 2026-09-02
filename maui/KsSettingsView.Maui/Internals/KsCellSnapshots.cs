using System.Collections.Generic;
using Microsoft.Maui;

namespace KsSettingsView.Internals;

/// <summary>値を読み取り専用で表示する Cell の写し。</summary>
/// <remarks>
/// どの派生にも当てはまらない Cell の写しもこの形になる。輸送側の基底 DTO と同じく、
/// 共通フィールドだけを持つ読み取り専用の行として扱われる。
/// </remarks>
internal sealed record KsLabelCellSnapshot : KsCellSnapshot
{
    /// <summary>行の右側に表示する値文字列。</summary>
    public string? ValueText { get; init; }
}

/// <summary>タップで処理を実行する Cell の写し。</summary>
internal sealed record KsCommandCellSnapshot : KsCellSnapshot
{
    /// <summary>行の右側に表示する値文字列。</summary>
    public string? ValueText { get; init; }

    /// <summary>Disclosure Indicator を隠すかどうか。</summary>
    public bool HideArrow { get; init; }
}

/// <summary>任意の View を内容として表示する Cell の写し。</summary>
/// <remarks>
/// View の実体は写しに載らない。載るのは実体の世代だけで、実体そのものは変換経路が抱え、
/// gateway が輸送 DTO を組み立てるときに引き当てる (maui/ADR-0020)。共通行レイアウトの
/// スロットを持たない Cell のため、タイトル・説明文・ヒントは常に未指定で写される。
/// </remarks>
internal sealed record KsCustomCellSnapshot : KsCellSnapshot
{
    /// <summary>内容として実体化されている View の世代。実体が入れ替わるたびに変わる。</summary>
    public string ContentToken { get; init; } = string.Empty;

    /// <summary>Disclosure Indicator を表示するかどうか。</summary>
    public bool ShowArrowIndicator { get; init; }

    /// <summary>行タップの通知先を持つかどうか。</summary>
    public bool HasTapHandler { get; init; }
}

/// <summary>ボタン用途の Cell の写し。</summary>
/// <remarks>説明文を持たない Cell のため、共通の説明文は常に未指定で写される。</remarks>
internal sealed record KsButtonCellSnapshot : KsCellSnapshot
{
    /// <summary>行の右側に表示する値文字列。</summary>
    public string? ValueText { get; init; }

    /// <summary>タイトルの揃え位置。未指定は Native 既定 (中央寄せ)。</summary>
    public TextAlignment? TitleAlignment { get; init; }
}

/// <summary>ON/OFF スイッチを持つ Cell の写し。</summary>
internal sealed record KsSwitchCellSnapshot : KsCellSnapshot
{
    /// <summary>行の右側に表示する値文字列。</summary>
    public string? ValueText { get; init; }

    /// <summary>現在の ON/OFF 値。</summary>
    public bool IsOn { get; init; }

    /// <summary>強調表示の色 (ARGB)。未指定は既定スタイルを継承。</summary>
    public int? AccentColor { get; init; }
}

/// <summary>チェックボックスを持つ Cell の写し。</summary>
internal sealed record KsCheckboxCellSnapshot : KsCellSnapshot
{
    /// <summary>行の右側に表示する値文字列。</summary>
    public string? ValueText { get; init; }

    /// <summary>チェック状態。</summary>
    public bool IsChecked { get; init; }

    /// <summary>強調表示の色 (ARGB)。未指定は既定スタイルを継承。</summary>
    public int? AccentColor { get; init; }
}

/// <summary>行全体のタップでチェックを切り替える Cell の写し。</summary>
internal sealed record KsSimpleCheckCellSnapshot : KsCellSnapshot
{
    /// <summary>行の右側に表示する値文字列。</summary>
    public string? ValueText { get; init; }

    /// <summary>チェック状態。</summary>
    public bool IsChecked { get; init; }

    /// <summary>強調表示の色 (ARGB)。未指定は既定スタイルを継承。</summary>
    public int? AccentColor { get; init; }
}

/// <summary>同一グループ内で 1 つだけ選択される Cell の写し。</summary>
internal sealed record KsRadioCellSnapshot : KsCellSnapshot
{
    /// <summary>行の右側に表示する値文字列。</summary>
    public string? ValueText { get; init; }

    /// <summary>同一選択グループの識別子。</summary>
    public string GroupId { get; init; } = string.Empty;

    /// <summary>この Cell の値。</summary>
    public string Value { get; init; } = string.Empty;

    /// <summary>グループ内の現在選択値。</summary>
    public string SelectedValue { get; init; } = string.Empty;

    /// <summary>強調表示の色 (ARGB)。未指定は既定スタイルを継承。</summary>
    public int? AccentColor { get; init; }
}

/// <summary>テキスト入力欄を持つ Cell の写し。</summary>
/// <remarks>入力 control 自身が値を表示するため、共通の値文字列は持たない。</remarks>
internal sealed record KsEntryCellSnapshot : KsCellSnapshot
{
    /// <summary>現在のテキスト値。</summary>
    public string Text { get; init; } = string.Empty;

    /// <summary>プレースホルダ。</summary>
    public string? Placeholder { get; init; }

    /// <summary>プレースホルダの文字色 (ARGB)。未指定は既定スタイルを継承。</summary>
    public int? PlaceholderColor { get; init; }

    /// <summary>正規化したキーボード種別。</summary>
    public KsKeyboardKind Keyboard { get; init; }

    /// <summary>パスワードマスクを掛けるかどうか。</summary>
    public bool IsPassword { get; init; }

    /// <summary>テキストの揃え位置。未指定は Native 既定 (末尾寄せ)。</summary>
    public TextAlignment? TextAlignment { get; init; }

    /// <summary>最大文字数。未指定は無制限。</summary>
    public int? MaxLength { get; init; }

    /// <summary>強調表示の色 (ARGB)。未指定は既定スタイルを継承。</summary>
    public int? AccentColor { get; init; }
}

/// <summary>一覧から項目を選ぶ Cell の候補 1 件 (主表示 + 任意の副表示) の写し。</summary>
/// <remarks>
/// 表示射影は facade で適用済みであり、Native 側で解き直すことはない。候補 1 件を 1 要素で運ぶため、
/// 主表示と副表示の件数がずれることが構造的に起こらない。
/// </remarks>
/// <param name="Text">主表示テキスト</param>
/// <param name="SubText">副表示テキスト。副表示なしは null (空文字列は生成側で null へ揃える)</param>
internal sealed record KsPickerItemSnapshot(string Text, string? SubText = null);

/// <summary>一覧から項目を選ぶ Cell の写し。</summary>
internal sealed record KsPickerCellSnapshot : KsCellSnapshot
{
    /// <summary>行の右側に表示する値文字列。</summary>
    public string? ValueText { get; init; }

    /// <summary>選択候補の項目 (表示整形済み)。</summary>
    public IReadOnlyList<KsPickerItemSnapshot> Items { get; init; } = [];

    /// <summary>選択モード。</summary>
    public PickerSelectionMode SelectionMode { get; init; }

    /// <summary>単一選択モードの選択 index。未選択は null。</summary>
    public int? SelectedIndex { get; init; }

    /// <summary>複数選択モードの選択 index 群 (昇順・重複なし)。</summary>
    public IReadOnlyList<int> SelectedIndices { get; init; } = [];

    /// <summary>複数選択モードでの選択上限。0 で無制限。</summary>
    public int MaxSelectedNumber { get; init; }

    /// <summary>選択面のタイトル。</summary>
    public string? PageTitle { get; init; }

    /// <summary>強調表示の色 (ARGB)。未指定は既定スタイルを継承。</summary>
    public int? AccentColor { get; init; }
}

/// <summary>数値を選ぶ Cell の写し。</summary>
internal sealed record KsNumberPickerCellSnapshot : KsCellSnapshot
{
    /// <summary>行の右側に表示する値文字列。</summary>
    public string? ValueText { get; init; }

    /// <summary>選択できる最小値。</summary>
    public int Min { get; init; }

    /// <summary>選択できる最大値。</summary>
    public int Max { get; init; }

    /// <summary>選択の刻み幅。</summary>
    public int Step { get; init; }

    /// <summary>現在の値。</summary>
    public int Number { get; init; }

    /// <summary>値に付ける単位文字列。空文字列で単位なし。</summary>
    public string Unit { get; init; } = string.Empty;

    /// <summary>選択面のタイトル。</summary>
    public string? PickerTitle { get; init; }

    /// <summary>強調表示の色 (ARGB)。未指定は既定スタイルを継承。</summary>
    public int? AccentColor { get; init; }
}

/// <summary>時刻を選ぶ Cell の写し。</summary>
internal sealed record KsTimePickerCellSnapshot : KsCellSnapshot
{
    /// <summary>行の右側に表示する値文字列。</summary>
    public string? ValueText { get; init; }

    /// <summary>現在の時刻 ("HH:mm")。</summary>
    public string Time { get; init; } = KsWireValues.Time(default);

    /// <summary>表示フォーマット。未指定は Native 既定。</summary>
    public string? Format { get; init; }

    /// <summary>選択面の時制。true で 24時間制、false で 12時間制 (core/ADR-0028)。</summary>
    public bool Is24Hour { get; init; } = true;

    /// <summary>選択面のタイトル。</summary>
    public string? PickerTitle { get; init; }

    /// <summary>強調表示の色 (ARGB)。未指定は既定スタイルを継承。</summary>
    public int? AccentColor { get; init; }
}

/// <summary>日付を選ぶ Cell の写し。</summary>
internal sealed record KsDatePickerCellSnapshot : KsCellSnapshot
{
    /// <summary>行の右側に表示する値文字列。</summary>
    public string? ValueText { get; init; }

    /// <summary>現在の日付 ("yyyy-MM-dd")。</summary>
    public string Date { get; init; } = KsWireValues.Date(KsWireValues.DefaultDate);

    /// <summary>表示フォーマット。未指定は Native 既定。</summary>
    public string? Format { get; init; }

    /// <summary>選択できる最小日付 ("yyyy-MM-dd")。未指定は制限なし。</summary>
    public string? MinDate { get; init; }

    /// <summary>選択できる最大日付 ("yyyy-MM-dd")。未指定は制限なし。</summary>
    public string? MaxDate { get; init; }

    /// <summary>選択面のタイトル。</summary>
    public string? PickerTitle { get; init; }

    /// <summary>Today ボタンの表示文字列。null または空で非表示。</summary>
    public string? TodayText { get; init; }

    /// <summary>選択面の形式。未指定は Native 既定。</summary>
    public DatePickerUIStyle? UIStyle { get; init; }

    /// <summary>Android の選択面の OK / CANCEL 操作色 (ARGB)。未指定は null。</summary>
    public int? AndroidButtonColor { get; init; }

    /// <summary>強調表示の色 (ARGB)。未指定は既定スタイルを継承。</summary>
    public int? AccentColor { get; init; }
}
