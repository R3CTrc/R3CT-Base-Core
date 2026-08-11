package com.r3ct.base_core.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public class MailMessage {
    // Zaktualizowany obiekt EMPTY posiadający NIL_UUID
    public static final MailMessage EMPTY = new MailMessage("", Util.NIL_UUID, "", NonNullList.withSize(3, ItemStack.EMPTY), 0L);

    private final String senderName;
    private final UUID senderId;
    private final String message;
    private final NonNullList<ItemStack> attachedItems;
    private final long timestamp;

    public static final Codec<MailMessage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("SenderName", "Unknown").forGetter(MailMessage::getSenderName),
            UUIDUtil.CODEC.optionalFieldOf("SenderId", Util.NIL_UUID).forGetter(MailMessage::getSenderId),
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
            ).fieldOf("AttachedItems").forGetter(MailMessage::getAttachedItems),
            Codec.LONG.optionalFieldOf("Timestamp", 0L).forGetter(MailMessage::getTimestamp)
    ).apply(instance, MailMessage::new));

    // Główny konstruktor
    public MailMessage(String senderName, UUID senderId, String message, NonNullList<ItemStack> attachedItems, long timestamp) {
        this.senderName = senderName != null ? senderName : "Unknown";
        this.senderId = senderId != null ? senderId : Util.NIL_UUID;
        this.message = message != null ? message : "";
        this.timestamp = timestamp;

        this.attachedItems = NonNullList.withSize(3, ItemStack.EMPTY);
        if (attachedItems != null) {
            for (int i = 0; i < Math.min(attachedItems.size(), 3); i++) {
                this.attachedItems.set(i, attachedItems.get(i));
            }
        }
    }

    // Konstruktor wstecznie kompatybilny (gdyby coś ze starych zapisów potrzebowało go wywołać)
    public MailMessage(String senderName, String message, NonNullList<ItemStack> attachedItems) {
        this(senderName, Util.NIL_UUID, message, attachedItems, 0L);
    }

    public String getSenderName() { return senderName; }
    public UUID getSenderId() { return senderId; }
    public String getMessage() { return message; }
    public NonNullList<ItemStack> getAttachedItems() { return attachedItems; }
    public long getTimestamp() { return timestamp; }

    public boolean isEmpty() {
        if (!this.message.isEmpty()) return false;
        for (ItemStack stack : this.attachedItems) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }
}