# Minute Rush

Minute Rush is a Fabric 26.3 minigame. Everyone receives the same randomly selected task, and the first player to complete it wins the round. The first player to reach 10 round wins takes the match. A round times out after 60 seconds and immediately starts a new task.

## Running

1. Install Java 25.
2. Run `./gradlew build`.
3. Use the generated Fabric server/client run configuration.

Operators can use:

- `/minute-rush start` - start a match (requires at least two players)
- `/minute-rush solo-start` - start an isolated one-player testing match in a singleplayer world
- `/minute-rush solo-exit` - stop singleplayer testing and reset its score
- `/minute-rush status` - inspect match state
- `/minute-rush reset` - reset the match and scores
- `/minute-rush config score <value>` - set the winning score
- `/minute-rush config duration <seconds>` - set the round timeout

The initial task set demonstrates placing a stone block at spawn and jumping once. Settings default to 10 points and 60 seconds and can be changed with the config commands.

Singleplayer testing is explicit and does not change normal multiplayer rules. Run `/minute-rush solo-start` as the player, test the tasks normally, and use `/minute-rush solo-exit` to stop. A regular `/minute-rush start` still requires at least two players.
