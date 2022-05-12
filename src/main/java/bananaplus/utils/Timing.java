package bananaplus.utils;

public enum Timing {
    Normal,
    Iterations
}

// Just paste these in for whatever module you are making it for
/*
   private final Setting<Timing> timing = sgGeneral.add(new EnumSetting.Builder<Timing>()
            .name("timing")
            .description("The mode to use for delays")
            .defaultValue(Timing.Iterations)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("place-delay")
            .description("How many ticks between block placements.")
            .defaultValue(1)
            .visible(() -> timing.get() == Timing.Normal)
            .build()
    );

    private Setting<Integer> bpt = sgGeneral.add(new IntSetting.Builder()
            .name("blocks-per-tick")
            .description("How many blocks to place per tick")
            .defaultValue(5)
            .range(1,10)
            .sliderRange(1,10)
            .visible(() -> timing.get() == Timing.Iterations)
            .build()
    );

    private Setting<Integer> iterationDelay = sgGeneral.add(new IntSetting.Builder()
            .name("iteration-delay")
            .description("How many ticks to wait for the next iteration")
            .defaultValue(9)
            .range(1,20)
            .sliderRange(1,20)
            .visible(() -> timing.get() == Timing.Iterations)
            .build()
    );
 */

// Put these into the fields
// private int blockPlaced, iterationTimer;

// Put these into the event handler tick even pre
/*
        if (iterationTimer < iterationDelay.get()) iterationTimer++;
        else {
            iterationTimer = 0;
            blockPlaced = 0;
        }
*/