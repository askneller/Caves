/*
 * Copyright 2019 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.caves;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.Component;
import org.terasology.math.Region3i;
import org.terasology.rendering.nui.properties.Range;
import org.terasology.utilities.procedural.Noise;
import org.terasology.world.generation.ConfigurableFacetProvider;
import org.terasology.world.generation.Facet;
import org.terasology.world.generation.FacetProviderPlugin;
import org.terasology.world.generation.GeneratingRegion;
import org.terasology.world.generation.Produces;
import org.terasology.world.generation.Requires;
import org.terasology.world.generator.plugin.RegisterPlugin;

@RegisterPlugin
@Produces(CaveFloorFacet.class)
@Requires(@Facet(CaveFacet.class))
public class CaveFloorProvider implements ConfigurableFacetProvider, FacetProviderPlugin {
    private static final Logger logger = LoggerFactory.getLogger(CaveFloorProvider.class);
    private Noise densityNoiseGen;
    private boolean hasPrinted = false;

    private CaveFloorConfiguration configuration = new CaveFloorConfiguration();

    /**
     * A value to indicate that no cave floor was found (either because no cave exists in this region,
     * or because the cave extends below this region, in which case the floor will be provided in the
     * below region's CaveFloorFacet).
     */
    private static final float NO_CAVE = -999999999.0f;

    /**
     * If no region calls region.getFacet(<the provided facet class>) then this method 'process' will never be called
     * @param region
     */
    @Override
    public void process(GeneratingRegion region) {
        CaveFacet caveFacet = region.getRegionFacet(CaveFacet.class);
        CaveFloorFacet floorFacet =
                new CaveFloorFacet(region.getRegion(), region.getBorderForFacet(CaveFloorFacet.class));

        Region3i worldRegion = caveFacet.getWorldRegion();
        for (int x = worldRegion.minX(); x <= worldRegion.maxX(); ++x) {
            for (int z = worldRegion.minZ(); z <= worldRegion.maxZ(); ++z) {
                // The first false we encounter may be the ceiling.
                // So the floor is indicated by the first false after the we encounter a true
                boolean foundTrue = false;
                int y;
                for (y = worldRegion.maxY(); y >= worldRegion.minY(); --y) {
                    boolean cave = caveFacet.getWorld(x, y, z);
                    if (!foundTrue && cave) {
                        foundTrue = true;
                    } else if (foundTrue && !cave) {
                        floorFacet.setWorld(x, z, y + 1);
                        break;
                    }
                }
                if (y < worldRegion.minY()) {
                    // Didn't find any cave
                    floorFacet.setWorld(x, z, NO_CAVE);
                }
            }
        }
        region.setRegionFacet(CaveFloorFacet.class, floorFacet);
    }

    @Override
    public String getConfigurationName() {
        return "Cave Floor";
    }

    @Override
    public Component getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(Component configuration) {
        this.configuration = (CaveFloorConfiguration) configuration;
    }

    private static class CaveFloorConfiguration implements Component {
        @Range(min = 0, max = 1.0f, increment = 0.05f, precision = 2, description = "Blah")
        private float density = 0.15f;

    }

}
