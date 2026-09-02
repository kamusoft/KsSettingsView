namespace KsSettingsView.Internals;

/// <summary>
/// Cell の内容を interop 境界へ運ぶために写し取った値の共通基底。
/// </summary>
/// <remarks>
/// platform 非依存の中間表現であり、gateway の実装がこれを platform の輸送 DTO へ変換する。
/// Cell 種ごとに派生を持ち (maui/ADR-0011)、gateway は派生の型で対応する DTO を選ぶ。
/// 値は輸送できる表現へ正規化済み (壁時計値は固定書式の文字列、選択 index は昇順・重複なし) で、
/// 変換先を問わず同じ結果になる。
/// </remarks>
internal abstract record KsCellSnapshot
{
    /// <summary>行の主タイトル。</summary>
    public string Title { get; init; } = string.Empty;

    /// <summary>タイトルの下に表示する説明文。</summary>
    public string? Description { get; init; }

    /// <summary>補足として表示するヒントテキスト。</summary>
    public string? HintText { get; init; }

    /// <summary>行が有効かどうか。</summary>
    public bool IsEnabled { get; init; }

    /// <summary>行を表示するかどうか。</summary>
    public bool IsVisible { get; init; }

    /// <summary>この行だけのスタイル上書き。全項目が未指定のときは null (Theme をそのまま継承)。</summary>
    public KsCellStyleSnapshot? Style { get; init; }
}
