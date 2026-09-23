package com.dexer.aquanaut.common.entity;

import javax.annotation.Nullable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * Everything the mod places in the water that a player can label: the cursors that pin a location,
 * and the submarine drones.
 *
 * <p>
 * What they have in common is not their physics — a cursor is a buoy that yields to a nudge and a
 * drone is a hull that drives itself — but their <em>identity</em>. Each one wears a tag: a name and
 * a colour, server-authoritative, synced to every client, unique across the whole save, and drawn
 * above the entity as a billboard. Anything that reads or writes that identity lives here, so the
 * two kinds cannot drift apart and a third kind would need to bring only its own body.
 *
 * <h3>One tag namespace</h3>
 * Uniqueness is deliberately global rather than per-kind. A tag exists to be unambiguous when the
 * compass or a notebook entry names it, so a cursor and a drone compete for the same names and
 * {@link #firstFreeName} skips whatever is already worn by either.
 *
 * <h3>The tag is the whole entity state</h3>
 * Name and colour are the only synced data here, and they are the only thing persisted beyond the
 * hitbox. Subclasses keep their own state — the drone's pilot, for instance — but a taggable that
 * carried nothing else would still round-trip through a save correctly.
 *
 * <h3>Breaking one returns its item, wearing its tag</h3>
 * A taggable is equipment, not wildlife: any hit that lands on it breaks it up into the item that
 * places it again. Subclasses say which item that is through {@link #taggedItem()} and what it sounds
 * like through {@link #playBrokenSound()}.
 *
 * <p>
 * That item carries the tag it came from, so picking a marker up and putting it down again — anywhere,
 * any time later — brings the same name and colour back. It is the tag that makes a marker worth
 * keeping across the trip, and an item stack is the only thing that survives the swim: the marker
 * itself is gone the moment it is struck. A stack with no tag on it, or one whose name another marker
 * has claimed in the meantime, is given a fresh tag instead, because the rule that no two markers
 * share a name outranks the item's memory.
 */
public abstract class AbstractTaggableEntity extends Entity {

    private static final EntityDataAccessor<String> TAG_NAME = SynchedEntityData.defineId(
            AbstractTaggableEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> TAG_COLOR = SynchedEntityData.defineId(
            AbstractTaggableEntity.class, EntityDataSerializers.INT);

    /**
     * Where the tag is written, both in an entity's own save data and on the item that stands in for
     * it. One pair of keys for both, so the two copies of a tag cannot drift apart.
     */
    private static final String TAG_NAME_KEY = "TagName";
    private static final String TAG_COLOR_KEY = "TagColor";

    protected AbstractTaggableEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** Outcome of a rename, so the editor can say precisely what went wrong. */
    public enum TagUpdate {
        APPLIED,
        INVALID_NAME,
        NAME_TAKEN,
        UNKNOWN_COLOR
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TAG_NAME, "");
        builder.define(TAG_COLOR, TagRules.TAG_COLORS[0]);
    }

    // ------------------------------------------------------------------
    // tag
    // ------------------------------------------------------------------

    /** The label drawn above the entity, e.g. {@code #7} or a name the player chose. */
    public String getTagName() {
        return this.entityData.get(TAG_NAME);
    }

    /** The tag colour as {@code 0xRRGGBB}. */
    public int getTagColor() {
        return this.entityData.get(TAG_COLOR);
    }

    public boolean hasTag() {
        return !getTagName().isEmpty();
    }

    /** The billboarded label. */
    public Component getTagComponent() {
        return Component.literal(getTagName());
    }

    /**
     * A short word for what kind of marker this is — {@code cursor}, {@code drone} — for the lists
     * that hold more than one kind.
     *
     * <p>
     * Kept to a word on purpose: it is a right-aligned column in a narrow panel, not a sentence. A
     * list of one kind shows no column at all, so the word only ever appears where it earns its room.
     */
    public abstract Component kindLabel();

    protected void setTag(String name, int color) {
        this.entityData.set(TAG_NAME, name);
        this.entityData.set(TAG_COLOR, color);
    }

    /**
     * Gives a freshly placed taggable its first tag: the lowest auto-number nobody else wears, and a
     * random colour.
     */
    public void assignPlacementTag(ServerLevel level) {
        String name = TagRules.firstFreeName(candidate -> isTagNameTaken(level, candidate, this));
        setTag(name, TagRules.pickColor(level.random::nextInt, -1));
    }

    /**
     * Applies a tag the player typed in the editor.
     *
     * <p>
     * Everything is validated here rather than on the client: the name is normalized and checked for
     * length, checked against every other taggable for duplication, and the colour has to be one the
     * editor actually offers. A rejected update leaves the entity exactly as it was.
     */
    public TagUpdate applyCustomTag(ServerLevel level, String rawName, int color) {
        String name = TagRules.normalizeName(rawName);
        if (!TagRules.isValidName(name)) {
            return TagUpdate.INVALID_NAME;
        }
        if (isTagNameTaken(level, name, this)) {
            return TagUpdate.NAME_TAKEN;
        }
        if (!TagRules.isPaletteColor(color)) {
            return TagUpdate.UNKNOWN_COLOR;
        }
        setTag(name, color);
        return TagUpdate.APPLIED;
    }

    /**
     * Whether any <em>other</em> taggable already wears this name. Searches every dimension, because
     * a tag has to be unique across the whole save, not just the level the entity was placed in.
     */
    public static boolean isTagNameTaken(ServerLevel level, String name, @Nullable Entity self) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (ServerLevel other : level.getServer().getAllLevels()) {
            for (Entity entity : other.getAllEntities()) {
                if (entity == self || !(entity instanceof AbstractTaggableEntity taggable)) {
                    continue;
                }
                if (TagRules.sameName(taggable.getTagName(), name)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // interaction
    // ------------------------------------------------------------------

    /**
     * Right-click opens the tag editor on the client; the server just consumes the click, so the
     * held placement item does not place a second entity on top of this one.
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            TagEditor.open(this);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    /**
     * A hit breaks the taggable up into its item.
     *
     * <p>
     * Deliberately not damage: these are pieces of equipment with no health to take, and a player
     * swinging at one means "pick that up". The client reports the hit as accepted so the swing
     * animates, but only the server decides, so the item cannot be duplicated by a prediction that
     * later turns out to be wrong.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (!this.level().isClientSide) {
            playBrokenSound();
            this.spawnAtLocation(this.getPickResult());
            this.discard();
        }
        return true;
    }

    /** The sound a taggable makes as it comes apart and drops its item. */
    protected void playBrokenSound() {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.NEUTRAL, 0.8F, 1.0F);
    }

    // ------------------------------------------------------------------
    // the tag's round trip through the item
    // ------------------------------------------------------------------

    /** The item that deploys this kind of marker. */
    protected abstract Item taggedItem();

    /**
     * The item that stands in for this marker when it is picked up, with its tag written on it.
     *
     * <p>
     * Also what a middle-click pick-block hands over, which is how a player can copy a marker's tag
     * onto another one without walking to it.
     */
    @Override
    public final ItemStack getPickResult() {
        ItemStack stack = new ItemStack(taggedItem());
        if (hasTag()) {
            writeTagTo(stack);
        }
        return stack;
    }

    /**
     * The tag name an item remembers, exactly as it was written — normalization and validation are the
     * reader's job, since a stack can be edited and a tampered one must not be trusted.
     */
    public static String tagNameOf(ItemStack stack) {
        return dataOf(stack).getString(TAG_NAME_KEY);
    }

    /** The tag colour an item remembers, as {@code 0xRRGGBB}. */
    public static int tagColorOf(ItemStack stack) {
        return dataOf(stack).getInt(TAG_COLOR_KEY);
    }

    /**
     * Takes the tag the item remembers, so a marker that was picked up comes back wearing the name it
     * had.
     *
     * <p>
     * Validated here rather than trusted: the name is normalized and re-checked against the uniqueness
     * rule, because the item may have been in a chest while that name was claimed by another marker,
     * and the colour has to be one the editor offers. Anything that fails falls back to a fresh
     * automatic tag, which is the one outcome that can never produce two markers with the same name.
     *
     * @param player told when a name the marker was meant to get had been claimed in the meantime, so
     *               a marker that comes back as {@code #7} instead of {@code #3} is explained rather
     *               than merely surprising
     */
    public void adoptTagFrom(ServerLevel level, ItemStack stack, @Nullable Player player) {
        String remembered = TagRules.normalizeName(tagNameOf(stack));
        if (TagRules.isValidName(remembered) && !isTagNameTaken(level, remembered, this)) {
            int color = tagColorOf(stack);
            setTag(remembered, TagRules.isPaletteColor(color) ? color : TagRules.pickColor(level.random::nextInt, -1));
            return;
        }

        assignPlacementTag(level);
        // Only a name that was meant to be restored can be "taken"; a plain stack from the creative
        // menu has nothing to explain.
        if (player != null && TagRules.isValidName(remembered)) {
            player.displayClientMessage(
                    Component.translatable("message.aquanaut.tag.renamed", remembered, getTagName()), true);
        }
    }

    /** Writes this marker's tag onto the item that stands in for it. */
    private void writeTagTo(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(TAG_NAME_KEY, getTagName());
            tag.putInt(TAG_COLOR_KEY, getTagColor());
        });
    }

    /**
     * An item stack's custom tag. The read path runs once per hover and once per placement; the tag
     * holds a name and a colour, so copying it is cheaper than the bookkeeping needed to borrow it —
     * and {@code copyTag()} is the only accessor vanilla still supports.
     */
    private static CompoundTag dataOf(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    // ------------------------------------------------------------------
    // persistence
    // ------------------------------------------------------------------

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains(TAG_NAME_KEY)) {
            setTag(tag.getString(TAG_NAME_KEY), tag.getInt(TAG_COLOR_KEY));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString(TAG_NAME_KEY, getTagName());
        tag.putInt(TAG_COLOR_KEY, getTagColor());
    }
}
