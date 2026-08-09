package com.margins;

import org.junit.jupiter.api.Test;

import com.margins.rogue.Weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarginScreenLightingTest {

    @Test
    void clearDayDoesNotDimTheWorld() {
        assertEquals(0f, MarginScreen.ambientDarkness(true, false), 0f);
    }

    @Test
    void fogAndNightStackWithoutBecomingPitchBlack() {
        float foggyDay = MarginScreen.ambientDarkness(true, true);
        float clearNight = MarginScreen.ambientDarkness(false, false);
        float foggyNight = MarginScreen.ambientDarkness(false, true);

        assertTrue(foggyDay > 0f, "daytime fog still has atmosphere");
        assertTrue(clearNight > foggyDay, "night is darker than daytime fog");
        assertTrue(foggyNight > clearNight, "fog deepens the night");
        assertTrue(foggyNight <= 0.50f, "the darkest combination remains playable");
    }

    @Test
    void torchGlowIsRestrainedByDayAndStrengthensInDarkness() {
        float day = MarginScreen.torchGlowAlpha(0f);
        float night = MarginScreen.torchGlowAlpha(0.36f);
        float foggyNight = MarginScreen.torchGlowAlpha(0.50f);

        assertTrue(day < 0.15f, "daylight never gets a giant orange light disc");
        assertTrue(night > day, "the same torch becomes useful at night");
        assertTrue(foggyNight >= night, "deeper darkness never weakens the torch glow");
        assertTrue(foggyNight <= 0.48f, "additive light remains controlled");
    }

    @Test
    void weatherPresentationHasDistinctReadableStrengths() {
        assertEquals(0f, MarginScreen.rainIntensity(Weather.CLEAR), 0f);
        assertTrue(MarginScreen.rainIntensity(Weather.RAIN) > 0f);
        assertTrue(MarginScreen.rainIntensity(Weather.STORM)
                > MarginScreen.rainIntensity(Weather.RAIN));
        assertTrue(MarginScreen.weatherDarkness(true, Weather.STORM)
                > MarginScreen.weatherDarkness(true, Weather.RAIN));
        assertTrue(MarginScreen.weatherDarkness(true, Weather.FOG)
                > MarginScreen.weatherDarkness(true, Weather.RAIN));
    }

    @Test
    void weatherFramesLoopAndLightningRemainsRestrained() {
        assertEquals(0, MarginScreen.loopingWeatherFrame(0f, 0.1f, 2));
        assertEquals(1, MarginScreen.loopingWeatherFrame(0.1f, 0.1f, 2));
        assertEquals(0, MarginScreen.loopingWeatherFrame(0.2f, 0.1f, 2));
        assertTrue(MarginScreen.stormFlashAlpha(0f) > 0f);
        assertTrue(MarginScreen.stormFlashAlpha(0f) <= 0.15f);
        assertEquals(0f, MarginScreen.stormFlashAlpha(1f), 0f);
    }

    @Test
    void stormDropsUseIndependentNormalizedCycles() {
        float first = MarginScreen.stormDropPhase(3.25f, 0x12345678, 456f);
        float second = MarginScreen.stormDropPhase(3.25f, 0x76543210, 456f);

        assertTrue(first >= 0f && first < 1f);
        assertTrue(second >= 0f && second < 1f);
        assertTrue(Math.abs(first - second) > 0.0001f);
        assertEquals(0f, MarginScreen.stormDropPhase(3f, 42, 0f), 0f);
    }

    @Test
    void fogSpecksFadeAtBothEndsAndBloomWhileAlive() {
        assertEquals(0f, MarginScreen.fogSpeckAlpha(0f), 0.0001f);
        assertEquals(0f, MarginScreen.fogSpeckAlpha(1f), 0.0001f);
        assertTrue(MarginScreen.fogSpeckAlpha(0.5f) > 0.9f);
        assertTrue(MarginScreen.fogSpeckScale(0.75f) > MarginScreen.fogSpeckScale(0.1f));
    }

    @Test
    void fogSpeckPhaseLoopsWithoutLeavingNormalizedRange() {
        float first = MarginScreen.fogSpeckPhase(0f, 0x1234abcd);
        float later = MarginScreen.fogSpeckPhase(97.25f, 0x1234abcd);

        assertTrue(first >= 0f && first < 1f);
        assertTrue(later >= 0f && later < 1f);
    }

    @Test
    void coldSnapParticlesAreLocalShortLivedBlooms() {
        assertEquals(0f, MarginScreen.coldSnapParticleAlpha(0f), 0.0001f);
        assertEquals(0f, MarginScreen.coldSnapParticleAlpha(1f), 0.0001f);
        assertTrue(MarginScreen.coldSnapParticleAlpha(0.5f) > 0.9f);

        float first = MarginScreen.coldSnapParticlePhase(0f, 0x2468ace);
        float later = MarginScreen.coldSnapParticlePhase(81.5f, 0x2468ace);
        assertTrue(first >= 0f && first < 1f);
        assertTrue(later >= 0f && later < 1f);
    }
}
