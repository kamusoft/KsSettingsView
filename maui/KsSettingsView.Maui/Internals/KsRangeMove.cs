using System.Collections.Generic;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// 連続した範囲の移動を、1 件ずつの移動の並びへ分解する小道具。
/// </summary>
/// <remarks>
/// 表示側へ運べる移動操作は 1 件単位のため、複数件をまとめた移動は分解して送る。
/// 単純に 1 件ずつ同じ移動先へ送ると範囲内の並びが反転するため、方向ごとに送り方を変える。
/// </remarks>
internal static class KsRangeMove
{
    /// <summary>
    /// <paramref name="from"/> から始まる <paramref name="count"/> 件を
    /// <paramref name="to"/> へ移す手順を、元の並びを保つ順序で返す。
    /// </summary>
    /// <remarks>
    /// 手順の各要素は「移動元の位置」と「移動先の位置」の組であり、1 件ずつ順に適用する。
    /// 移動先の位置は、移動対象を取り除いた後の並びにおける挿入位置として解釈する。
    /// i 番目の手順が動かすのは、移動前の範囲の i 番目の要素になる。
    /// </remarks>
    /// <param name="from">移動元の先頭位置</param>
    /// <param name="to">移動先の先頭位置</param>
    /// <param name="count">移動する件数</param>
    public static IEnumerable<(int Source, int Destination)> Steps(int from, int to, int count)
    {
        if (from < 0 || to < 0 || from == to || count <= 0)
        {
            yield break;
        }

        // 後方へずらす向きでは、先頭から順に 1 つずつ後ろへ送れば並びが保たれる。
        // 前方へ送る向きでは、先頭を範囲の末尾に当たる位置へ送り続けると並びが保たれる。
        bool forward = to > from;
        for (int i = 0; i < count; i++)
        {
            yield return forward ? (from, to + count - 1) : (from + i, to + i);
        }
    }
}
