using Microsoft.Maui.Controls;

namespace KsSettingsView.Internals;

/// <summary>
/// Section の header / footer に置く View の可否を、置き場所を知っている側へ尋ねる口。
/// </summary>
/// <remarks>
/// accessory のプロパティは、値を確定させる前にここへ尋ねる。プロパティが値を確定させた後では、
/// それまで置かれていた View の論理上の所有が先に解かれてしまい、後から多重配置を見つけて例外に
/// しても元へ戻せないため。設定ツリーに載っていない Section には尋ねる相手がいないので、その間の
/// 多重配置は設定ツリーへ入る時点まで持ち越される。
/// root の header / footer は SettingsView が変換経路を直に持っているため、この口を介さない。
/// 検査と失敗時の状態保全の規律は maui/ADR-0022。
/// </remarks>
internal interface IKsAccessoryViewGuard
{
    /// <summary>この Section のこの位置に置けない View なら例外を送出する。</summary>
    /// <param name="section">これから置く Section</param>
    /// <param name="target">これから置く位置</param>
    /// <param name="view">置こうとしている View。null なら何もしない</param>
    void EnsureAccessoryViewCanBePlaced(Section section, KsAccessoryTarget target, View? view);
}
