package cally72jhb.addon.system.modules.render.notexture;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.utils.IScreenFactory;
import meteordevelopment.meteorclient.settings.BlockDataSetting;
import meteordevelopment.meteorclient.settings.IBlockData;
import meteordevelopment.meteorclient.utils.misc.IChangeable;
import meteordevelopment.meteorclient.utils.misc.ICopyable;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;

public class BlockData implements ICopyable<BlockData>, ISerializable<BlockData>, IChangeable, IBlockData<BlockData>, IScreenFactory {
    public SettingColor sideColor;
    public boolean automatic;

    private boolean changed;

    public BlockData(SettingColor sideColor, boolean automatic) {
        this.sideColor = sideColor;
        this.automatic = automatic;
    }

    @Override
    public WidgetScreen createScreen(GuiTheme theme, Block block, BlockDataSetting<BlockData> setting) {
        return new BlockDataScreen(theme, this, block, setting);
    }

    @Override
    public WidgetScreen createScreen(GuiTheme theme) {
        return new BlockDataScreen(theme, this, null, null);
    }

    @Override
    public boolean isChanged() {
        return changed;
    }

    public void changed() {
        changed = true;
    }

    @Override
    public BlockData set(BlockData value) {
        sideColor.set(value.sideColor);
        automatic = value.automatic;

        changed = value.changed;

        return this;
    }

    @Override
    public BlockData copy() {
        return new BlockData(new SettingColor(sideColor), automatic);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.put("sideColor", sideColor.toTag());
        tag.putBoolean("automatic", automatic);

        tag.putBoolean("changed", changed);

        return tag;
    }

    @Override
    public BlockData fromTag(NbtCompound tag) {
        sideColor.fromTag(tag.getCompound("sideColor"));
        automatic = tag.getBoolean("automatic");

        changed = tag.getBoolean("changed");

        return this;
    }
}
