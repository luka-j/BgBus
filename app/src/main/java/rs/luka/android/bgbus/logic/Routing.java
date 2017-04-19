package rs.luka.android.bgbus.logic;

import rs.luka.android.bgbus.model.FullPath;

/**
 * Specifies class which can be used to find optimal route. Requires only one method, {@link #findRoute()} which
 * returns {@link rs.luka.android.bgbus.logic.Routing.Path} representing a solution.
 * Created by luka on 14.10.15.
 */
public interface Routing {
    /**
     *
     * @return Path between given points
     */
    Path findRoute();

    /**
     * Represents possible path. Requires two methods, one checking whether this Path satisfies conditions
     * (i. e. last node is the required goal) and one returning that solution in a friendlier format
     * ({@link FullPath}).
     */
    interface Path {
        boolean isGoal();
        FullPath reconstruct();
    }
}
