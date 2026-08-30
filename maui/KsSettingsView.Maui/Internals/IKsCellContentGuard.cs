using Microsoft.Maui.Controls;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// CustomCell の内容に置く View の可否を、置き場所を知っている側へ尋ねる口。
/// </summary>
/// <remarks>
/// 内容のプロパティは、値を確定させる前にここへ尋ねる。プロパティが値を確定させた後では、
/// それまでの内容の論理上の所有が先に解かれてしまい、後から多重配置を見つけて例外にしても
/// 元へ戻せないため。設定ツリーに載っていない Cell には尋ねる相手がいないので、その間の
/// 多重配置は設定ツリーへ入る時点まで持ち越される。
/// 検査と失敗時の状態保全の規律は maui/ADR-0022。
/// </remarks>
internal interface IKsCellContentGuard
{
    /// <summary>この Cell の内容として置けない View なら例外を送出する。</summary>
    /// <param name="cell">これから置く Cell</param>
    /// <param name="content">置こうとしている View。null なら何もしない</param>
    void EnsureContentCanBePlaced(CustomCell cell, View? content);
}
