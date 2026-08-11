package com.r3ct.base_core.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class MailMessage {
    public static final MailMessage EMPTY = new MailMessage("", "", NonNullList.withSize(3, ItemStack.EMPTY));

    private final String senderName;
    private final String message;
    private final NonNullList<ItemStack> attachedItems;

    public static final Codec<MailMessage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("SenderName", "Unknown").forGetter(MailMessage::getSenderName),
            Codec.STRING.optionalFieldOf("Message", "").forGetter(MailMessage::getMessage),
            ItemStack.OPTIONAL_CODEC.listOf().xmap(
                    list -> {
                        NonNullList<ItemStack> nonNullList = NonNullList.withSize(3, ItemStack.EMPTY);
                        for (int i = 0; i < Math.min(list.size(), 3); i++) {
                            nonNullList.set(i, list.get(i));
                        }
                        return nonNullList;
                    },
                    nonNullList -> (List<ItemStack>) nonNullList
            ).fieldOf("AttachedItems").forGetter(MailMessage::getAttachedItems)
    ).apply(instance, MailMessage::new));

    public MailMessage(String senderName, String message, NonNullList<ItemStack> attachedItems) {
        this.senderName = senderName != null ? senderName : "Unknown";
        this.message = message != null ? message : "";

        this.attachedItems = NonNullList.withSize(3, ItemStack.EMPTY);
        if (attachedItems != null) {
            for (int i = 0; i < Math.min(attachedItems.size(), 3); i++) {
                this.attachedItems.set(i, attachedItems.get(i));
            }
        }
    }

    public String getSenderName() { return senderName; }
    public String getMessage() { return message; }
    public NonNullList<ItemStack> getAttachedItems() { return attachedItems; }

    public boolean isEmpty() {
        if (!this.message.isEmpty()) return false;
        for (ItemStack stack : this.attachedItems) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }
}