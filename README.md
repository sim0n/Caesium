# Caesium
Caesium is a powerful Java bytecode obfuscator written by [sim0n](https://github.com/sim0n) for fun, and released for the public.

![Image of Caesium UI](https://i.imgur.com/drrn9ib.png)

### Currently available mutators
* Class Folder (Turns classes into folders)
* Control Flow
* Crasher (Will crash almost every GUI based RE tool)
* Local Variable
* Line Number
* Number
* Polymorph
* Reference (invokedynamics)
* String
* Trim (Currently only trims math functions)

## Notes
You have to add every dependency your jar relies on.
Caesium is very optimised and the performance loss shouldn't be more than 5-10% (unless you're using reference mutation)

## Usage
- Run the jar.
- Select mutators in the mutators tab.
- Hit mutate. Done!

## Community 
If you want to join the discord for Caesium to talk, ask questions or anything then feel free to join [the discord](https://discord.gg/kxC2FYMfNZ)

## Special thanks to
![yourkit logo](https://www.yourkit.com/images/yklogo.png)

[YourKit](https://www.yourkit.com/) is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>, <a href="https://www.yourkit.com/.net/profiler/">YourKit .NET Profiler</a>, and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>. They support open source projects with their fully featured application profilers. It's used to ensure that this project will be as fast as possible.