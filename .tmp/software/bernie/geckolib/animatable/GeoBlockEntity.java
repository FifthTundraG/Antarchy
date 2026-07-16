package software.bernie.geckolib.animatable;

import net.minecraft.class_1937;
import net.minecraft.class_2586;
import net.minecraft.class_3218;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.GeckoLibConstants;
import software.bernie.geckolib.GeckoLibServices;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.constant.dataticket.SerializableDataTicket;
import software.bernie.geckolib.util.RenderUtil;

/**
 * The {@link GeoAnimatable} interface specific to {@link class_2586 BlockEntities}
 *
 * @see <a href="https://github.com/bernie-g/geckolib/wiki/Block-Animations">GeckoLib Wiki - Block Animations</a>
 */
public interface GeoBlockEntity extends GeoAnimatable {
	/**
	 * Get server-synced animation data via its relevant {@link SerializableDataTicket}.
	 * <p>
	 * Should only be used on the <u>client-side</u>
	 * <p>
	 * <b><u>DO NOT OVERRIDE</u></b>
	 *
	 * @param dataTicket The data ticket for the data to retrieve
	 * @return The synced data, or null if no data of that type has been synced
	 */
	@ApiStatus.NonExtendable
	@Nullable
	default <D> D getAnimData(SerializableDataTicket<D> dataTicket) {
		return getAnimatableInstanceCache().getManagerForId(0).getData(dataTicket);
	}

	/**
	 * Saves an arbitrary piece of data to this animatable's {@link AnimatableManager}
	 * <p>
	 * <b><u>DO NOT OVERRIDE</u></b>
	 *
	 * @param dataTicket The DataTicket to sync the data for
	 * @param data The data to sync
	 */
	@ApiStatus.NonExtendable
	default <D> void setAnimData(SerializableDataTicket<D> dataTicket, D data) {
		class_2586 blockEntity = (class_2586)this;
		class_1937 level = blockEntity.method_10997();

		if (level == null) {
			GeckoLibConstants.LOGGER.error("Attempting to set animation data for BlockEntity too early! Must wait until after the BlockEntity has been set in the world. (" + blockEntity.getClass().toString() + ")");

			return;
		}

		if (level.method_8608()) {
			getAnimatableInstanceCache().getManagerForId(0).setData(dataTicket, data);
		}
		else {
			GeckoLibServices.NETWORK.syncBlockEntityAnimData(blockEntity.method_11016(), dataTicket, data, (class_3218)level);
		}
	}

	/**
	 * Trigger an animation for this BlockEntity, based on the controller name and animation name
	 * <p>
	 * This can be fired from either the client or the server, but optimally you would call it from the server
	 * <p>
	 * <b><u>DO NOT OVERRIDE</u></b>
	 *
	 * @param controllerName The name of the controller name the animation belongs to, or null to do an inefficient lazy search
	 * @param animName The name of animation to trigger. This needs to have been registered with the controller via {@link AnimationController#triggerableAnim AnimationController.triggerableAnim}
	 */
	@ApiStatus.NonExtendable
	default void triggerAnim(@Nullable String controllerName, String animName) {
		class_2586 blockEntity = (class_2586)this;
		class_1937 level = blockEntity.method_10997();

		if (level == null) {
			GeckoLibConstants.LOGGER.error("Attempting to trigger an animation for a BlockEntity too early! Must wait until after the BlockEntity has been set in the world. (" + blockEntity.getClass().toString() + ")");

			return;
		}

		if (level.method_8608()) {
			if (controllerName != null) {
				getAnimatableInstanceCache().getManagerForId(0).tryTriggerAnimation(controllerName, animName);
			}
			else {
				getAnimatableInstanceCache().getManagerForId(0).tryTriggerAnimation(animName);
			}
		}
		else {
			GeckoLibServices.NETWORK.triggerBlockEntityAnim(blockEntity.method_11016(), controllerName, animName, (class_3218)level);
		}
	}

	/**
	 * Stop a previously triggered animation for this BlockEntity for the given controller name and animation name
	 * <p>
	 * This can be fired from either the client or the server, but optimally you would call it from the server
	 * <p>
	 * <b><u>DO NOT OVERRIDE</u></b>
	 *
	 * @param controllerName The name of the controller name the animation belongs to, or null to do an inefficient lazy search
	 * @param animName The name of the triggered animation to stop, or null to stop any currently playing triggered animation
	 */
	@ApiStatus.NonExtendable
	default void stopTriggeredAnim(@Nullable String controllerName, @Nullable String animName) {
		class_2586 blockEntity = (class_2586)this;
		class_1937 level = blockEntity.method_10997();

		if (level == null) {
			GeckoLibConstants.LOGGER.error("Attempting to stop a triggered animation for a BlockEntity too early! Must wait until after the BlockEntity has been set in the world. (" + blockEntity.getClass().toString() + ")");

			return;
		}

		if (level.method_8608()) {
			AnimatableManager<GeoAnimatable> animatableManager = getAnimatableInstanceCache().getManagerForId(0);

			if (controllerName != null) {
				animatableManager.stopTriggeredAnimation(controllerName, animName);
			}
			else {
				animatableManager.stopTriggeredAnimation(animName);
			}
		}
		else {
			GeckoLibServices.NETWORK.stopTriggeredBlockEntityAnim(blockEntity.method_11016(), (class_3218)level, controllerName, animName);
		}
	}

	/**
	 * Returns the current age/tick of the animatable instance
	 * <p>
	 * By default this is just the animatable's age in ticks, but this method allows for non-ticking custom animatables to provide their own values
	 *
	 * @param blockEntity The BlockEntity representing this animatable
	 * @return The current tick/age of the animatable, for animation purposes
	 */
	@Override
	default double getTick(Object blockEntity) {
		return RenderUtil.getCurrentTick();
	}
}
