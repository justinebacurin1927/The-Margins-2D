package com.margins.fx;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.margins.asset.Assets;

import java.util.ArrayList;
import java.util.Iterator;

public class ParticleSystem {
    private final ArrayList<Particle> particles = new ArrayList<>();
    private float fireflySpawn;

    public void update(float delta) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.life -= delta;
            if (p.life <= 0) { it.remove(); continue; }
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.vx *= 0.96f;
            p.vy *= 0.96f;
            if (p.type == 1) p.vy -= 40f * delta;
            if (p.type == 2) {
                p.vx += MathUtils.random(-2f, 2f) * delta;
                p.vy += MathUtils.random(-2f, 2f) * delta;
                float maxV = 8f;
                p.vx = MathUtils.clamp(p.vx, -maxV, maxV);
                p.vy = MathUtils.clamp(p.vy, -maxV, maxV);
            }
        }
    }

    public void render(SpriteBatch batch) {
        for (Particle p : particles) {
            float t = p.life / p.maxLife;
            float alpha = 1f;
            if (t < 0.2f) alpha = t / 0.2f;
            if (t > 0.8f) alpha = (1f - t) / 0.2f;

            if (p.type == 2) {
                alpha *= 0.4f + 0.6f * (float) Math.sin(p.life * 4f + p.x * 0.1f);
            }

            float s = p.size;
            if (p.type == 2) s *= 1.5f + 0.5f * (float) Math.sin(p.life * 3f);

            batch.setColor(p.r, p.g, p.b, alpha);
            batch.draw(Assets.particleTex, p.x - s / 2f, p.y - s / 2f, s, s);
            if (p.type == 2) {
                batch.setColor(p.r, p.g, p.b, alpha * 0.2f);
                batch.draw(Assets.particleTex, p.x - s, p.y - s, s * 2, s * 2);
            }
            batch.setColor(1, 1, 1, 1);
        }
    }

    public void dust(float sx, float sy) {
        for (int i = 0; i < 5; i++) {
            particles.add(new Particle(
                sx + MathUtils.random(-6f, 6f), sy + MathUtils.random(-2f, 2f),
                MathUtils.random(-12f, 12f), MathUtils.random(-6f, -2f),
                0.4f + MathUtils.random(0.3f),
                4f + MathUtils.random(3f),
                0.80f, 0.72f, 0.58f, 0
            ));
        }
    }

    public void sparkle(float sx, float sy) {
        for (int i = 0; i < 8; i++) {
            float angle = MathUtils.random(0f, MathUtils.PI2);
            float speed = MathUtils.random(15f, 35f);
            particles.add(new Particle(
                sx + MathUtils.random(-6f, 6f), sy + MathUtils.random(-6f, 6f),
                (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed,
                0.5f + MathUtils.random(0.3f),
                2f + MathUtils.random(2f),
                1f, 0.85f, 0.35f, 1
            ));
        }
    }

    public void manageFireflies(float vw, float vh, float darkness, float delta) {
        if (darkness < 0.12f) { fireflySpawn = 0; return; }
        int count = 0;
        for (Particle p : particles) if (p.type == 2) count++;
        int target = (int) ((darkness - 0.12f) / 0.63f * 12f);
        target = MathUtils.clamp(target, 0, 12);

        fireflySpawn += delta;
        while (count < target && fireflySpawn > 0.15f) {
            particles.add(new Particle(
                MathUtils.random(40f, vw - 40f), MathUtils.random(40f, vh * 0.6f),
                MathUtils.random(-3f, 3f), MathUtils.random(-3f, 3f),
                2f + MathUtils.random(3f),
                2.5f + MathUtils.random(2f),
                0.5f, 0.85f, 0.35f, 2
            ));
            fireflySpawn = 0;
            count++;
        }
    }

    private static class Particle {
        float x, y, vx, vy, life, maxLife, size, r, g, b;
        int type;

        Particle(float x, float y, float vx, float vy, float life, float size, float r, float g, float b, int type) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.life = life; this.maxLife = life; this.size = size;
            this.r = r; this.g = g; this.b = b; this.type = type;
        }
    }
}
