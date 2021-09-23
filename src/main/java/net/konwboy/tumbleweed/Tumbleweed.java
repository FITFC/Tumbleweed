package net.konwboy.tumbleweed;

import net.konwboy.tumbleweed.client.ModelTumbleweed;
import net.konwboy.tumbleweed.client.RenderTumbleweed;
import net.konwboy.tumbleweed.common.EntityTumbleweed;
import net.konwboy.tumbleweed.common.TumbleweedConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ObjectHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Tumbleweed.MOD_ID)
@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
public class Tumbleweed {

	public static final String MOD_ID = "tumbleweed";
	public static final String MOD_NAME = "Tumbleweed";

	public static Logger logger = LogManager.getLogger(MOD_ID);

	public static final ResourceLocation TUMBLEWEED_ENTITY = new ResourceLocation(MOD_ID, "tumbleweed");

	@ObjectHolder("tumbleweed:tumbleweed")
	public static EntityType<EntityTumbleweed> TUMBLEWEED;

	public Tumbleweed() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TumbleweedConfig.SPEC);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void commonSetup(FMLCommonSetupEvent event) {
		// All items/blocks are now registered
		TumbleweedConfig.canLoad = true;
		TumbleweedConfig.load();
	}

	@SubscribeEvent
	public static void layerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(RenderTumbleweed.MAIN_LAYER, ModelTumbleweed::createLayer);
	}

	@SubscribeEvent
	public static void entityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(TUMBLEWEED, RenderTumbleweed::new);
	}

	@SubscribeEvent
	public static void registerEntities(RegistryEvent.Register<EntityType<?>> event) {
		EntityType<EntityTumbleweed> tumbleweed = EntityType.Builder
				.of(EntityTumbleweed::new, MobCategory.MISC)
				.setTrackingRange(128)
				.setUpdateInterval(30)
				.setShouldReceiveVelocityUpdates(true)
				.setCustomClientFactory((p, w) -> new EntityTumbleweed(TUMBLEWEED, w))
				.build("tumbleweed");

		tumbleweed.setRegistryName(TUMBLEWEED_ENTITY);
		event.getRegistry().register(tumbleweed);
	}
}