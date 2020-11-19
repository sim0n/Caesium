# Caesium
Caesium is a Java bytecode obfuscator that I wrote for fun and figured I'd make public

Currently supported mutators
* Class Folder (Turns classes into folders)
* Control Flow
* Crasher (Will crash almost every GUI based RE tool)
* Local Variable
* Number
* Reference (invokedynamics)
* String

## Usage
`-H` Provides a help menu

`-I [input]` The input jar

examples: 

`java -jar caesium.jar -H`

`java -jar caesium.jar -I test.jar`