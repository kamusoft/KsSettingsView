namespace KsSettingsView;

/// <summary>
/// スクロール命令で、対象を表示範囲のどこへ合わせるかを表す位置。
/// </summary>
/// <remarks>
/// 表示範囲は、一覧の内容の余白を除いた、実際に見えている領域。
/// </remarks>
public enum ScrollPosition
{
    /// <summary>対象の上端を表示範囲の上端へ合わせる。</summary>
    Start,

    /// <summary>対象の中央を表示範囲の中央へ合わせる。</summary>
    Center,

    /// <summary>対象の下端を表示範囲の下端へ合わせる。</summary>
    End,
}
