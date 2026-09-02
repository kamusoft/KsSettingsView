using KsSettingsView.Internals;
using Microsoft.Maui.Primitives;
using NUnit.Framework;

namespace KsSettingsView.Tests;

/// <summary>
/// 割り当て領域を満たすビューの大きさが、制約と大きさの指定だけから決まることを確認する。
/// </summary>
/// <remarks>
/// 大きさの指定が無い <see cref="Dimension.Unset"/> と、上限が無い
/// <see cref="Dimension.Maximum"/> を未指定の表現として使う。
/// </remarks>
[TestFixture]
public class FillMeasureTests
{
    private const double Unset = Dimension.Unset;
    private const double NoMaximum = Dimension.Maximum;

    /// <summary>両方向の制約が定まっていれば、制約だけで大きさを決められる。</summary>
    [Test]
    public void FiniteConstraintsCanBeResolved()
    {
        Assert.That(KsFillMeasure.CanResolve(320d, 480d), Is.True);
        Assert.That(KsFillMeasure.CanResolve(0d, 0d), Is.True);
    }

    /// <summary>どちらかの方向の制約が定まっていなければ、制約だけでは決められない。</summary>
    [Test]
    public void NonFiniteConstraintCannotBeResolved()
    {
        Assert.That(KsFillMeasure.CanResolve(320d, double.PositiveInfinity), Is.False);
        Assert.That(KsFillMeasure.CanResolve(double.PositiveInfinity, 480d), Is.False);
        Assert.That(KsFillMeasure.CanResolve(double.PositiveInfinity, double.PositiveInfinity), Is.False);
        Assert.That(KsFillMeasure.CanResolve(double.NaN, 480d), Is.False);
        Assert.That(KsFillMeasure.CanResolve(320d, double.NaN), Is.False);
    }

    /// <summary>指定が無ければ、制約をそのまま満たす。</summary>
    [Test]
    public void UnspecifiedSizeFillsTheConstraint()
    {
        Assert.That(KsFillMeasure.ResolveLength(320d, Unset, Unset, NoMaximum), Is.EqualTo(320d));
    }

    /// <summary>明示指定があれば、制約より大きくても小さくても指定どおりになる。</summary>
    [Test]
    public void ExplicitSizeWinsOverTheConstraint()
    {
        Assert.That(KsFillMeasure.ResolveLength(320d, 100d, Unset, NoMaximum), Is.EqualTo(100d));
        Assert.That(KsFillMeasure.ResolveLength(320d, 500d, Unset, NoMaximum), Is.EqualTo(500d));
    }

    /// <summary>明示指定は最小の指定を下回らない。</summary>
    [Test]
    public void ExplicitSizeIsRaisedToTheMinimum()
    {
        Assert.That(KsFillMeasure.ResolveLength(320d, 100d, 150d, NoMaximum), Is.EqualTo(150d));
        Assert.That(KsFillMeasure.ResolveLength(320d, 100d, 50d, NoMaximum), Is.EqualTo(100d));
    }

    /// <summary>明示指定は最大の指定を上回らず、クランプは最小より後に効く。</summary>
    [Test]
    public void ExplicitSizeIsCappedToTheMaximumAfterTheMinimum()
    {
        Assert.That(KsFillMeasure.ResolveLength(320d, 500d, Unset, 200d), Is.EqualTo(200d));
        Assert.That(KsFillMeasure.ResolveLength(320d, 100d, 150d, 120d), Is.EqualTo(120d));
    }

    /// <summary>明示指定が無い場合、最大の指定は制約より小さいときだけ効く。</summary>
    [Test]
    public void MaximumCapsOnlyWhenItIsSmallerThanTheConstraint()
    {
        Assert.That(KsFillMeasure.ResolveLength(320d, Unset, Unset, 200d), Is.EqualTo(200d));
        Assert.That(KsFillMeasure.ResolveLength(320d, Unset, Unset, 400d), Is.EqualTo(320d));
    }

    /// <summary>明示指定が無い場合、最小の指定だけでは大きさは変わらない。</summary>
    [Test]
    public void MinimumAloneDoesNotChangeTheLength()
    {
        Assert.That(KsFillMeasure.ResolveLength(320d, Unset, 500d, NoMaximum), Is.EqualTo(320d));
    }

    /// <summary>制約が定まっていない方向は、満たすべき領域が無いため大きさを持たない。</summary>
    [Test]
    public void NonFiniteConstraintHasNoLength()
    {
        Assert.That(KsFillMeasure.ResolveLength(double.PositiveInfinity, Unset, Unset, NoMaximum), Is.EqualTo(0d));
        Assert.That(KsFillMeasure.ResolveLength(double.NaN, Unset, Unset, NoMaximum), Is.EqualTo(0d));
    }

    /// <summary>制約が定まっていなくても、明示指定と最大の指定はそのまま効く。</summary>
    [Test]
    public void SpecifiedSizeStillAppliesWithoutAConstraint()
    {
        Assert.That(
            KsFillMeasure.ResolveLength(double.PositiveInfinity, 100d, Unset, NoMaximum),
            Is.EqualTo(100d));
        Assert.That(
            KsFillMeasure.ResolveLength(double.PositiveInfinity, Unset, Unset, 200d),
            Is.EqualTo(200d));
    }

    /// <summary>負の制約は大きさとして扱わず 0 にする。</summary>
    [Test]
    public void NegativeConstraintHasNoLength()
    {
        Assert.That(KsFillMeasure.ResolveLength(-10d, Unset, Unset, NoMaximum), Is.EqualTo(0d));
    }
}
