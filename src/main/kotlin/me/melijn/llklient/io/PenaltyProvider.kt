package me.melijn.llklient.io

interface PenaltyProvider {
    /**
     * This method allows for adding custom penalties to [nodes][LavalinkSocket], making it possible to
     * change how the node selection system works on a per-guild per-node basis.
     * By using the provided [Penalties][LavalinkLoadBalancer.Penalties] class you can fetch default penalties like CPU or Players.
     *
     * @param penalties - Instance of [Penalties][LavalinkLoadBalancer.Penalties] class representing the node to check.
     * @return total penalty to add to this node.
     */
    fun getPenalty(penalties: LavalinkLoadBalancer.Penalties): Int
}