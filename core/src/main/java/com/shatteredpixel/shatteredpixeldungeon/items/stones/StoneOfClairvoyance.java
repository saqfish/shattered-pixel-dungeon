/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2018 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.items.stones;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CheckedCell;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ShadowCaster;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class StoneOfClairvoyance extends Runestone {
	
	private static final int DIST = 8;
	
	{
		image = ItemSpriteSheet.STONE_RAIDO;
	}
	
	@Override
	protected void activate(final int cell) {
		boolean[] FOV = new boolean[Dungeon.level.length()];
		Point c = Dungeon.level.cellToPoint(cell);
		ShadowCaster.castShadow(c.x, c.y, FOV, DIST);
		
		int sX = Math.max(0, c.x - DIST);
		int eX = Math.min(Dungeon.level.width()-1, c.x + DIST);
		
		int sY = Math.max(0, c.y - DIST);
		int eY = Math.min(Dungeon.level.height()-1, c.y + DIST);
		
		ArrayList<Trap> disarmCandidates = new ArrayList<>();
		
		boolean noticed = false;
		for (int y = sY; y <= eY; y++){
			int curr = y*Dungeon.level.width() + sX;
			for ( int x = sX; x <= eX; x++){
				
				if (FOV[curr]){
					curUser.sprite.parent.addToBack( new CheckedCell( curr ) );
					
					if (Dungeon.level.secret[curr]) {
						Dungeon.level.discover(curr);
						
						if (Dungeon.level.heroFOV[curr]) {
							GameScene.discoverTile(curr, Dungeon.level.map[curr]);
							ScrollOfMagicMapping.discover(curr);
							noticed = true;
						}
					}
					
					if (Dungeon.level.traps.get(curr) != null){
						disarmCandidates.add(Dungeon.level.traps.get(curr));
					}
					
				}
				curr++;
			}
		}
		
		Collections.sort(disarmCandidates, new Comparator<Trap>() {
			@Override
			public int compare(Trap o1, Trap o2) {
				float diff = Dungeon.level.trueDistance(cell, o1.pos) - Dungeon.level.trueDistance(cell, o2.pos);
				if (diff < 0){
					return -1;
				} else if (diff == 0){
					return Random.Int(2) == 0 ? -1 : 1;
				} else {
					return 1;
				}
			}
		});
		
		//disarms at most two traps
		if (disarmCandidates.size() > 0){
			disarmCandidates.get(0).disarm();
			CellEmitter.get(disarmCandidates.get(0).pos).burst(Speck.factory(Speck.STEAM), 6);
			if (disarmCandidates.size() > 1){
				disarmCandidates.get(1).disarm();
				CellEmitter.get(disarmCandidates.get(1).pos).burst(Speck.factory(Speck.STEAM), 6);
			}
		}
		
		if (noticed) {
			Sample.INSTANCE.play( Assets.SND_SECRET );
		}
		
		Sample.INSTANCE.play( Assets.SND_READ );
	}
	
}