package com.craisinlord.antarchy.content.block.entity;

import com.craisinlord.antarchy.content.AntarchyObjects;
import com.craisinlord.antarchy.content.block.SeashellBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class SeashellBlockEntity extends RandomizableContainerBlockEntity implements GeoBlockEntity {
    private static final String MAIN_CONTROLLER = "main_controller";
    private static final String TRANSITION_CONTROLLER = "transition_controller";
    private static final String OPEN_TRIGGER = "close_to_open";
    private static final String CLOSE_TRIGGER = "open_to_close";
    private static final RawAnimation OPEN_ANIM = RawAnimation.begin().thenPlayAndHold("open");
    private static final RawAnimation CLOSED_ANIM = RawAnimation.begin().thenPlayAndHold("closed");
    private static final Component TITLE = Component.translatable("block.antarchy.seashell");

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
    private boolean visualOpen;
    private boolean initializedVisualState;

    public SeashellBlockEntity(BlockPos pos, BlockState blockState) {
        super(AntarchyObjects.SEASHELL_BLOCK_ENTITY.get(), pos, blockState);
        this.visualOpen = blockState.getValue(SeashellBlock.POWERED);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SeashellBlockEntity seashell) {
        seashell.syncVisualState(state.getValue(SeashellBlock.POWERED));
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, SeashellBlockEntity seashell) {
        seashell.syncVisualState(state.getValue(SeashellBlock.POWERED));
    }

    public boolean canAcceptItem(@Nullable Player player, ItemStack stack) {
        this.unpackLootTable(player);
        return !stack.isEmpty() && this.getFirstEmptySlot() >= 0;
    }

    public boolean tryInsert(@Nullable Player player, InteractionHand hand, ItemStack heldStack) {
        this.unpackLootTable(player);
        int slot = this.getFirstEmptySlot();
        if (slot < 0 || heldStack.isEmpty()) {
            return false;
        }

        ItemStack inserted = heldStack.copyWithCount(1);
        this.items.set(slot, inserted);
        if (player == null || !player.getAbilities().instabuild) {
            heldStack.shrink(1);
        }

        this.setChangedAndSync();
        return true;
    }

    public boolean tryRemove(Player player, InteractionHand hand) {
        this.unpackLootTable(player);
        int slot = this.getLastFilledSlot();
        if (slot < 0) {
            return false;
        }

        ItemStack removed = this.items.get(slot);
        this.items.set(slot, ItemStack.EMPTY);
        if (player.getItemInHand(hand).isEmpty()) {
            player.setItemInHand(hand, removed);
        } else if (!player.addItem(removed)) {
            player.drop(removed, false);
        }

        this.setChangedAndSync();
        return true;
    }

    public boolean hasAnyContents() {
        return this.items.stream().anyMatch(stack -> !stack.isEmpty());
    }

    public void dropContents(Level level, BlockPos pos) {
        this.unpackLootTable(null);
        Containers.dropContents(level, pos, this.items);
        this.items = NonNullList.withSize(9, ItemStack.EMPTY);
        this.setChanged();
    }

    public void onPowerStateChanged(boolean open) {
        this.syncVisualState(open);
        this.triggerAnim(TRANSITION_CONTROLLER, open ? OPEN_TRIGGER : CLOSE_TRIGGER);
        this.setChangedAndSync();
    }

    public void syncVisualState(boolean open) {
        if (!this.initializedVisualState) {
            this.visualOpen = open;
            this.initializedVisualState = true;
            return;
        }

        this.visualOpen = open;
    }

    public boolean isVisualOpen() {
        return this.visualOpen;
    }

    public List<DisplayedStack> getDisplayedStacks() {
        List<ItemStack> inserted = new ArrayList<>();
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                inserted.add(stack);
            }
        }

        List<DisplayedStack> displayed = new ArrayList<>(inserted.size());
        for (int i = 0; i < inserted.size(); i++) {
            displayed.add(new DisplayedStack(inserted.get(i), displayOffsetFor(i)));
        }
        return displayed;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, MAIN_CONTROLLER, state ->
                state.setAndContinue(this.visualOpen ? OPEN_ANIM : CLOSED_ANIM)));
        controllers.add(new AnimationController<>(this, TRANSITION_CONTROLLER, state -> PlayState.STOP)
                .triggerableAnim(OPEN_TRIGGER, RawAnimation.begin().thenPlay(OPEN_TRIGGER))
                .triggerableAnim(CLOSE_TRIGGER, RawAnimation.begin().thenPlay(CLOSE_TRIGGER)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }

    @Override
    protected Component getDefaultName() {
        return TITLE;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.threeRows(containerId, inventory, this);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return !stack.isEmpty() && this.items.get(slot).isEmpty();
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("VisualOpen", this.visualOpen);
        tag.putBoolean("VisualInitialized", this.initializedVisualState);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.visualOpen = tag.getBoolean("VisualOpen");
        this.initializedVisualState = tag.getBoolean("VisualInitialized");
    }

    private void setChangedAndSync() {
        this.setChanged();
        if (this.level != null) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private int getFirstEmptySlot() {
        for (int i = 0; i < this.items.size(); i++) {
            if (this.items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int getLastFilledSlot() {
        for (int i = this.items.size() - 1; i >= 0; i--) {
            if (!this.items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private static Vec3 displayOffsetFor(int insertionIndex) {
        return switch (insertionIndex) {
            case 0 -> offset(1, 0);
            case 1 -> offset(0, 0);
            case 2 -> offset(2, 0);
            case 3 -> offset(1, 1);
            case 4 -> offset(0, 1);
            case 5 -> offset(2, 1);
            case 6 -> offset(1, 2);
            case 7 -> offset(0, 2);
            case 8 -> offset(2, 2);
            default -> Vec3.ZERO;
        };
    }

    private static Vec3 offset(int column, int row) {
        double x = -0.22D + column * 0.22D;
        double z = -0.22D + row * 0.22D;
        return new Vec3(x, 0.155D, z);
    }

    public record DisplayedStack(ItemStack stack, Vec3 offset) {
    }
}
