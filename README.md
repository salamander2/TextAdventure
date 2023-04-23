# Text Adventure Game

_By Michael Harwood, 2023 (The first version was written in 2014, but then it sat incomplete for many years)_

### The purpose is to have a complex game that resembles Zork, which can handle fairly natural language input.

There are some other, better but more complex, ways of handling language.  This suffices for a high school project.

This project requires knowledge of how objects, arraylists, and hashmaps work. There is some string manipulation too, but not a lot.  

Some elegant features of the way that this game is designed:

1. It is incredibly easy to add new locations (aka "rooms")
2. The rooms can connect in any way to other rooms - bendy paths, one way paths, ...
3. It is incredibly easy to add new items
4. It is incredibly easy to add new commands.  
However: all of the commands end up in one large switch statement, eventually it might be better to find some other data structure.
5. Rooms and items can have custom methods that run when they are accessed. This allows much more fine tuning of the game. 
