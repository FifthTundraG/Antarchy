package com.craisinlord.antarchy.content.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

public class DorrieMenu extends AbstractContainerMenu {
    // Set by the Fabric platform during registration so common entity code can open the menu.
    @SuppressWarnings("unchecked")
    public static Supplier<MenuType<DorrieMenu>> TYPE_SUPPLIER = () -> {
        throw new IllegalStateException("DorrieMenu.TYPE_SUPPLIER accessed before registration");
    };

    private static final int EQUIPMENT_SLOTS = 1;
    private static final int PLAYER_INV_ROWS = 3;
    private static final int PLAYER_INV_COLS = 9;

    // GUI layout constants (relative to background image top-left)
    public static final int SADDLE_SLOT_X = 80;
    public static final int SADDLE_SLOT_Y = 18;
    public static final int PLAYER_INV_X = 8;
    public static final int PLAYER_INV_Y = 48;
    public static final int HOTBAR_Y = 106;

    private final Container equipment;

    // Server-side constructor — receives the real Dorrie inventory.
    public DorrieMenu(int syncId, Inventory playerInventory, Container equipment) {
        super(TYPE_SUPPLIER.get(), syncId);
        this.equipment = equipment;
        checkContainerSize(equipment, EQUIPMENT_SLOTS);

        // Slot 0: saddle
        this.addSlot(new Slot(equipment, 0, SADDLE_SLOT_X, SADDLE_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.SADDLE);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // Player inventory (3 × 9)
        for (int row = 0; row < PLAYER_INV_ROWS; row++) {
            for (int col = 0; col < PLAYER_INV_COLS; col++) {
                this.addSlot(new Slot(
                        playerInventory,
                        col + row * PLAYER_INV_COLS + PLAYER_INV_COLS,
                        PLAYER_INV_X + col * 18,
                        PLAYER_INV_Y + row * 18
                ));
            }
        }

        // Hotbar (9)
        for (int col = 0; col < PLAYER_INV_COLS; col++) {
            this.addSlot(new Slot(playerInventory, col, PLAYER_INV_X + col * 18, HOTBAR_Y));
        }
    }

    // Client-side factory — syncs slot data from server.
    public static DorrieMenu clientCreate(int syncId, Inventory playerInventory) {
        return new DorrieMenu(syncId, playerInventory, new SimpleContainer(EQUIPMENT_SLOTS));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.equipment.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        int equipEnd = EQUIPMENT_SLOTS;
        int invEnd = equipEnd + PLAYER_INV_ROWS * PLAYER_INV_COLS;
        int totalSlots = invEnd + PLAYER_INV_COLS;

        if (index < equipEnd) {
            // Equipment slot → player inventory
            if (!this.moveItemStackTo(stack, equipEnd, totalSlots, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player inventory/hotbar → try equipment slot first
            if (stack.is(Items.SADDLE) && this.slots.get(0).mayPlace(stack)) {
                if (!this.moveItemStackTo(stack, 0, equipEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < invEnd) {
                // Inventory → hotbar
                if (!this.moveItemStackTo(stack, invEnd, totalSlots, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Hotbar → inventory
                if (!this.moveItemStackTo(stack, equipEnd, invEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return result;
    }
}
