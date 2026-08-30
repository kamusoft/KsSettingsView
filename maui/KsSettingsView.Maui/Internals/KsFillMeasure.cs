using System;
using Microsoft.Maui.Primitives;

namespace KsSettingsView.Maui.Internals;

/// <summary>
/// 割り当て領域を満たすビューの大きさを、親から与えられた制約だけから決める計算。
/// </summary>
/// <remarks>
/// SettingsView は与えられた領域を満たす一覧であり、内容ぶんの大きさを持たない。そのため
/// 制約が定まってさえいれば、Native Host へ問い合わせずに大きさを決められる。
/// 明示指定・最小・最大の優先順位は、MAUI が platform への measure 制約を組み立てるときの
/// 規則に合わせてあり、指定がある場合の結果は問い合わせた場合と一致する。
/// 設計判断: maui/ADR-0014。
/// </remarks>
internal static class KsFillMeasure
{
    /// <summary>制約だけで大きさを決められるかどうかを返す。</summary>
    /// <remarks>
    /// どちらかの方向の制約が定まっていない場合、親は内容ぶんの大きさを尋ねている。
    /// それに答えられるのは中身を知っている Native Host だけなので、この計算では引き受けない。
    /// </remarks>
    /// <param name="widthConstraint">横方向の制約</param>
    /// <param name="heightConstraint">縦方向の制約</param>
    /// <returns>両方向の制約が定まっていれば true</returns>
    public static bool CanResolve(double widthConstraint, double heightConstraint)
        => double.IsFinite(widthConstraint) && double.IsFinite(heightConstraint);

    /// <summary>1 方向の大きさを、制約と大きさの指定から決める。</summary>
    /// <remarks>
    /// 明示指定があればそれを最小・最大でクランプした値、無ければ制約そのもの (最大の指定が
    /// 制約より小さければ最大) を満たすべき大きさとする。制約が定まっていない方向は満たすべき
    /// 領域が決まらないため、大きさを持たない。
    /// 呼び出し側は <see cref="CanResolve"/> が true の制約に対して使う前提であり、
    /// 非有限の制約に対する 0 は「無限制約では 0 とする」仕様ではなく防御の既定値。
    /// </remarks>
    /// <param name="constraint">親から与えられた制約</param>
    /// <param name="explicitSize">明示指定された大きさ (未指定なら NaN)</param>
    /// <param name="minimumSize">最小の指定 (未指定なら NaN)</param>
    /// <param name="maximumSize">最大の指定 (未指定なら正の無限大)</param>
    /// <returns>決まった大きさ</returns>
    public static double ResolveLength(
        double constraint,
        double explicitSize,
        double minimumSize,
        double maximumSize)
    {
        if (Dimension.IsExplicitSet(explicitSize))
        {
            double length = Math.Max(explicitSize, Dimension.ResolveMinimum(minimumSize));
            return Dimension.IsMaximumSet(maximumSize) ? Math.Min(length, maximumSize) : length;
        }

        if (Dimension.IsMaximumSet(maximumSize) && maximumSize < constraint)
        {
            return maximumSize;
        }

        return double.IsFinite(constraint) ? Math.Max(0d, constraint) : 0d;
    }
}
