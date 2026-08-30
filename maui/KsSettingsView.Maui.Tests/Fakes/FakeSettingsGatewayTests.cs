using System.Collections.Generic;
using KsSettingsView.Maui.Internals;
using NUnit.Framework;

namespace KsSettingsView.Maui.Tests.Fakes;

/// <summary>
/// テスト用 gateway が Bridge 側の ID 採番と no-op 契約を再現していることを確認する。
/// </summary>
[TestFixture]
public class FakeSettingsGatewayTests
{
    /// <summary>setRoot は Section と配下 Cell の ID を配置順に採番して返す。</summary>
    [Test]
    public void SetRootNumbersSectionAndCellIds()
    {
        FakeSettingsGateway gateway = new();
        Section section = MakeSection("general", "a", "b");

        IReadOnlyList<KsSectionIdentity> identities = gateway.SetRoot([section]);

        Assert.That(identities, Has.Count.EqualTo(1));
        Assert.That(identities[0].CellIds, Has.Count.EqualTo(2));
        Assert.That(identities[0].CellIds[0], Is.Not.EqualTo(identities[0].CellIds[1]));
        Assert.That(gateway.SectionIds, Is.EqualTo(new[] { identities[0].SectionId }));
        Assert.That(gateway.CellIdsOf(identities[0].SectionId), Is.EqualTo(identities[0].CellIds));
    }

    /// <summary>setRoot は呼び出しごとに新しい ID を採番する。</summary>
    [Test]
    public void SetRootReplacesPreviousIdentities()
    {
        FakeSettingsGateway gateway = new();
        string first = gateway.SetRoot([MakeSection("general", "a")])[0].SectionId;

        string second = gateway.SetRoot([MakeSection("general", "a")])[0].SectionId;

        Assert.That(second, Is.Not.EqualTo(first));
        Assert.That(gateway.SectionIds, Is.EqualTo(new[] { second }));
    }

    /// <summary>範囲外の挿入位置は端へ丸められる。</summary>
    [Test]
    public void InsertSectionClampsIndex()
    {
        FakeSettingsGateway gateway = new();
        string existing = gateway.SetRoot([MakeSection("general")])[0].SectionId;

        KsSectionIdentity? inserted = gateway.InsertSection(MakeSection("added"), 99);

        Assert.That(inserted, Is.Not.Null);
        Assert.That(gateway.SectionIds, Is.EqualTo(new[] { existing, inserted!.SectionId }));
    }

    /// <summary>未知の Section を対象にした置き換えは no-op になり null を返す。</summary>
    [Test]
    public void ReplaceSectionReturnsNullForUnknownId()
    {
        FakeSettingsGateway gateway = new();
        gateway.SetRoot([MakeSection("general")]);

        Assert.That(gateway.ReplaceSection("unknown", MakeSection("other"), []), Is.Null);
    }

    /// <summary>Section の置き換えでは Section の ID が維持され、Cell は採番し直される。</summary>
    [Test]
    public void ReplaceSectionKeepsSectionIdAndRenumbersCells()
    {
        FakeSettingsGateway gateway = new();
        KsSectionIdentity original = gateway.SetRoot([MakeSection("general", "a")])[0];

        KsSectionIdentity? replaced = gateway.ReplaceSection(
            original.SectionId,
            MakeSection("general", "a", "b"),
            []);

        Assert.That(replaced, Is.Not.Null);
        Assert.That(replaced!.SectionId, Is.EqualTo(original.SectionId));
        Assert.That(replaced.CellIds, Has.Count.EqualTo(2));
        Assert.That(replaced.CellIds, Has.No.Member(original.CellIds[0]));
    }

    /// <summary>未知の Section への Cell 挿入は no-op になり null を返す。</summary>
    [Test]
    public void InsertCellReturnsNullForUnknownSection()
    {
        FakeSettingsGateway gateway = new();

        Assert.That(gateway.InsertCell(new LabelCell(), "unknown", 0), Is.Null);
    }

    /// <summary>未知の Cell を対象にした置き換えは no-op になり null を返す。</summary>
    [Test]
    public void ReplaceCellReturnsNullForUnknownId()
    {
        FakeSettingsGateway gateway = new();
        gateway.SetRoot([MakeSection("general", "a")]);

        Assert.That(gateway.ReplaceCell("unknown", new LabelCell()), Is.Null);
    }

    /// <summary>Cell の移動と削除は同一 Section 内の並びへ反映される。</summary>
    [Test]
    public void MoveAndRemoveCellUpdateOrder()
    {
        FakeSettingsGateway gateway = new();
        KsSectionIdentity identity = gateway.SetRoot([MakeSection("general", "a", "b", "c")])[0];

        gateway.MoveCell(identity.CellIds[2], 0);
        gateway.RemoveCell(identity.CellIds[1]);

        Assert.That(
            gateway.CellIdsOf(identity.SectionId),
            Is.EqualTo(new[] { identity.CellIds[2], identity.CellIds[0] }));
    }

    /// <summary>accessory の更新は ID 検査の対象外で、削除済みの ID でもそのまま記録される。</summary>
    [Test]
    public void UpdateAccessoryIsRecordedEvenForUnknownSection()
    {
        FakeSettingsGateway gateway = new();

        gateway.UpdateAccessory(KsAccessoryTarget.SectionHeader, "unknown", "text");

        Assert.That(
            gateway.Calls,
            Has.Exactly(1).EqualTo(
                new GatewayCall.UpdateAccessory(KsAccessoryTarget.SectionHeader, "unknown", "text")));
    }

    /// <summary>Host の解放が回数で観測できる。</summary>
    [Test]
    public void ReleaseHostIsObservable()
    {
        FakeSettingsGateway gateway = new();

        gateway.ReleaseHost();
        gateway.ReleaseHost();

        Assert.That(gateway.ReleaseHostCount, Is.EqualTo(2));
        Assert.That(gateway.Calls, Has.Count.EqualTo(2));
    }

    /// <summary>呼び出しは順番に記録され、破棄できる。</summary>
    [Test]
    public void CallsAreRecordedInOrderAndCanBeCleared()
    {
        FakeSettingsGateway gateway = new();
        KsSectionIdentity identity = gateway.SetRoot([MakeSection("general", "a")])[0];

        gateway.RemoveCell(identity.CellIds[0]);

        Assert.That(gateway.Calls, Has.Count.EqualTo(2));
        Assert.That(gateway.Calls[0], Is.InstanceOf<GatewayCall.SetRoot>());
        Assert.That(gateway.Calls[1], Is.InstanceOf<GatewayCall.RemoveCell>());

        gateway.ClearCalls();

        Assert.That(gateway.Calls, Is.Empty);
    }

    /// <summary>header テキストと Cell のタイトルを指定した Section を組み立てる。</summary>
    private static Section MakeSection(string headerText, params string[] cellTitles)
    {
        Section section = new() { HeaderText = headerText };
        foreach (string title in cellTitles)
        {
            section.Cells.Add(new LabelCell { Title = title });
        }

        return section;
    }
}
