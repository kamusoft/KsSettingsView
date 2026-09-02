using System.Collections.Generic;

namespace KsSettingsView.Internals;

/// <summary>
/// facade から Native Bridge を操作する境界 (maui/ADR-0009)。
/// </summary>
/// <remarks>
/// メソッドは Bridge の公開 API と 1 対 1 に対応し、facade 型から輸送 DTO への変換は実装が担う。
/// Section / Cell の ID は実装 (Bridge) が採番して返し、呼び出し側は返された ID だけを以後の操作へ渡す。
/// 未知の ID を指定した操作は no-op になり、戻り値を持つものは null を返す。
/// 全メソッドを UI スレッドから呼ぶ (Bridge 側の呼び出し規約であり、実装は marshal しない)。
/// Native Host の生成は platform ごとに引数と戻り値が異なるため、この境界には含めず実装型が公開する。
/// gateway 自身の破棄口は持たない — Bridge と内部所有 Store の最終的な解放は、gateway への
/// 参照がなくなった後の GC と binding のファイナライザに委ねる。
/// </remarks>
internal interface IKsSettingsGateway
{
    /// <summary>設定ツリー全体を置き換え、採番された Section / Cell の ID を配置順に返す。</summary>
    /// <param name="sections">新しい設定ツリーの Section 群</param>
    IReadOnlyList<KsSectionIdentity> SetRoot(IReadOnlyList<Section> sections);

    /// <summary>Section を指定位置へ挿入し、採番された ID を返す。</summary>
    /// <param name="section">挿入する Section</param>
    /// <param name="index">挿入位置。範囲外は端へ丸められる</param>
    KsSectionIdentity? InsertSection(Section section, int index);

    /// <summary>指定 ID の Section を削除する。</summary>
    /// <param name="sectionId">対象 Section の ID</param>
    void RemoveSection(string sectionId);

    /// <summary>Section の順序を変更する。</summary>
    /// <param name="from">移動元の位置</param>
    /// <param name="to">移動先の位置。範囲外は端へ丸められる</param>
    void MoveSection(int from, int to);

    /// <summary>指定 ID の Section の内容を置き換える。</summary>
    /// <remarks>
    /// Section の ID は維持される。配下の Cell は既定では新しい ID で作り直されるが、
    /// <paramref name="retainedCellIds"/> を渡した分だけは採番済みの ID を引き継ぐ。
    /// 引き継ぎは同じ Section インスタンスの内容差し替え (可視性の切り替え等) で使い、
    /// 別インスタンスへの置き換えでは渡さない。
    /// </remarks>
    /// <param name="sectionId">対象 Section の ID</param>
    /// <param name="newSection">置き換え後の内容を持つ Section</param>
    /// <param name="retainedCellIds">配下 Cell へ引き継ぐ ID (配置順)。空で全て新規採番</param>
    KsSectionIdentity? ReplaceSection(
        string sectionId,
        Section newSection,
        IReadOnlyList<string> retainedCellIds);

    /// <summary>指定 Section の指定位置へ Cell を挿入し、採番された ID を返す。</summary>
    /// <param name="cell">挿入する Cell</param>
    /// <param name="sectionId">挿入先 Section の ID</param>
    /// <param name="index">挿入位置。範囲外は端へ丸められる</param>
    string? InsertCell(CellBase cell, string sectionId, int index);

    /// <summary>指定 ID の Cell を削除する。</summary>
    /// <param name="cellId">対象 Cell の ID</param>
    void RemoveCell(string cellId);

    /// <summary>指定 ID の Cell を同一 Section 内で移動する。</summary>
    /// <param name="cellId">対象 Cell の ID</param>
    /// <param name="index">移動先の位置。範囲外は端へ丸められる</param>
    void MoveCell(string cellId, int index);

    /// <summary>指定 ID の Cell の内容を置き換え、維持された ID を返す。</summary>
    /// <remarks>可視性を変える更新はこの単発の置き換えで行う (バッチには載せられない)。</remarks>
    /// <param name="cellId">対象 Cell の ID</param>
    /// <param name="newCell">置き換え後の内容を持つ Cell</param>
    string? ReplaceCell(string cellId, CellBase newCell);

    /// <summary>複数 Cell の内容を 1 バッチとして置き換える。</summary>
    /// <param name="updates">対象 ID と置き換え後の内容の並び</param>
    void ReplaceCells(IReadOnlyList<KsCellUpdate> updates);

    /// <summary>Root / Section の header・footer のテキストを更新する。</summary>
    /// <remarks>
    /// 未知の Section ID は Store 側で no-op になり、state 更新も更新通知も発生しない
    /// (core/ADR-0020)。Root 対象は ID を参照せず、従来どおり通知される。
    /// </remarks>
    /// <param name="target">更新対象</param>
    /// <param name="sectionId">Section を対象にするときの ID。Root 対象では参照されない</param>
    /// <param name="text">表示するテキスト。null で解除</param>
    void UpdateAccessory(KsAccessoryTarget target, string? sectionId, string? text);

    /// <summary>Root / Section の header・footer に表示する platform view を更新する。</summary>
    /// <remarks>
    /// 未知の Section ID の扱いは <see cref="UpdateAccessory"/> と同じ。渡した view は取り付け
    /// 直前に既存の親から切り離されるため、同じ実体を続けて指定しても失敗しない。
    /// 値の等価判定に頼らない明示の経路であり、同じ内容を指し直しても握りつぶされない。
    /// </remarks>
    /// <param name="target">更新対象</param>
    /// <param name="sectionId">Section を対象にするときの ID。Root 対象では参照されない</param>
    /// <param name="view">表示する platform view。null で解除</param>
    void UpdateAccessoryView(KsAccessoryTarget target, string? sectionId, object? view);

    /// <summary>表示中の accessory 領域の高さを測り直すよう要求する。</summary>
    /// <remarks>
    /// 一過性の要求であり状態は変化しない。view accessory の中身が自分の必要サイズを変えたときに
    /// 呼ぶ。対象が表示されていないとき、および固定高さの領域では表示が変わらない。
    /// </remarks>
    /// <param name="target">再計測する対象</param>
    /// <param name="sectionId">Section を対象にするときの ID。Root 対象では参照されない</param>
    void InvalidateAccessoryMeasurement(KsAccessoryTarget target, string? sectionId);

    /// <summary>画面全体の既定スタイルを適用する。</summary>
    /// <remarks>
    /// 設定ツリーの構造とは独立した表示状態であり、Section / Cell の ID は変わらない。
    /// 同値のスタイルを再適用しても表示は更新されない。
    /// </remarks>
    /// <param name="theme">適用する既定スタイル</param>
    void SetTheme(KsThemeSnapshot theme);

    /// <summary>見た目スタイルを適用する。</summary>
    /// <remarks>
    /// Native 側でも見た目スタイルは Store ではなく View / Controller のプロパティであり、この
    /// 操作だけは Store 公開操作との 1 対 1 (maui/ADR-0002) の枠外にある (maui/ADR-0023)。
    /// 設定ツリーの構造と Section / Cell の ID は変わらない。
    /// </remarks>
    /// <param name="style">適用する見た目スタイル</param>
    void SetStyle(SettingsViewStyle style);

    /// <summary>Cell の icon として輸送する platform 画像の引き当て先を差し込む。</summary>
    /// <remarks>
    /// 輸送 DTO を組み立てるたびにここから引く。差し込まれていない間は icon なしとして扱う。
    /// </remarks>
    /// <param name="icons">解決済み platform 画像を持つ引き当て先</param>
    void AttachIcons(IKsIconStore icons);

    /// <summary>輸送 DTO へ載せる platform view の引き当て先を差し込む。</summary>
    /// <remarks>
    /// Section / Cell の輸送 DTO を組み立てるたびにここから引く。差し込まれていない間は view なし
    /// として扱い、accessory はテキストの指定だけが載り、Cell の内容は空になる。
    /// </remarks>
    /// <param name="views">実体化済み platform view を持つ引き当て先</param>
    void AttachPlatformViews(IKsPlatformViewStore views);

    /// <summary>Native Host だけを解放し、設定ツリーの状態は維持する。</summary>
    /// <remarks>冪等であり、Host 不在時は何も起こらない。</remarks>
    void ReleaseHost();

    /// <summary>ユーザー操作の通知先を差し込む。</summary>
    /// <remarks>
    /// 実装は通知先を強く保持し、Bridge へ渡す通知チャネルの実体が回収されないようにする。
    /// 冪等であり、同じ通知先で呼び直しても二重に通知されない。
    /// </remarks>
    /// <param name="sink">通知を受け取る受け口</param>
    void AttachInteractions(IKsInteractionSink sink);

    /// <summary>ユーザー操作の通知先を外す。</summary>
    /// <remarks>冪等であり、解除後の操作は通知されない。</remarks>
    void DetachInteractions();
}
