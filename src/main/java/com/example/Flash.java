package com.example;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Flash implements ModInitializer {

	public static final String MOD_ID = "flash";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * Crea un Identifier usando el MOD_ID como namespace.
	 * Ejemplo: Flash.id("mi_objeto") -> "flash:mi_objeto"
	 */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		LOGGER.info("¡El mod Flash se ha inicializado correctamente!");
	}
}
