package com.margins;

import com.margins.rogue.RogueTile;
import com.margins.rogue.RogueTileMap;
import com.margins.rogue.world.WorldSpine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarginScreenGroundMaterialTest {

    private static RogueTileMap clearing() {
        RogueTileMap map = new RogueTileMap(40, 30);
        map.fill(RogueTile.FLOOR);
        return map;
    }

    @Test
    void copperRoadHasCobbledCenterAndWornShoulders() {
        RogueTileMap map = clearing();
        WorldSpine spine = new WorldSpine(map.getWidth(), map.getHeight());
        int center = MarginScreen.groundMaterialCell(map, 20, spine.roadY());
        int shoulder = MarginScreen.groundMaterialCell(map, 20, spine.roadY() + 1);

        assertTrue(center >= 12 && center <= 14, "road center uses the cobblestone family");
        assertTrue(shoulder >= 7 && shoulder <= 9, "road shoulder uses gravel/packed dirt");
    }

    @Test
    void waterCreatesMudThenDampGrass() {
        RogueTileMap map = clearing();
        map.setTile(12, 6, RogueTile.POND);

        int mud = MarginScreen.groundMaterialCell(map, 11, 6);
        int damp = MarginScreen.groundMaterialCell(map, 10, 6);
        assertTrue(mud == 10 || mud == 11, "the immediate pond bank is mud");
        assertEquals(3, damp, "the second bank row is damp grass");
    }

    @Test
    void treesSeatIntoAContinuousLeafAndRootVerge() {
        RogueTileMap map = clearing();
        map.setTile(10, 10, RogueTile.WALL);

        int verge = MarginScreen.groundMaterialCell(map, 11, 10);
        assertTrue(verge >= 4 && verge <= 6, "a forest edge uses leaf litter or roots");
    }

    @Test
    void workedWellGetsAStoneApron() {
        RogueTileMap map = clearing();
        map.setTile(7, 7, RogueTile.WELL);

        int apron = MarginScreen.groundMaterialCell(map, 8, 7);
        assertTrue(apron >= 12 && apron <= 14, "the well apron uses worked stone");
    }

    @Test
    void materialChoiceIsStableForTheSameMapPosition() {
        RogueTileMap map = clearing();
        int expected = MarginScreen.groundMaterialCell(map, 32, 24);
        for (int i = 0; i < 100; i++) {
            assertEquals(expected, MarginScreen.groundMaterialCell(map, 32, 24));
        }
    }
}
