package me.suff.mc.regen.level.structures;

import net.minecraft.core.Registry;
import net.minecraft.world.level.levelgen.feature.StructurePieceType;

import java.util.Locale;

public class RPiece {

    public static StructurePieceType setPieceId(StructurePieceType p_67164_, String p_67165_) {
        return Registry.register(Registry.STRUCTURE_PIECE, p_67165_.toLowerCase(Locale.ROOT), p_67164_);
    }

    public static StructurePieceType TIMELORD_HUT = setPieceId(RegenHuts.GraveyardPiece::new, "regen:timelord_hut");


}